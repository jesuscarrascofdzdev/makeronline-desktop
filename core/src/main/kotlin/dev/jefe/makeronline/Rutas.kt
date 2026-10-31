package dev.jefe.makeronline

import java.nio.file.Path

/**
 * Dónde guarda el programa cada cosa.
 *
 * Los programas de terminal usan la carpeta desde la que se lanzan, como siempre
 * ([carpetaActual]). La app de escritorio usará la carpeta de datos del usuario (fase 4)
 * y dejará elegir dónde van las descargas y las subidas.
 */
class Rutas @JvmOverloads constructor(
    /** Carpeta base: sesión de Chrome, candado, usuarios.json y descargados.txt. */
    val datos: Path,
    /** Donde se guardan los modelos descargados: <descargas>/<usuario>/<título>_<id>/. */
    val descargas: Path = datos.resolve("modelos-descargados"),
    /** Carpeta de subidas, con pendientes/, subidos/ y errores/ dentro. */
    val subidas: Path = datos.resolve("subidas"),
) {
    /** Perfil de Chrome con la sesión de MakerOnline. Nunca sale de este ordenador. */
    val perfilChrome: Path get() = datos.resolve("chrome-profile")

    /** Candado que impide que dos programas usen Chrome a la vez (ver AgentLock). */
    val candado: Path get() = datos.resolve(".agente.lock")

    /** Lista de usuarios del Downloader. */
    val usuarios: Path get() = datos.resolve("usuarios.json")

    /** Ajustes de la app de escritorio que se recuerdan de una vez para otra. */
    val ajustes: Path get() = datos.resolve("ajustes.properties")

    /**
     * Un id de modelo por línea. Segunda protección además del like: aunque el like
     * falle o se vacíe la carpeta de descargas, lo que está aquí no se vuelve a descargar.
     * Está fuera de la carpeta de descargas para que no se pierda al vaciarla.
     * Para volver a descargar un modelo, se borra su línea.
     */
    val registro: Path get() = datos.resolve("descargados.txt")

    /** Donde estaba antes el registro: si existe, se pasa a [registro]. */
    val registroAntiguo: Path get() = descargas.resolve("descargados.txt")

    /** Modelos preparados para subir. */
    val pendientes: Path get() = subidas.resolve("pendientes")

    /** Modelos ya subidos como borrador. */
    val subidos: Path get() = subidas.resolve("subidos")

    /** Modelos que no se han podido subir, con error.txt y una captura. */
    val errores: Path get() = subidas.resolve("errores")

    companion object {
        /** La carpeta desde la que se lanza el programa: lo de siempre en la versión de terminal. */
        @JvmStatic
        fun carpetaActual(): Rutas = Rutas(Path.of(""))
    }
}
