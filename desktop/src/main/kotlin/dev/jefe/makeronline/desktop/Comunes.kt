package dev.jefe.makeronline.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.nio.file.Path
import javax.swing.JFileChooser

// Piezas que usan varias pantallas.

/** Los mensajes de lo que va haciendo core, siempre con lo último a la vista. */
@Composable
fun ListaMensajes(mensajes: List<Evento.Mensaje>, modifier: Modifier) {
    val lista = rememberLazyListState()
    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) lista.scrollToItem(mensajes.size - 1)
    }
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Text(Textos.MENSAJES, style = MaterialTheme.typography.titleSmall)
            if (mensajes.isEmpty()) {
                Text(
                    Textos.SIN_MENSAJES,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(Modifier.fillMaxSize(), state = lista) {
                items(mensajes) { m ->
                    val color = when (m.tipo) {
                        TipoMensaje.ERROR, TipoMensaje.AVISO -> MaterialTheme.colorScheme.error
                        TipoMensaje.CORRECTO, TipoMensaje.LIKE -> VERDE
                        TipoMensaje.NOTA, TipoMensaje.SALTADO -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    val texto = m.texto.trim('\n', '\r')
                    Text(
                        if (m.tipo.simbolo.isEmpty()) texto else m.tipo.simbolo + " " + texto,
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
            }
        }
    }
}

/** Un botón redondo de "elegir una opción" con su texto. */
@Composable
fun Eleccion(texto: String, elegida: Boolean, activa: Boolean, alElegir: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = elegida, onClick = alElegir, enabled = activa)
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Una casilla de marcar con su texto. */
@Composable
fun Casilla(texto: String, marcada: Boolean, activa: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = marcada, onCheckedChange = alCambiar, enabled = activa)
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Ventana de Windows para elegir una o varias carpetas. Devuelve las elegidas (vacía si se cancela). */
fun elegirCarpetas(titulo: String): List<Path> {
    val selector = JFileChooser().apply {
        dialogTitle = titulo
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isMultiSelectionEnabled = true
    }
    if (selector.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return emptyList()
    val elegidas = selector.selectedFiles.takeIf { it.isNotEmpty() } ?: arrayOf(selector.selectedFile)
    return elegidas.filterNotNull().map { it.toPath() }
}
