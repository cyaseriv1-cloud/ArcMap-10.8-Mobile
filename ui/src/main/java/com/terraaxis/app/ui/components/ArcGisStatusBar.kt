package com.terraaxis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.terraaxis.app.viewmodel.MainViewModel

@Composable
fun ArcGisStatusBar(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val utm by viewModel.cursorUtm.collectAsState()
    val scale by viewModel.mapScale.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    val formattedScale = String.format(java.util.Locale.US, "1:%,d", scale)
    val formattedLon = String.format(java.util.Locale.US, "%.5f°", viewModel.viewport.centerLon)
    val formattedLat = String.format(java.util.Locale.US, "%.5f°", viewModel.viewport.centerLat)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status prompt
            statusMsg?.let { msg ->
                Text(
                    msg,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Geographic coordinates
            Text(
                "WGS84: $formattedLon, $formattedLat",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall
            )

            // Projected UTM coordinates
            Text(
                "UTM: ${utm.format()}",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall
            )

            // Scale
            Text(
                "Escala: $formattedScale",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
