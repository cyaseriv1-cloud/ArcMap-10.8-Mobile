package com.terraaxis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.terraaxis.app.viewmodel.MainViewModel
import com.terraaxis.core.geometry.BoundingBox
import com.terraaxis.core.geometry.GeometryType
import com.terraaxis.core.model.VectorLayer
import com.terraaxis.core.symbology.LayerSymbology

@Composable
fun TableOfContentsDrawer(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val layers by viewModel.layers.collectAsState()
    val activeLayer by viewModel.activeLayer.collectAsState()

    ModalDrawerSheet(
        modifier = Modifier.width(340.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Tabla de Contenidos",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Text(
                "Capas y Simbología (${layers.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Layers List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(layers) { index, layer ->
                    val isActive = activeLayer?.id == layer.id

                    LayerItemCard(
                        layer = layer,
                        isActive = isActive,
                        isFirst = index == 0,
                        isLast = index == layers.size - 1,
                        onSelectLayer = { viewModel.setActiveLayer(layer) },
                        onToggleVisibility = { viewModel.toggleLayerVisibility(layer.id) },
                        onOpenTable = {
                            viewModel.openAttributeTable(layer)
                            onClose()
                        },
                        onZoomToLayer = {
                            if (layer.boundingBox != BoundingBox.EMPTY) {
                                viewModel.viewport.zoomToExtent(layer.boundingBox)
                                viewModel.updateCoordinates(viewModel.viewport.centerLon, viewModel.viewport.centerLat)
                                viewModel.updateRender()
                            }
                        },
                        onMoveUp = { viewModel.moveLayerUp(layer.id) },
                        onMoveDown = { viewModel.moveLayerDown(layer.id) },
                        onOpacityChange = { newOp -> viewModel.setLayerOpacity(layer.id, newOp) },
                        onDeleteLayer = { viewModel.removeLayer(layer.id) },
                        viewModel = viewModel
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Bottom action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.loadSampleData() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reiniciar", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = {
                        // Create a new blank vector layer for digitizing
                        val newLayer = VectorLayer(
                            name = "Capa Nueva ${layers.size + 1}",
                            geometryType = GeometryType.POLYGON
                        )
                        viewModel.addLayer(newLayer)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nueva Capa", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun LayerItemCard(
    layer: VectorLayer,
    isActive: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onSelectLayer: () -> Unit,
    onToggleVisibility: () -> Unit,
    onOpenTable: () -> Unit,
    onZoomToLayer: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onDeleteLayer: () -> Unit,
    viewModel: MainViewModel
) {
    var expandedControls by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }

    val cardColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectLayer),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Main Layer Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = layer.isVisible,
                    onCheckedChange = { onToggleVisibility() },
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Symbology preview swatch
                SymbologySwatch(layer = layer)

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        layer.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                        maxLines = 2
                    )
                    Text(
                        "${layer.features.size} entidades • ${layer.geometryType.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(
                    onClick = { expandedControls = !expandedControls },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (expandedControls) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expandir opciones",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Quick Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Propiedades / Simbología
                IconButton(onClick = { showPropertiesDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "Propiedades de Capa",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onOpenTable, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.TableChart,
                        contentDescription = "Tabla de Atributos",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onZoomToLayer, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.CenterFocusStrong,
                        contentDescription = "Zoom a capa",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Subir capa",
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = "Bajar capa",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Expanded Controls: Opacity & Delete
            if (expandedControls) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // Opacity Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Opacidad: ${(layer.opacity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(90.dp)
                    )
                    Slider(
                        value = layer.opacity,
                        onValueChange = { onOpacityChange(it) },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Delete Layer Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDeleteLayer,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar Capa", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    // Diálogo de Propiedades / Simbología
    if (showPropertiesDialog) {
        LayerPropertiesDialog(
            layer = layer,
            viewModel = viewModel,
            onDismiss = { showPropertiesDialog = false }
        )
    }
}

@Composable
private fun SymbologySwatch(layer: VectorLayer) {
    val symbology = layer.symbology
    val primaryColor = when (symbology) {
        is LayerSymbology.Simple -> Color(symbology.fillColor.toArgbInt())
        is LayerSymbology.UniqueValues -> Color(symbology.valueColors.values.firstOrNull()?.toArgbInt() ?: 0xFF2196F3.toInt())
    }
    val strokeColor = when (symbology) {
        is LayerSymbology.Simple -> Color(symbology.strokeColor.toArgbInt())
        is LayerSymbology.UniqueValues -> Color(symbology.strokeColor.toArgbInt())
    }

    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (layer.geometryType) {
            GeometryType.POINT, GeometryType.MULTI_POINT -> {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                        .border(1.5.dp, strokeColor, CircleShape)
                )
            }
            GeometryType.POLYLINE, GeometryType.MULTI_POLYLINE -> {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(4.dp)
                        .background(strokeColor)
                )
            }
            GeometryType.POLYGON, GeometryType.MULTI_POLYGON -> {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(primaryColor.copy(alpha = 0.7f))
                        .border(1.5.dp, strokeColor)
                )
            }
            else -> {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}