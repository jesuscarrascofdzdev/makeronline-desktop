package dev.jefe.makeronline.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {
    val app = EstadoApp(EstadoApp.rutas())
    application {
        Window(
            onCloseRequest = {
                app.cerrar() // cierra Chrome si estaba abierto
                exitApplication()
            },
            title = Textos.TITULO_APP,
            state = rememberWindowState(width = 1100.dp, height = 760.dp),
        ) {
            App(app)
        }
    }
}
