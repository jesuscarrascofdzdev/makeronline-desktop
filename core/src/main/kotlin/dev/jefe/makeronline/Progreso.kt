package dev.jefe.makeronline

/**
 * Cómo avisa core de lo que va haciendo.
 *
 * Los programas de terminal usan [ProgresoConsola], que lo imprime. La app de
 * escritorio tendrá su propia versión que lo pinta en pantalla.
 *
 * Los textos van sin símbolo delante: cada versión pone el suyo (la terminal usa
 * →, ✔, ✘...). Un texto puede tener varias líneas.
 */
interface Progreso {

    // ------------------------------------------------------------ mensajes

    /** Información normal. */
    fun info(texto: String)

    /** Un paso del trabajo: "Abriendo …", "Dando like". */
    fun paso(texto: String)

    /** Algo ha salido bien: "Descargado en …", "Borrador guardado …". */
    fun correcto(texto: String)

    /** Un modelo que se salta: "Ya tenía like, lo salto". */
    fun saltado(texto: String)

    /** Like dado o que ya estaba. */
    fun like(texto: String)

    /** Detalle secundario que no impide seguir: "no se ha podido guardar la portada…". */
    fun nota(texto: String)

    /** Algo ha fallado. */
    fun error(texto: String)

    // ------------------------------------------------------------ eventos

    /** Empieza a revisar un usuario de la lista (índice desde 1). */
    fun usuario(indice: Int, total: Int, nombre: String)

    /** Empieza un modelo, para descargar o para subir (índice desde 1). */
    fun modelo(indice: Int, total: Int, titulo: String)

    /** Va leyendo la lista de modelos de un usuario página a página (con "todos"). */
    fun leyendoPaginas(pagina: Int, ultima: Int)

    /** Pausa entre descargas antes del siguiente modelo. */
    fun pausa(segundos: Long)

    /** MakerOnline pide un captcha: la persona tiene que resolverlo en la ventana de Chrome. */
    fun captcha(para: String, minutos: Long)

    /** El captcha se ha resuelto o se ha acabado el tiempo: la ventana de Chrome vuelve a apartarse. */
    fun captchaTerminado()
}
