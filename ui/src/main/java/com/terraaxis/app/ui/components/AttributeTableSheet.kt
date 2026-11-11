package com.terraaxis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.terraaxis.app.viewmodel.MainViewModel
import com.terraaxis.core.model.GeometryProperty
import com.terraaxis.core.model.VectorLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributeTableSheet(
    layer: VectorLayer,
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortAscending by remember { mutableStateOf(true) }
    var sortedField by remember { mutableStateOf<String?>(null) }
    var showCalcGeometryDialog by remember { mutableStateOf(false) }

    val selectedCount = layer.selectedFeatureIds.size
    val totalCount = layer.features.size

    val filteredFeatures = remember(layer.features, searchQuery, sortedField, sortAscending) {
        var list = if (searchQuery.isBlank()) {
            layer.features.toList()
        } else {
            layer.features.filter { feat ->
                feat.attributes.values.any { it?.toString()?.contains(searchQuery, ignoreCase = true) == true }
            }
        }
        if (sortedField != null) {
            list = list.sortedWith { a, b ->
                val vA = a.attributes[sortedField]
                val vB = b.attributes[sortedField]
                val comp = when {
                    vA == null && vB == null -> 0
                    vA == null -> -1
                    vB == null -> 1
                    vA is Comparable<*> && vB is Comparable<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        (vA as Comparable<Any>).compareTo(vB as Any)
                    }
                    else -> vA.toString().compareTo(vB.toString())
                }
                if (sortAscending) comp else -comp
            }
        }
        list
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Tabla de Atributos: ${layer.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "$totalCount registros ($selectedCount seleccionados)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    IconButton(onClick = { showCalcGeometryDialog = true }) {
                        Icon(Icons.Default.Calculate, contentDescription = "Calcular Geometría")
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Toolbar & Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar en la tabla...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                FilledTonalButton(
                    onClick = {
                        layer.selectAll()
                        viewModel.updateRender()
                    }
                ) {
                    Text("Todos")
                }

                OutlinedButton(
                    onClick = {
                        layer.clearSelection()
                        viewModel.updateRender()
                    }
                ) {
                    Text("Limpiar")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Table with Horizontal and Vertical Scrolling
            val horizontalScrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(50.dp).padding(horizontal = 4.dp)) {
                            Text("#", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }

                        for (field in layer.fields) {
                            Row(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clickable {
                                        if (sortedField == field.name) {
                                            sortAscending = !sortAscending
                                        } else {
                                            sortedField = field.name
                                            sortAscending = true
                                        }
                                    }
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    field.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                if (sortedField == field.name) {
                                    Icon(
                                        if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Data Rows
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(filteredFeatures) { feat ->
                            val isSelected = layer.selectedFeatureIds.contains(feat.id)
                            val rowColor = if (isSelected) {
                                Color(0xFF80DEEA).copy(alpha = 0.6f) // Cyan selection
                            } else {
                                Color.Transparent
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowColor)
                                    .clickable {
                                        if (isSelected) {
                                            layer.deselectFeature(feat.id)
                                        } else {
                                            layer.selectFeature(feat.id, append = true)
                                        }
                                        viewModel.updateRender()
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.width(50.dp).padding(horizontal = 4.dp)) {
                                    Text(
                                        feat.id.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                for (field in layer.fields) {
                                    Box(modifier = Modifier.width(140.dp).padding(horizontal = 4.dp)) {
                                        val valStr = feat.attributes[field.name]?.toString() ?: "<null>"
                                        Text(
                                            valStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    if (showCalcGeometryDialog) {
        CalculateGeometryDialog(
            layer = layer,
            onDismiss = { showCalcGeometryDialog = false },
            onCalculate = { fieldName, property ->
                viewModel.runCalculateGeometry(layer, fieldName, property)
                showCalcGeometryDialog = false
            }
        )
    }
}

@Composable
fun CalculateGeometryDialog(
    layer: VectorLayer,
    onDismiss: () -> Unit,
    onCalculate: (String, GeometryProperty) -> Unit
) {
    var fieldName by remember { mutableStateOf("AREA_CALC") }
    var selectedProperty by remember { mutableStateOf(GeometryProperty.AREA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calcular Geometría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Calcula propiedades geométricas directamente sobre los registros (ArcMap Calculate Geometry):",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = fieldName,
                    onValueChange = { fieldName = it },
                    label = { Text("Nombre del Campo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedProperty == GeometryProperty.AREA,
                            onClick = { selectedProperty = GeometryProperty.AREA }
                        )
                        Text("Área Planar (m²)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedProperty == GeometryProperty.LENGTH_OR_PERIMETER,
                            onClick = { selectedProperty = GeometryProperty.LENGTH_OR_PERIMETER }
                        )
                        Text("Longitud / Perímetro (m)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedProperty == GeometryProperty.CENTROID_X,
                            onClick = { selectedProperty = GeometryProperty.CENTROID_X }
                        )
                        Text("Coordenada Centroide X")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedProperty == GeometryProperty.CENTROID_Y,
                            onClick = { selectedProperty = GeometryProperty.CENTROID_Y }
                        )
                        Text("Coordenada Centroide Y")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fieldName.isNotBlank()) {
                        onCalculate(fieldName.trim(), selectedProperty)
                    }
                }
            ) {
                Text("Calcular")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
