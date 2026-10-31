package dev.jefe.makeronline

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Aviso de "parar". Lo pide la persona (botón "Parar" en la app, o escribir "parar" en la
 * terminal) y core lo comprueba solo en los momentos en que parar no deja nada a medias:
 * entre modelos, durante las pausas, mientras espera una subida o un captcha...
 *
 * Se puede pedir desde cualquier hilo.
 */
class Cancelacion {
    private val pedido = AtomicBoolean(false)

    /** Pide parar. Se puede llamar varias veces. */
    fun pedir() = pedido.set(true)

    /** true si alguien ha pedido parar. */
    fun pedida(): Boolean = pedido.get()

    /** Si se ha pedido parar, lanza [CanceladoException]. Se llama en los puntos seguros. */
    fun comprobar() {
        if (pedido.get()) throw CanceladoException()
    }
}

/** Se ha parado porque la persona lo ha pedido. No es un error. */
class CanceladoException : RuntimeException("Parado a petición tuya")
