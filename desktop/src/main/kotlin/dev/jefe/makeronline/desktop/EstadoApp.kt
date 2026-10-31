package dev.jefe.makeronline.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.jefe.makeronline.Rutas
import java.nio.file.Path

/** Las pantallas del menú lateral. */
enum class Pantalla(val titulo: String) {
    SESION(Textos.MENU_SESION),
    DESCARGAR(Textos.MENU_DESCARGAR),
}

/**
 * El estado de toda la app: la pantalla elegida y el de cada pantalla.
 * Se crea una vez al arrancar.
 */
class EstadoApp(val rutas: Rutas) {
    val hilo = HiloNavegador()
    val sesion = EstadoSesion(rutas, hilo)

    var pantalla by mutableStateOf(Pantalla.SESION)

    /** Al cerrar la ventana: cierra Chrome si estaba abierto y para el hilo del navegador. */
    fun cerrar() {
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
