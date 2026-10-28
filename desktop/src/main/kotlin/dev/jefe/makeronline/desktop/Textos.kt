package dev.jefe.makeronline.desktop

/**
 * Todos los textos que ve la persona, en un único sitio.
 * Para traducir la app más adelante, solo habrá que tocar aquí.
 */
object Textos {
    const val TITULO_APP = "MakerOnline Desktop"
    const val VENTANA_OK = "La ventana funciona. Fase 0 lista."

    fun coreConectado(carpetaSesion: String) =
        "El módulo core está conectado (carpeta de la sesión: $carpetaSesion)."
}
