package dev.jefe.makeronline.desktop

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Copia a subidas/pendientes/ las carpetas de modelos que la persona arrastra a la app
 * (o elige con "Añadir carpetas…"). Se copian, no se mueven: las originales no se tocan.
 */
object CopiaCarpetas {

    /** Más archivos que esto no parece la carpeta de un modelo (por si se arrastra "Documentos"). */
    private const val MAX_ARCHIVOS = 200

    /**
     * @property copiadas  nombres de las carpetas copiadas
     * @property problemas lo que no se ha copiado y por qué, para enseñárselo a la persona
     */
    data class Resultado(val copiadas: List<String>, val problemas: List<String>)

    fun copiar(origenes: List<Path>, pendientes: Path): Resultado {
        Files.createDirectories(pendientes)
        val destino = pendientes.toAbsolutePath().normalize()
        val copiadas = ArrayList<String>()
        val problemas = ArrayList<String>()
        val revisadas = origenes.map { it.toAbsolutePath().normalize() }.flatMap { o ->
            // Una carpeta que contiene pendientes/ (por ejemplo la de datos de la app) no se mira por dentro
            if (destino.startsWith(o)) listOf(o) else expandir(o)
        }
        for (o in revisadas) {
            val nombre = o.fileName?.toString() ?: continue
            try {
                when {
                    !Files.isDirectory(o) -> problemas += Textos.noEsCarpeta(nombre)
                    o.parent == destino -> problemas += Textos.yaEnPendientes(nombre)
                    destino.startsWith(o) -> problemas += Textos.contienePendientes(nombre)
                    Files.exists(destino.resolve(nombre)) -> problemas += Textos.yaExisteEnPendientes(nombre)
                    contar(o) > MAX_ARCHIVOS -> problemas += Textos.demasiadosArchivos(nombre, MAX_ARCHIVOS)
                    else -> {
                        copiarArbol(o, destino.resolve(nombre))
                        copiadas += nombre
                    }
                }
            } catch (e: IOException) {
                problemas += Textos.noSePuedeCopiar(nombre, e.message ?: e.toString())
            }
        }
        return Resultado(copiadas, problemas)
    }

    /**
     * Si se arrastra una carpeta que no tiene modelo pero sí subcarpetas (por ejemplo
     * "mis-modelos/" con una carpeta por modelo), se toma cada subcarpeta.
     */
    private fun expandir(dir: Path): List<Path> {
        if (!Files.isDirectory(dir)) return listOf(dir)
        val dentro = Files.list(dir).use { it.toList() }
        val tieneModelo = dentro.any { Files.isRegularFile(it) && esModelo(it) }
        val subcarpetas = dentro.filter { Files.isDirectory(it) }.sorted()
        return if (!tieneModelo && subcarpetas.isNotEmpty()) subcarpetas else listOf(dir)
    }

    private fun esModelo(p: Path): Boolean {
        val n = p.fileName.toString().lowercase()
        return n.endsWith(".stl") || n.endsWith(".3mf")
    }

    private fun contar(dir: Path): Long = Files.walk(dir).use { s -> s.limit(MAX_ARCHIVOS + 2L).count() - 1 }

    /** Copia la carpeta entera; si algo falla a medias, borra lo copiado para no dejarla a medias. */
    private fun copiarArbol(origen: Path, destino: Path) {
        try {
            Files.walk(origen).use { s ->
                s.forEach { p ->
                    val d = destino.resolve(origen.relativize(p).toString())
                    if (Files.isDirectory(p)) Files.createDirectories(d)
                    else Files.copy(p, d, StandardCopyOption.COPY_ATTRIBUTES)
                }
            }
        } catch (e: Exception) {
            borrar(destino)
            throw if (e is IOException) e else IOException(e.message, e)
        }
    }

    private fun borrar(dir: Path) {
        try {
            if (!Files.exists(dir)) return
            Files.walk(dir).use { s -> s.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
        } catch (_: Exception) {
            // lo que quede se ve en la lista y se puede borrar a mano
        }
    }
}
