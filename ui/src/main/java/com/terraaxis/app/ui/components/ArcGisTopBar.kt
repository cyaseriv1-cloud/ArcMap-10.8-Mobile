package com.terraaxis.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.terraaxis.app.viewmodel.MainViewModel
import com.terraaxis.map.interaction.MapToolMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcGisTopBar(
    viewModel: MainViewModel,
    onOpenToc: () -> Unit,
    onOpenToolbox: () -> Unit
) {
    val activeTool by viewModel.activeTool.collectAsState()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "TerraAxis",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "GIS Mobile",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenToc) {
                        Icon(Icons.Default.Menu, contentDescription = "Tabla de Contenidos (TOC)")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.zoomFullExtent() }) {
                        Icon(Icons.Default.ZoomOutMap, contentDescription = "Extensión Completa")
                    }
                    IconButton(onClick = { viewModel.zoomIn() }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Acercar")
                    }
                    IconButton(onClick = { viewModel.zoomOut() }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Alejar")
                    }
                    IconButton(
                        onClick = onOpenToolbox,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Build, contentDescription = "ArcToolbox")
                    }
                }
            )

            // Tools Bar Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = activeTool == MapToolMode.PAN,
                    onClick = { viewModel.setTool(MapToolMode.PAN) },
                    label = { Text("Pan") },
                    leadingIcon = { Icon(Icons.Default.PanTool, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = activeTool == MapToolMode.IDENTIFY,
                    onClick = { viewModel.setTool(MapToolMode.IDENTIFY) },
                    label = { Text("Identificar (i)") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = activeTool == MapToolMode.SELECT,
                    onClick = { viewModel.setTool(MapToolMode.SELECT) },
                    label = { Text("Selección") },
                    leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = activeTool == MapToolMode.MEASURE_DISTANCE,
                    onClick = { viewModel.setTool(MapToolMode.MEASURE_DISTANCE) },
                    label = { Text("Medir Distancia") },
                    leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = activeTool == MapToolMode.MEASURE_AREA,
                    onClick = { viewModel.setTool(MapToolMode.MEASURE_AREA) },
                    label = { Text("Medir Área") },
                    leadingIcon = { Icon(Icons.Default.SquareFoot, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = activeTool == MapToolMode.DIGITIZE_POINT,
                    onClick = { viewModel.setTool(MapToolMode.DIGITIZE_POINT) },
                    label = { Text("+ Punto") },
                    leadingIcon = { Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = activeTool == MapToolMode.DIGITIZE_LINE,
                    onClick = { viewModel.setTool(MapToolMode.DIGITIZE_LINE) },
                    label = { Text("+ Línea") },
                    leadingIcon = { Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = activeTool == MapToolMode.DIGITIZE_POLYGON,
                    onClick = { viewModel.setTool(MapToolMode.DIGITIZE_POLYGON) },
                    label = { Text("+ Polígono") },
                    leadingIcon = { Icon(Icons.Default.Polyline, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}
