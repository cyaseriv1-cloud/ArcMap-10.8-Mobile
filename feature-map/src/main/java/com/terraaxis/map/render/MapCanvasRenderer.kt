package com.terraaxis.map.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
import com.terraaxis.core.crs.ProjectionEngine
import com.terraaxis.map.basemap.BasemapType
import com.terraaxis.map.basemap.TileManager
import com.terraaxis.map.interaction.DigitizeSession
import com.terraaxis.map.interaction.MeasureSession
import com.terraaxis.map.viewport.MapViewport
import kotlin.math.floor
import kotlin.math.roundToInt

class MapCanvasRenderer {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        color = Color.rgb(20, 20, 20)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val textHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(45, 0, 0, 0)
        strokeWidth = 1.2f
    }

    private val gridTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
        color = Color.rgb(100, 100, 100)
        typeface = Typeface.MONOSPACE
    }

    private val selectionFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(130, 0, 255, 255) // ArcGIS Cyan fill
    }

    private val selectionStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.rgb(0, 255, 255) // ArcGIS Cyan outline
        strokeWidth = 7f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val selectionGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(80, 0, 200, 255)
        strokeWidth = 14f
    }

    private val measurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.rgb(233, 30, 99) // Magenta measure line
        strokeWidth = 6f
        pathEffect = DashPathEffect(floatArrayOf(20f, 12f), 0f)
    }

    private val measureNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(233, 30, 99)
    }

    private val measureNodeInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val scaleBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private val scaleBarWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val scaleBarStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 2f
    }

    private val scaleBarTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        color = Color.BLACK
        typeface = Typeface.DEFAULT_BOLD
    }

    // GPS Location Paints
    private val gpsHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(45, 0, 121, 193)
    }

    private val gpsHaloStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(120, 0, 121, 193)
        strokeWidth = 2f
    }

    private val gpsDotWhiteBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val gpsDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(0, 121, 193) // ESRI Blue
    }

    private val tileBitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    private val attributionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f
        color = Color.rgb(60, 60, 60)
        typeface = Typeface.DEFAULT_BOLD
    }

    private val attributionBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(175, 255, 255, 255)
    }

    private val path = Path()

    fun render(
        canvas: Canvas,
        viewport: MapViewport,
        layers: List<VectorLayer>,
        measureSession: MeasureSession? = null,
        digitizeSession: DigitizeSession? = null,
        isDarkMode: Boolean = false,
        gpsLocation: Point? = null,
        gpsAccuracy: Float? = null,
        basemap: BasemapType = BasemapType.NONE,
        tileManager: TileManager? = null
    ) {
        // Adapt theme colors
        if (isDarkMode) {
            canvas.drawColor(Color.rgb(22, 26, 33)) // Dark slate
            gridPaint.color = Color.argb(35, 255, 255, 255)
            gridTextPaint.color = Color.rgb(160, 175, 190)
            scaleBarPaint.color = Color.rgb(230, 230, 230)
            scaleBarWhitePaint.color = Color.rgb(40, 48, 60)
            scaleBarStrokePaint.color = Color.rgb(230, 230, 230)
            scaleBarTextPaint.color = Color.rgb(230, 230, 230)
            textHaloPaint.color = Color.rgb(22, 26, 33)
            textPaint.color = Color.rgb(240, 240, 240)
            attributionBgPaint.color = Color.argb(190, 22, 26, 33)
            attributionPaint.color = Color.rgb(200, 210, 225)
        } else {
            canvas.drawColor(Color.rgb(246, 247, 248)) // Light paper
            gridPaint.color = Color.argb(45, 0, 0, 0)
            gridTextPaint.color = Color.rgb(100, 100, 100)
            scaleBarPaint.color = Color.BLACK
            scaleBarWhitePaint.color = Color.WHITE
            scaleBarStrokePaint.color = Color.BLACK
            scaleBarTextPaint.color = Color.BLACK
            textHaloPaint.color = Color.WHITE
            textPaint.color = Color.rgb(20, 20, 20)
            attributionBgPaint.color = Color.argb(180, 255, 255, 255)
            attributionPaint.color = Color.rgb(50, 50, 50)
        }

        // 1. Renderizar Mapa Base (OpenStreetMap, Esri Satélite, CartoDB, etc.)
        if (basemap != BasemapType.NONE && tileManager != null) {
            drawBasemap(canvas, viewport, basemap, tileManager)
            // Hacer la cuadrícula más sutil sobre imágenes satelitales
            if (basemap == BasemapType.ESRI_IMAGERY) {
                gridPaint.color = Color.argb(30, 255, 255, 255)
                gridTextPaint.color = Color.rgb(220, 220, 220)
            }
        }

        // 2. Draw Cartographic Coordinate Grid (Graticule)
        drawGraticule(canvas, viewport)

        // 3. Draw Vector Layers (rendered in reverse order: layer at index 0 is on top)
        for (i in layers.indices.reversed()) {
            val layer = layers[i]
            if (!layer.isVisible) continue
            drawLayer(canvas, viewport, layer)
        }

        // 4. Draw GPS Location Indicator (Google Maps / ArcMap Collector Blue Dot)
        gpsLocation?.let { drawGpsLocation(canvas, viewport, it, gpsAccuracy) }

        // 5. Draw Active Measure Session
        measureSession?.let { drawMeasureOverlay(canvas, viewport, it) }

        // 6. Draw Active Digitize Session
        digitizeSession?.let { drawDigitizeOverlay(canvas, viewport, it) }

        // 7. Draw Cartographic Scale Bar & North Arrow
        drawScaleBar(canvas, viewport)
        drawNorthArrow(canvas, viewport)
    }

    private fun drawBasemap(
        canvas: Canvas,
        viewport: MapViewport,
        basemap: BasemapType,
        tileManager: TileManager
    ) {
        if (basemap == BasemapType.NONE || basemap.tileUrlTemplate.isBlank()) return

        val currentZoom = viewport.zoom
        val z = floor(currentZoom).toInt().coerceIn(basemap.minZoom, basemap.maxZoom)
        val nTiles = 1 shl z
        val worldSizeMeters = 40075016.686
        val halfWorld = 20037508.34
        val tileMeters = worldSizeMeters / nTiles

        val screenW = viewport.screenWidth
        val screenH = viewport.screenHeight
        val mpp = viewport.metersPerPixel
        if (mpp <= 0.0) return

        val mercCenter = ProjectionEngine.wgs84ToWebMercator(viewport.centerLat, viewport.centerLon)

        // Bounding box en Web Mercator del área visible en pantalla
        val minMercX = mercCenter.x + (0f - screenW / 2f) * mpp
        val maxMercX = mercCenter.x + (screenW - screenW / 2f) * mpp
        val maxMercY = mercCenter.y - (0f - screenH / 2f) * mpp
        val minMercY = mercCenter.y - (screenH - screenH / 2f) * mpp

        val minTileX = floor((minMercX + halfWorld) / tileMeters).toInt().coerceIn(0, nTiles - 1)
        val maxTileX = floor((maxMercX + halfWorld) / tileMeters).toInt().coerceIn(0, nTiles - 1)
        val minTileY = floor((halfWorld - maxMercY) / tileMeters).toInt().coerceIn(0, nTiles - 1)
        val maxTileY = floor((halfWorld - minMercY) / tileMeters).toInt().coerceIn(0, nTiles - 1)

        for (tx in minTileX..maxTileX) {
            for (ty in minTileY..maxTileY) {
                val tileLeftMercX = -halfWorld + tx * tileMeters
                val tileRightMercX = -halfWorld + (tx + 1) * tileMeters
                val tileTopMercY = halfWorld - ty * tileMeters
                val tileBottomMercY = halfWorld - (ty + 1) * tileMeters

                val left = (screenW / 2f + (tileLeftMercX - mercCenter.x) / mpp).toFloat()
                val right = (screenW / 2f + (tileRightMercX - mercCenter.x) / mpp).toFloat()
                val top = (screenH / 2f + (mercCenter.y - tileTopMercY) / mpp).toFloat()
                val bottom = (screenH / 2f + (mercCenter.y - tileBottomMercY) / mpp).toFloat()

                val destRect = RectF(left, top, right, bottom)
                val bmp = tileManager.getTile(basemap, z, tx, ty)
                if (bmp != null) {
                    canvas.drawBitmap(bmp, null, destRect, tileBitmapPaint)
                }
            }
        }

        // Atribución de fuente de datos cartográfica
        if (basemap.attribution.isNotBlank()) {
            val attrText = basemap.attribution
            val textWidth = attributionPaint.measureText(attrText)
            val rectLeft = screenW - textWidth - 20f
            val rectTop = screenH - 24f
            canvas.drawRect(rectLeft - 6f, rectTop - 18f, screenW - 6f, rectTop + 6f, attributionBgPaint)
            canvas.drawText(attrText, rectLeft, rectTop, attributionPaint)
        }
    }

    private fun drawGpsLocation(canvas: Canvas, viewport: MapViewport, loc: Point, accuracy: Float?) {
        val (sx, sy) = viewport.geoToScreen(loc.x, loc.y)
        if (sx < -200 || sx > viewport.screenWidth + 200 || sy < -200 || sy > viewport.screenHeight + 200) return

        // 1. Halo de precisión en metros convertido a píxeles
        val mpp = viewport.metersPerPixel
        val radiusPx = if (accuracy != null && accuracy > 0f && mpp > 0.0) {
            (accuracy / mpp).toFloat().coerceIn(16f, 300f)
        } else {
            36f
        }

        canvas.drawCircle(sx, sy, radiusPx, gpsHaloPaint)
        canvas.drawCircle(sx, sy, radiusPx, gpsHaloStrokePaint)

        // 2. Círculo exterior blanco
        canvas.drawCircle(sx, sy, 14f, gpsDotWhiteBorder)

        // 3. Punto azul interior brillante
        canvas.drawCircle(sx, sy, 10f, gpsDotPaint)

        // 4. Centro blanco
        canvas.drawCircle(sx, sy, 3f, gpsDotWhiteBorder)
    }

    private fun drawGraticule(canvas: Canvas, viewport: MapViewport) {
        val step = calculateGridStep(viewport.zoom)
        val pTopLeft = viewport.screenToGeo(0f, 0f)
        val pBottomRight = viewport.screenToGeo(viewport.screenWidth, viewport.screenHeight)

        val minLon = minOf(pTopLeft.x, pBottomRight.x)
        val maxLon = maxOf(pTopLeft.x, pBottomRight.x)
        val minLat = minOf(pTopLeft.y, pBottomRight.y)
        val maxLat = maxOf(pTopLeft.y, pBottomRight.y)

        val minLabelSpacingPx = 90f // Mínima separación en px entre etiquetas para evitar solapamiento

        // Longitude lines
        var lon = floor(minLon / step) * step
        var lastLonLabelX = -999f
        while (lon <= maxLon + step * 0.5) {
            val (sx, _) = viewport.geoToScreen(lon, viewport.centerLat)
            if (sx in 0f..viewport.screenWidth) {
                canvas.drawLine(sx, 0f, sx, viewport.screenHeight, gridPaint)
                // Solo dibujar etiqueta si hay suficiente espacio desde la anterior
                if (sx - lastLonLabelX >= minLabelSpacingPx) {
                    val lonText = formatCoord(lon, isLat = false)
                    canvas.drawText(lonText, sx + 4f, 30f, gridTextPaint)
                    lastLonLabelX = sx
                }
            }
            lon += step
        }

        // Latitude lines
        var lat = floor(minLat / step) * step
        var lastLatLabelY = 999f
        while (lat <= maxLat + step * 0.5) {
            val (_, sy) = viewport.geoToScreen(viewport.centerLon, lat)
            if (sy in 0f..viewport.screenHeight) {
                canvas.drawLine(0f, sy, viewport.screenWidth, sy, gridPaint)
                // Solo dibujar etiqueta si hay suficiente espacio desde la anterior
                if (lastLatLabelY - sy >= minLabelSpacingPx) {
                    val latText = formatCoord(lat, isLat = true)
                    canvas.drawText(latText, 10f, sy - 4f, gridTextPaint)
                    lastLatLabelY = sy
                }
            }
            lat += step
        }
    }

    private fun formatCoord(value: Double, isLat: Boolean): String {
        val direction = if (isLat) {
            if (value >= 0) "N" else "S"
        } else {
            if (value >= 0) "E" else "W"
        }
        val absVal = kotlin.math.abs(value)
        return String.format(java.util.Locale.US, "%.3f°%s", absVal, direction)
    }

    private fun calculateGridStep(zoom: Double): Double = when {
        zoom >= 16 -> 0.005
        zoom >= 14 -> 0.01
        zoom >= 12 -> 0.02
        zoom >= 10 -> 0.05
        zoom >= 8 -> 0.2
        zoom >= 6 -> 0.5
        else -> 2.0
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

            // Draw label with halo
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
        if (sx < -60 || sx > viewport.screenWidth + 60 || sy < -60 || sy > viewport.screenHeight + 60) return

        val baseRadius = if (symbology is LayerSymbology.Simple) symbology.pointSizeDp * 2.2f else 18f

        // Draw normal point
        canvas.drawCircle(sx, sy, baseRadius, fillPaint)
        canvas.drawCircle(sx, sy, baseRadius, strokePaint)

        // If selected: draw glowing cyan outline ON TOP!
        if (isSelected) {
            canvas.drawCircle(sx, sy, baseRadius + 8f, selectionGlowPaint)
            canvas.drawCircle(sx, sy, baseRadius + 4f, selectionStrokePaint)
            canvas.drawCircle(sx, sy, baseRadius + 4f, selectionFillPaint)
        }
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

        // Draw normal polyline first
        canvas.drawPath(path, strokePaint)

        // If selected: draw cyan selection ON TOP!
        if (isSelected) {
            canvas.drawPath(path, selectionGlowPaint)
            canvas.drawPath(path, selectionStrokePaint)
        }
    }

    private fun drawPolygon(canvas: Canvas, viewport: MapViewport, polygon: Polygon, isSelected: Boolean) {
        val rings = polygon.rings
        if (rings.isEmpty() || rings[0].size < 3) return

        path.reset()
        path.fillType = Path.FillType.EVEN_ODD

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

        // 1. Draw regular polygon fill and stroke FIRST
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)

        // 2. If selected: draw ArcGIS Cyan highlight OVER the normal fill and stroke!
        if (isSelected) {
            canvas.drawPath(path, selectionFillPaint)
            canvas.drawPath(path, selectionGlowPaint)
            canvas.drawPath(path, selectionStrokePaint)
        }
    }

    private fun drawLabel(canvas: Canvas, viewport: MapViewport, geom: Geometry, text: String) {
        val (lx, ly) = when (geom) {
            is Point -> {
                val (px, py) = viewport.geoToScreen(geom.x, geom.y)
                Pair(px + 16f, py - 12f)
            }
            is Polygon -> {
                val c = geom.centroid()
                val (cx, cy) = viewport.geoToScreen(c.x, c.y)
                Pair(cx, cy)
            }
            is Polyline -> {
                val mid = geom.points.getOrNull(geom.points.size / 2) ?: Point(0.0, 0.0)
                val (mx, my) = viewport.geoToScreen(mid.x, mid.y)
                Pair(mx, my - 10f)
            }
            else -> {
                val (bx, by) = viewport.geoToScreen(geom.boundingBox.centerX, geom.boundingBox.centerY)
                Pair(bx, by)
            }
        }

        if (lx in -100f..viewport.screenWidth + 100f && ly in -100f..viewport.screenHeight + 100f) {
            // Draw halo first, then text on top
            canvas.drawText(text, lx, ly, textHaloPaint)
            canvas.drawText(text, lx, ly, textPaint)
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
            canvas.drawCircle(px, py, 16f, measureNodePaint)
            canvas.drawCircle(px, py, 10f, measureNodeInnerPaint)
            canvas.drawCircle(px, py, 4f, measureNodePaint)
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
            canvas.drawRect(px - 10f, py - 10f, px + 10f, py + 10f, strokePaint)
        }
    }

    private fun drawScaleBar(canvas: Canvas, viewport: MapViewport) {
        val mpp = viewport.metersPerPixel
        val targetPixels = 200.0 // Desired width in pixels for scale bar
        val rawMeters = targetPixels * mpp

        // Round rawMeters to a nice round number (e.g., 50, 100, 200, 500, 1000, 2000, 5000, 10000)
        val niceMeters = getNiceScaleDistance(rawMeters)
        val barWidthPx = (niceMeters / mpp).toFloat()

        val startX = 32f
        val startY = viewport.screenHeight - 50f
        val barHeight = 12f

        // Draw alternating black/white segments
        val halfW = barWidthPx / 2f
        // Left segment (black)
        canvas.drawRect(startX, startY, startX + halfW, startY + barHeight, scaleBarPaint)
        // Right segment (white)
        canvas.drawRect(startX + halfW, startY, startX + barWidthPx, startY + barHeight, scaleBarWhitePaint)
        // Outline
        canvas.drawRect(startX, startY, startX + barWidthPx, startY + barHeight, scaleBarStrokePaint)
        // Center divider line
        canvas.drawLine(startX + halfW, startY, startX + halfW, startY + barHeight, scaleBarStrokePaint)

        // Label
        val label = if (niceMeters >= 1000.0) {
            "${(niceMeters / 1000.0).roundToInt()} km"
        } else {
            "${niceMeters.roundToInt()} m"
        }

        canvas.drawText("0", startX - 4f, startY - 8f, scaleBarTextPaint)
        canvas.drawText(label, startX + barWidthPx - 20f, startY - 8f, scaleBarTextPaint)
    }

    private fun getNiceScaleDistance(rawMeters: Double): Double {
        val exponent = floor(kotlin.math.log10(rawMeters))
        val pow10 = Math.pow(10.0, exponent)
        val fraction = rawMeters / pow10
        val niceFraction = when {
            fraction < 1.5 -> 1.0
            fraction < 3.5 -> 2.0
            fraction < 7.5 -> 5.0
            else -> 10.0
        }
        return niceFraction * pow10
    }

    private fun drawNorthArrow(canvas: Canvas, viewport: MapViewport) {
        val cx = viewport.screenWidth - 70f
        val cy = 100f
        val size = 26f

        // Draw north arrow
        val arrowPath = Path()
        arrowPath.moveTo(cx, cy - size)
        arrowPath.lineTo(cx - size * 0.45f, cy + size * 0.7f)
        arrowPath.lineTo(cx, cy + size * 0.35f)
        arrowPath.close()

        canvas.drawPath(arrowPath, scaleBarPaint)

        val rightArrowPath = Path()
        rightArrowPath.moveTo(cx, cy - size)
        rightArrowPath.lineTo(cx + size * 0.45f, cy + size * 0.7f)
        rightArrowPath.lineTo(cx, cy + size * 0.35f)
        rightArrowPath.close()

        canvas.drawPath(rightArrowPath, scaleBarWhitePaint)
        canvas.drawPath(rightArrowPath, scaleBarStrokePaint)

        canvas.drawText("N", cx - 8f, cy - size - 6f, scaleBarTextPaint)
    }
}