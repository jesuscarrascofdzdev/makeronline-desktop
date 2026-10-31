package dev.jefe.makeronline.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import dev.jefe.makeronline.ListaUsuarios
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Ventana para añadir y quitar usuarios de la lista (botón "Editar" de la pantalla Descargar). */
@Composable
fun DialogoUsuarios(e: EstadoEdicion, alcance: CoroutineScope) {
    val puedeAgregar = e.libre && !e.buscando && e.texto.isNotBlank()
    fun agregar() {
        if (puedeAgregar) alcance.launch { e.agregar() }
    }

    DialogWindow(
        onCloseRequest = { e.cerrar() },
        state = rememberDialogState(width = 640.dp, height = 700.dp),
        title = Textos.EDITAR_TITULO,
    ) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(Textos.EDITAR_TITULO, style = MaterialTheme.typography.headlineSmall)
                Text(Textos.EDITAR_EXPLICACION, style = MaterialTheme.typography.bodyMedium)

                // --- Añadir ---
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = e.texto,
                        onValueChange = { e.texto = it },
                        label = { Text(Textos.CAMPO_AGREGAR) },
                        singleLine = true,
                        enabled = !e.buscando,
                        modifier = Modifier.weight(1f).onPreviewKeyEvent {
                            // ENTER en el campo hace lo mismo que el botón "Añadir"
                            if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyDown) {
                                agregar()
                                true
                            } else {
                                false
                            }
                        },
                    )
                    Button(enabled = puedeAgregar, onClick = { agregar() }) { Text(Textos.AGREGAR) }
                }
                if (e.buscando) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(Textos.BUSCANDO_USUARIO, style = MaterialTheme.typography.bodySmall)
                } else if (!e.libre) {
                    Text(
                        Textos.chromeOcupado(e.tarea ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                e.aviso?.let { Text(it, color = VERDE) }
                e.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                // --- La lista ---
                Text(
                    Textos.usuariosTitulo(e.usuarios.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Card(Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(Modifier.fillMaxSize().padding(vertical = 4.dp)) {
                        item { FilaCreador() }
                        if (e.usuarios.isEmpty()) {
                            item {
                                Text(
                                    Textos.SIN_USUARIOS,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }
                        items(e.usuarios) { entrada -> FilaUsuario(e, entrada) }
                    }
                }

                Text(
                    Textos.NOTA_COPIA,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { e.cerrar() }) { Text(Textos.CERRAR) }
                }
            }
        }
    }
}

/** El creador de la app: siempre el primero y sin botón de quitar (ver [Creador]). */
@Composable
private fun FilaCreador() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(Creador.NOMBRE, style = MaterialTheme.typography.bodyMedium)
        Text(
            Textos.CREADOR_FIJO,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Un usuario de la lista con su botón "Quitar" (que pide confirmación). */
@Composable
private fun FilaUsuario(e: EstadoEdicion, entrada: String) {
    val desc = ListaUsuarios.describir(entrada)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(desc.nombre, style = MaterialTheme.typography.bodyMedium)
            if (desc.detalle.isNotEmpty()) {
                Text(
                    desc.detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (e.aQuitar == entrada) {
            Text(Textos.QUITAR_PREGUNTA, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { e.quitar(entrada) }) {
                Text(Textos.SI_QUITAR, color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = { e.aQuitar = null }) { Text(Textos.NO) }
        } else {
            TextButton(enabled = !e.buscando, onClick = { e.aQuitar = entrada }) { Text(Textos.QUITAR) }
        }
    }
}
