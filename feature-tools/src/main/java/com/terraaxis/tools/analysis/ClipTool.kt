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

    private const val INSIDE = 0 // 0000
    private const val LEFT = 1   // 0001
    private const val RIGHT = 2  // 0010
    private const val BOTTOM = 4 // 0100
    private const val TOP = 8    // 1000

    private fun computeOutCode(x: Double, y: Double, box: BoundingBox): Int {
        var code = INSIDE
        if (x < box.minX) code = code or LEFT
        else if (x > box.maxX) code = code or RIGHT
        if (y < box.minY) code = code or BOTTOM
        else if (y > box.maxY) code = code or TOP
        return code
    }

    private fun clipPolyline(line: Polyline, box: BoundingBox): Polyline? {
        val pts = line.points
        if (pts.size < 2) return null

        val resultPts = mutableListOf<Point>()

        for (i in 0 until pts.size - 1) {
            var x0 = pts[i].x
            var y0 = pts[i].y
            var x1 = pts[i + 1].x
            var y1 = pts[i + 1].y

            var code0 = computeOutCode(x0, y0, box)
            var code1 = computeOutCode(x1, y1, box)
            var accept = false

            while (true) {
                if ((code0 or code1) == 0) {
                    accept = true
                    break
                } else if ((code0 and code1) != 0) {
                    break
                } else {
                    var x = 0.0
                    var y = 0.0
                    val codeOut = if (code0 != 0) code0 else code1

                    if ((codeOut and TOP) != 0) {
                        x = x0 + (x1 - x0) * (box.maxY - y0) / (y1 - y0)
                        y = box.maxY
                    } else if ((codeOut and BOTTOM) != 0) {
                        x = x0 + (x1 - x0) * (box.minY - y0) / (y1 - y0)
                        y = box.minY
                    } else if ((codeOut and RIGHT) != 0) {
                        y = y0 + (y1 - y0) * (box.maxX - x0) / (x1 - x0)
                        x = box.maxX
                    } else if ((codeOut and LEFT) != 0) {
                        y = y0 + (y1 - y0) * (box.minX - x0) / (x1 - x0)
                        x = box.minX
                    }

                    if (codeOut == code0) {
                        x0 = x
                        y0 = y
                        code0 = computeOutCode(x0, y0, box)
                    } else {
                        x1 = x
                        y1 = y
                        code1 = computeOutCode(x1, y1, box)
                    }
                }
            }

            if (accept) {
                val p0 = Point(x0, y0)
                val p1 = Point(x1, y1)
                if (resultPts.isEmpty() || resultPts.last() != p0) {
                    resultPts.add(p0)
                }
                resultPts.add(p1)
            }
        }

        return if (resultPts.size >= 2) Polyline(resultPts) else null
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
