package com.terraaxis.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.terraaxis.app.viewmodel.MainViewModel
import com.terraaxis.core.model.VectorLayer

enum class ToolboxTool {
    NONE,
    BUFFER,
    CLIP,
    SELECT_BY_ATTRIBUTES,
    EXPORT_SHP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcToolboxDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val layers by viewModel.layers.collectAsState()
    var selectedTool by remember { mutableStateOf(ToolboxTool.NONE) }
    var selectedLayerIndex by remember { mutableStateOf(0) }

    val activeLayer = layers.getOrNull(selectedLayerIndex) ?: layers.firstOrNull()

    // Parameters
    var bufferDistanceText by remember { mutableStateOf("1000") }
    var sqlWhereClause by remember { mutableStateOf("POBLACION > 20000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("ArcToolbox Móvil")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTool == ToolboxTool.NONE) {
                    Text(
                        "Selecciona una herramienta de geoprocesamiento:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    ListItem(
                        headlineContent = { Text("Buffer (Área de Influencia)") },
                        supportingContent = { Text("Analysis Tools > Proximity") },
                        leadingContent = { Icon(Icons.Default.Adjust, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { selectedTool = ToolboxTool.BUFFER }
                    )
                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Clip (Recortar por Extensión)") },
                        supportingContent = { Text("Analysis Tools > Extract") },
                        leadingContent = { Icon(Icons.Default.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { selectedTool = ToolboxTool.CLIP }
                    )
                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Select by Attributes (Consulta SQL)") },
                        supportingContent = { Text("Selection Tools > Query") },
                        leadingContent = { Icon(Icons.Default.FilterAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { selectedTool = ToolboxTool.SELECT_BY_ATTRIBUTES }
                    )
                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Exportar Capa a Shapefile (.shp)") },
                        supportingContent = { Text("Conversion Tools > To Shapefile") },
                        leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { selectedTool = ToolboxTool.EXPORT_SHP }
                    )
                } else {
                    // Back button to catalog
                    Row(
                        modifier = Modifier.clickable { selectedTool = ToolboxTool.NONE },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Volver al Catálogo de Herramientas", style = MaterialTheme.typography.labelMedium)
                    }

                    HorizontalDivider()

                    // Layer selector
                    Text("Capa de Entrada (Input Layer):", style = MaterialTheme.typography.labelMedium)
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
                            modifier = Modifier.menuAnchor().fillMaxWidth()
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

                    when (selectedTool) {
                        ToolboxTool.BUFFER -> {
                            OutlinedTextField(
                                value = bufferDistanceText,
                                onValueChange = { bufferDistanceText = it },
                                label = { Text("Distancia del Buffer (metros)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Genera un polígono de zona de influencia con el radio especificado.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        ToolboxTool.CLIP -> {
                            Text(
                                "Recorta las geometrías de la capa seleccionada utilizando la extensión actual del mapa como máscara.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        ToolboxTool.SELECT_BY_ATTRIBUTES -> {
                            OutlinedTextField(
                                value = sqlWhereClause,
                                onValueChange = { sqlWhereClause = it },
                                label = { Text("Cláusula WHERE") },
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Ejemplos: [POBLACION] > 50000 o [CATEGORIA] = 'Residencial'",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        ToolboxTool.EXPORT_SHP -> {
                            Text(
                                "Exporta la capa completa en formato ESRI Shapefile (.shp, .dbf, .shx, .prj) listo para abrir en cualquier SIG.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
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
                                ToolboxTool.SELECT_BY_ATTRIBUTES -> {
                                    viewModel.runSelectByAttributes(layer, sqlWhereClause)
                                }
                                ToolboxTool.EXPORT_SHP -> {
                                    // Save to app external files or cache
                                    val targetDir = java.io.File(android.os.Environment.getExternalStorageDirectory(), "TerraAxis/Exports")
                                    com.terraaxis.data.shapefile.ShapefileWriter.write(layer, targetDir)
                                    viewModel.updateRender()
                                }
                                else -> {}
                            }
                        }
                        onDismiss()
                    }
                ) {
                    Text("Ejecutar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (selectedTool == ToolboxTool.NONE) "Cerrar" else "Cancelar")
            }
        }
    )
}
