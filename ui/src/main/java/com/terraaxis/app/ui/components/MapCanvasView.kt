package com.terraaxis.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.terraaxis.app.viewmodel.MainViewModel
import com.terraaxis.map.basemap.TileManager
import com.terraaxis.map.interaction.MapToolMode
import com.terraaxis.map.render.MapCanvasRenderer

@Composable
fun MapCanvasView(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val renderer = remember { MapCanvasRenderer() }
    val currentBasemap by viewModel.currentBasemap.collectAsState()
    val tileManager = remember {
        TileManager(context.cacheDir.resolve("arcmap_tiles")).apply {
            onTileLoaded = {
                viewModel.updateRender()
            }
        }
    }

    val layers by viewModel.layers.collectAsState()
    val activeTool by viewModel.activeTool.collectAsState()
    val measureSession by viewModel.measureSession.collectAsState()
    val digitizeSession by viewModel.digitizeSession.collectAsState()
    val renderTrigger by viewModel.mapRenderTrigger.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val gpsLocation by viewModel.gpsLocation.collectAsState()
    val gpsAccuracy by viewModel.gpsAccuracy.collectAsState()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, _ ->
                    // Pan
                    if (pan.x != 0f || pan.y != 0f) {
                        viewModel.viewport.panByPixels(pan.x, pan.y)
                    }

                    // Pinch zoom around touch focal point
                    if (zoom != 1f) {
                        val zoomDelta = kotlin.math.ln(zoom.toDouble()) / kotlin.math.ln(2.0)
                        viewModel.zoomAround(centroid.x, centroid.y, zoomDelta)
                    } else if (pan.x != 0f || pan.y != 0f) {
                        viewModel.updateCoordinates(viewModel.viewport.centerLon, viewModel.viewport.centerLat)
                        viewModel.updateRender()
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    viewModel.handleMapTap(offset.x, offset.y)
                }
            }
    ) {
        // Sync viewport dimensions
        if (size.width > 0 && size.height > 0) {
            viewModel.viewport.screenWidth = size.width
            viewModel.viewport.screenHeight = size.height
        }

        // Trigger recomposition on state updates
        @Suppress("UNUSED_VARIABLE")
        val trigger = renderTrigger

        drawIntoCanvas { canvas ->
            renderer.render(
                canvas = canvas.nativeCanvas,
                viewport = viewModel.viewport,
                layers = layers,
                measureSession = if (activeTool == MapToolMode.MEASURE_DISTANCE || activeTool == MapToolMode.MEASURE_AREA) measureSession else null,
                digitizeSession = if (activeTool == MapToolMode.DIGITIZE_LINE || activeTool == MapToolMode.DIGITIZE_POLYGON) digitizeSession else null,
                isDarkMode = theme == com.terraaxis.app.viewmodel.ArcMapTheme.DARK,
                gpsLocation = gpsLocation,
                gpsAccuracy = gpsAccuracy,
                basemap = currentBasemap,
                tileManager = tileManager
            )
        }
    }
}