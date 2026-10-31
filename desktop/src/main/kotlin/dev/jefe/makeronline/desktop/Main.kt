package dev.jefe.makeronline.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import javax.swing.UIManager

fun main() {
    // Para que la ventana de elegir carpetas se vea como las de Windows
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (_: Exception) {
        // si no se puede, se ve con el aspecto de Java: funciona igual
    }
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
