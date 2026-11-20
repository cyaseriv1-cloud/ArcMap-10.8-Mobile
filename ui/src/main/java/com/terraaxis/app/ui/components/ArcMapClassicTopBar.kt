package com.terraaxis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terraaxis.app.viewmodel.ArcMapViewMode
import com.terraaxis.app.viewmodel.MainViewModel
import com.terraaxis.map.interaction.MapToolMode

/**
 * Barra superior auténtica estilo ArcMap 10.8:
 * - Título: Untitled - ArcMap 10.8
 * - Menú: File, Edit, View, Selection, Geoprocessing, Windows
 * - Barra Estándar: Add Data (+), ArcCatalog, ArcToolbox, TOC
 * - Barra Tools: Pan, Zoom In, Zoom Out, Full Extent, Select, Identify, Measure, Editor
 */
@Composable
fun ArcMapClassicTopBar(
    viewModel: MainViewModel,
    onOpenToc: () -> Unit,
    onOpenToolbox: () -> Unit,
    onOpenCatalog: () -> Unit
) {
    val activeTool by viewModel.activeTool.collectAsState()
    val isEditorActive by viewModel.isEditorActive.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    var activeMenu by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEBE9E4)) // Fondo gris clásico de ArcGIS Desktop
            .border(1.dp, Color(0xFFB0B0B0))
    ) {
        // ── 1. Barra de Título ArcMap ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF335E8A)) // Azul clásico de ventana ArcMap
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icono ArcMap (globo con meridiano)
                Icon(
                    Icons.Default.Public,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Untitled - ArcMap 10.8 (Esri Mobile)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Indicador de modo
            Surface(
                color = if (viewMode == ArcMapViewMode.DATA_VIEW) Color(0xFF0079C1) else Color(0xFFE65100),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.padding(2.dp)
            ) {
                Text(
                    text = if (viewMode == ArcMapViewMode.DATA_VIEW) "DATA VIEW" else "LAYOUT VIEW",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }

        // ── 2. Menú Clásico de ArcMap (File, Edit, View, Selection, etc.) ────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0EFEA))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val menus = listOf("File", "Edit", "View", "Selection", "Geoprocessing", "Windows")
            menus.forEach { menuName ->
                Box {
                    Text(
                        text = menuName,
                        fontSize = 12.sp,
                        fontWeight = if (activeMenu == menuName) FontWeight.Bold else FontWeight.Normal,
                        color = Color.Black,
                        modifier = Modifier
                            .clickable {
                                activeMenu = if (activeMenu == menuName) null else menuName
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    DropdownMenu(
                        expanded = activeMenu == menuName,
                        onDismissRequest = { activeMenu = null }
                    ) {
                        when (menuName) {
                            "File" -> {
                                DropdownMenuItem(
                                    text = { Text("Add Data... (Shapefile)") },
                                    leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null, tint = Color(0xFFF57F17)) },
                                    onClick = {
                                        activeMenu = null
                                        onOpenCatalog()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Muestra Oficial Esri") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.loadSampleData()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Layout View (Lámina)") },
                                    leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.setViewMode(ArcMapViewMode.LAYOUT_VIEW)
                                    }
                                )
                            }
                            "Edit" -> {
                                DropdownMenuItem(
                                    text = { Text(if (isEditorActive) "Stop Editing" else "Start Editing") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.toggleEditor(!isEditorActive)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Selection") },
                                    leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.layers.value.forEach { it.clearSelection() }
                                        viewModel.updateRender()
                                    }
                                )
                            }
                            "View" -> {
                                DropdownMenuItem(
                                    text = { Text("Data View (Mapa)") },
                                    leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.setViewMode(ArcMapViewMode.DATA_VIEW)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Layout View (Lámina)") },
                                    leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.setViewMode(ArcMapViewMode.LAYOUT_VIEW)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Full Extent") },
                                    leadingIcon = { Icon(Icons.Default.ZoomOutMap, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.zoomFullExtent()
                                    }
                                )
                            }
                            "Selection" -> {
                                DropdownMenuItem(
                                    text = { Text("Select by Attributes...") },
                                    leadingIcon = { Icon(Icons.Default.FilterAlt, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        onOpenToolbox()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Selected Features") },
                                    leadingIcon = { Icon(Icons.Default.Deselect, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.layers.value.forEach { it.clearSelection() }
                                        viewModel.updateRender()
                                    }
                                )
                            }
                            "Geoprocessing" -> {
                                DropdownMenuItem(
                                    text = { Text("ArcToolbox...") },
                                    leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = Color.Red) },
                                    onClick = {
                                        activeMenu = null
                                        onOpenToolbox()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Buffer (Área de Influencia)") },
                                    leadingIcon = { Icon(Icons.Default.Adjust, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        onOpenToolbox()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clip (Recortar)") },
                                    leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        onOpenToolbox()
                                    }
                                )
                            }
                            "Windows" -> {
                                DropdownMenuItem(
                                    text = { Text("Table of Contents (TOC)") },
                                    leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        onOpenToc()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("ArcCatalog") },
                                    leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFFF57F17)) },
                                    onClick = {
                                        activeMenu = null
                                        onOpenCatalog()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("ArcToolbox") },
                                    leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = Color.Red) },
                                    onClick = {
                                        activeMenu = null
                                        onOpenToolbox()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFB0B0B0))

        // ── 3. Barras de Herramientas Estándar y Tools (Iconos de ArcMap) ────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // BOTÓN CLÁSICO ADD DATA (+)
            Surface(
                color = Color(0xFFFFD54F),
                shape = RoundedCornerShape(3.dp),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .clickable { onOpenCatalog() }
                    .padding(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Data (ArcCatalog)",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Add Data", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                }
            }

            // BOTÓN ARCCATALOG (Archivador amarillo)
            IconButton(
                onClick = onOpenCatalog,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.FolderSpecial,
                    contentDescription = "ArcCatalog",
                    tint = Color(0xFFF57F17),
                    modifier = Modifier.size(20.dp)
                )
            }

            // BOTÓN ARCTOOLBOX (Caja roja oficial)
            IconButton(
                onClick = onOpenToolbox,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = "ArcToolbox",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(20.dp)
                )
            }

            // BOTÓN TABLA DE CONTENIDO (TOC)
            IconButton(
                onClick = onOpenToc,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "Table of Contents",
                    tint = Color(0xFF0079C1),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Separador vertical
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(Color(0xFFB0B0B0))
                    .padding(horizontal = 2.dp)
            )

            // HERRAMIENTAS TOOLS DE ARCMAP
            // 1. Pan (Mano)
            ArcMapToolButton(
                icon = Icons.Default.PanTool,
                label = "Pan",
                isSelected = activeTool == MapToolMode.PAN,
                onClick = { viewModel.setTool(MapToolMode.PAN) }
            )

            // 2. Zoom In
            ArcMapToolButton(
                icon = Icons.Default.ZoomIn,
                label = "Zoom In",
                isSelected = false,
                onClick = { viewModel.zoomIn() }
            )

            // 3. Zoom Out
            ArcMapToolButton(
                icon = Icons.Default.ZoomOut,
                label = "Zoom Out",
                isSelected = false,
                onClick = { viewModel.zoomOut() }
            )

            // 4. Full Extent
            ArcMapToolButton(
                icon = Icons.Default.ZoomOutMap,
                label = "Full Extent",
                isSelected = false,
                onClick = { viewModel.zoomFullExtent() }
            )

            // 5. Select Features
            ArcMapToolButton(
                icon = Icons.Default.TouchApp,
                label = "Select",
                isSelected = activeTool == MapToolMode.SELECT,
                tint = if (activeTool == MapToolMode.SELECT) Color(0xFF00E5FF) else Color(0xFF333333),
                onClick = { viewModel.setTool(MapToolMode.SELECT) }
            )

            // 6. Clear Selection
            ArcMapToolButton(
                icon = Icons.Default.Deselect,
                label = "Clear",
                isSelected = false,
                onClick = {
                    viewModel.layers.value.forEach { it.clearSelection() }
                    viewModel.updateRender()
                }
            )

            // 7. Identify (i en azul)
            ArcMapToolButton(
                icon = Icons.Default.Info,
                label = "Identify",
                isSelected = activeTool == MapToolMode.IDENTIFY,
                tint = Color(0xFF0079C1),
                onClick = { viewModel.setTool(MapToolMode.IDENTIFY) }
            )

            // 8. Measure (Regla)
            ArcMapToolButton(
                icon = Icons.Default.Straighten,
                label = "Measure",
                isSelected = activeTool == MapToolMode.MEASURE_DISTANCE || activeTool == MapToolMode.MEASURE_AREA,
                tint = Color(0xFFE91E63),
                onClick = { viewModel.setTool(MapToolMode.MEASURE_DISTANCE) }
            )

            // Separador vertical
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(Color(0xFFB0B0B0))
                    .padding(horizontal = 2.dp)
            )

            // 9. Barra Editor (Digitalización de geometrías)
            ArcMapToolButton(
                icon = Icons.Default.Edit,
                label = "Editor",
                isSelected = isEditorActive,
                tint = if (isEditorActive) Color(0xFF4CAF50) else Color(0xFF555555),
                onClick = { viewModel.toggleEditor(!isEditorActive) }
            )

            if (isEditorActive) {
                ArcMapToolButton(
                    icon = Icons.Default.AddLocation,
                    label = "+Punto",
                    isSelected = activeTool == MapToolMode.DIGITIZE_POINT,
                    onClick = { viewModel.setTool(MapToolMode.DIGITIZE_POINT) }
                )
                ArcMapToolButton(
                    icon = Icons.Default.Timeline,
                    label = "+Línea",
                    isSelected = activeTool == MapToolMode.DIGITIZE_LINE,
                    onClick = { viewModel.setTool(MapToolMode.DIGITIZE_LINE) }
                )
                ArcMapToolButton(
                    icon = Icons.Default.Polyline,
                    label = "+Polígono",
                    isSelected = activeTool == MapToolMode.DIGITIZE_POLYGON,
                    onClick = { viewModel.setTool(MapToolMode.DIGITIZE_POLYGON) }
                )
            }
        }
    }
}

@Composable
private fun ArcMapToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    tint: Color = Color.Black,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFFB0D0F0) else Color.Transparent
    val borderCol = if (isSelected) Color(0xFF0079C1) else Color.Transparent

    Box(
        modifier = Modifier
            .size(30.dp)
            .background(bgColor, RoundedCornerShape(3.dp))
            .border(1.dp, borderCol, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF005A9E) else tint,
            modifier = Modifier.size(18.dp)
        )
    }
}
