package com.terraaxis.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.terraaxis.app.viewmodel.MainViewModel
import com.terraaxis.map.interaction.MapToolMode

@Composable
fun MeasureHud(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeTool by viewModel.activeTool.collectAsState()
    val measureSession by viewModel.measureSession.collectAsState()
    val digitizeSession by viewModel.digitizeSession.collectAsState()

    val isMeasuring = activeTool == MapToolMode.MEASURE_DISTANCE || activeTool == MapToolMode.MEASURE_AREA
    val isDigitizing = activeTool == MapToolMode.DIGITIZE_LINE || activeTool == MapToolMode.DIGITIZE_POLYGON

    if (!isMeasuring && !isDigitizing) return

    Card(
        modifier = modifier.padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isMeasuring) {
                Text(
                    if (activeTool == MapToolMode.MEASURE_DISTANCE) "Medición de Distancia" else "Medición de Área",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (activeTool == MapToolMode.MEASURE_DISTANCE) {
                    Text(
                        "Distancia: ${measureSession.formatDistance()}",
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        "Área: ${measureSession.formatArea()}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Perímetro: ${measureSession.formatDistance()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    "${measureSession.points.size} vértices marcados (toca en el mapa para agregar)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            measureSession.removeLastPoint()
                            viewModel.updateRender()
                        },
                        enabled = measureSession.points.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deshacer")
                    }

                    Button(
                        onClick = {
                            measureSession.clear()
                            viewModel.updateRender()
                        }
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Limpiar")
                    }
                }
            } else if (isDigitizing) {
                Text(
                    "Digitalización de Entidad",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    "${digitizeSession.points.size} vértices agregados",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            digitizeSession.removeLast()
                            viewModel.updateRender()
                        },
                        enabled = digitizeSession.points.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deshacer")
                    }

                    Button(
                        onClick = { viewModel.completeDigitizing() },
                        enabled = (activeTool == MapToolMode.DIGITIZE_LINE && digitizeSession.points.size >= 2) ||
                                (activeTool == MapToolMode.DIGITIZE_POLYGON && digitizeSession.points.size >= 3)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Guardar Entidad")
                    }
                }
            }
        }
    }
}
