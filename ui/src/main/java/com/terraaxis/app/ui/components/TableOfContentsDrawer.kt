package com.terraaxis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.terraaxis.app.viewmodel.MainViewModel
import com.terraaxis.core.geometry.GeometryType
import com.terraaxis.core.model.VectorLayer

@Composable
fun TableOfContentsDrawer(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val layers by viewModel.layers.collectAsState()
    val activeLayer by viewModel.activeLayer.collectAsState()

    ModalDrawerSheet(
        modifier = Modifier.width(320.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tabla de Contenidos",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Text(
                "Capas del Mapa (${layers.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(layers) { layer ->
                    val isActive = activeLayer?.id == layer.id
                    LayerItemCard(
                        layer = layer,
                        isActive = isActive,
                        onSelectLayer = { viewModel.setActiveLayer(layer) },
                        onToggleVisibility = { viewModel.toggleLayerVisibility(layer.id) },
                        onOpenTable = {
                            viewModel.openAttributeTable(layer)
                            onClose()
                        },
                        onZoomToLayer = {
                            if (layer.boundingBox != com.terraaxis.core.geometry.BoundingBox.EMPTY) {
                                viewModel.viewport.zoomToExtent(layer.boundingBox)
                                viewModel.updateRender()
                            }
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Button(
                onClick = { viewModel.loadSampleData() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recargar Datos de Muestra")
            }
        }
    }
}

@Composable
private fun LayerItemCard(
    layer: VectorLayer,
    isActive: Boolean,
    onSelectLayer: () -> Unit,
    onToggleVisibility: () -> Unit,
    onOpenTable: () -> Unit,
    onZoomToLayer: () -> Unit
) {
    val cardColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectLayer),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = layer.isVisible,
                onCheckedChange = { onToggleVisibility() }
            )

            val geomIcon = when (layer.geometryType) {
                GeometryType.POINT, GeometryType.MULTI_POINT -> Icons.Default.Place
                GeometryType.POLYLINE, GeometryType.MULTI_POLYLINE -> Icons.Default.Timeline
                GeometryType.POLYGON, GeometryType.MULTI_POLYGON -> Icons.Default.Layers
                else -> Icons.Default.Map
            }

            Icon(
                geomIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    layer.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    "${layer.features.size} entidades",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(onClick = onOpenTable) {
                Icon(
                    Icons.Default.TableChart,
                    contentDescription = "Tabla de Atributos",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onZoomToLayer) {
                Icon(
                    Icons.Default.CenterFocusStrong,
                    contentDescription = "Zoom a capa",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
