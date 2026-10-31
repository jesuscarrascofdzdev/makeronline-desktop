package dev.jefe.makeronline.desktop

import dev.jefe.makeronline.Cancelacion
import dev.jefe.makeronline.Revision
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.swing.SwingUtilities

/**
 * "Revisar antes de guardar" en la app: en vez del ENTER / "no" de la terminal, la ventana
 * enseña los botones Guardar borrador y Descartar.
 *
 * [confirmar] lo llama el Uploader en el hilo del navegador y espera a que la persona pulse
 * un botón ([responder]). Mientras espera se puede parar (botón Parar): entonces no se guarda.
 *
 * @param alPedir se llama en el hilo de la ventana con el nombre del modelo a revisar,
 *                y con null cuando ya se ha respondido
 */
class RevisionApp(
    private val cancelacion: Cancelacion,
    private val alPedir: (String?) -> Unit,
) : Revision {

    @Volatile
    private var respuesta: CompletableFuture<Boolean>? = null

    override fun confirmar(modelo: String): Boolean {
        val f = CompletableFuture<Boolean>()
        respuesta = f
        SwingUtilities.invokeLater { alPedir(modelo) }
        try {
            while (true) {
                cancelacion.comprobar() // si se pide parar, no se guarda
                try {
                    return f.get(500, TimeUnit.MILLISECONDS)
                } catch (_: TimeoutException) {
                    // sigue esperando
                }
            }
        } finally {
            respuesta = null
            SwingUtilities.invokeLater { alPedir(null) }
        }
    }

    /** Desde la ventana: true = guardar el borrador, false = descartarlo (se queda en pendientes/). */
    fun responder(guardar: Boolean) {
        respuesta?.complete(guardar)
    }
}
