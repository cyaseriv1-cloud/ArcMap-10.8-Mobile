package com.terraaxis.map.interaction

import com.terraaxis.core.crs.ProjectionEngine
import com.terraaxis.core.geometry.Point

enum class MapToolMode {
    PAN,
    IDENTIFY,
    SELECT,
    MEASURE_DISTANCE,
    MEASURE_AREA,
    DIGITIZE_POINT,
    DIGITIZE_LINE,
    DIGITIZE_POLYGON
}

class MeasureSession {
    val points = mutableListOf<Point>()

    fun addPoint(p: Point) {
        points.add(p)
    }

    fun removeLastPoint() {
        if (points.isNotEmpty()) {
            points.removeAt(points.size - 1)
        }
    }

    fun clear() {
        points.clear()
    }

    val totalDistanceMeters: Double
        get() {
            if (points.size < 2) return 0.0
            var dist = 0.0
            for (i in 0 until points.size - 1) {
                dist += ProjectionEngine.geodesicDistanceMeters(points[i], points[i + 1])
            }
            return dist
        }

    val areaSquareMeters: Double
        get() {
            if (points.size < 3) return 0.0
            return ProjectionEngine.geodesicPolygonAreaSquareMeters(points)
        }

    fun formatDistance(): String {
        val d = totalDistanceMeters
        return if (d >= 1000.0) {
            String.format(java.util.Locale.US, "%.3f km", d / 1000.0)
        } else {
            String.format(java.util.Locale.US, "%.1f m", d)
        }
    }

    fun formatArea(): String {
        val a = areaSquareMeters
        return when {
            a >= 1000000.0 -> String.format(java.util.Locale.US, "%.3f km²", a / 1000000.0)
            a >= 10000.0 -> String.format(java.util.Locale.US, "%.2f ha", a / 10000.0)
            else -> String.format(java.util.Locale.US, "%.1f m²", a)
        }
    }
}

class DigitizeSession {
    val points = mutableListOf<Point>()
    var targetLayerId: String? = null

    fun addPoint(p: Point) {
        points.add(p)
    }

    fun removeLast() {
        if (points.isNotEmpty()) points.removeAt(points.size - 1)
    }

    fun clear() {
        points.clear()
        targetLayerId = null
    }
}
