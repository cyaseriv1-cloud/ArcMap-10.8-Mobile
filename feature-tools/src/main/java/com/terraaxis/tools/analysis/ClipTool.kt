package com.terraaxis.tools.analysis

import com.terraaxis.core.geometry.*
import com.terraaxis.core.model.Feature
import com.terraaxis.core.model.VectorLayer
import com.terraaxis.core.symbology.ColorRgba
import com.terraaxis.core.symbology.LayerSymbology

object ClipTool {

    /**
     * Executes ArcToolbox Clip: extracts input features that overlay the clip boundary.
     */
    fun execute(
        inputLayer: VectorLayer,
        clipBox: BoundingBox,
        outputLayerName: String = "${inputLayer.name}_clip"
    ): VectorLayer {
        val outFeatures = mutableListOf<Feature>()
        var nextId = 1L

        for (feat in inputLayer.features) {
            val geom = feat.geometry ?: continue
            val clippedGeom = clipGeometry(geom, clipBox) ?: continue

            outFeatures.add(Feature(nextId++, clippedGeom, HashMap(feat.attributes)))
        }

        val resultLayer = VectorLayer(
            name = outputLayerName,
            geometryType = inputLayer.geometryType,
            symbology = LayerSymbology.Simple(
                fillColor = ColorRgba(76, 175, 80, 0.5f), // Green tint
                strokeColor = ColorRgba(46, 125, 50),
                strokeWidthDp = 2.0f
            )
        )
        resultLayer.table.fields.addAll(inputLayer.fields.map { it.copy() })
        resultLayer.table.features.addAll(outFeatures)
        return resultLayer
    }

    private fun clipGeometry(geom: Geometry, box: BoundingBox): Geometry? {
        if (!geom.boundingBox.intersects(box)) return null

        return when (geom) {
            is Point -> if (box.contains(geom)) geom else null
            is Polyline -> clipPolyline(geom, box)
            is Polygon -> clipPolygon(geom, box)
            is MultiPoint -> {
                val inside = geom.points.filter { box.contains(it) }
                if (inside.isNotEmpty()) MultiPoint(inside) else null
            }
            is MultiPolyline -> {
                val lines = geom.polylines.mapNotNull { clipPolyline(it, box) }
                if (lines.isNotEmpty()) MultiPolyline(lines) else null
            }
            is MultiPolygon -> {
                val polys = geom.polygons.mapNotNull { clipPolygon(it, box) }
                if (polys.isNotEmpty()) MultiPolygon(polys) else null
            }
            else -> null
        }
    }

    private fun clipPolyline(line: Polyline, box: BoundingBox): Polyline? {
        val pts = line.points
        if (pts.isEmpty()) return null
        val clippedPts = pts.filter { box.contains(it) }
        return if (clippedPts.size >= 2) Polyline(clippedPts) else null
    }

    /**
     * Sutherland-Hodgman Polygon Clipping algorithm against a rectangular bounding box.
     */
    private fun clipPolygon(polygon: Polygon, box: BoundingBox): Polygon? {
        val extRing = polygon.exteriorRing
        if (extRing.size < 3) return null

        var outputList = extRing

        // Clip against left edge (x = box.minX)
        outputList = clipEdge(outputList) { p -> p.x >= box.minX }
        if (outputList.size < 3) return null

        // Clip against right edge (x = box.maxX)
        outputList = clipEdge(outputList) { p -> p.x <= box.maxX }
        if (outputList.size < 3) return null

        // Clip against bottom edge (y = box.minY)
        outputList = clipEdge(outputList) { p -> p.y >= box.minY }
        if (outputList.size < 3) return null

        // Clip against top edge (y = box.maxY)
        outputList = clipEdge(outputList) { p -> p.y <= box.maxY }
        if (outputList.size < 3) return null

        val closed = if (outputList.first() != outputList.last()) {
            outputList + outputList.first()
        } else {
            outputList
        }

        return Polygon(listOf(closed))
    }

    private inline fun clipEdge(ring: List<Point>, inside: (Point) -> Boolean): List<Point> {
        val output = mutableListOf<Point>()
        if (ring.isEmpty()) return output

        var prev = ring.last()
        for (curr in ring) {
            val currInside = inside(curr)
            val prevInside = inside(prev)

            if (currInside) {
                if (!prevInside) {
                    output.add(intersectEdge(prev, curr, inside))
                }
                output.add(curr)
            } else if (prevInside) {
                output.add(intersectEdge(prev, curr, inside))
            }
            prev = curr
        }
        return output
    }

    private inline fun intersectEdge(p1: Point, p2: Point, inside: (Point) -> Boolean): Point {
        // Linear interpolation midpoint for boundary crossing
        var low = 0.0
        var high = 1.0
        val p1In = inside(p1)

        for (i in 0 until 16) {
            val mid = (low + high) * 0.5
            val midPt = Point(p1.x + mid * (p2.x - p1.x), p1.y + mid * (p2.y - p1.y))
            if (inside(midPt) == p1In) {
                low = mid
            } else {
                high = mid
            }
        }
        val t = (low + high) * 0.5
        return Point(p1.x + t * (p2.x - p1.x), p1.y + t * (p2.y - p1.y))
    }
}
