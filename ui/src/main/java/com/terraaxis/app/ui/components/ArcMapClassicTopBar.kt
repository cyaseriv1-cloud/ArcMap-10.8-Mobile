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
import com.terraaxis.app.viewmodel.AppLanguage
import com.terraaxis.app.viewmodel.ArcMapTheme
import com.terraaxis.app.viewmodel.ArcMapViewMode
import com.terraaxis.app.viewmodel.MainViewModel
import com.terraaxis.map.interaction.MapToolMode

/**
 * Barra superior auténtica estilo ArcMap 10.8:
 * - Soporte bilingüe completo (Español / Inglés)
 * - Conmutador de Tema Claro / Tema Oscuro (Dark Theme)
 * - Herramienta Find (Buscador de Calles y POIs)
 * - Título, Menús clásicos y Barras de herramientas
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
    val theme by viewModel.theme.collectAsState()
    val language by viewModel.language.collectAsState()

    val isDark = theme == ArcMapTheme.DARK
    val isEs = language == AppLanguage.ES

    val titleBarBg = if (isDark) Color(0xFF161B22) else Color(0xFF335E8A)
    val menuBarBg = if (isDark) Color(0xFF21262D) else Color(0xFFF0EFEA)
    val toolbarBg = if (isDark) Color(0xFF1A1F26) else Color(0xFFEBE9E4)
    val borderColor = if (isDark) Color(0xFF30363D) else Color(0xFFB0B0B0)
    val textColor = if (isDark) Color(0xFFE6EDF3) else Color.Black

    var activeMenu by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(toolbarBg)
            .border(1.dp, borderColor)
    ) {
        // ── 1. Barra de Título ArcMap ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(titleBarBg)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Public,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    if (isEs) "Sin título - ArcMap 10.8 (Esri Mobile)" else "Untitled - ArcMap 10.8 (Esri Mobile)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Selector de Idioma (ES / EN)
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.clickable { viewModel.toggleLanguage() }
                ) {
                    Text(
                        text = if (isEs) "ES" else "EN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }

                // Selector de Tema (Claro / Oscuro)
                IconButton(
                    onClick = { viewModel.toggleTheme() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Cambiar Tema",
                        tint = if (isDark) Color(0xFFFFD54F) else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Indicador de modo (Data View / Layout View)
                Surface(
                    color = if (viewMode == ArcMapViewMode.DATA_VIEW) Color(0xFF0079C1) else Color(0xFFE65100),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = if (viewMode == ArcMapViewMode.DATA_VIEW) "DATA" else "LAYOUT",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // ── 2. Menú Clásico de ArcMap (Español / Inglés) ─────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(menuBarBg)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val menus = if (isEs) {
                listOf("Archivo", "Edición", "Ver", "Selección", "Geoprocesamiento", "Ventanas")
            } else {
                listOf("File", "Edit", "View", "Selection", "Geoprocessing", "Windows")
            }

            menus.forEach { menuName ->
                Box {
                    Text(
                        text = menuName,
                        fontSize = 12.sp,
                        fontWeight = if (activeMenu == menuName) FontWeight.Bold else FontWeight.Normal,
                        color = textColor,
                        modifier = Modifier
                            .clickable {
                                activeMenu = if (activeMenu == menuName) null else menuName
                            }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )

                    DropdownMenu(
                        expanded = activeMenu == menuName,
                        onDismissRequest = { activeMenu = null }
                    ) {
                        when (menuName) {
                            "Archivo", "File" -> {
                                DropdownMenuItem(
                                    text = { Text(if (isEs) "Añadir Datos... (Shapefile)" else "Add Data... (Shapefile)") },
                                    leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null, tint = Color(0xFFF57F17)) },
                                    onClick = {
                                        activeMenu = null
                                        onOpenCatalog()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEs) "Añadir mapa base... (Satélite/OSM)" else "Add Basemap... (Satellite/OSM)") },
                                    leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF0079C1)) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.openBasemapDialog()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEs) "Cargar Muestra Esri (Cusco)" else "Load Esri Sample Data") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.loadSampleData()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEs) "Vista de Composición (Layout)" else "Layout View (Sheet)") },
                                    leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.setViewMode(ArcMapViewMode.LAYOUT_VIEW)
                                    }
                                )
                            }
                            "Edición", "Edit" -> {
                                DropdownMenuItem(
                                    text = { Text(if (isEditorActive) (if (isEs) "Detener Edición" else "Stop Editing") else (if (isEs) "Iniciar Edición" else "Start Editing")) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.toggleEditor(!isEditorActive)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEs) "Limpiar Selección" else "Clear Selection") },
                                    leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.layers.value.forEach { it.clearSelection() }
                                        viewModel.updateRender()
                                    }
                                )
                            }
                            "Ver", "View" -> {
                                DropdownMenuItem(
                                    text = { Text(if (isEs) "Vista de Datos (Mapa)" else "Data View (Map)") },
                                    leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.setViewMode(ArcMapViewMode.DATA_VIEW)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEs) "Vista de Composición (Lámina)" else "Layout View (Paper Sheet)") },
                                    leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.setViewMode(ArcMapViewMode.LAYOUT_VIEW)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEs) "Extensión Completa" else "Full Extent") },
                                    leadingIcon = { Icon(Icons.Default.ZoomOutMap, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.zoomFullExtent()
                                    }
                                )
                            }
                            "Selección", "Selection" -> {
                                DropdownMenuItem(
                                    text = { Text(if (isEs) "Seleccionar por Atributos (SQL)..." else "Select by Attributes...") },
                                    leadingIcon = { Icon(Icons.Default.FilterAlt, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        onOpenToolbox()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEs) "Deseleccionar Entidades" else "Clear Selected Features") },
                                    leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.layers.value.forEach { it.clearSelection() }
                                        viewModel.updateRender()
                                    }
                                )
                            }
                            "Geoprocesamiento", "Geoprocessing" -> {
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
                            "Ventanas", "Windows" -> {
                                DropdownMenuItem(
                                    text = { Text(if (isEs) "Tabla de Contenidos (TOC)" else "Table of Contents") },
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
                                DropdownMenuItem(
                                    text = { Text(if (isEs) "Buscar (Find / Geocodificador)" else "Find (Places / Streets)") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF0079C1)) },
                                    onClick = {
                                        activeMenu = null
                                        viewModel.toggleSearch(true)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = borderColor)

        // ── 3. Barras de Herramientas Estándar y Tools ────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Data (ArcCatalog)",
                        tint = Color.Black,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        if (isEs) "Añadir Datos" else "Add Data",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.Black
                    )
                }
            }

            // BOTÓN CLÁSICO MAPA BASE (ADD BASEMAP)
            Surface(
                color = if (isDark) Color(0xFF1E3A5F) else Color(0xFFE3F2FD),
                shape = RoundedCornerShape(3.dp),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .clickable { viewModel.openBasemapDialog() }
                    .padding(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = "Añadir Mapa Base",
                        tint = Color(0xFF0079C1),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        if (isEs) "Mapa Base" else "Basemap",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isDark) Color.White else Color(0xFF0D47A1)
                    )
                }
            }

            // BOTÓN BUSCADOR / FIND (Icono de Búsqueda)
            IconButton(
                onClick = { viewModel.toggleSearch(true) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Buscar Calles y Lugares",
                    tint = Color(0xFF0079C1),
                    modifier = Modifier.size(19.dp)
                )
            }

            // BOTÓN ARCCATALOG (Archivador amarillo)
            IconButton(
                onClick = onOpenCatalog,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Default.FolderSpecial,
                    contentDescription = "ArcCatalog",
                    tint = Color(0xFFF57F17),
                    modifier = Modifier.size(19.dp)
                )
            }

            // BOTÓN ARCTOOLBOX (Caja roja oficial)
            IconButton(
                onClick = onOpenToolbox,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = "ArcToolbox",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(19.dp)
                )
            }

            // BOTÓN TABLA DE CONTENIDO (TOC)
            IconButton(
                onClick = onOpenToc,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "Table of Contents",
                    tint = Color(0xFF0079C1),
                    modifier = Modifier.size(19.dp)
                )
            }

            // Separador vertical
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(borderColor)
            )

            // HERRAMIENTAS TOOLS
            // 1. Pan (Mano)
            ArcMapToolButton(
                icon = Icons.Default.PanTool,
                label = "Pan",
                isSelected = activeTool == MapToolMode.PAN,
                isDark = isDark,
                onClick = { viewModel.setTool(MapToolMode.PAN) }
            )

            // 2. Zoom In
            ArcMapToolButton(
                icon = Icons.Default.ZoomIn,
                label = "Zoom In",
                isSelected = false,
                isDark = isDark,
                onClick = { viewModel.zoomIn() }
            )

            // 3. Zoom Out
            ArcMapToolButton(
                icon = Icons.Default.ZoomOut,
                label = "Zoom Out",
                isSelected = false,
                isDark = isDark,
                onClick = { viewModel.zoomOut() }
            )

            // 4. Full Extent
            ArcMapToolButton(
                icon = Icons.Default.ZoomOutMap,
                label = "Full Extent",
                isSelected = false,
                isDark = isDark,
                onClick = { viewModel.zoomFullExtent() }
            )

            // 5. Select Features
            ArcMapToolButton(
                icon = Icons.Default.TouchApp,
                label = "Select",
                isSelected = activeTool == MapToolMode.SELECT,
                isDark = isDark,
                tint = if (activeTool == MapToolMode.SELECT) Color(0xFF00E5FF) else (if (isDark) Color.White else Color.Black),
                onClick = { viewModel.setTool(MapToolMode.SELECT) }
            )

            // 6. Clear Selection
            ArcMapToolButton(
                icon = Icons.Default.CleaningServices,
                label = "Clear",
                isSelected = false,
                isDark = isDark,
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
                isDark = isDark,
                tint = Color(0xFF0079C1),
                onClick = { viewModel.setTool(MapToolMode.IDENTIFY) }
            )

            // 8. Measure (Regla)
            ArcMapToolButton(
                icon = Icons.Default.Straighten,
                label = "Measure",
                isSelected = activeTool == MapToolMode.MEASURE_DISTANCE || activeTool == MapToolMode.MEASURE_AREA,
                isDark = isDark,
                tint = Color(0xFFE91E63),
                onClick = { viewModel.setTool(MapToolMode.MEASURE_DISTANCE) }
            )

            // Separador vertical
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(borderColor)
            )

            // 9. Barra Editor (Digitalización)
            ArcMapToolButton(
                icon = Icons.Default.Edit,
                label = "Editor",
                isSelected = isEditorActive,
                isDark = isDark,
                tint = if (isEditorActive) Color(0xFF4CAF50) else (if (isDark) Color.LightGray else Color(0xFF555555)),
                onClick = { viewModel.toggleEditor(!isEditorActive) }
            )

            if (isEditorActive) {
                ArcMapToolButton(
                    icon = Icons.Default.AddLocation,
                    label = "+Punto",
                    isSelected = activeTool == MapToolMode.DIGITIZE_POINT,
                    isDark = isDark,
                    onClick = { viewModel.setTool(MapToolMode.DIGITIZE_POINT) }
                )
                ArcMapToolButton(
                    icon = Icons.Default.Timeline,
                    label = "+Línea",
                    isSelected = activeTool == MapToolMode.DIGITIZE_LINE,
                    isDark = isDark,
                    onClick = { viewModel.setTool(MapToolMode.DIGITIZE_LINE) }
                )
                ArcMapToolButton(
                    icon = Icons.Default.Polyline,
                    label = "+Polígono",
                    isSelected = activeTool == MapToolMode.DIGITIZE_POLYGON,
                    isDark = isDark,
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
    isDark: Boolean,
    tint: Color = if (isDark) Color.White else Color.Black,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) (if (isDark) Color(0xFF1E3A5F) else Color(0xFFB0D0F0)) else Color.Transparent
    val borderCol = if (isSelected) Color(0xFF0079C1) else Color.Transparent

    Box(
        modifier = Modifier
            .size(28.dp)
            .background(bgColor, RoundedCornerShape(3.dp))
            .border(1.dp, borderCol, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) (if (isDark) Color(0xFF64B5F6) else Color(0xFF005A9E)) else tint,
            modifier = Modifier.size(17.dp)
        )
    }
}
