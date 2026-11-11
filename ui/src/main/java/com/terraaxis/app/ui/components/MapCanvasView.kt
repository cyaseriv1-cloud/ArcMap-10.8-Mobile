package com.terraaxis.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.terraaxis.app.viewmodel.MainViewModel
import com.terraaxis.map.interaction.MapToolMode
import com.terraaxis.map.render.MapCanvasRenderer

@Composable
fun MapCanvasView(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val renderer = remember { MapCanvasRenderer() }
    val layers by viewModel.layers.collectAsState()
    val activeTool by viewModel.activeTool.collectAsState()
    val measureSession by viewModel.measureSession.collectAsState()
    val digitizeSession by viewModel.digitizeSession.collectAsState()
    val renderTrigger by viewModel.mapRenderTrigger.collectAsState()

    var canvasSize by remember { mutableStateOf(Offset(1080f, 1920f)) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(activeTool) {
                if (activeTool == MapToolMode.PAN) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        viewModel.viewport.panByPixels(pan.x, pan.y)
                        if (zoom != 1f) {
                            val zoomDelta = kotlin.math.ln(zoom.toDouble()) / kotlin.math.ln(2.0)
                            viewModel.viewport.zoomBy(zoomDelta)
                        }
                        viewModel.updateCoordinates(viewModel.viewport.centerLon, viewModel.viewport.centerLat)
                        viewModel.updateRender()
                    }
                }
            }
            .pointerInput(activeTool) {
                detectTapGestures { offset ->
                    viewModel.handleMapTap(offset.x, offset.y)
                }
            }
    ) {
        // Update viewport dimensions
        if (size.width > 0 && size.height > 0) {
            viewModel.viewport.screenWidth = size.width
            viewModel.viewport.screenHeight = size.height
        }

        // Trigger recomposition on state changes
        val trigger = renderTrigger

        drawIntoCanvas { canvas ->
            renderer.render(
                canvas = canvas.nativeCanvas,
                viewport = viewModel.viewport,
                layers = layers,
                measureSession = if (activeTool == MapToolMode.MEASURE_DISTANCE || activeTool == MapToolMode.MEASURE_AREA) measureSession else null,
                digitizeSession = if (activeTool == MapToolMode.DIGITIZE_LINE || activeTool == MapToolMode.DIGITIZE_POLYGON) digitizeSession else null
            )
        }
    }
}
