package com.terraaxis.core.geometry

import kotlin.math.*

enum class GeometryType {
    POINT,
    POLYLINE,
    POLYGON,
    MULTI_POINT,
    MULTI_POLYLINE,
    MULTI_POLYGON,
    UNKNOWN
}

sealed interface Geometry {
    val geometryType: GeometryType
    val boundingBox: BoundingBox
}

data class Point(
    val x: Double,
    val y: Double,
    val z: Double? = null,
    val m: Double? = null
) : Geometry {
    override val geometryType: GeometryType get() = GeometryType.POINT
    override val boundingBox: BoundingBox get() = BoundingBox(x, y, x, y)

    fun distanceTo(other: Point): Double {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }
}

data class BoundingBox(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double
) {
    val width: Double get() = maxX - minX
    val height: Double get() = maxY - minY
    val centerX: Double get() = (minX + maxX) / 2.0
    val centerY: Double get() = (minY + maxY) / 2.0

    fun contains(x: Double, y: Double): Boolean =
        x in minX..maxX && y in minY..maxY

    fun contains(point: Point): Boolean = contains(point.x, point.y)

    fun intersects(other: BoundingBox): Boolean =
        minX <= other.maxX && maxX >= other.minX &&
        minY <= other.maxY && maxY >= other.minY

    fun expandToInclude(point: Point): BoundingBox = BoundingBox(
        min(minX, point.x),
        min(minY, point.y),
        max(maxX, point.x),
        max(maxY, point.y)
    )

    fun expandToInclude(other: BoundingBox): BoundingBox = BoundingBox(
        min(minX, other.minX),
        min(minY, other.minY),
        max(maxX, other.maxX),
        max(maxY, other.maxY)
    )

    companion object {
        val EMPTY = BoundingBox(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE)

        fun fromPoints(points: Collection<Point>): BoundingBox {
            if (points.isEmpty()) return EMPTY
            var minX = Double.MAX_VALUE
            var minY = Double.MAX_VALUE
            var maxX = -Double.MAX_VALUE
            var maxY = -Double.MAX_VALUE
            for (p in points) {
                if (p.x < minX) minX = p.x
                if (p.y < minY) minY = p.y
                if (p.x > maxX) maxX = p.x
                if (p.y > maxY) maxY = p.y
            }
            return BoundingBox(minX, minY, maxX, maxY)
        }
    }
}

data class Polyline(
    val points: List<Point>
) : Geometry {
    override val geometryType: GeometryType get() = GeometryType.POLYLINE
    override val boundingBox: BoundingBox by lazy { BoundingBox.fromPoints(points) }

    fun lengthPlanar(): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += points[i].distanceTo(points[i + 1])
        }
        return total
    }
}

data class Polygon(
    /**
     * rings[0] is the exterior ring (ordered clockwise by GIS convention).
     * rings[1..n] are interior hole rings (ordered counter-clockwise).
     */
    val rings: List<List<Point>>
) : Geometry {
    override val geometryType: GeometryType get() = GeometryType.POLYGON
    override val boundingBox: BoundingBox by lazy {
        BoundingBox.fromPoints(rings.flatten())
    }

    val exteriorRing: List<Point> get() = rings.firstOrNull() ?: emptyList()

    /**
     * Calculates planar area using the Shoelace formula.
     * Subtracts the area of interior holes.
     */
    fun areaPlanar(): Double {
        if (rings.isEmpty()) return 0.0
        val outerArea = abs(ringArea(rings[0]))
        var holesArea = 0.0
        for (i in 1 until rings.size) {
            holesArea += abs(ringArea(rings[i]))
        }
        return max(0.0, outerArea - holesArea)
    }

    /**
     * Perimeter of exterior ring plus all interior rings.
     */
    fun perimeterPlanar(): Double {
        var perimeter = 0.0
        for (ring in rings) {
            if (ring.size < 2) continue
            for (i in 0 until ring.size - 1) {
                perimeter += ring[i].distanceTo(ring[i + 1])
            }
            // Close the loop if last != first
            if (ring.first() != ring.last()) {
                perimeter += ring.last().distanceTo(ring.first())
            }
        }
        return perimeter
    }

    /**
     * Point in polygon test using Ray-Casting Algorithm.
     * Considers holes: point must be inside exterior ring and NOT inside any interior hole.
     */
    fun containsPoint(p: Point): Boolean {
        if (!boundingBox.contains(p)) return false
        if (!isPointInRing(p, exteriorRing)) return false
        for (i in 1 until rings.size) {
            if (isPointInRing(p, rings[i])) return false
        }
        return true
    }

    fun centroid(): Point {
        val ring = exteriorRing
        if (ring.isEmpty()) return Point(0.0, 0.0)
        var signedArea = 0.0
        var cx = 0.0
        var cy = 0.0
        val n = ring.size
        for (i in 0 until n) {
            val p0 = ring[i]
            val p1 = ring[(i + 1) % n]
            val a = p0.x * p1.y - p1.x * p0.y
            signedArea += a
            cx += (p0.x + p1.x) * a
            cy += (p0.y + p1.y) * a
        }
        signedArea *= 0.5
        if (abs(signedArea) < 1e-9) {
            return Point(ring.map { it.x }.average(), ring.map { it.y }.average())
        }
        cx /= (6.0 * signedArea)
        cy /= (6.0 * signedArea)
        return Point(cx, cy)
    }

    companion object {
        fun fromSimpleRing(points: List<Point>): Polygon = Polygon(listOf(points))

        private fun ringArea(points: List<Point>): Double {
            if (points.size < 3) return 0.0
            var area = 0.0
            val n = points.size
            for (i in 0 until n) {
                val j = (i + 1) % n
                area += points[i].x * points[j].y
                area -= points[j].x * points[i].y
            }
            return area * 0.5
        }

        private fun isPointInRing(p: Point, ring: List<Point>): Boolean {
            var inside = false
            val n = ring.size
            var j = n - 1
            for (i in 0 until n) {
                val pi = ring[i]
                val pj = ring[j]
                if (((pi.y > p.y) != (pj.y > p.y)) &&
                    (p.x < (pj.x - pi.x) * (p.y - pi.y) / (pj.y - pi.y) + pi.x)
                ) {
                    inside = !inside
                }
                j = i
            }
            return inside
        }
    }
}

data class MultiPoint(val points: List<Point>) : Geometry {
    override val geometryType: GeometryType get() = GeometryType.MULTI_POINT
    override val boundingBox: BoundingBox by lazy { BoundingBox.fromPoints(points) }
}

data class MultiPolyline(val polylines: List<Polyline>) : Geometry {
    override val geometryType: GeometryType get() = GeometryType.MULTI_POLYLINE
    override val boundingBox: BoundingBox by lazy {
        polylines.map { it.boundingBox }.fold(BoundingBox.EMPTY) { acc, bb -> acc.expandToInclude(bb) }
    }
}

data class MultiPolygon(val polygons: List<Polygon>) : Geometry {
    override val geometryType: GeometryType get() = GeometryType.MULTI_POLYGON
    override val boundingBox: BoundingBox by lazy {
        polygons.map { it.boundingBox }.fold(BoundingBox.EMPTY) { acc, bb -> acc.expandToInclude(bb) }
    }

    fun containsPoint(p: Point): Boolean = polygons.any { it.containsPoint(p) }
}
