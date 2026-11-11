package com.terraaxis.map.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import com.terraaxis.core.geometry.Geometry
import com.terraaxis.core.geometry.MultiPoint
import com.terraaxis.core.geometry.MultiPolygon
import com.terraaxis.core.geometry.MultiPolyline
import com.terraaxis.core.geometry.Point
import com.terraaxis.core.geometry.Polygon
import com.terraaxis.core.geometry.Polyline
import com.terraaxis.core.model.Feature
import com.terraaxis.core.model.VectorLayer
import com.terraaxis.core.symbology.ColorRgba
import com.terraaxis.core.symbology.LayerSymbology
import com.terraaxis.map.interaction.DigitizeSession
import com.terraaxis.map.interaction.MeasureSession
import com.terraaxis.map.viewport.MapViewport

class MapCanvasRenderer {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        color = Color.DKGRAY
        typeface = Typeface.DEFAULT_BOLD
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(40, 0, 0, 0)
        strokeWidth = 1f
    }
    private val selectionFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(120, 0, 255, 255) // Cyan fill
    }
    private val selectionStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.rgb(0, 255, 255) // Cyan stroke
        strokeWidth = 6f
    }
    private val measurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.rgb(233, 30, 99) // Pink / Magenta measure line
        strokeWidth = 5f
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }
    private val measureNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(233, 30, 99)
    }

    private val path = Path()

    fun render(
        canvas: Canvas,
        viewport: MapViewport,
        layers: List<VectorLayer>,
        measureSession: MeasureSession? = null,
        digitizeSession: DigitizeSession? = null
    ) {
        // 1. Draw clean GIS background (Light warm background like ArcMap paper)
        canvas.drawColor(Color.rgb(248, 249, 250))

        // 2. Draw Cartographic Coordinate Grid (Graticule)
        drawGraticule(canvas, viewport)

        // 3. Draw Vector Layers (rendered in reverse order: layer at index 0 is on top)
        for (i in layers.indices.reversed()) {
            val layer = layers[i]
            if (!layer.isVisible) continue
            drawLayer(canvas, viewport, layer)
        }

        // 4. Draw Active Measure Session
        measureSession?.let { drawMeasureOverlay(canvas, viewport, it) }

        // 5. Draw Active Digitize Session
        digitizeSession?.let { drawDigitizeOverlay(canvas, viewport, it) }
    }

    private fun drawGraticule(canvas: Canvas, viewport: MapViewport) {
        val step = calculateGridStep(viewport.zoom)
        val pTopLeft = viewport.screenToGeo(0f, 0f)
        val pBottomRight = viewport.screenToGeo(viewport.screenWidth, viewport.screenHeight)

        val minLon = minOf(pTopLeft.x, pBottomRight.x)
        val maxLon = maxOf(pTopLeft.x, pBottomRight.x)
        val minLat = minOf(pTopLeft.y, pBottomRight.y)
        val maxLat = maxOf(pTopLeft.y, pBottomRight.y)

        var lon = (minLon / step).toInt() * step
        while (lon <= maxLon) {
            val (sx, _) = viewport.geoToScreen(lon, viewport.centerLat)
            canvas.drawLine(sx, 0f, sx, viewport.screenHeight, gridPaint)
            lon += step
        }

        var lat = (minLat / step).toInt() * step
        while (lat <= maxLat) {
            val (_, sy) = viewport.geoToScreen(viewport.centerLon, lat)
            canvas.drawLine(0f, sy, viewport.screenWidth, sy, gridPaint)
            lat += step
        }
    }

    private fun calculateGridStep(zoom: Double): Double = when {
        zoom >= 16 -> 0.005
        zoom >= 14 -> 0.01
        zoom >= 12 -> 0.05
        zoom >= 10 -> 0.1
        zoom >= 8 -> 0.5
        zoom >= 6 -> 1.0
        else -> 5.0
    }

    private fun drawLayer(canvas: Canvas, viewport: MapViewport, layer: VectorLayer) {
        val symbology = layer.symbology
        val selectedIds = layer.selectedFeatureIds

        for (feature in layer.features) {
            val geom = feature.geometry ?: continue
            val isSelected = selectedIds.contains(feature.id)

            val fillColor = symbology.getFillColor(feature)
            val strokeColor = symbology.getStrokeColor(feature)
            val strokeWidth = symbology.getStrokeWidth(feature)

            fillPaint.color = fillColor.toArgbInt()
            fillPaint.alpha = (fillColor.alpha * layer.opacity * 255).toInt().coerceIn(0, 255)

            strokePaint.color = strokeColor.toArgbInt()
            strokePaint.alpha = (strokeColor.alpha * layer.opacity * 255).toInt().coerceIn(0, 255)
            strokePaint.strokeWidth = strokeWidth * 2.5f

            when (geom) {
                is Point -> drawPoint(canvas, viewport, geom, symbology, isSelected)
                is Polyline -> drawPolyline(canvas, viewport, geom, isSelected)
                is Polygon -> drawPolygon(canvas, viewport, geom, isSelected)
                is MultiPoint -> geom.points.forEach { drawPoint(canvas, viewport, it, symbology, isSelected) }
                is MultiPolyline -> geom.polylines.forEach { drawPolyline(canvas, viewport, it, isSelected) }
                is MultiPolygon -> geom.polygons.forEach { drawPolygon(canvas, viewport, it, isSelected) }
                else -> {}
            }

            // Draw label if configured
            layer.labelFieldName?.let { fieldName ->
                val label = feature.getString(fieldName)
                if (!label.isNullOrBlank()) {
                    drawLabel(canvas, viewport, geom, label)
                }
            }
        }
    }

    private fun drawPoint(
        canvas: Canvas,
        viewport: MapViewport,
        point: Point,
        symbology: LayerSymbology,
        isSelected: Boolean
    ) {
        val (sx, sy) = viewport.geoToScreen(point.x, point.y)
        if (sx < -50 || sx > viewport.screenWidth + 50 || sy < -50 || sy > viewport.screenHeight + 50) return

        val baseRadius = if (symbology is LayerSymbology.Simple) symbology.pointSizeDp * 2f else 16f

        if (isSelected) {
            canvas.drawCircle(sx, sy, baseRadius + 6f, selectionStrokePaint)
            canvas.drawCircle(sx, sy, baseRadius + 6f, selectionFillPaint)
        }

        canvas.drawCircle(sx, sy, baseRadius, fillPaint)
        canvas.drawCircle(sx, sy, baseRadius, strokePaint)
    }

    private fun drawPolyline(canvas: Canvas, viewport: MapViewport, polyline: Polyline, isSelected: Boolean) {
        val pts = polyline.points
        if (pts.size < 2) return

        path.reset()
        val (startX, startY) = viewport.geoToScreen(pts[0].x, pts[0].y)
        path.moveTo(startX, startY)

        for (i in 1 until pts.size) {
            val (px, py) = viewport.geoToScreen(pts[i].x, pts[i].y)
            path.lineTo(px, py)
        }

        if (isSelected) {
            canvas.drawPath(path, selectionStrokePaint)
        }
        canvas.drawPath(path, strokePaint)
    }

    private fun drawPolygon(canvas: Canvas, viewport: MapViewport, polygon: Polygon, isSelected: Boolean) {
        val rings = polygon.rings
        if (rings.isEmpty() || rings[0].size < 3) return

        path.reset()
        for (ring in rings) {
            if (ring.size < 3) continue
            val (startX, startY) = viewport.geoToScreen(ring[0].x, ring[0].y)
            path.moveTo(startX, startY)
            for (i in 1 until ring.size) {
                val (px, py) = viewport.geoToScreen(ring[i].x, ring[i].y)
                path.lineTo(px, py)
            }
            path.close()
        }

        if (isSelected) {
            canvas.drawPath(path, selectionFillPaint)
            canvas.drawPath(path, selectionStrokePaint)
        }

        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawLabel(canvas: Canvas, viewport: MapViewport, geom: Geometry, text: String) {
        val (lx, ly) = when (geom) {
            is Point -> viewport.geoToScreen(geom.x, geom.y)
            is Polygon -> {
                val c = geom.centroid()
                viewport.geoToScreen(c.x, c.y)
            }
            is Polyline -> {
                val mid = geom.points.getOrNull(geom.points.size / 2) ?: Point(0.0, 0.0)
                viewport.geoToScreen(mid.x, mid.y)
            }
            else -> viewport.geoToScreen(geom.boundingBox.centerX, geom.boundingBox.centerY)
        }

        if (lx in 0f..viewport.screenWidth && ly in 0f..viewport.screenHeight) {
            canvas.drawText(text, lx + 12f, ly - 8f, textPaint)
        }
    }

    private fun drawMeasureOverlay(canvas: Canvas, viewport: MapViewport, session: MeasureSession) {
        val pts = session.points
        if (pts.isEmpty()) return

        path.reset()
        val (startX, startY) = viewport.geoToScreen(pts[0].x, pts[0].y)
        path.moveTo(startX, startY)

        for (i in 1 until pts.size) {
            val (px, py) = viewport.geoToScreen(pts[i].x, pts[i].y)
            path.lineTo(px, py)
        }

        canvas.drawPath(path, measurePaint)

        // Draw nodes
        for (p in pts) {
            val (px, py) = viewport.geoToScreen(p.x, p.y)
            canvas.drawCircle(px, py, 14f, measureNodePaint)
            canvas.drawCircle(px, py, 18f, strokePaint)
        }
    }

    private fun drawDigitizeOverlay(canvas: Canvas, viewport: MapViewport, session: DigitizeSession) {
        val pts = session.points
        if (pts.isEmpty()) return

        path.reset()
        val (startX, startY) = viewport.geoToScreen(pts[0].x, pts[0].y)
        path.moveTo(startX, startY)

        for (i in 1 until pts.size) {
            val (px, py) = viewport.geoToScreen(pts[i].x, pts[i].y)
            path.lineTo(px, py)
        }

        canvas.drawPath(path, strokePaint)

        // Draw vertex handles
        for (p in pts) {
            val (px, py) = viewport.geoToScreen(p.x, p.y)
            canvas.drawRect(px - 10f, py - 10f, px + 10f, py + 10f, fillPaint)
        }
    }
}
