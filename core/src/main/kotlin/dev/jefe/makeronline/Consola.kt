package dev.jefe.makeronline

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Lo que la persona escribe en la consola (solo para los programas de terminal).
 *
 * Un hilo lee la consola en segundo plano:
 *  - "parar" + ENTER pide parar ([Cancelacion]);
 *  - cualquier otra línea queda guardada para quien la pida (por ejemplo [RevisionConsola]).
 */
object EntradaConsola {
    private val lineas = LinkedBlockingQueue<String>()
    @Volatile private var cancelacion: Cancelacion? = null
    @Volatile private var progreso: Progreso? = null
    private var iniciada = false

    /** Empieza a leer la consola (una sola vez) y explica cómo parar. */
    @JvmStatic
    @Synchronized
    fun iniciar(cancelacion: Cancelacion, progreso: Progreso) {
        this.cancelacion = cancelacion
        this.progreso = progreso
        progreso.info("(Para pararlo sin dejar nada a medias, escribe parar y pulsa ENTER)")
        if (iniciada) return
        iniciada = true
        val hilo = Thread({ leer() }, "consola")
        hilo.isDaemon = true // no impide que el programa termine
        hilo.start()
    }

    /** La siguiente línea escrita (sin contar "parar"), o null si en [ms] no se ha escrito nada. */
    @JvmStatic
    fun siguienteLinea(ms: Long): String? = lineas.poll(ms, TimeUnit.MILLISECONDS)

    private fun leer() {
        val lector = BufferedReader(InputStreamReader(System.`in`, Charset.defaultCharset()))
        while (true) {
            val linea = try {
                lector.readLine()
            } catch (_: IOException) {
                null
            } ?: return // la consola se ha cerrado
            if (linea.trim().equals("parar", ignoreCase = true)) {
                if (cancelacion?.pedida() != true) {
                    progreso?.info("⏹ Vale: paro en cuanto se pueda sin dejar nada a medias…")
                }
                cancelacion?.pedir()
            } else {
                lineas.put(linea)
            }
        }
    }
}

/**
 * [Revision] para la terminal: ENTER guarda el borrador; "no" + ENTER lo descarta.
 */
class RevisionConsola(
    private val progreso: Progreso,
    private val cancelacion: Cancelacion,
) : Revision {

    override fun confirmar(modelo: String): Boolean {
        progreso.info(
            "   ⏸ Revisa el formulario de $modelo en la ventana de Chrome.\n" +
                "     Pulsa ENTER aquí para guardar el borrador, o escribe no y pulsa ENTER para descartarlo."
        )
        while (true) {
            cancelacion.comprobar()
            val linea = EntradaConsola.siguienteLinea(500) ?: continue
            return !linea.trim().equals("no", ignoreCase = true)
        }
    }
}
