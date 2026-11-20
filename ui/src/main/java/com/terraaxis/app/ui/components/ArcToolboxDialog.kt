package com.terraaxis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terraaxis.app.viewmodel.MainViewModel
import java.io.File

enum class ToolboxTool {
    NONE,
    BUFFER,
    CLIP,
    SIMPLIFY,
    SELECT_BY_ATTRIBUTES,
    EXPORT_SHP
}

/**
 * Diálogo ArcToolbox auténtico de ArcGIS Desktop 10.8:
 * - Árbol jerárquico de Toolboxes de Esri:
 *   - Analysis Tools (Extract, Proximity)
 *   - Cartography Tools (Generalization)
 *   - Selection Tools (Query)
 *   - Conversion Tools (To Shapefile)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcToolboxDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val layers by viewModel.layers.collectAsState()
    var selectedTool by remember { mutableStateOf(ToolboxTool.NONE) }
    var selectedLayerIndex by remember { mutableStateOf(0) }

    val activeLayer = layers.getOrNull(selectedLayerIndex) ?: layers.firstOrNull()

    // Parámetros de herramientas
    var bufferDistanceText by remember { mutableStateOf("500") }
    var simplifyToleranceText by remember { mutableStateOf("50") }
    var sqlWhereClause by remember { mutableStateOf("POBLACION > 20000") }

    // Estados de expansión de carpetas del árbol
    var expandAnalysis by remember { mutableStateOf(true) }
    var expandCartography by remember { mutableStateOf(false) }
    var expandSelection by remember { mutableStateOf(true) }
    var expandConversion by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icono caja roja de ArcToolbox
                Surface(
                    color = Color(0xFFD32F2F),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("ArcToolbox", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("ArcGIS Desktop 10.8 Geoprocessing", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedTool == ToolboxTool.NONE) {
                    Text(
                        "Cajas de herramientas disponibles:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    // ── 1. ANALYSIS TOOLS ──
                    ArcToolboxFolderItem(
                        title = "Analysis Tools",
                        isExpanded = expandAnalysis,
                        onToggle = { expandAnalysis = !expandAnalysis }
                    ) {
                        ArcToolboxToolItem(
                            toolName = "Buffer",
                            toolCategory = "Proximity > Buffer (Área de influencia)",
                            icon = Icons.Default.Adjust,
                            onClick = { selectedTool = ToolboxTool.BUFFER }
                        )
                        ArcToolboxToolItem(
                            toolName = "Clip",
                            toolCategory = "Extract > Clip (Recortar por extensión)",
                            icon = Icons.Default.ContentCut,
                            onClick = { selectedTool = ToolboxTool.CLIP }
                        )
                    }

                    // ── 2. SELECTION TOOLS ──
                    ArcToolboxFolderItem(
                        title = "Selection Tools",
                        isExpanded = expandSelection,
                        onToggle = { expandSelection = !expandSelection }
                    ) {
                        ArcToolboxToolItem(
                            toolName = "Select by Attributes",
                            toolCategory = "Query > Selección SQL de registros",
                            icon = Icons.Default.FilterAlt,
                            onClick = { selectedTool = ToolboxTool.SELECT_BY_ATTRIBUTES }
                        )
                    }

                    // ── 3. CARTOGRAPHY TOOLS ──
                    ArcToolboxFolderItem(
                        title = "Cartography Tools",
                        isExpanded = expandCartography,
                        onToggle = { expandCartography = !expandCartography }
                    ) {
                        ArcToolboxToolItem(
                            toolName = "Simplify Line",
                            toolCategory = "Generalization > Ramer-Douglas-Peucker",
                            icon = Icons.Default.AutoFixHigh,
                            onClick = { selectedTool = ToolboxTool.SIMPLIFY }
                        )
                    }

                    // ── 4. CONVERSION TOOLS ──
                    ArcToolboxFolderItem(
                        title = "Conversion Tools",
                        isExpanded = expandConversion,
                        onToggle = { expandConversion = !expandConversion }
                    ) {
                        ArcToolboxToolItem(
                            toolName = "To Shapefile",
                            toolCategory = "To Shapefile > Exportar a .shp / .dbf",
                            icon = Icons.Default.FileDownload,
                            onClick = { selectedTool = ToolboxTool.EXPORT_SHP }
                        )
                    }
                } else {
                    // Vista de ejecución de herramienta (ArcMap Tool Dialog)
                    Row(
                        modifier = Modifier
                            .clickable { selectedTool = ToolboxTool.NONE }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Volver al árbol de ArcToolbox", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0079C1))
                    }

                    HorizontalDivider()

                    // Selector de Capa de Entrada
                    Text("Input Features (Capa de entrada):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    var expandedLayerMenu by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expandedLayerMenu,
                        onExpandedChange = { expandedLayerMenu = !expandedLayerMenu }
                    ) {
                        OutlinedTextField(
                            value = activeLayer?.name ?: "Sin capas",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLayerMenu) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLayerMenu,
                            onDismissRequest = { expandedLayerMenu = false }
                        ) {
                            layers.forEachIndexed { index, l ->
                                DropdownMenuItem(
                                    text = { Text(l.name) },
                                    onClick = {
                                        selectedLayerIndex = index
                                        expandedLayerMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Parámetros específicos según la herramienta
                    when (selectedTool) {
                        ToolboxTool.BUFFER -> {
                            OutlinedTextField(
                                value = bufferDistanceText,
                                onValueChange = { bufferDistanceText = it },
                                label = { Text("Distance [value or field] (meters)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Crea polígonos de zona de influencia a una distancia geodésica especificada alrededor de las entidades de entrada.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        ToolboxTool.CLIP -> {
                            Text(
                                "Recorta las entidades de la capa seleccionada utilizando la extensión actual del mapa como capa de recorte.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                        }
                        ToolboxTool.SIMPLIFY -> {
                            OutlinedTextField(
                                value = simplifyToleranceText,
                                onValueChange = { simplifyToleranceText = it },
                                label = { Text("Simplification Tolerance (meters)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Simplifica líneas y polígonos eliminando vértices innecesarios mediante el algoritmo cartográfico Ramer-Douglas-Peucker.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        ToolboxTool.SELECT_BY_ATTRIBUTES -> {
                            OutlinedTextField(
                                value = sqlWhereClause,
                                onValueChange = { sqlWhereClause = it },
                                label = { Text("WHERE Clause (SQL Expression)") },
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Expresión SQL para seleccionar entidades. Ejemplos:\nPOBLACION > 20000\nCATEGORIA = 'Residencial'\nNOMBRE LIKE '%San%'",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        ToolboxTool.EXPORT_SHP -> {
                            Text(
                                "Exporta la capa completa a un archivo Shapefile (.shp, .dbf, .shx, .prj) dentro de la carpeta Exports del almacenamiento.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                        }
                        else -> {}
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTool != ToolboxTool.NONE) {
                Button(
                    onClick = {
                        activeLayer?.let { layer ->
                            when (selectedTool) {
                                ToolboxTool.BUFFER -> {
                                    val dist = bufferDistanceText.toDoubleOrNull() ?: 500.0
                                    viewModel.runBuffer(layer, dist)
                                }
                                ToolboxTool.CLIP -> {
                                    viewModel.runClip(layer)
                                }
                                ToolboxTool.SIMPLIFY -> {
                                    val tol = simplifyToleranceText.toDoubleOrNull() ?: 50.0
                                    viewModel.runSimplify(layer, tol)
                                }
                                ToolboxTool.SELECT_BY_ATTRIBUTES -> {
                                    viewModel.runSelectByAttributes(layer, sqlWhereClause)
                                }
                                ToolboxTool.EXPORT_SHP -> {
                                    try {
                                        val targetDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "Exports")
                                        com.terraaxis.data.shapefile.ShapefileWriter.write(layer, targetDir)
                                        viewModel.updateRender()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                else -> {}
                            }
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0079C1))
                ) {
                    Text("OK (Ejecutar)")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (selectedTool == ToolboxTool.NONE) "Close" else "Cancel")
            }
        }
    )
}

@Composable
private fun ArcToolboxFolderItem(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(3.dp))
            .background(Color(0xFFFAFAFA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.Gray
            )
        }

        if (isExpanded) {
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 2.dp, bottom = 4.dp, end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = content
            )
        }
    }
}

@Composable
private fun ArcToolboxToolItem(
    toolName: String,
    toolCategory: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF0079C1), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(toolName, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Black)
            Text(toolCategory, fontSize = 9.sp, color = Color.Gray)
        }
    }
}
