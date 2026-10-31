package dev.jefe.makeronline.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

/** Verde y rojo para los ✔ y ✘ (se leen bien en tema claro). */
private val VERDE = Color(0xFF1B7F3A)

@Composable
fun PantallaSesion(s: EstadoSesion, carpetaDatos: String) {
    val scope = rememberCoroutineScope()

    // La primera vez que se abre la pantalla, se comprueba la sesión
    LaunchedEffect(Unit) {
        if (s.estado == null) s.comprobar()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(Textos.SESION_TITULO, style = MaterialTheme.typography.headlineSmall)
        Text(Textos.SESION_EXPLICACION, style = MaterialTheme.typography.bodyMedium)

        val e = s.estado

        // --- Chrome ---
        Tarjeta {
            Text(Textos.CHROME, style = MaterialTheme.typography.titleMedium)
            when {
                e == null -> Text(if (s.ocupado) Textos.COMPROBANDO else Textos.SIN_COMPROBAR)
                e.chromeInstalado -> Text("✔ " + Textos.CHROME_OK, color = VERDE)
                else -> {
                    Text("✘ " + Textos.CHROME_FALTA, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { abrirEnNavegador(Textos.URL_CHROME) }) { Text(Textos.DESCARGAR_CHROME) }
                }
            }
        }

        // --- MakerOnline ---
        Tarjeta {
            Text(Textos.MAKERONLINE, style = MaterialTheme.typography.titleMedium)
            when {
                e == null -> Text(if (s.ocupado) Textos.COMPROBANDO else Textos.SIN_COMPROBAR)
                e.iniciada -> Text(
                    "✔ " + (e.nombre?.let { Textos.sesionIniciadaComo(it) } ?: Textos.SESION_INICIADA),
                    color = VERDE,
                )
                else -> Text("✘ " + Textos.sinSesion(e.motivo), color = MaterialTheme.colorScheme.error)
            }
        }

        // --- Acciones ---
        if (s.esperandoLogin) {
            Tarjeta {
                Text(Textos.INSTRUCCIONES_LOGIN, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(enabled = !s.ocupado, onClick = { scope.launch { s.yaHeEntrado() } }) {
                        Text(Textos.YA_HE_ENTRADO)
                    }
                    OutlinedButton(enabled = !s.ocupado, onClick = { scope.launch { s.cancelarInicio() } }) {
                        Text(Textos.CANCELAR)
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = !s.ocupado && e?.chromeInstalado != false,
                    onClick = { scope.launch { s.abrirParaIniciar() } },
                ) {
                    Text(if (e?.iniciada == true) Textos.VOLVER_A_INICIAR else Textos.INICIAR_SESION)
                }
                OutlinedButton(enabled = !s.ocupado, onClick = { scope.launch { s.comprobar() } }) {
                    Text(Textos.COMPROBAR)
                }
            }
        }

        if (s.ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Text(
            Textos.carpetaDatos(carpetaDatos),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Abre una dirección en el navegador normal de la persona. */
private fun abrirEnNavegador(url: String) {
    try {
        Desktop.getDesktop().browse(URI(url))
    } catch (_: Exception) {
        // sin navegador por defecto: no pasa nada
    }
}
