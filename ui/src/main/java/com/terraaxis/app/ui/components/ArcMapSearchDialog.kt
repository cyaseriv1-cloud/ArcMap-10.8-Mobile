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
import com.terraaxis.app.viewmodel.MainViewModel
import com.terraaxis.core.model.Feature
import com.terraaxis.core.model.VectorLayer

/**
 * Herramienta Find / Geocodificador de ArcMap 10.8:
 * Permite buscar calles, lugares (POIs), avenidas, sectores o coordenadas directas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcMapSearchDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val results = remember(searchQuery) { viewModel.searchPlaces(searchQuery) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF0079C1),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Find (Buscar Lugares y Calles)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("ArcMap Geocoder & Feature Search", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Ej. Vía de Evitamiento, San Antonio, Uvima...") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE91E63)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Acceso rápido a búsquedas comunes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = { searchQuery = "Evitamiento" },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Evitamiento", fontSize = 10.sp)
                    }
                    FilledTonalButton(
                        onClick = { searchQuery = "San Antonio" },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("San Antonio", fontSize = 10.sp)
                    }
                    FilledTonalButton(
                        onClick = { searchQuery = "Uvima" },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Uvima 7", fontSize = 10.sp)
                    }
                }

                HorizontalDivider()

                // Lista de Resultados encontrados
                if (searchQuery.isNotBlank() && results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No se encontraron coincidencias para \"$searchQuery\"", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(results) { (layer, feat) ->
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
                                    color = Color(0xFFE8EEF5),
                                    shape = RoundedCornerShape(3.dp),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF0079C1), modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(featName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color.Black)
                                    Text("${layer.name} • $featSub", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Icon(Icons.Default.Navigation, contentDescription = "Ir", tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            }
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
