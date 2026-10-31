package dev.jefe.makeronline.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.jefe.makeronline.AgenteException
import dev.jefe.makeronline.Rutas
import dev.jefe.makeronline.Sesion
import kotlin.coroutines.cancellation.CancellationException

/**
 * Lo que enseña la pantalla Sesión y lo que hacen sus botones.
 *
 * Las funciones `suspend` se llaman desde la ventana: el trabajo con Chrome va al hilo del
 * navegador y, al volver, el estado se cambia ya en el hilo de la ventana.
 */
class EstadoSesion(
    rutas: Rutas,
    private val hilo: HiloNavegador,
    private val ocupacion: Ocupacion,
) {
    private val sesion = Sesion(rutas)

    /** null = todavía sin comprobar. */
    var estado by mutableStateOf<Sesion.Estado?>(null)
        private set

    /** true mientras esta pantalla tiene a Chrome trabajando (para la barra de progreso). */
    var ocupado by mutableStateOf(false)
        private set

    /** Un error para enseñar en rojo, o null. */
    var error by mutableStateOf<String?>(null)
        private set

    /** true mientras Chrome está abierto esperando a que la persona inicie sesión. */
    var esperandoLogin by mutableStateOf(false)
        private set

    /** Si se pueden pulsar los botones: Chrome no está haciendo otra cosa (por ejemplo, descargar). */
    val libre: Boolean get() = ocupacion.libre

    /** Chrome abierto para iniciar sesión. Solo se toca desde el hilo del navegador. */
    @Volatile
    private var abierta: Sesion.Abierta? = null

    suspend fun comprobar() {
        if (!ocupacion.empezar(Textos.TAREA_COMPROBANDO)) return
        ocupado = true
        error = null
        try {
            estado = hilo.ejecutar { sesion.comprobar(ProgresoNulo) }
        } catch (e: CancellationException) {
            throw e // la app se está cerrando: no es un error
        } catch (e: Exception) {
            error = mensajeDe(e)
        } finally {
            ocupado = false
            ocupacion.terminar()
        }
    }

    suspend fun abrirParaIniciar() {
        if (!ocupacion.empezar(Textos.TAREA_INICIANDO_SESION)) return
        ocupado = true
        error = null
        try {
            hilo.ejecutar { abierta = sesion.abrirParaIniciar(ProgresoNulo) }
            esperandoLogin = true // Chrome sigue ocupado hasta que se cierre (ver cerrarChrome)
        } catch (e: CancellationException) {
            throw e // la app se está cerrando: no es un error
        } catch (e: Exception) {
            error = mensajeDe(e)
            ocupacion.terminar()
        } finally {
            ocupado = false
        }
    }

    /** La persona dice que ya ha entrado: se cierra Chrome (así se guarda la sesión) y se comprueba. */
    suspend fun yaHeEntrado() {
        cerrarChrome()
        comprobar()
    }

    suspend fun cancelarInicio() {
        cerrarChrome()
        comprobar()
    }

    fun alCerrarApp() {
        hilo.alCerrar { cerrarAbierta() }
    }

    private suspend fun cerrarChrome() {
        ocupado = true
        try {
            hilo.ejecutar { cerrarAbierta() }
        } catch (_: Exception) {
            // si ya estaba cerrado, no pasa nada
        } finally {
            ocupado = false
            esperandoLogin = false
            ocupacion.terminar()
        }
    }

    private fun cerrarAbierta() {
        abierta?.close()
        abierta = null
    }

    private fun mensajeDe(e: Exception): String? =
        if (e is AgenteException) e.message else Textos.errorInesperado(e) // p. ej. otro programa usa Chrome
}
