package dev.jefe.makeronline.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.jefe.makeronline.AgenteException
import dev.jefe.makeronline.Rutas
import dev.jefe.makeronline.Sesion

/**
 * Lo que enseña la pantalla Sesión y lo que hacen sus botones.
 *
 * Las funciones `suspend` se llaman desde la ventana: el trabajo con Chrome va al hilo del
 * navegador y, al volver, el estado se cambia ya en el hilo de la ventana.
 */
class EstadoSesion(rutas: Rutas, private val hilo: HiloNavegador) {

    private val sesion = Sesion(rutas)

    /** null = todavía sin comprobar. */
    var estado by mutableStateOf<Sesion.Estado?>(null)
        private set

    /** true mientras Chrome está trabajando: los botones se desactivan. */
    var ocupado by mutableStateOf(false)
        private set

    /** Un error para enseñar en rojo, o null. */
    var error by mutableStateOf<String?>(null)
        private set

    /** true mientras Chrome está abierto esperando a que la persona inicie sesión. */
    var esperandoLogin by mutableStateOf(false)
        private set

    /** Chrome abierto para iniciar sesión. Solo se toca desde el hilo del navegador. */
    @Volatile
    private var abierta: Sesion.Abierta? = null

    suspend fun comprobar() {
        if (ocupado) return
        trabajar { estado = hilo.ejecutar { sesion.comprobar(ProgresoNulo) } }
    }

    suspend fun abrirParaIniciar() {
        if (ocupado) return
        trabajar {
            hilo.ejecutar { abierta = sesion.abrirParaIniciar(ProgresoNulo) }
            esperandoLogin = true
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
        trabajar { hilo.ejecutar { cerrarAbierta() } }
        esperandoLogin = false
    }

    private fun cerrarAbierta() {
        abierta?.close()
        abierta = null
    }

    /** Marca la pantalla como ocupada mientras dura [bloque] y convierte los fallos en un mensaje. */
    private suspend fun trabajar(bloque: suspend () -> Unit) {
        ocupado = true
        error = null
        try {
            bloque()
        } catch (e: AgenteException) {
            error = e.message // por ejemplo: otro programa está usando Chrome
        } catch (e: Exception) {
            error = Textos.errorInesperado(e)
        } finally {
            ocupado = false
        }
    }
}
