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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terraaxis.app.viewmodel.MainViewModel
import com.terraaxis.core.geometry.BoundingBox
import com.terraaxis.core.geometry.GeometryType
import com.terraaxis.core.model.VectorLayer
import com.terraaxis.core.symbology.LayerSymbology

/**
 * Tabla de Contenidos (Table of Contents - TOC) fiel a ArcMap 10.8:
 * - Pestaña List By Drawing Order
 * - Marco de datos "Layers"
 * - Árbol jerárquico con leyenda y símbolo visible debajo de cada capa
 * - Acceso directo a Tabla de Atributos, Simbología, Zoom a Capa y Orden de dibujo
 */
@Composable
fun ArcMapTableOfContentsDrawer(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onOpenCatalog: () -> Unit
) {
    val layers by viewModel.layers.collectAsState()
    val activeLayer by viewModel.activeLayer.collectAsState()

    ModalDrawerSheet(
        modifier = Modifier.width(330.dp),
        drawerContainerColor = Color(0xFFF7F6F2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // ── Encabezado de la Ventana TOC ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF335E8A), RoundedCornerShape(2.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Table of Contents",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            // ── Pestañas clásicas de ArcMap (List by Drawing Order, Source, etc.) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEBE9E4))
                    .border(1.dp, Color(0xFFD0D0D0))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Surface(
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB0B0B0)),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0079C1))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Drawing Order", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
                IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Folder, contentDescription = "Source", tint = Color.Gray, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = "Visibility", tint = Color.Gray, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = "Selection", tint = Color.Gray, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Marco de Datos: Layers (Data Frame) ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8EEF5), RoundedCornerShape(2.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Public,
                    contentDescription = null,
                    tint = Color(0xFF0079C1),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Layers",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1A237E)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "(${layers.size} capas)",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            HorizontalDivider(color = Color(0xFFB0B0B0), modifier = Modifier.padding(vertical = 4.dp))

            // ── Lista de Capas en Árbol con Leyenda debajo ────────────────────
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(layers) { index, layer ->
                    val isActive = activeLayer?.id == layer.id

                    ArcMapLayerTreeItem(
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
                        onDeleteLayer = { viewModel.removeLayer(layer.id) },
                        viewModel = viewModel
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFB0B0B0), modifier = Modifier.padding(vertical = 4.dp))

            // ── Barra inferior de acciones del TOC ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = Color(0xFFFFD54F),
                    shape = RoundedCornerShape(3.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB0B0B0)),
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clickable {
                            onClose()
                            onOpenCatalog()
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Data", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                    }
                }

                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(3.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0079C1)),
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clickable {
                            onClose()
                            viewModel.openBasemapDialog()
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF0079C1), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mapa Base", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0079C1))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArcMapLayerTreeItem(
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
    onDeleteLayer: () -> Unit,
    viewModel: MainViewModel
) {
    var showPropertiesDialog by remember { mutableStateOf(false) }

    val itemBg = if (isActive) Color(0xFFD4E4F7) else Color(0xFFFFFFFF)
    val itemBorder = if (isActive) Color(0xFF0079C1) else Color(0xFFE0E0E0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(itemBg, RoundedCornerShape(2.dp))
            .border(1.dp, itemBorder, RoundedCornerShape(2.dp))
            .clickable(onClick = onSelectLayer)
            .padding(4.dp)
    ) {
        // Fila 1: Checkbox + Nombre de Capa + Botones de Acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = layer.isVisible,
                onCheckedChange = { onToggleVisibility() },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = layer.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = Color.Black,
                    maxLines = 1
                )
                Text(
                    text = "${layer.features.size} feat • ${layer.geometryType.name}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            // Botones de acción rápida estilo ArcMap
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tabla de Atributos
                IconButton(onClick = onOpenTable, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.TableChart, contentDescription = "Tabla", tint = Color(0xFF0079C1), modifier = Modifier.size(16.dp))
                }
                // Simbología / Propiedades
                IconButton(onClick = { showPropertiesDialog = true }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Palette, contentDescription = "Simbología", tint = Color(0xFF7B1FA2), modifier = Modifier.size(16.dp))
                }
                // Zoom to Layer
                IconButton(onClick = onZoomToLayer, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Zoom", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                }
                // Eliminar
                IconButton(onClick = onDeleteLayer, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFD32F2F), modifier = Modifier.size(15.dp))
                }
            }
        }

        // Fila 2: LEYENDA Y PARCHE DE SÍMBOLO IDENTADO DEBAJO DEL NOMBRE (Estilo ArcMap Desktop)
        Row(
            modifier = Modifier
                .padding(start = 28.dp, top = 2.dp, bottom = 2.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArcMapSymbolSwatch(layer = layer)
            Spacer(modifier = Modifier.width(6.dp))
            val legendText = when (val s = layer.symbology) {
                is LayerSymbology.UniqueValues -> "Valores únicos por: ${s.fieldName}"
                is LayerSymbology.Simple -> "Símbolo simple (${layer.geometryType.name.lowercase()})"
            }
            Text(
                text = legendText,
                fontSize = 10.sp,
                color = Color.DarkGray,
                maxLines = 1
            )
        }
    }

    if (showPropertiesDialog) {
        LayerPropertiesDialog(
            layer = layer,
            viewModel = viewModel,
            onDismiss = { showPropertiesDialog = false }
        )
    }
}

@Composable
private fun ArcMapSymbolSwatch(layer: VectorLayer) {
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
        modifier = Modifier.size(20.dp),
        contentAlignment = Alignment.Center
    ) {
        when (layer.geometryType) {
            GeometryType.POINT, GeometryType.MULTI_POINT -> {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                        .border(1.dp, strokeColor, CircleShape)
                )
            }
            GeometryType.POLYLINE, GeometryType.MULTI_POLYLINE -> {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(3.dp)
                        .background(strokeColor)
                )
            }
            GeometryType.POLYGON, GeometryType.MULTI_POLYGON -> {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(primaryColor.copy(alpha = 0.8f))
                        .border(1.dp, strokeColor)
                )
            }
            else -> {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
            }
        }
    }
}
