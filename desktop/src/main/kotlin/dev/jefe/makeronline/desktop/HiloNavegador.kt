package dev.jefe.makeronline.desktop

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * El único hilo que toca Chrome.
 *
 * Playwright no admite varios hilos: todo lo que use el navegador (comprobar la sesión,
 * descargar, subir...) se hace aquí, de uno en uno. Así la ventana de la app nunca se
 * congela mientras Chrome trabaja.
 */
class HiloNavegador : AutoCloseable {

    private val ejecutor: ExecutorService = Executors.newSingleThreadExecutor { tarea ->
        Thread(tarea, "navegador").apply { isDaemon = true } // no impide cerrar la app
    }
    private val dispatcher = ejecutor.asCoroutineDispatcher()

    /** Ejecuta [tarea] en el hilo del navegador y devuelve su resultado sin bloquear la ventana. */
    suspend fun <T> ejecutar(tarea: () -> T): T = withContext(dispatcher) { tarea() }

    /**
     * Al cerrar la app: ejecuta [tarea] (por ejemplo, cerrar Chrome) en el hilo del navegador
     * y espera como mucho unos segundos.
     */
    fun alCerrar(tarea: () -> Unit) {
        try {
            ejecutor.submit(tarea).get(10, TimeUnit.SECONDS)
        } catch (_: Exception) {
            // la app se cierra igualmente
        }
    }

    override fun close() {
        ejecutor.shutdownNow()
    }
}
