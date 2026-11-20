package com.terraaxis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.terraaxis.core.geometry.GeometryType
import com.terraaxis.core.model.VectorLayer
import com.terraaxis.core.symbology.ColorRgba
import com.terraaxis.core.symbology.LayerSymbology

/**
 * Diálogo de Propiedades de Capa estilo ArcMap:
 * Cambiar color de relleno, borde, grosor, y activar/desactivar etiquetas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerPropertiesDialog(
    layer: VectorLayer,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val currentSymbology = layer.symbology as? LayerSymbology.Simple ?: LayerSymbology.Simple()

    var fillColorHex by remember { mutableStateOf(currentSymbology.fillColor.toHexString()) }
    var strokeColorHex by remember { mutableStateOf(currentSymbology.strokeColor.toHexString()) }
    var strokeWidthText by remember { mutableStateOf(currentSymbology.strokeWidthDp.toString()) }
    var pointSizeText by remember { mutableStateOf(currentSymbology.pointSizeDp.toString()) }
    var labelField by remember { mutableStateOf(layer.labelFieldName ?: "") }
    var fillAlphaFloat by remember { mutableStateOf(currentSymbology.fillColor.alpha) }

    // Colores predefinidos estilo ArcGIS
    val presetColors = listOf(
        "#E91E63" to "Rosa",
        "#2196F3" to "Azul",
        "#4CAF50" to "Verde",
        "#FF9800" to "Naranja",
        "#9C27B0" to "Púrpura",
        "#F44336" to "Rojo",
        "#00BCD4" to "Cian",
        "#FFEB3B" to "Amarillo",
        "#795548" to "Marrón",
        "#607D8B" to "Gris Azul",
        "#000000" to "Negro",
        "#FFFFFF" to "Blanco"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Propiedades: ${layer.name}")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                // ── Color de Relleno ──────────────────────────────
                Text("Color de Relleno", style = MaterialTheme.typography.labelLarge)

                // Paleta de colores predefinidos
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetColors.take(6).forEach { (hex, name) ->
                        val selected = fillColorHex.uppercase() == hex.uppercase()
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable { fillColorHex = hex }
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetColors.drop(6).forEach { (hex, name) ->
                        val selected = fillColorHex.uppercase() == hex.uppercase()
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable { fillColorHex = hex }
                        )
                    }
                }

                OutlinedTextField(
                    value = fillColorHex,
                    onValueChange = { fillColorHex = it },
                    label = { Text("HEX Relleno") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(fillColorHex))
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Opacidad del relleno
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Opacidad: ${(fillAlphaFloat * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(100.dp)
                    )
                    Slider(
                        value = fillAlphaFloat,
                        onValueChange = { fillAlphaFloat = it },
                        valueRange = 0.05f..1.0f,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()

                // ── Color y Grosor del Borde ──────────────────────
                Text("Contorno / Borde", style = MaterialTheme.typography.labelLarge)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetColors.take(6).forEach { (hex, _) ->
                        val selected = strokeColorHex.uppercase() == hex.uppercase()
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable { strokeColorHex = hex }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = strokeColorHex,
                        onValueChange = { strokeColorHex = it },
                        label = { Text("HEX Borde") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = strokeWidthText,
                        onValueChange = { strokeWidthText = it },
                        label = { Text("Grosor") },
                        singleLine = true,
                        modifier = Modifier.width(80.dp)
                    )
                }

                // Tamaño de punto (solo para capas de puntos)
                if (layer.geometryType == GeometryType.POINT || layer.geometryType == GeometryType.MULTI_POINT) {
                    HorizontalDivider()
                    Text("Símbolo de Punto", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = pointSizeText,
                        onValueChange = { pointSizeText = it },
                        label = { Text("Tamaño (dp)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()

                // ── Etiquetas ─────────────────────────────────────
                Text("Etiquetas", style = MaterialTheme.typography.labelLarge)

                val fieldNames = layer.fields.map { it.name }
                var expandedLabelMenu by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expandedLabelMenu,
                    onExpandedChange = { expandedLabelMenu = !expandedLabelMenu }
                ) {
                    OutlinedTextField(
                        value = if (labelField.isBlank()) "Sin etiquetas" else labelField,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Campo de Etiqueta") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLabelMenu) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedLabelMenu,
                        onDismissRequest = { expandedLabelMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sin etiquetas") },
                            onClick = {
                                labelField = ""
                                expandedLabelMenu = false
                            }
                        )
                        fieldNames.forEach { fn ->
                            DropdownMenuItem(
                                text = { Text(fn) },
                                onClick = {
                                    labelField = fn
                                    expandedLabelMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fillColor = parseHexColorRgba(fillColorHex, fillAlphaFloat)
                    val strokeColor = parseHexColorRgba(strokeColorHex, 1.0f)
                    val strokeWidth = strokeWidthText.toFloatOrNull() ?: 1.5f
                    val pointSize = pointSizeText.toFloatOrNull() ?: 8f

                    val newSymbology = LayerSymbology.Simple(
                        fillColor = fillColor,
                        strokeColor = strokeColor,
                        strokeWidthDp = strokeWidth,
                        pointSizeDp = pointSize
                    )
                    viewModel.setLayerSymbology(layer.id, newSymbology)
                    viewModel.setLayerLabelField(layer.id, labelField.ifBlank { null })
                    onDismiss()
                }
            ) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun ColorRgba.toHexString(): String {
    return "#%02X%02X%02X".format(red.coerceIn(0, 255), green.coerceIn(0, 255), blue.coerceIn(0, 255))
}

private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.trimStart('#').padEnd(6, '0').take(6)
        val rgb = clean.toLong(16)
        Color(
            red = ((rgb shr 16) and 0xFF) / 255f,
            green = ((rgb shr 8) and 0xFF) / 255f,
            blue = (rgb and 0xFF) / 255f
        )
    } catch (e: Exception) {
        Color.Gray
    }
}

private fun parseHexColorRgba(hex: String, alpha: Float): ColorRgba {
    return try {
        val clean = hex.trimStart('#').padEnd(6, '0').take(6)
        val rgb = clean.toLong(16)
        ColorRgba(
            red = ((rgb shr 16) and 0xFF).toInt(),
            green = ((rgb shr 8) and 0xFF).toInt(),
            blue = (rgb and 0xFF).toInt(),
            alpha = alpha.coerceIn(0.0f, 1.0f)
        )
    } catch (e: Exception) {
        ColorRgba(128, 128, 128, alpha)
    }
}
