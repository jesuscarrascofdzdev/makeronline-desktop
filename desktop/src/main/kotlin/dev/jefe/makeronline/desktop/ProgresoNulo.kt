package dev.jefe.makeronline.desktop

import dev.jefe.makeronline.Progreso

/** [Progreso] que no enseña nada: para operaciones cortas como comprobar la sesión. */
object ProgresoNulo : Progreso {
    override fun info(texto: String) {}
    override fun paso(texto: String) {}
    override fun correcto(texto: String) {}
    override fun saltado(texto: String) {}
    override fun like(texto: String) {}
    override fun nota(texto: String) {}
    override fun error(texto: String) {}
    override fun usuario(indice: Int, total: Int, nombre: String) {}
    override fun modelo(indice: Int, total: Int, titulo: String) {}
    override fun leyendoPaginas(pagina: Int, ultima: Int) {}
    override fun pausa(segundos: Long) {}
    override fun captcha(para: String, minutos: Long) {}
    override fun captchaTerminado() {}
}
