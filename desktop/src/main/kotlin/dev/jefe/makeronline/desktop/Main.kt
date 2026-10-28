package dev.jefe.makeronline.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.jefe.makeronline.Browsers

/** Fase 0: ventana de prueba para comprobar que Compose y el módulo core funcionan. */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = Textos.TITULO_APP,
        state = rememberWindowState(width = 640.dp, height = 400.dp),
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(Textos.TITULO_APP, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    Text(Textos.VENTANA_OK)
                    Spacer(Modifier.height(8.dp))
                    Text(Textos.coreConectado(Browsers.PROFILE_DIR.toString()))
                }
            }
        }
    }
}
