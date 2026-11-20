package com.terraaxis.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.terraaxis.app.viewmodel.MainViewModel
import com.terraaxis.map.render.MapCanvasRenderer

/**
 * Vista de Composición Cartográfica (Layout View) de ArcMap 10.8:
 * Renderiza el mapa sobre una lámina de impresión con membrete, título,
 * leyenda automática de capas, escala gráfica y flecha de norte.
 */
@Composable
fun ArcMapLayoutView(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val layers by viewModel.layers.collectAsState()
    val layoutTitle by viewModel.layoutTitle.collectAsState()
    val renderer = remember { MapCanvasRenderer() }
    var showTitleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF808080)) // Fondo gris de escritorio típico de ArcMap Layout
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Barra de control de Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEBE9E4), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFFB0B0B0), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Print,
                    contentDescription = null,
                    tint = Color(0xFF0079C1),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Layout View (Lámina A4)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = { showTitleDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Título", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = {
                        viewModel.updateRender()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0079C1)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exportar", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Hoja de papel (Paper Canvas)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(2.dp))
                .background(Color.White)
                .border(1.dp, Color.DarkGray)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Título Cartográfico
                Text(
                    text = layoutTitle.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "GOBIERNO REGIONAL / SISTEMA DE INFORMACIÓN GEOGRÁFICA",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Marco del mapa (Data Frame)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.5.dp, Color.Black)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (size.width > 0 && size.height > 0) {
                            viewModel.viewport.screenWidth = size.width
                            viewModel.viewport.screenHeight = size.height
                        }
                        drawIntoCanvas { canvas ->
                            renderer.render(
                                canvas = canvas.nativeCanvas,
                                viewport = viewModel.viewport,
                                layers = layers
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Membrete / Leyenda y Metadatos inferiores
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Black)
                        .background(Color(0xFFFAFAFA))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Leyenda compacta
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "LEYENDA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        layers.filter { it.isVisible }.take(4).forEach { layer ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF0079C1))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${layer.name} (${layer.features.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Cajetín Institucional
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.width(160.dp)
                    ) {
                        Text("PROYECCIÓN: WGS 84 / UTM 19S", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("FUENTE: ArcMap 10.8 Esri Mobile", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        Text("ESCALA: 1:${viewModel.mapScale.collectAsState().value}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                    }
                }
            }
        }
    }

    if (showTitleDialog) {
        var newTitle by remember { mutableStateOf(layoutTitle) }
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            title = { Text("Editar Título del Mapa") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Título de la lámina") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setLayoutTitle(newTitle)
                    showTitleDialog = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTitleDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
