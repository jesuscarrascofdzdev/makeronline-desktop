package dev.jefe.makeronline

/**
 * "Revisar antes de guardar" (Uploader --revisar): con el formulario relleno y todo subido,
 * se le pregunta a la persona si se guarda el borrador.
 *
 * En la terminal es [RevisionConsola] (ENTER o "no"); en la app serán dos botones.
 */
fun interface Revision {
    /**
     * Espera a que la persona mire el formulario en Chrome y decida.
     *
     * @param modelo el nombre de la carpeta del modelo, para el mensaje
     * @return true para guardar el borrador; false para descartarlo (la carpeta se queda en pendientes/)
     * @throws CanceladoException si mientras tanto se pide parar
     */
    fun confirmar(modelo: String): Boolean
}
