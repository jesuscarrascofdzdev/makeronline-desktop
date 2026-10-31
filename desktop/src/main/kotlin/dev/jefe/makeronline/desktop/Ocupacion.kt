package dev.jefe.makeronline.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Qué está haciendo Chrome ahora mismo, compartido por todas las pantallas: mientras hay
 * una tarea en marcha (descargar, comprobar la sesión...) no se puede empezar otra.
 * Solo se usa desde el hilo de la ventana.
 */
class Ocupacion {
    /** La tarea en marcha, para enseñarla ("Descargando…"), o null si Chrome está libre. */
    var tarea by mutableStateOf<String?>(null)
        private set

    val libre: Boolean get() = tarea == null

    /** Marca [que] como la tarea en marcha. Devuelve false si ya había otra. */
    fun empezar(que: String): Boolean {
        if (tarea != null) return false
        tarea = que
        return true
    }

    fun terminar() {
        tarea = null
    }
}
