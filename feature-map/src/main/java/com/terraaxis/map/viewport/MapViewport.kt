package com.terraaxis.map.viewport

import com.terraaxis.core.crs.ProjectionEngine
import com.terraaxis.core.geometry.BoundingBox
import com.terraaxis.core.geometry.Point
import kotlin.math.*

data class MapViewport(
    var centerLon: Double = -71.9675,
    var centerLat: Double = -13.5320,
    var zoom: Double = 12.0,
    var screenWidth: Float = 1080f,
    var screenHeight: Float = 1920f
) {
    val scaleDenominator: Long
        get() {
            // Earth circumference at equator ~ 40,075,016 meters
            // Resolution (meters/pixel) = 40075016.686 * cos(lat) / (256 * 2^zoom)
            val latRad = Math.toRadians(centerLat)
            val resolutionMetersPerPixel = 156543.03392 * cos(latRad) / 2.0.pow(zoom)
            // Assuming standard mobile DPI of ~300 dpi (0.000084666 m/pixel standard screen pitch)
            val scale = resolutionMetersPerPixel / 0.000264583
            return scale.toLong().coerceAtLeast(100L)
        }

    fun setScale(targetScale: Long) {
        if (targetScale <= 0) return
        val latRad = Math.toRadians(centerLat)
        val numerator = 156543.03392 * cos(latRad)
        val denominator = targetScale * 0.000264583
        if (denominator > 0.0) {
            val calculatedZoom = log2(numerator / denominator)
            zoom = calculatedZoom.coerceIn(2.0, 21.0)
        }
    }

    fun geoToScreen(lon: Double, lat: Double): Pair<Float, Float> {
        val mercCenter = ProjectionEngine.wgs84ToWebMercator(centerLat, centerLon)
        val mercPoint = ProjectionEngine.wgs84ToWebMercator(lat, lon)

        val worldSize = 256.0 * 2.0.pow(zoom)
        val metersPerPixel = 40075016.686 / worldSize

        val dx = (mercPoint.x - mercCenter.x) / metersPerPixel
        val dy = (mercCenter.y - mercPoint.y) / metersPerPixel // Screen Y is inverted

        val screenX = (screenWidth / 2f) + dx.toFloat()
        val screenY = (screenHeight / 2f) + dy.toFloat()
        return Pair(screenX, screenY)
    }

    fun screenToGeo(screenX: Float, screenY: Float): Point {
        val mercCenter = ProjectionEngine.wgs84ToWebMercator(centerLat, centerLon)
        val worldSize = 256.0 * 2.0.pow(zoom)
        val metersPerPixel = 40075016.686 / worldSize

        val dx = (screenX - screenWidth / 2f) * metersPerPixel
        val dy = (screenHeight / 2f - screenY) * metersPerPixel

        val mercX = mercCenter.x + dx
        val mercY = mercCenter.y + dy

        return ProjectionEngine.webMercatorToWgs84(mercX, mercY)
    }

    fun zoomToExtent(box: BoundingBox, paddingFactor: Double = 1.2) {
        if (box == BoundingBox.EMPTY) return
        centerLon = box.centerX
        centerLat = box.centerY

        val mercMin = ProjectionEngine.wgs84ToWebMercator(box.minY, box.minX)
        val mercMax = ProjectionEngine.wgs84ToWebMercator(box.maxY, box.maxX)
        val spanX = abs(mercMax.x - mercMin.x) * paddingFactor
        val spanY = abs(mercMax.y - mercMin.y) * paddingFactor

        val metersPerPixelX = spanX / maxOf(1f, screenWidth)
        val metersPerPixelY = spanY / maxOf(1f, screenHeight)
        val maxMetersPerPixel = maxOf(metersPerPixelX, metersPerPixelY)

        if (maxMetersPerPixel > 0.0) {
            val calculatedZoom = log2(40075016.686 / (256.0 * maxMetersPerPixel))
            zoom = calculatedZoom.coerceIn(2.0, 20.0)
        }
    }

    fun panByPixels(dx: Float, dy: Float) {
        val newCenter = screenToGeo(screenWidth / 2f - dx, screenHeight / 2f - dy)
        centerLon = newCenter.x
        centerLat = newCenter.y
    }

    val metersPerPixel: Double
        get() {
            val worldSize = 256.0 * 2.0.pow(zoom)
            return 40075016.686 / worldSize
        }

    fun zoomBy(delta: Double) {
        zoom = (zoom + delta).coerceIn(2.0, 21.0)
    }

    fun zoomAround(focalX: Float, focalY: Float, delta: Double) {
        val focalGeo = screenToGeo(focalX, focalY)
        val focalMerc = ProjectionEngine.wgs84ToWebMercator(focalGeo.y, focalGeo.x)

        zoom = (zoom + delta).coerceIn(2.0, 21.0)

        val newMpp = metersPerPixel
        val dx = (focalX - screenWidth / 2f) * newMpp
        val dy = (screenHeight / 2f - focalY) * newMpp

        val newMercCenterX = focalMerc.x - dx
        val newMercCenterY = focalMerc.y - dy

        val newCenter = ProjectionEngine.webMercatorToWgs84(newMercCenterX, newMercCenterY)
        centerLon = newCenter.x
        centerLat = newCenter.y
    }
}
