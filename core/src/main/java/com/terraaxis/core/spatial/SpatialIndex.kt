package com.terraaxis.core.spatial

import com.terraaxis.core.geometry.*
import com.terraaxis.core.model.Feature
import kotlin.math.floor
import kotlin.math.sqrt

class SpatialIndex(
    private val cellSizeDegrees: Double = 0.05
) {
    private val grid = HashMap<Pair<Int, Int>, MutableList<Feature>>()

    fun clear() {
        grid.clear()
    }

    fun build(features: Collection<Feature>) {
        clear()
        features.forEach { insert(it) }
    }

    private fun cellCoord(v: Double): Int = floor(v / cellSizeDegrees).toInt()

    fun insert(feature: Feature) {
        val geom = feature.geometry ?: return
        val box = geom.boundingBox
        if (box == BoundingBox.EMPTY) return

        val minCellX = cellCoord(box.minX)
        val maxCellX = cellCoord(box.maxX)
        val minCellY = cellCoord(box.minY)
        val maxCellY = cellCoord(box.maxY)

        for (cx in minCellX..maxCellX) {
            for (cy in minCellY..maxCellY) {
                val cell = grid.getOrPut(Pair(cx, cy)) { mutableListOf() }
                cell.add(feature)
            }
        }
    }

    /**
     * Finds candidate features intersecting the given query bounding box.
     */
    fun query(box: BoundingBox): List<Feature> {
        if (box == BoundingBox.EMPTY) return emptyList()

        val minCellX = cellCoord(box.minX)
        val maxCellX = cellCoord(box.maxX)
        val minCellY = cellCoord(box.minY)
        val maxCellY = cellCoord(box.maxY)

        val candidates = LinkedHashSet<Feature>()
        for (cx in minCellX..maxCellX) {
            for (cy in minCellY..maxCellY) {
                grid[Pair(cx, cy)]?.let { list ->
                    for (f in list) {
                        val geom = f.geometry
                        if (geom != null && geom.boundingBox.intersects(box)) {
                            candidates.add(f)
                        }
                    }
                }
            }
        }
        return candidates.toList()
    }

    /**
     * Hit-test for map touch: finds the feature under point [touchPoint] within a tolerance [toleranceDegrees].
     */
    fun hitTest(touchPoint: Point, toleranceDegrees: Double): Feature? {
        val searchBox = BoundingBox(
            touchPoint.x - toleranceDegrees,
            touchPoint.y - toleranceDegrees,
            touchPoint.x + toleranceDegrees,
            touchPoint.y + toleranceDegrees
        )
        val candidates = query(searchBox)

        // Test precise geometry:
        // Priority 1: Points (closest to touch)
        // Priority 2: Polylines (closest to touch)
        // Priority 3: Polygons (containing point or closest boundary)

        var bestPointFeature: Feature? = null
        var bestPointDist = Double.MAX_VALUE

        var bestLineFeature: Feature? = null
        var bestLineDist = Double.MAX_VALUE

        var foundPolygonFeature: Feature? = null

        for (f in candidates) {
            when (val geom = f.geometry) {
                is Point -> {
                    val d = geom.distanceTo(touchPoint)
                    if (d <= toleranceDegrees && d < bestPointDist) {
                        bestPointDist = d
                        bestPointFeature = f
                    }
                }
                is MultiPoint -> {
                    for (pt in geom.points) {
                        val d = pt.distanceTo(touchPoint)
                        if (d <= toleranceDegrees && d < bestPointDist) {
                            bestPointDist = d
                            bestPointFeature = f
                        }
                    }
                }
                is Polyline -> {
                    val d = distancePointToPolyline(touchPoint, geom.points)
                    if (d <= toleranceDegrees && d < bestLineDist) {
                        bestLineDist = d
                        bestLineFeature = f
                    }
                }
                is MultiPolyline -> {
                    for (line in geom.polylines) {
                        val d = distancePointToPolyline(touchPoint, line.points)
                        if (d <= toleranceDegrees && d < bestLineDist) {
                            bestLineDist = d
                            bestLineFeature = f
                        }
                    }
                }
                is Polygon -> {
                    if (geom.containsPoint(touchPoint) && foundPolygonFeature == null) {
                        foundPolygonFeature = f
                    }
                }
                is MultiPolygon -> {
                    if (geom.containsPoint(touchPoint) && foundPolygonFeature == null) {
                        foundPolygonFeature = f
                    }
                }
                else -> {
                    if (geom != null && geom.boundingBox.contains(touchPoint) && foundPolygonFeature == null) {
                        foundPolygonFeature = f
                    }
                }
            }
        }

        return bestPointFeature ?: bestLineFeature ?: foundPolygonFeature
    }

    private fun distancePointToPolyline(p: Point, points: List<Point>): Double {
        if (points.size < 2) return Double.MAX_VALUE
        var minDist = Double.MAX_VALUE
        for (i in 0 until points.size - 1) {
            val d = distancePointToSegment(p, points[i], points[i + 1])
            if (d < minDist) minDist = d
        }
        return minDist
    }

    private fun distancePointToSegment(p: Point, a: Point, b: Point): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-12) return p.distanceTo(a)

        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq).coerceIn(0.0, 1.0)
        val projX = a.x + t * dx
        val projY = a.y + t * dy
        val distX = p.x - projX
        val distY = p.y - projY
        return sqrt(distX * distX + distY * distY)
    }
}
