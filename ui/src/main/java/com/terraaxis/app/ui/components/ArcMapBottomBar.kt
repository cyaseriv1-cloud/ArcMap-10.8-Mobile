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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terraaxis.app.viewmodel.ArcMapViewMode
import com.terraaxis.app.viewmodel.MainViewModel

/**
 * Barra inferior auténtica de ArcMap 10.8:
 * - Esquina izquierda: Conmutador de Vistas (Data View 🗺️ / Layout View 📄 / Refresh 🔄)
 * - Barra de Estado: Estado actual, Coordenadas Geográficas (grados/min/seg), Coordenadas UTM y Escala.
 */
@Composable
fun ArcMapBottomBar(
    viewModel: MainViewModel
) {
    val statusMsg by viewModel.statusMessage.collectAsState()
    val cursorUtm by viewModel.cursorUtm.collectAsState()
    val mapScale by viewModel.mapScale.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val isDark = theme == com.terraaxis.app.viewmodel.ArcMapTheme.DARK

    val barBg = if (isDark) Color(0xFF161B22) else Color(0xFFEBE9E4)
    val borderCol = if (isDark) Color(0xFF30363D) else Color(0xFFB0B0B0)
    val textColor = if (isDark) Color(0xFFE6EDF3) else Color.Black
    val switchBg = if (isDark) Color(0xFF21262D) else Color(0xFFD6D4CF)

    Surface(
        color = barBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── CONMUTADOR DE VISTAS CLÁSICO DE ARCMAP (Data View / Layout View) ──
            Row(
                modifier = Modifier
                    .background(switchBg, RoundedCornerShape(2.dp))
                    .border(1.dp, borderCol, RoundedCornerShape(2.dp))
                    .padding(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Data View (Globo)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (viewMode == ArcMapViewMode.DATA_VIEW) (if (isDark) Color(0xFF1E3A5F) else Color.White) else Color.Transparent,
                            RoundedCornerShape(2.dp)
                        )
                        .border(
                            1.dp,
                            if (viewMode == ArcMapViewMode.DATA_VIEW) Color(0xFF0079C1) else Color.Transparent,
                            RoundedCornerShape(2.dp)
                        )
                        .clickable { viewModel.setViewMode(ArcMapViewMode.DATA_VIEW) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = "Data View",
                        tint = if (viewMode == ArcMapViewMode.DATA_VIEW) Color(0xFF0079C1) else Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Botón Layout View (Lámina de papel)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (viewMode == ArcMapViewMode.LAYOUT_VIEW) (if (isDark) Color(0xFF1E3A5F) else Color.White) else Color.Transparent,
                            RoundedCornerShape(2.dp)
                        )
                        .border(
                            1.dp,
                            if (viewMode == ArcMapViewMode.LAYOUT_VIEW) Color(0xFF0079C1) else Color.Transparent,
                            RoundedCornerShape(2.dp)
                        )
                        .clickable { viewModel.setViewMode(ArcMapViewMode.LAYOUT_VIEW) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = "Layout View",
                        tint = if (viewMode == ArcMapViewMode.LAYOUT_VIEW) Color(0xFF0079C1) else Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Botón Refresh (Actualizar)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { viewModel.updateRender() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = if (isDark) Color.LightGray else Color.DarkGray,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // ── BARRA DE ESTADO Y COORDENADAS ──
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mensaje de estado
                Text(
                    text = statusMsg ?: "Listo",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                Text("|", color = Color.Gray, fontSize = 11.sp)

                // Coordenadas WGS 84
                val lon = viewModel.viewport.centerLon
                val lat = viewModel.viewport.centerLat
                val lonDir = if (lon >= 0) "E" else "W"
                val latDir = if (lat >= 0) "N" else "S"
                val coordText = String.format(java.util.Locale.US, "%.4f°%s  %.4f°%s", kotlin.math.abs(lon), lonDir, kotlin.math.abs(lat), latDir)

                Text(
                    text = coordText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isDark) Color(0xFF4DD0E1) else Color(0xFF004D40)
                )

                Text("|", color = Color.Gray, fontSize = 11.sp)

                // Coordenadas UTM
                Text(
                    text = "UTM: ${cursorUtm.format()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isDark) Color(0xFF90CAF9) else Color(0xFF1A237E)
                )

                Text("|", color = Color.Gray, fontSize = 11.sp)

                // Escala
                Text(
                    text = "1: $mapScale",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}
