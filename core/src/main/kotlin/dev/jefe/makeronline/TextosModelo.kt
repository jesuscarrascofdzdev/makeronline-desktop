package dev.jefe.makeronline

import java.nio.charset.Charset
import java.nio.charset.CharacterCodingException
import java.nio.file.Files
import java.nio.file.Path
import java.text.Normalizer

/**
 * Los textos de un modelo escritos en un .txt (con el Bloc de notas, por ejemplo), en vez de
 * en info.json. Cada dato en su línea, como "Nombre: valor"; la descripción va al final y
 * ocupa todas las líneas que sigan:
 *
 * ```
 * Título: Llavero gato
 * Categoría: Toys&Games > Action Figures
 * Etiquetas: gato, llavero, regalo
 * Licencia: CC0 (aka CC Zero)      (opcional)
 * Tipo: FDM                        (opcional)
 * Descripción:
 * Todo lo que venga después, con sus saltos de línea.
 * ```
 *
 * Los nombres valen con o sin tilde y en mayúsculas o minúsculas, y también en inglés
 * (Title, Category, Tags, License, Print type, Description). Las líneas vacías antes de la
 * descripción se ignoran.
 */
object TextosModelo {

    /** Nombre (sin tildes, en minúsculas) → qué dato es. */
    private val CLAVES = mapOf(
        "titulo" to "titulo", "title" to "titulo",
        "categoria" to "categoria", "category" to "categoria",
        "etiquetas" to "etiquetas", "tags" to "etiquetas",
        "licencia" to "licencia", "license" to "licencia",
        "tipo" to "tipo", "tipo de impresion" to "tipo", "print type" to "tipo", "printtype" to "tipo",
        "descripcion" to "descripcion", "description" to "descripcion",
    )

    /**
     * Lee el .txt. Admite UTF-8 (lo normal en el Bloc de notas de Windows 10 y 11) y, si no lo es,
     * la codificación antigua de Windows.
     *
     * @throws IllegalStateException con todas las líneas que no se entienden
     */
    @JvmStatic
    fun leer(archivo: Path): ModelMeta {
        val bytes = Files.readAllBytes(archivo)
        val texto = try {
            Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            String(bytes, Charset.forName("windows-1252"))
        }
        return interpretar(texto)
    }

    /** Igual que [leer], con el texto ya leído. */
    @JvmStatic
    fun interpretar(texto: String): ModelMeta {
        val datos = HashMap<String, String>()
        val descripcion = ArrayList<String>()
        val problemas = ArrayList<String>()
        var enDescripcion = false
        for ((i, linea) in texto.removePrefix("\uFEFF").lines().withIndex()) {
            if (enDescripcion) {
                descripcion += linea.trimEnd()
                continue
            }
            if (linea.isBlank()) continue
            val dosPuntos = linea.indexOf(':')
            if (dosPuntos < 0) {
                problemas += "línea ${i + 1}: no entiendo \"${linea.trim()}\" (cada dato va como Nombre: valor)"
                continue
            }
            val nombre = linea.substring(0, dosPuntos).trim()
            val valor = linea.substring(dosPuntos + 1).trim()
            when (val clave = CLAVES[normalizar(nombre)]) {
                null -> problemas += "línea ${i + 1}: no conozco \"$nombre\" " +
                    "(valen Título, Categoría, Etiquetas, Licencia, Tipo y Descripción)"
                "descripcion" -> {
                    enDescripcion = true
                    if (valor.isNotEmpty()) descripcion += valor
                }
                else -> {
                    if (clave in datos) problemas += "línea ${i + 1}: \"$nombre\" está repetido"
                    datos[clave] = valor
                }
            }
        }
        if (problemas.isNotEmpty()) throw IllegalStateException(problemas.joinToString("\n"))

        // Sin líneas vacías al principio ni al final de la descripción
        val desc = descripcion.dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }.joinToString("\n")
        return ModelMeta(
            datos["titulo"],
            desc,
            datos["etiquetas"]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            datos["categoria"]?.split('>')?.map { it.trim() },
            datos["licencia"]?.ifBlank { null },
            datos["tipo"]?.ifBlank { null },
        )
    }

    private fun normalizar(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
            .lowercase().replace(Regex("\\s+"), " ").trim()
}
