package dev.jefe.makeronline.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.jefe.makeronline.Rutas
import java.nio.file.Path

/** Las pantallas del menú lateral. */
enum class Pantalla(val titulo: String) {
    SESION(Textos.MENU_SESION),
    SUBIR(Textos.MENU_SUBIR),
    DESCARGAR(Textos.MENU_DESCARGAR),
}

/**
 * El estado de toda la app: la pantalla elegida y el de cada pantalla.
 * Se crea una vez al arrancar.
 */
class EstadoApp(val rutas: Rutas) {
    val hilo = HiloNavegador()
    /** Qué está haciendo Chrome; lo comparten todas las pantallas. */
    val ocupacion = Ocupacion()
    val sesion = EstadoSesion(rutas, hilo, ocupacion)
    /** Lo que se recuerda de una vez para otra (ajustes.properties). */
    val ajustes = Ajustes(rutas.ajustes)
    val subida = EstadoSubida(rutas, hilo, ocupacion)
    val descarga = EstadoDescarga(rutas, hilo, ocupacion, ajustes)
    /** La ventana de añadir y quitar usuarios (botón "Editar" de Descargar). */
    val edicion = EstadoEdicion(rutas, hilo, ocupacion, descarga)

    var pantalla by mutableStateOf(Pantalla.SESION)

    /** Al cerrar la ventana: para lo que esté en marcha, cierra Chrome y para el hilo del navegador. */
    fun cerrar() {
        subida.alCerrarApp()
        descarga.alCerrarApp()
        sesion.alCerrarApp()
        hilo.close()
    }

    companion object {
        /**
         * Propiedad con la carpeta de datos. Durante la fase 2 la pone la tarea "run" de Gradle
         * (ver desktop/build.gradle.kts) con la raíz del repo, para compartir la sesión,
         * usuarios.json y descargados.txt con los programas de terminal.
         */
        private const val PROPIEDAD_DATOS = "makeronline.datos"

        fun rutas(): Rutas = Rutas(Path.of(System.getProperty(PROPIEDAD_DATOS) ?: "").toAbsolutePath())
    }
}
