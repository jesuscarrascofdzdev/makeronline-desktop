package dev.jefe.makeronline

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.playwright.Page
import com.microsoft.playwright.Request
import java.io.BufferedWriter
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.IdentityHashMap
import java.util.function.Consumer

/**
 * TEMPORAL (Uploader --diagnostico). Graba lo que pasa en la página mientras se suben
 * los archivos y al guardar el borrador, para saber cómo detectar que una subida ha
 * terminado sin inventarse selectores. Cuando esté resuelto, se borra.
 *
 * Qué apunta, en un .txt:
 *  - cada vez que cambian, el texto y el HTML de las zonas de subida, de las filas de
 *    cada archivo, de los botones de guardar y de los mensajes de la web;
 *  - las peticiones de red de la página: método, dirección SIN parámetros, código y tiempo;
 *  - de las respuestas JSON de MakerOnline, solo su forma (nombres de campos y tipos)
 *    y los valores de code/message.
 *
 * Qué NO apunta: cabeceras, cookies, tokens, parámetros de las direcciones ni el
 * contenido de los archivos.
 */
class DiagnosticoSubida(
    private val page: Page,
    private val archivo: Path,
    private val progreso: Progreso,
) : AutoCloseable {

    private val salida: BufferedWriter = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)
    private val inicio = System.currentTimeMillis()
    private val empezadas = IdentityHashMap<Request, Long>()
    /** Última foto de cada zona (con los porcentajes cambiados por N%) y su último texto. */
    private val ultimoHtml = HashMap<String, String>()
    private val ultimoTexto = HashMap<String, String>()
    private var nombres: List<String> = emptyList()

    private val alEmpezar = Consumer<Request> { r -> empieza(r) }
    private val alTerminar = Consumer<Request> { r -> termina(r, null) }
    private val alFallar = Consumer<Request> { r -> termina(r, r.failure() ?: "?") }

    init {
        linea("Diagnóstico de subida · " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        linea("Los tiempos [s] cuentan desde aquí. FOTO = cómo está la página; RED = peticiones.")
        page.onRequest(alEmpezar)
        page.onRequestFinished(alTerminar)
        page.onRequestFailed(alFallar)
    }

    /** Nombres de los archivos que se van a subir, para encontrar sus filas en la página. */
    fun archivos(vararg nombres: String) {
        this.nombres = nombres.distinct()
        linea("Archivos: " + this.nombres.joinToString(", "))
    }

    /** Apunta un momento del proceso: "portada elegida", "pulso Save to draft"... */
    fun marca(texto: String) = linea("==== $texto ====")

    /** Apunta cómo está la página ahora (solo lo que ha cambiado desde la última vez). */
    fun foto(momento: String) {
        val json = try {
            page.evaluate(JS_FOTO, nombres) as String
        } catch (e: Exception) {
            linea("FOTO [$momento] no se ha podido leer la página: " + primeraLinea(e))
            return
        }
        val zonas = JSON.readTree(json)
        val vistas = HashSet<String>()
        for ((clave, valor) in zonas.properties()) {
            vistas += clave
            val texto = valor.path("texto").asText("")
            val html = valor.path("html").asText("")
            val htmlNormal = html.replace(PORCENTAJE, "N%")
            if (ultimoHtml[clave] != htmlNormal) {
                linea("FOTO [$momento] $clave\n      texto: $texto" + (if (html.isEmpty()) "" else "\n      html:  $html"))
            } else if (ultimoTexto[clave] != texto) {
                linea("FOTO [$momento] $clave  texto: $texto")
            }
            ultimoHtml[clave] = htmlNormal
            ultimoTexto[clave] = texto
        }
        for (clave in ultimoHtml.keys - vistas) {
            linea("FOTO [$momento] $clave: ya no está en la página")
            ultimoHtml.remove(clave)
            ultimoTexto.remove(clave)
        }
    }

    /** Hace fotos cada medio segundo durante unos segundos. */
    fun observar(momento: String, segundos: Int) {
        val fin = System.currentTimeMillis() + segundos * 1000L
        while (System.currentTimeMillis() < fin) {
            foto(momento)
            page.waitForTimeout(500.0)
        }
        foto(momento)
    }

    /**
     * Hace fotos cada segundo hasta que la persona pulsa ENTER en la consola, para
     * ver cómo avanzan las subidas hasta el final. Se rinde a los [maxMinutos].
     */
    fun esperarEnter(maxMinutos: Int) {
        progreso.info(
            "🔎 Diagnóstico: mira la ventana de Chrome. Cuando TODOS los archivos estén al 100 % " +
                "(o hayan fallado), haz clic en esta consola y pulsa ENTER para guardar el borrador."
        )
        marca("esperando ENTER")
        val fin = System.currentTimeMillis() + maxMinutos * 60_000L
        while (System.`in`.available() == 0) {
            if (System.currentTimeMillis() > fin) {
                marca("nadie ha pulsado ENTER en $maxMinutos minutos")
                throw IllegalStateException("Diagnóstico: no se ha pulsado ENTER en $maxMinutos minutos; no se guarda el borrador")
            }
            foto("subiendo")
            page.waitForTimeout(1000.0)
        }
        while (System.`in`.available() > 0) System.`in`.read() // se descarta lo escrito
        foto("ENTER pulsado")
        marca("ENTER pulsado")
    }

    private var cerrado = false

    override fun close() {
        if (cerrado) return
        cerrado = true
        page.offRequest(alEmpezar)
        page.offRequestFinished(alTerminar)
        page.offRequestFailed(alFallar)
        for ((r, t0) in empezadas) {
            linea("RED … ${r.method()} ${sinParametros(r.url())} sigue sin terminar tras ${System.currentTimeMillis() - t0} ms")
        }
        linea("Fin del diagnóstico")
        try {
            salida.close()
        } catch (_: Exception) {
            // no es grave
        }
        progreso.info("🔎 Diagnóstico guardado en " + archivo.toAbsolutePath())
    }

    // ------------------------------------------------------------ red

    private fun empieza(r: Request) {
        if (!interesa(r)) return
        empezadas[r] = System.currentTimeMillis()
        // Ojo: aquí no se puede pedir nada a Playwright sobre la petición (cabeceras, tamaño...):
        // espera a que la petición termine y frenaría el programa mientras se sube el archivo.
        linea("RED → ${r.method()} ${sinParametros(r.url())} [${r.resourceType()}]")
    }

    private fun termina(r: Request, fallo: String?) {
        val t0 = empezadas.remove(r) ?: return
        val ms = System.currentTimeMillis() - t0
        val url = sinParametros(r.url())
        if (fallo != null) {
            linea("RED ✘ ${r.method()} $url falló tras $ms ms: $fallo")
            return
        }
        val respuesta = try { r.response() } catch (_: Exception) { null }
        var forma = ""
        if (respuesta != null && esDeMakerOnline(r.url())) {
            val tipo = try { respuesta.headerValue("content-type") } catch (_: Exception) { null } ?: ""
            if (tipo.contains("json")) {
                forma = try {
                    "\n      respuesta: " + forma(JSON.readTree(respuesta.text()), 0)
                } catch (e: Exception) {
                    "\n      respuesta: (no se ha podido leer: ${primeraLinea(e)})"
                }
            }
        }
        val enviado = try {
            r.sizes().requestBodySize.let {
                when {
                    it >= 1024 -> " · envió ${it / 1024} KB"
                    it > 0 -> " · envió $it bytes"
                    else -> ""
                }
            }
        } catch (_: Exception) {
            ""
        }
        linea("RED ← ${respuesta?.status() ?: "?"} ${r.method()} $url ($ms ms$enviado)$forma")
    }

    /** Solo peticiones de datos; ni estilos, ni scripts, ni imágenes, ni estadísticas. */
    private fun interesa(r: Request): Boolean {
        if (r.resourceType() in IGNORADOS) return false
        val host = host(r.url())
        return ESTADISTICAS.none { host.contains(it) }
    }

    // ------------------------------------------------------------ utilidades

    @Synchronized
    private fun linea(texto: String) {
        val s = (System.currentTimeMillis() - inicio) / 1000.0
        try {
            salida.write("[%7.1f s] %s%n".format(java.util.Locale.ROOT, s, texto))
            salida.flush()
        } catch (_: Exception) {
            // si no se puede escribir, el diagnóstico no debe romper la subida
        }
    }

    private companion object {
        val JSON = ObjectMapper()
        val PORCENTAJE = Regex("""\d+(\.\d+)?\s*%""")
        val IGNORADOS = setOf("stylesheet", "script", "font", "image", "media", "texttrack", "manifest")
        val ESTADISTICAS = listOf(
            "google-analytics", "googletagmanager", "doubleclick", "facebook", "clarity.ms",
            "hotjar", "sentry", "tiktok", "bing.com", "baidu",
        )
        /** De las respuestas solo se apuntan los valores de estos campos; del resto, el tipo. */
        val VISIBLES = setOf("code", "message", "msg", "status", "success", "captchalevel", "errcode", "errmsg")

        fun host(url: String): String = try { URI(url).host ?: "" } catch (_: Exception) { "" }

        fun esDeMakerOnline(url: String) = host(url).endsWith("makeronline.com")

        /** La dirección sin ?parámetros ni #fragmento (ahí pueden ir firmas o tokens). */
        fun sinParametros(url: String) = url.substringBefore('#').substringBefore('?')

        fun primeraLinea(e: Exception) = (e.message ?: e.toString()).lineSequence().first()

        /** La forma de un JSON: {code:0, data:{id:número, url:texto, list:[3× {...}]}} */
        fun forma(n: JsonNode, nivel: Int): String = when {
            n.isObject -> if (nivel > 4) "{…}" else n.properties().joinToString(", ", "{", "}") { (k, v) ->
                "$k:" + if (k.lowercase() in VISIBLES && v.isValueNode) v.asText().take(120) else forma(v, nivel + 1)
            }
            n.isArray -> if (n.isEmpty) "[]" else "[${n.size()}× ${forma(n[0], nivel + 1)}]"
            n.isTextual -> "texto"
            n.isNumber -> "número"
            n.isBoolean -> "sí/no"
            n.isNull -> "null"
            else -> n.nodeType.name.lowercase()
        }

        /**
         * Lee de la página, sin tocar nada:
         *  - las zonas de subida, buscadas por su título (textos que se ven en la web);
         *  - la fila de cada archivo, buscada por su nombre;
         *  - la portada (primer .el-upload-dragger, el que ya usa el Uploader);
         *  - los botones de guardar, enviar y siguiente paso;
         *  - los mensajes y ventanas de la web (.el-message, .el-dialog...).
         * Del HTML quita los dibujos (svg), las imágenes en línea y los parámetros de las direcciones.
         */
        val JS_FOTO = """
            (nombres) => {
              const limpia = (h) => h
                .replace(/<svg[\s\S]*?<\/svg>/gi, '<svg/>')
                .replace(/(src|href)="([^"]*)"/gi, (m, a, v) =>
                    a + '="' + (v.startsWith('data:') ? 'data:…' : v.replace(/[?#].*$/, '?…')) + '"')
                .replace(/\s{2,}/g, ' ')
                .slice(0, 6000);
              const texto = (e) => (e.innerText || e.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 300);
              const propio = (e) => [...e.childNodes].filter(n => n.nodeType === 3).map(n => n.textContent).join('').trim();
              const visible = (e) => !!(e.offsetWidth || e.offsetHeight || e.getClientRects().length);
              const todos = [...document.querySelectorAll('body *')];
              const out = {};

              for (const t of ['Print Profile Files', 'Print Profile Pictures', 'Model Files']) {
                const titulo = todos.find(e => propio(e).replace(/^\*\s*/, '') === t && visible(e));
                if (!titulo) continue;
                let z = titulo;
                for (let i = 0; i < 8 && z.parentElement; i++) {
                  z = z.parentElement;
                  if (z.querySelector('input[type=file]')) break;
                }
                out['zona "' + t + '"'] = { texto: texto(z), html: limpia(z.outerHTML) };
              }

              for (const nombre of nombres) {
                todos.filter(e => propio(e).includes(nombre) && visible(e)).forEach((hoja, i) => {
                  let fila = hoja;
                  for (let k = 0; k < 6 && fila.parentElement; k++) {
                    const r = fila.getBoundingClientRect();
                    if (r.height >= 40 && r.width >= 300) break;
                    fila = fila.parentElement;
                  }
                  out['fila "' + nombre + '" #' + (i + 1)] = { texto: texto(fila), html: limpia(fila.outerHTML) };
                });
              }

              const arrastre = document.querySelector('.el-upload-dragger');
              if (arrastre && visible(arrastre)) {
                const z = arrastre.closest('.el-form-item') || arrastre.parentElement || arrastre;
                out['portada (primer .el-upload-dragger)'] = { texto: texto(z), html: limpia(z.outerHTML) };
              }

              [...document.querySelectorAll('button')]
                .filter(b => visible(b) && /draft|submit|next step/i.test(b.textContent))
                .forEach(b => out['botón "' + texto(b).slice(0, 40) + '"'] =
                    { texto: texto(b) + ' · disabled=' + b.disabled, html: limpia(b.outerHTML) });

              [...document.querySelectorAll('.el-message, .el-message-box, .el-notification, .el-dialog')]
                .filter(visible)
                .forEach((m, i) => out['mensaje/ventana #' + (i + 1)] = { texto: m.className + ' | ' + texto(m), html: '' });

              out['dirección'] = { texto: location.origin + location.pathname, html: '' };
              return JSON.stringify(out);
            }
        """.trimIndent()
    }
}
