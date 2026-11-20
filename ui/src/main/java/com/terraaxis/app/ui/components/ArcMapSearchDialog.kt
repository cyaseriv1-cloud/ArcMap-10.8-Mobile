package com.terraaxis.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terraaxis.app.viewmodel.AppLanguage
import com.terraaxis.app.viewmodel.ArcMapTheme
import com.terraaxis.app.viewmodel.MainViewModel

/**
 * Herramienta Find / Geocodificador de ArcMap 10.8:
 * - Pestaña 1: Entidades locales (Capa actual, vías, POIs y sectores)
 * - Pestaña 2: Ciudades y direcciones del mundo (OpenStreetMap Nominatim Geocoder)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcMapSearchDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(1) } // 0: Local, 1: Mundial por defecto
    var searchQuery by remember { mutableStateOf("") }

    val localResults = remember(searchQuery) { viewModel.searchPlaces(searchQuery) }
    val worldResults by viewModel.worldSearchResults.collectAsState()
    val isSearchingWorld by viewModel.isSearchingWorld.collectAsState()
    val language by viewModel.language.collectAsState()
    val theme by viewModel.theme.collectAsState()

    val isEs = language == AppLanguage.ES
    val isDark = theme == ArcMapTheme.DARK

    val dialogBg = if (isDark) Color(0xFF1E222B) else Color.White
    val textColor = if (isDark) Color(0xFFE6EDF3) else Color(0xFF1E293B)
    val subTextColor = if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)

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
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF0079C1),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        if (isEs) "Find (Buscador y Geocodificador)" else "Find (Geocoder & Search)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        if (isEs) "ArcMap World & Local Feature Search" else "ArcMap World & Local Feature Search",
                        style = MaterialTheme.typography.labelSmall,
                        color = subTextColor
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Selector de Modo (Local vs Mundial)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = if (isDark) Color(0xFF282E3A) else Color(0xFFF1F5F9),
                    contentColor = Color(0xFF0079C1)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                if (isEs) "📍 Entidades Locales" else "📍 Local Layers",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                if (isEs) "🌍 Ciudades del Mundo" else "🌍 World Cities",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                // Campo de Entrada de Búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (selectedTab == 1) {
                            viewModel.searchWorldPlaces(it)
                        }
                    },
                    placeholder = {
                        Text(
                            if (selectedTab == 0) {
                                if (isEs) "Ej. Evitamiento, San Antonio, Uvima..." else "E.g. Street, POI, Zone..."
                            } else {
                                if (isEs) "Ej. Lima, París, Tokio, Madrid, New York..." else "E.g. Paris, Tokyo, London, Rome..."
                            },
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (selectedTab == 0) Icons.Default.LocationOn else Icons.Default.Public,
                            contentDescription = null,
                            tint = Color(0xFF0079C1)
                        )
                    },
                    trailingIcon = {
                        if (isSearchingWorld) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF0079C1)
                            )
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.clearWorldSearchResults()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Acceso rápido con botones según la pestaña
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (selectedTab == 0) {
                        QuickSearchChip("Evitamiento") { searchQuery = "Evitamiento" }
                        QuickSearchChip("San Antonio") { searchQuery = "San Antonio" }
                        QuickSearchChip("Uvima 7") { searchQuery = "Uvima" }
                        QuickSearchChip("Aeropuerto") { searchQuery = "Aeropuerto" }
                    } else {
                        QuickSearchChip("Lima") {
                            searchQuery = "Lima, Perú"
                            viewModel.searchWorldPlaces(searchQuery)
                        }
                        QuickSearchChip("París") {
                            searchQuery = "París, Francia"
                            viewModel.searchWorldPlaces(searchQuery)
                        }
                        QuickSearchChip("Tokio") {
                            searchQuery = "Tokio, Japón"
                            viewModel.searchWorldPlaces(searchQuery)
                        }
                        QuickSearchChip("Madrid") {
                            searchQuery = "Madrid, España"
                            viewModel.searchWorldPlaces(searchQuery)
                        }
                    }
                }

                HorizontalDivider(color = if (isDark) Color(0xFF384252) else Color(0xFFE2E8F0))

                // ── RESULTADOS SEGÚN LA PESTAÑA ──────────────────────────────────
                if (selectedTab == 0) {
                    // Pestaña 0: Resultados Locales
                    if (searchQuery.isNotBlank() && localResults.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (isEs) "No se encontraron entidades para \"$searchQuery\"" else "No features found for \"$searchQuery\"",
                                color = subTextColor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            items(localResults) { (layer, feat) ->
                                val featName = feat.getString("NOMBRE") ?: "Entidad #${feat.id}"
                                val featSub = feat.getString("CATEGORIA") ?: feat.getString("TIPO") ?: layer.name

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            layer.selectFeature(feat.id, append = false)
                                            viewModel.zoomToFeature(feat)
                                            onDismiss()
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xFF0079C1).copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(3.dp),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF0079C1), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(featName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = textColor)
                                        Text("${layer.name} • $featSub", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                                    }
                                    Icon(Icons.Default.Navigation, contentDescription = "Ir", tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                }
                                HorizontalDivider(color = if (isDark) Color(0xFF282E3A) else Color(0xFFF1F5F9))
                            }
                        }
                    }
                } else {
                    // Pestaña 1: Resultados Mundiales (OpenStreetMap Geocoder)
                    if (searchQuery.isNotBlank() && worldResults.isEmpty() && !isSearchingWorld) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (isEs) "Escribe el nombre de una ciudad o dirección del mundo..." else "Type a world city or address...",
                                color = subTextColor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            items(worldResults) { place ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.zoomToLocation(place.lat, place.lon, zoomLevel = 14.5)
                                            onDismiss()
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(3.dp),
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = place.displayName.split(",").firstOrNull() ?: place.displayName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textColor
                                        )
                                        Text(
                                            text = place.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = subTextColor,
                                            maxLines = 2
                                        )
                                        Text(
                                            text = "Coords: %.4f, %.4f • %s".format(place.lat, place.lon, place.type),
                                            fontSize = 9.sp,
                                            color = Color(0xFF0079C1)
                                        )
                                    }
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Volar a la ciudad", tint = Color(0xFF0079C1), modifier = Modifier.size(18.dp))
                                }
                                HorizontalDivider(color = if (isDark) Color(0xFF282E3A) else Color(0xFFF1F5F9))
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

@Composable
private fun QuickSearchChip(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        modifier = Modifier.height(26.dp)
    ) {
        Text(label, fontSize = 10.sp)
    }
}
