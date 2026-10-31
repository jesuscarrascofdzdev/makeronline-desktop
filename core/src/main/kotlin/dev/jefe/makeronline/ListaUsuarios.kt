package dev.jefe.makeronline

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * La lista de usuarios del Downloader (usuarios.json): un array JSON de textos que admite
 * comentarios // y una coma de más al final. Cada entrada puede ser:
 *  - "@usuario_1234567 / NombreVisible";
 *  - solo el @ o solo el nombre;
 *  - la URL del perfil (o su id), con o sin un nombre delante: "Ricardo · https://…/<id>.html".
 *    Es lo más fiable: no depende del buscador de MakerOnline.
 *
 * La lee el Downloader y la lee y la escribe la app (al añadir o quitar usuarios).
 */
object ListaUsuarios {

    /**
     * Lo leído de usuarios.json.
     *
     * @property entradas las entradas, sin vacías ni repetidas (sin distinguir mayúsculas)
     * @property error    por qué no se ha podido leer (para enseñárselo a la persona), o null
     * @property existe   false si el archivo todavía no existe
     */
    data class Lectura(val entradas: List<String>, val error: String?, val existe: Boolean)

    /**
     * Cómo enseñar una entrada.
     *
     * @property nombre    el nombre visible (o lo que haya)
     * @property detalle   el @ o cómo se busca, para ponerlo en pequeño
     * @property porEnlace true si va por la URL del perfil (no usa el buscador)
     */
    data class Descripcion(val nombre: String, val detalle: String, val porEnlace: Boolean)

    private val JSON_LISTA = JsonMapper.builder()
        .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS, JsonReadFeature.ALLOW_TRAILING_COMMA)
        .build()
    private val UUID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private val SEPARADOR = Regex("""\s*/\s*""")

    /** Lo que va al principio del archivo cada vez que se guarda: las instrucciones. */
    private val CABECERA = """
        |[
        |  // Usuarios que repasa el Downloader: uno por línea, entre comillas y separados por comas.
        |  // Lo más fiable es "Nombre · URL de su perfil", que es como los guarda la app al añadirlos:
        |  //   "Pepe · https://www.makeronline.com/en/user/personalInfo/<id>.html",
        |  // También vale "@usuario_1234567 / NombreVisible", solo el @ o solo el nombre visible.
        |  // Las líneas que empiezan por // se ignoran. La app reescribe este archivo al añadir o
        |  // quitar usuarios; la primera vez guarda una copia del anterior en usuarios.json.bak.
        |""".trimMargin()

    /** Lee usuarios.json. No lo crea si no existe (eso lo hace el Downloader). */
    @JvmStatic
    fun leer(archivo: Path): Lectura {
        if (!Files.exists(archivo)) return Lectura(emptyList(), null, existe = false)
        val raiz = try {
            JSON_LISTA.readTree(archivo.toFile())
        } catch (e: JsonProcessingException) {
            val donde = e.location?.let { " (línea ${it.lineNr})" } ?: ""
            return Lectura(
                emptyList(),
                "usuarios.json no es válido$donde: ${e.originalMessage}" +
                    "\n  Tiene que ser una lista: [\"usuario1\", \"usuario2\"]",
                existe = true,
            )
        }
        if (raiz == null || !raiz.isArray) {
            return Lectura(emptyList(), "usuarios.json tiene que ser una lista: [\"usuario1\", \"usuario2\"]", existe = true)
        }
        val vistos = HashSet<String>()
        val entradas = ArrayList<String>()
        for (n in raiz) {
            val s = n.asText("").trim()
            if (s.isNotEmpty() && vistos.add(s.lowercase())) entradas += s
        }
        return Lectura(entradas, null, existe = true)
    }

    /** Nombre y detalle de una entrada, para enseñarla en una lista. */
    @JvmStatic
    fun describir(entrada: String): Descripcion {
        val id = UUID.find(entrada)?.value
        if (id != null) {
            // "Nombre · https://…/<id>.html", solo la URL o solo el id
            val delante = entrada.substring(0, entrada.indexOf(id))
            val nombre = delante.substringBefore("http").trim().trimEnd('·', '-', '|', ':', ',').trim()
            return Descripcion(nombre.ifEmpty { "Perfil ${id.take(8)}…" }, "por enlace al perfil", porEnlace = true)
        }
        val partes = entrada.split(SEPARADOR)
        if (partes.size == 2 && partes.all { it.isNotBlank() }) {
            // "@usuario / Nombre" (o al revés)
            val primeroEsHandle = partes[0].startsWith("@") || !partes[1].startsWith("@")
            val handle = if (primeroEsHandle) partes[0] else partes[1]
            val nombre = if (primeroEsHandle) partes[1] else partes[0]
            return Descripcion(nombre, handle, porEnlace = false)
        }
        return Descripcion(entrada, "", porEnlace = false)
    }

    /**
     * Si este usuario ya está en la lista, devuelve su entrada; si no, null. Se compara por el
     * id de la URL del perfil, por el @ y, en las entradas que son solo un nombre, por el nombre.
     */
    @JvmStatic
    fun repetida(entradas: List<String>, id: String, handle: String, nombre: String): String? {
        val h = handle.lowercase()
        return entradas.firstOrNull { e ->
            val uuid = UUID.find(e)?.value
            if (uuid != null) return@firstOrNull uuid.equals(id, ignoreCase = true)
            val partes = e.split(SEPARADOR).map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            val mismoHandle = h.isNotEmpty() && partes.any { it == h || "@$it" == h }
            // "@usuario / Nombre": vale el @ (puede haber dos cuentas con el mismo nombre visible)
            if (partes.size != 1) mismoHandle else mismoHandle || partes[0] == nombre.lowercase()
        }
    }

    /**
     * Los [fijos] primero y luego los de la [lista], sin repetir a nadie que ya esté entre los
     * fijos (mismo id de perfil o mismo texto).
     */
    @JvmStatic
    fun conFijos(fijos: List<String>, lista: List<String>): List<String> {
        val ids = fijos.mapNotNull { UUID.find(it)?.value?.lowercase() }.toSet()
        val textos = fijos.map { it.lowercase() }.toSet()
        return fijos + lista.filter { e ->
            val id = UUID.find(e)?.value?.lowercase()
            e.lowercase() !in textos && (id == null || id !in ids)
        }
    }

    /** Añade una entrada al final de la lista y la guarda (si ya estaba igual, no hace nada). */
    @JvmStatic
    fun agregar(archivo: Path, entrada: String) {
        val actuales = paraEditar(archivo)
        if (actuales.none { it.equals(entrada, ignoreCase = true) }) guardar(archivo, actuales + entrada)
    }

    /** Quita una entrada de la lista y la guarda. */
    @JvmStatic
    fun quitar(archivo: Path, entrada: String) {
        val actuales = paraEditar(archivo)
        val quedan = actuales.filterNot { it.equals(entrada, ignoreCase = true) }
        if (quedan.size != actuales.size) guardar(archivo, quedan)
    }

    /**
     * Guarda la lista en usuarios.json, reescribiéndola entera con las instrucciones arriba.
     * Antes, si todavía no existe usuarios.json.bak, copia ahí el archivo tal como estaba,
     * porque al reescribirlo se pierden los comentarios que tuviera.
     */
    @JvmStatic
    fun guardar(archivo: Path, entradas: List<String>) {
        val copia = copiaDe(archivo)
        if (Files.exists(archivo) && !Files.exists(copia)) Files.copy(archivo, copia)
        val texto = buildString {
            append(CABECERA)
            entradas.forEachIndexed { i, e ->
                append("  ").append(JSON_LISTA.writeValueAsString(e))
                if (i < entradas.size - 1) append(',')
                append('\n')
            }
            append("]\n")
        }
        // Se escribe en un archivo aparte y luego se cambia por el bueno: si algo falla a medias,
        // usuarios.json no se queda cortado
        val temporal = archivo.resolveSibling(archivo.fileName.toString() + ".tmp")
        Files.writeString(temporal, texto, Charsets.UTF_8)
        try {
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /** Dónde se guarda la copia de seguridad: usuarios.json.bak, junto a usuarios.json. */
    @JvmStatic
    fun copiaDe(archivo: Path): Path = archivo.resolveSibling(archivo.fileName.toString() + ".bak")

    /** Las entradas actuales, leídas otra vez del archivo (por si se ha tocado a mano). */
    private fun paraEditar(archivo: Path): List<String> {
        val l = leer(archivo)
        if (l.error != null) {
            throw AgenteException(
                "No toco usuarios.json porque tiene un error. Arréglalo a mano y pulsa Recargar.\n" + l.error,
            )
        }
        return l.entradas
    }
}
