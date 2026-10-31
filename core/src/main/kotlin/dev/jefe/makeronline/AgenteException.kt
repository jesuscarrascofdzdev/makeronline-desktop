package dev.jefe.makeronline

/**
 * Un problema que impide seguir: no hay sesión, otro programa está usando Chrome,
 * la web no ha cargado... El mensaje está pensado para enseñárselo a la persona tal cual.
 *
 * Los programas de terminal lo muestran y terminan con [codigoSalida]; la app lo
 * enseñará en pantalla.
 */
class AgenteException @JvmOverloads constructor(
    mensaje: String,
    /** Código con el que termina el programa de terminal: 1 en general, 2 si el candado está ocupado. */
    val codigoSalida: Int = 1,
) : RuntimeException(mensaje)
