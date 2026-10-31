package dev.jefe.makeronline

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.Request
import java.net.URI
import java.util.IdentityHashMap
import java.util.function.Consumer

/**
 * Sabe cuándo han terminado de verdad las subidas de archivos del formulario de MakerOnline.
 *
 * Comprobado en la web el 31/10/2026 (Uploader --diagnostico, ver el documento de contexto):
 *  - cada archivo se sube con POST /api/file/upload y, si es .3mf, después llama a
 *    POST /api/file/parse-info;
 *  - cada .3mf es una fila .file-item que muestra "N%" y "Cancel" mientras sube y mientras
 *    se procesa; "Cancel" desaparece cuando ha terminado del todo;
 *  - el "100 %" NO significa terminado.
 *
 * Una subida ha terminado cuando no queda ninguna de esas peticiones en marcha, ninguna fila
 * con "Cancel" y pasan unos segundos sin que empiece otra.
 *
 * Hay que crearlo ANTES de elegir los archivos, para que vea todas las peticiones.
 */
class VigilanteSubidas(
    private val page: Page,
    private val progreso: Progreso,
) : AutoCloseable {

    private val enMarcha = IdentityHashMap<Request, Long>()
    private val fallos = ArrayList<String>()
    /** Última vez que algo avanzó: una petición empezó o acabó, o cambió un porcentaje. */
    private var ultimoAvance = System.currentTimeMillis()

    private val alEmpezar = Consumer<Request> { r ->
        if (esDeSubida(r)) {
            enMarcha[r] = System.currentTimeMillis()
            ultimoAvance = System.currentTimeMillis()
        }
    }
    private val alTerminar = Consumer<Request> { r -> termina(r, null) }
    private val alFallar = Consumer<Request> { r -> termina(r, r.failure() ?: "se ha cortado") }

    init {
        page.onRequest(alEmpezar)
        page.onRequestFinished(alTerminar)
        page.onRequestFailed(alFallar)
    }

    /**
     * Espera a que terminen todas las subidas que haya en marcha.
     *
     * @param que para los mensajes: "la portada", "los archivos"...
     * @throws IllegalStateException si una subida falla, si la web muestra un error, si
     *         lleva [SIN_AVANCE_MS] sin avanzar o si se pasa de [TOPE_MS]
     */
    fun esperar(que: String) {
        val inicio = System.currentTimeMillis()
        ultimoAvance = inicio
        var calmaDesde = 0L
        var textoAnterior = ""
        var textoAvisado = ""
        var ultimoAviso = 0L
        var avisado = false

        while (true) {
            val ahora = System.currentTimeMillis()

            if (fallos.isNotEmpty()) {
                throw IllegalStateException("Ha fallado la subida de $que: ${fallos.first()}. No se guarda el borrador.")
            }
            val errorWeb = mensajeDeError()
            if (errorWeb != null) {
                throw IllegalStateException("MakerOnline dice: $errorWeb (subiendo $que). No se guarda el borrador.")
            }

            val filas = filasSubiendo()
            val texto = filas.allInnerTexts().joinToString(" | ") { limpia(it) }
            if (texto != textoAnterior) {
                ultimoAvance = ahora
                textoAnterior = texto
            }
            val pendientes = enMarcha.size

            if (pendientes == 0 && filas.count() == 0) {
                if (calmaDesde == 0L) calmaDesde = ahora
                if (ahora - calmaDesde >= CALMA_MS) {
                    if (avisado) progreso.paso("Subida de $que terminada (${(ahora - inicio) / 1000} s)")
                    return
                }
            } else {
                calmaDesde = 0L
                if (!avisado) {
                    progreso.paso("Esperando a que termine la subida de $que")
                    avisado = true
                }
                // Por dónde va: cada 10 s si ha cambiado, cada 30 s si sigue igual
                val toca = if (texto != textoAvisado) AVISO_MS else 3 * AVISO_MS
                if (texto.isNotEmpty() && ahora - ultimoAviso >= toca) {
                    progreso.info("   … $texto")
                    textoAvisado = texto
                    ultimoAviso = ahora
                }
            }

            if (ahora - ultimoAvance > SIN_AVANCE_MS) {
                throw IllegalStateException("La subida de $que lleva ${SIN_AVANCE_MS / 60_000} minutos sin avanzar" +
                    " (¿conexión caída?). No se guarda el borrador.")
            }
            if (ahora - inicio > TOPE_MS) {
                throw IllegalStateException("La subida de $que ha tardado más de ${TOPE_MS / 60_000} minutos." +
                    " No se guarda el borrador.")
            }
            page.waitForTimeout(500.0)
        }
    }

    override fun close() {
        page.offRequest(alEmpezar)
        page.offRequestFinished(alTerminar)
        page.offRequestFailed(alFallar)
    }

    // ------------------------------------------------------------ red

    private fun termina(r: Request, fallo: String?) {
        if (enMarcha.remove(r) == null) return
        ultimoAvance = System.currentTimeMillis()
        val que = if (ruta(r.url()).endsWith(PARSE_INFO)) "al procesar el .3mf" else "al subir un archivo"
        if (fallo != null) {
            fallos += "$que: $fallo"
            return
        }
        val respuesta = try { r.response() } catch (_: Exception) { null }
        if (respuesta == null) return
        if (respuesta.status() >= 400) {
            fallos += "$que: la web ha respondido HTTP ${respuesta.status()}"
            return
        }
        // Si la respuesta es un JSON con "code" (como el resto de la API), code distinto de 0 es un error
        try {
            val j = JSON.readTree(respuesta.text())
            val code = j?.get("code")
            if (code != null && code.isNumber && code.asInt() != 0) {
                fallos += "$que: ${j.path("message").asText("código ${code.asInt()}")}"
            }
        } catch (_: Exception) {
            // no es JSON: con el código HTTP basta
        }
    }

    private fun esDeSubida(r: Request): Boolean {
        val url = r.url()
        if (!host(url).endsWith("makeronline.com")) return false
        val ruta = ruta(url)
        return ruta.endsWith(UPLOAD) || ruta.endsWith(PARSE_INFO)
    }

    // ------------------------------------------------------------ página

    /** Filas de archivo (.file-item) que todavía muestran "Cancel": están subiendo o procesándose. */
    private fun filasSubiendo(): Locator = page.locator(".file-item").filter(
        Locator.FilterOptions()
            .setVisible(true)
            .setHas(page.getByText("Cancel", Page.GetByTextOptions().setExact(true)))
    )

    private fun mensajeDeError(): String? {
        val aviso = page.locator(".el-message--error").filter(Locator.FilterOptions().setVisible(true))
        return if (aviso.count() > 0) limpia(aviso.first().innerText()) else null
    }

    private companion object {
        const val UPLOAD = "/api/file/upload"
        const val PARSE_INFO = "/api/file/parse-info"
        /** Tiempo sin peticiones ni filas pendientes para darlo por terminado. */
        const val CALMA_MS = 2_000L
        /** Si en este tiempo no avanza nada (ni porcentaje ni peticiones), se da por atascada. */
        const val SIN_AVANCE_MS = 2 * 60_000L
        /** Tope absoluto, por si avanza tan despacio que no acaba nunca. */
        const val TOPE_MS = 30 * 60_000L
        /** Cada cuánto se cuenta por dónde va la subida. */
        const val AVISO_MS = 10_000L

        val JSON = ObjectMapper()

        fun host(url: String): String = try { URI(url).host ?: "" } catch (_: Exception) { "" }
        fun ruta(url: String): String = try { URI(url).path ?: "" } catch (_: Exception) { "" }

        /** "karambit.3mf\n11.86MB\n45%\nCancel" → "karambit.3mf 11.86MB 45%" */
        fun limpia(texto: String): String =
            texto.replace(Regex("""\bCancel\b"""), "").replace(Regex("""\s+"""), " ").trim()
    }
}
