package dev.jefe.makeronline.desktop

/**
 * Todos los textos que ve la persona, en un único sitio.
 * Para traducir la app más adelante, solo habrá que tocar aquí.
 */
object Textos {
    const val TITULO_APP = "MakerOnline Desktop"

    // ------------------------------------------------------------ menú
    const val MENU_SESION = "Sesión"
    const val MENU_DESCARGAR = "Descargar"

    // ------------------------------------------------------------ sesión
    const val SESION_TITULO = "Sesión"
    const val SESION_EXPLICACION =
        "La app usa tu Google Chrome con un perfil propio. Inicias sesión en MakerOnline una vez " +
            "y se queda guardada en este ordenador; nunca sale de él."
    const val CHROME = "Google Chrome"
    const val CHROME_OK = "Instalado"
    const val CHROME_FALTA = "No lo encuentro. Sin Chrome la app no puede funcionar."
    const val DESCARGAR_CHROME = "Descargar Google Chrome"
    const val URL_CHROME = "https://www.google.com/chrome/"
    const val MAKERONLINE = "MakerOnline"
    const val COMPROBANDO = "Comprobando…"
    const val SIN_COMPROBAR = "Sin comprobar"
    const val SESION_INICIADA = "Sesión iniciada"
    fun sesionIniciadaComo(nombre: String) = "Sesión iniciada como $nombre"
    fun sinSesion(motivo: String?) = "Sin sesión" + (motivo?.let { ": $it" } ?: "")
    const val INICIAR_SESION = "Iniciar sesión"
    const val VOLVER_A_INICIAR = "Volver a iniciar sesión"
    const val COMPROBAR = "Comprobar otra vez"
    const val INSTRUCCIONES_LOGIN =
        "Se ha abierto Chrome en MakerOnline. Pulsa \"Log in\", elige \"Continuar con Google\" y entra " +
            "con tu cuenta. No inicies sesión en el propio Chrome (el botón de perfil de arriba a la " +
            "derecha), solo en la web. Cuando veas tu avatar en MakerOnline, vuelve aquí."
    const val YA_HE_ENTRADO = "Ya he entrado"
    const val CANCELAR = "Cancelar"
    fun carpetaDatos(ruta: String) = "Carpeta de datos: $ruta"

    // ------------------------------------------------------------ descargar
    const val DESCARGAR_PRONTO = "La pantalla para descargar llega en el siguiente paso (2b)."

    // ------------------------------------------------------------ errores
    fun errorInesperado(e: Throwable) =
        "Algo ha fallado: " + (e.message?.lineSequence()?.firstOrNull() ?: e.toString())
}
