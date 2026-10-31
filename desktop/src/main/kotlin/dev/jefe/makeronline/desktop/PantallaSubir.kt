package dev.jefe.makeronline.desktop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.unit.dp
import dev.jefe.makeronline.Rutas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun PantallaSubir(s: EstadoSubida, rutas: Rutas, alcance: CoroutineScope) {
    // Cada vez que se abre la pantalla se vuelve a mirar pendientes/ (por si has cambiado algo)
    LaunchedEffect(Unit) {
        if (!s.enMarcha) s.cargar()
    }

    // Arrastrar carpetas desde el Explorador a cualquier parte de la pantalla
    var encima by remember { mutableStateOf(false) }
    val destino = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                encima = true
            }

            override fun onExited(event: DragAndDropEvent) {
                encima = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                encima = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                encima = false
                val datos = event.awtTransferable
                if (!datos.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false
                val archivos = (datos.getTransferData(DataFlavor.javaFileListFlavor) as List<*>)
                    .filterIsInstance<File>().map { it.toPath() }
                alcance.launch { s.agregarCarpetas(archivos) }
                return true
            }
        }
    }

    Column(
        Modifier.fillMaxSize()
            .dragAndDropTarget(
                shouldStartDragAndDrop = { it.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) },
                target = destino,
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(Textos.SUBIR_TITULO, style = MaterialTheme.typography.headlineSmall)
        Text(Textos.SUBIR_EXPLICACION, style = MaterialTheme.typography.bodyMedium)

        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // --- Izquierda: pendientes y opciones ---
            Column(
                Modifier.width(400.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ListaPendientes(s, rutas, encima, alcance)
                Tarjeta {
                    Text(Textos.OPCIONES, style = MaterialTheme.typography.titleMedium)
                    Casilla(Textos.REVISAR, s.revisar, !s.enMarcha) { s.revisar = it }
                    Text(
                        Textos.REVISAR_AYUDA,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // --- Derecha: botones, revisión, progreso y mensajes ---
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonesSubir(s, alcance)
                PanelRevision(s)
                PanelProgresoSubida(s)
                PanelResumenSubida(s, rutas)
                s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                ListaMensajes(s.mensajes, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ListaPendientes(s: EstadoSubida, rutas: Rutas, encima: Boolean, alcance: CoroutineScope) {
    val editable = !s.enMarcha
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Textos.pendientesTitulo(s.carpetas.size), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            TextButton(enabled = editable, onClick = { s.cargar() }) { Text(Textos.RECARGAR) }
            TextButton(onClick = { abrir(rutas.pendientes) }) { Text(Textos.ABRIR_PENDIENTES) }
        }

        // Zona para soltar carpetas (también vale soltarlas en cualquier parte de la pantalla)
        Column(
            Modifier.fillMaxWidth()
                .border(
                    if (encima) 2.dp else 1.dp,
                    if (encima) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (encima) Textos.SUELTA_AHORA else Textos.SOLTAR_AQUI,
                style = MaterialTheme.typography.bodySmall,
                color = if (encima) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(enabled = !s.copiando, onClick = {
                val elegidas = elegirCarpetas(Textos.ELEGIR_CARPETAS)
                if (elegidas.isNotEmpty()) alcance.launch { s.agregarCarpetas(elegidas) }
            }) { Text(Textos.ANADIR_CARPETAS) }
        }
        if (s.copiando) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(Textos.COPIANDO, style = MaterialTheme.typography.bodySmall)
        }
        s.avisoCarpetas?.let { Text(it, color = VERDE, style = MaterialTheme.typography.bodySmall) }
        s.errorCarpetas?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        if (s.carpetas.isEmpty()) Text(Textos.NADA_EN_PENDIENTES, style = MaterialTheme.typography.bodySmall)
        for (c in s.carpetas) {
            val elegida = c.nombre == s.seleccionada
            Column(
                Modifier.fillMaxWidth()
                    .background(if (elegida) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                    .clickable(enabled = editable) { s.seleccionada = if (elegida) null else c.nombre }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(
                    (if (c.valida) "✔ " else "✘ ") + c.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (c.valida) VERDE else MaterialTheme.colorScheme.error,
                )
                if (c.valida) {
                    Text(
                        Textos.LISTA_PARA_SUBIR,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                for (p in c.problemas) {
                    Text("· $p", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                for (a in c.avisos) {
                    Text(
                        "⚠ $a",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Eleccion(Textos.todasLasValidas(s.carpetas.count { it.valida }), !s.soloSeleccionada, editable) {
            s.soloSeleccionada = false
        }
        Eleccion(Textos.SOLO_LA_SELECCIONADA, s.soloSeleccionada, editable && s.seleccionada != null) {
            s.soloSeleccionada = true
        }
    }
}

@Composable
private fun BotonesSubir(s: EstadoSubida, alcance: CoroutineScope) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(enabled = s.libre && !s.copiando, onClick = { alcance.launch { s.subir() } }) {
            Text(Textos.SUBIR)
        }
        Button(
            enabled = s.enMarcha && !s.parando,
            onClick = { s.parar() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
            Text(Textos.PARAR)
        }
        Text(
            Textos.SUBIR_AYUDA,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Con "Revisar" marcado: el modelo espera a que lo mires en Chrome y elijas. */
@Composable
private fun PanelRevision(s: EstadoSubida) {
    val modelo = s.porRevisar ?: return
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "⏸ " + Textos.revisarAhora(modelo),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { s.guardarBorrador() }) { Text(Textos.GUARDAR_BORRADOR) }
                OutlinedButton(onClick = { s.descartar() }) { Text(Textos.DESCARTAR) }
            }
        }
    }
}

@Composable
private fun PanelProgresoSubida(s: EstadoSubida) {
    if (!s.enMarcha) return
    Tarjeta {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        s.modeloActual?.let { Text(Textos.modeloActual(it), style = MaterialTheme.typography.titleSmall) }
        if (s.parando) Text(Textos.PARANDO_SUBIDA, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun PanelResumenSubida(s: EstadoSubida, rutas: Rutas) {
    val r = s.resumen ?: return
    Tarjeta {
        if (r.parado()) Text(Textos.RESUMEN_SUBIDA_PARADA, style = MaterialTheme.typography.titleSmall)
        Text(Textos.resumenSubida(r.subidos(), r.conError(), r.invalidos(), r.sinSubir()))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (r.subidos() > 0) TextButton(onClick = { abrir(rutas.subidos) }) { Text(Textos.ABRIR_SUBIDOS) }
            if (r.carpetasConError().isNotEmpty()) {
                TextButton(onClick = { abrir(rutas.errores) }) { Text(Textos.ABRIR_ERRORES) }
            }
        }
        for (carpeta in r.carpetasConError()) {
            // La captura si la hay (falló al subir); si no, el error.txt (no era válida)
            val captura = carpeta.resolve("error.png")
            val archivo = if (Files.exists(captura)) captura else carpeta.resolve("error.txt")
            TextButton(onClick = { abrir(archivo) }) { Text(Textos.verError(carpeta.fileName.toString())) }
        }
    }
}
