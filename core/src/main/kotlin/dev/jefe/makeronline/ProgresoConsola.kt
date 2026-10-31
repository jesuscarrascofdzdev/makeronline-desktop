package dev.jefe.makeronline

/**
 * [Progreso] para los programas de terminal: imprime los mismos textos que
 * imprimían antes. Los errores van a la salida de errores (en rojo en IntelliJ).
 */
class ProgresoConsola : Progreso {

    /** true mientras la última línea es la de "Leyendo la lista…" (se reescribe con \r). */
    private var lineaAbierta = false

    override fun info(texto: String) = linea(texto)

    override fun paso(texto: String) = linea("→ $texto")

    override fun correcto(texto: String) = linea("✔ $texto")

    override fun saltado(texto: String) = linea("↷ $texto")

    override fun like(texto: String) = linea("♥ $texto")

    override fun nota(texto: String) = linea("   ($texto)")

    override fun error(texto: String) {
        cerrarLinea()
        System.err.println("✘ $texto")
    }

    override fun usuario(indice: Int, total: Int, nombre: String) =
        linea("\n######## [$indice/$total] $nombre ########")

    override fun modelo(indice: Int, total: Int, titulo: String) =
        linea("\n=== [$indice/$total] $titulo ===")

    override fun leyendoPaginas(pagina: Int, ultima: Int) {
        print("\r→ Leyendo la lista de modelos: página $pagina/$ultima   ")
        lineaAbierta = true
    }

    override fun pausa(segundos: Long) = linea("… esperando $segundos s antes del siguiente")

    override fun captcha(para: String, minutos: Long) = linea(
        "   ⚠ MakerOnline pide captcha para $para. Resuélvelo en la ventana de Chrome (tienes $minutos minutos)"
    )

    override fun captchaTerminado() {
        // en la terminal no hace falta decir nada
    }

    private fun linea(texto: String) {
        cerrarLinea()
        println(texto)
    }

    /** Termina la línea de "Leyendo la lista…" antes de escribir otra cosa. */
    private fun cerrarLinea() {
        if (lineaAbierta) {
            println()
            lineaAbierta = false
        }
    }
}
