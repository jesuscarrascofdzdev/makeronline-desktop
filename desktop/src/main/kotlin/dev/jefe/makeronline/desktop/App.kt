package dev.jefe.makeronline.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** La ventana: menú a la izquierda y la pantalla elegida a la derecha. */
@Composable
fun App(app: EstadoApp) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                Menu(actual = app.pantalla, alElegir = { app.pantalla = it })
                // línea de separación
                Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (app.pantalla) {
                        Pantalla.SESION -> PantallaSesion(app.sesion, app.rutas.datos.toString())
                        Pantalla.DESCARGAR -> PantallaPronto(Textos.DESCARGAR_PRONTO)
                    }
                }
            }
        }
    }
}

@Composable
private fun Menu(actual: Pantalla, alElegir: (Pantalla) -> Unit) {
    Column(
        Modifier.width(210.dp).fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            Textos.TITULO_APP,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 16.dp),
        )
        for (p in Pantalla.entries) {
            if (p == actual) {
                FilledTonalButton(onClick = { alElegir(p) }, modifier = Modifier.fillMaxWidth()) { Text(p.titulo) }
            } else {
                TextButton(onClick = { alElegir(p) }, modifier = Modifier.fillMaxWidth()) { Text(p.titulo) }
            }
        }
    }
}

/** Para las pantallas que todavía no están hechas. */
@Composable
fun PantallaPronto(texto: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Un recuadro con el contenido en columna. */
@Composable
fun Tarjeta(contenido: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = contenido)
    }
}
