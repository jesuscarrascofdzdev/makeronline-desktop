package dev.jefe.makeronline.desktop

/**
 * El creador de la app. Al descargar toda la lista va siempre el primero: es la forma de
 * apoyar la app sin que cueste nada. Viene marcado, se puede desmarcar (y se recuerda, ver
 * [Ajustes]), pero no se puede quitar de la lista. No está en usuarios.json.
 */
object Creador {
    const val NOMBRE = "juji-nya"
    const val PERFIL = "https://www.makeronline.com/en/user/personalInfo/a9defb2a-a9c5-4e28-a46c-4a43c576ddfe.html"

    /** Como se le pasa al Downloader: "Nombre · URL del perfil", lo más fiable. */
    const val ENTRADA = NOMBRE + " · " + PERFIL
}
