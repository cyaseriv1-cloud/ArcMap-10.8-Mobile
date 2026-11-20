package com.terraaxis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terraaxis.app.viewmodel.AppLanguage
import com.terraaxis.app.viewmodel.ArcMapTheme
import com.terraaxis.app.viewmodel.MainViewModel
import com.terraaxis.map.basemap.BasemapType

/**
 * Diálogo clásico auténtico de ArcGIS Desktop 10.8: "Añadir mapa base..." (Add Basemap).
 * Permite seleccionar entre imágenes satelitales globales (Esri), OpenStreetMap,
 * fondos oscuros/claros minimalistas o topografía de relieve.
 */
@Composable
fun ArcMapBasemapDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val currentBasemap by viewModel.currentBasemap.collectAsState()
    val language by viewModel.language.collectAsState()
    val theme by viewModel.theme.collectAsState()

    val isEs = language == AppLanguage.ES
    val isDark = theme == ArcMapTheme.DARK

    val dialogBg = if (isDark) Color(0xFF1E222B) else Color(0xFFF9F9F9)
    val cardBg = if (isDark) Color(0xFF282E3A) else Color.White
    val textColor = if (isDark) Color(0xFFE6EDF3) else Color(0xFF1E293B)
    val subTextColor = if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)
    val borderColor = if (isDark) Color(0xFF384252) else Color(0xFFE2E8F0)
    val activeBorderColor = Color(0xFF0079C1)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF0079C1).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = null,
                            tint = Color(0xFF0079C1),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isEs) "Añadir mapa base..." else "Add Basemap...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = if (isEs) "ArcGIS Online & Open Map Services" else "ArcGIS Online & Open Map Services",
                        style = MaterialTheme.typography.labelSmall,
                        color = subTextColor
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BasemapType.values()) { basemap ->
                    val isSelected = currentBasemap == basemap

                    Surface(
                        color = cardBg,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) activeBorderColor else borderColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setBasemap(basemap)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icono representativo del mapa base
                            Surface(
                                color = getBasemapColor(basemap),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        getBasemapIcon(basemap),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isEs) basemap.titleEs else basemap.titleEn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = textColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isEs) basemap.descriptionEs else basemap.descriptionEn,
                                    fontSize = 11.sp,
                                    color = subTextColor,
                                    lineHeight = 14.sp
                                )
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Activo",
                                    tint = Color(0xFF0079C1),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEs) "Cerrar" else "Close", color = Color(0xFF0079C1), fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun getBasemapIcon(basemap: BasemapType): ImageVector {
    return when (basemap) {
        BasemapType.ESRI_IMAGERY -> Icons.Default.SatelliteAlt
        BasemapType.OPEN_STREET_MAP -> Icons.Default.Map
        BasemapType.CARTO_DARK -> Icons.Default.DarkMode
        BasemapType.CARTO_POSITRON -> Icons.Default.WbSunny
        BasemapType.OPEN_TOPO -> Icons.Default.Terrain
        BasemapType.NONE -> Icons.Default.LayersClear
    }
}

private fun getBasemapColor(basemap: BasemapType): Color {
    return when (basemap) {
        BasemapType.ESRI_IMAGERY -> Color(0xFF1B5E20) // Verde satélite
        BasemapType.OPEN_STREET_MAP -> Color(0xFF0277BD) // Azul mapa
        BasemapType.CARTO_DARK -> Color(0xFF263238) // Grafito
        BasemapType.CARTO_POSITRON -> Color(0xFF78909C) // Gris claro
        BasemapType.OPEN_TOPO -> Color(0xFF6D4C41) // Marrón montaña
        BasemapType.NONE -> Color(0xFF9E9E9E) // Gris neutro
    }
}
