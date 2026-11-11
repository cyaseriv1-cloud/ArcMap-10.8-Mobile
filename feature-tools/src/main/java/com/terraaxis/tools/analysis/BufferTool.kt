package com.terraaxis.tools.analysis

import com.terraaxis.core.crs.ProjectionEngine
import com.terraaxis.core.geometry.*
import com.terraaxis.core.model.Feature
import com.terraaxis.core.model.Field
import com.terraaxis.core.model.FieldType
import com.terraaxis.core.model.VectorLayer
import com.terraaxis.core.symbology.ColorRgba
import com.terraaxis.core.symbology.LayerSymbology
import kotlin.math.*

object BufferTool {

    private const val SEGMENTS_PER_CIRCLE = 32

    /**
     * Executes ArcToolbox Buffer: creates a buffer zone polygon of [distanceMeters] around each feature.
     */
    fun execute(
        inputLayer: VectorLayer,
        distanceMeters: Double,
        outputLayerName: String = "${inputLayer.name}_buffer_${distanceMeters.toInt()}m"
    ): VectorLayer {
        val outFeatures = mutableListOf<Feature>()

        for ((idx, feat) in inputLayer.features.withIndex()) {
            val geom = feat.geometry ?: continue
            val bufferedGeom = bufferGeometry(geom, distanceMeters) ?: continue

            val newAttrs = HashMap(feat.attributes)
            newAttrs["BUFF_DIST"] = distanceMeters
            outFeatures.add(Feature((idx + 1).toLong(), bufferedGeom, newAttrs))
        }

        val outFields = inputLayer.fields.map { it.copy() }.toMutableList()
        if (outFields.none { it.name == "BUFF_DIST" }) {
            outFields.add(Field("BUFF_DIST", FieldType.DOUBLE))
        }

        val resultLayer = VectorLayer(
            name = outputLayerName,
            geometryType = GeometryType.POLYGON,
            symbology = LayerSymbology.Simple(
                fillColor = ColorRgba(255, 167, 38, 0.45f), // Semi-transparent orange
                strokeColor = ColorRgba(239, 108, 0),
                strokeWidthDp = 2.0f
            )
        )
        resultLayer.table.fields.addAll(outFields)
        resultLayer.table.features.addAll(outFeatures)
        return resultLayer
    }

    private fun bufferGeometry(geom: Geometry, distanceMeters: Double): Geometry? {
        return when (geom) {
            is Point -> bufferPoint(geom, distanceMeters)
            is Polyline -> bufferPolyline(geom, distanceMeters)
            is Polygon -> bufferPolygon(geom, distanceMeters)
            is MultiPoint -> {
                val polys = geom.points.map { bufferPoint(it, distanceMeters) }
                MultiPolygon(polys)
            }
            is MultiPolyline -> {
                val polys = geom.polylines.mapNotNull { bufferPolyline(it, distanceMeters) }
                MultiPolygon(polys)
            }
            is MultiPolygon -> {
                val polys = geom.polygons.mapNotNull { bufferPolygon(it, distanceMeters) }
                MultiPolygon(polys)
            }
            else -> null
        }
    }

    private fun bufferPoint(point: Point, distanceMeters: Double): Polygon {
        // Approximate degrees for meters around latitude point.y
        val latRad = Math.toRadians(point.y)
        val degLat = distanceMeters / 111320.0
        val degLon = distanceMeters / (111320.0 * cos(latRad).coerceAtLeast(0.001))

        val ring = mutableListOf<Point>()
        for (i in 0 until SEGMENTS_PER_CIRCLE) {
            val angle = 2.0 * Math.PI * i / SEGMENTS_PER_CIRCLE
            val x = point.x + degLon * cos(angle)
            val y = point.y + degLat * sin(angle)
            ring.add(Point(x, y))
        }
        ring.add(ring.first()) // Close ring
        return Polygon(listOf(ring))
    }

    private fun bufferPolyline(polyline: Polyline, distanceMeters: Double): Polygon? {
        val pts = polyline.points
        if (pts.size < 2) return null

        val latAvg = pts.map { it.y }.average()
        val latRad = Math.toRadians(latAvg)
        val degLat = distanceMeters / 111320.0
        val degLon = distanceMeters / (111320.0 * cos(latRad).coerceAtLeast(0.001))

        val leftSide = mutableListOf<Point>()
        val rightSide = mutableListOf<Point>()

        for (i in 0 until pts.size - 1) {
            val p1 = pts[i]
            val p2 = pts[i + 1]

            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val len = sqrt(dx * dx + dy * dy)
            if (len < 1e-9) continue

            val nx = -dy / len * degLon
            val ny = dx / len * degLat

            leftSide.add(Point(p1.x + nx, p1.y + ny))
            leftSide.add(Point(p2.x + nx, p2.y + ny))
            rightSide.add(Point(p1.x - nx, p1.y - ny))
            rightSide.add(Point(p2.x - nx, p2.y - ny))
        }

        if (leftSide.isEmpty()) return null

        // Add cap at end
        val ring = mutableListOf<Point>()
        ring.addAll(leftSide)
        ring.addAll(rightSide.reversed())
        ring.add(ring.first()) // Close polygon

        return Polygon(listOf(ring))
    }

    private fun bufferPolygon(polygon: Polygon, distanceMeters: Double): Polygon? {
        val extRing = polygon.exteriorRing
        if (extRing.size < 3) return null

        val latAvg = extRing.map { it.y }.average()
        val latRad = Math.toRadians(latAvg)
        val degLat = distanceMeters / 111320.0
        val degLon = distanceMeters / (111320.0 * cos(latRad).coerceAtLeast(0.001))

        val centroid = polygon.centroid()
        val expandedRing = mutableListOf<Point>()

        for (pt in extRing) {
            val dx = pt.x - centroid.x
            val dy = pt.y - centroid.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < 1e-9) {
                expandedRing.add(pt)
            } else {
                val factor = (dist + maxOf(degLon, degLat)) / dist
                expandedRing.add(Point(centroid.x + dx * factor, centroid.y + dy * factor))
            }
        }

        return Polygon(listOf(expandedRing))
    }
}
