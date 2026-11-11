package com.terraaxis.tools.generalization

import com.terraaxis.core.geometry.*
import com.terraaxis.core.model.Feature
import com.terraaxis.core.model.VectorLayer
import kotlin.math.sqrt

object SimplifyLineTool {

    /**
     * Executes ArcToolbox Generalization / Simplify:
     * Reduces the number of vertices using Ramer-Douglas-Peucker with tolerance in degrees.
     */
    fun execute(
        inputLayer: VectorLayer,
        toleranceDegrees: Double = 0.001,
        outputLayerName: String = "${inputLayer.name}_simplified"
    ): VectorLayer {
        val outFeatures = mutableListOf<Feature>()
        var nextId = 1L

        for (feat in inputLayer.features) {
            val geom = feat.geometry ?: continue
            val simplified = simplifyGeometry(geom, toleranceDegrees)
            outFeatures.add(Feature(nextId++, simplified, HashMap(feat.attributes)))
        }

        val resultLayer = VectorLayer(
            name = outputLayerName,
            geometryType = inputLayer.geometryType,
            symbology = inputLayer.symbology
        )
        resultLayer.table.fields.addAll(inputLayer.fields.map { it.copy() })
        resultLayer.table.features.addAll(outFeatures)
        return resultLayer
    }

    private fun simplifyGeometry(geom: Geometry, epsilon: Double): Geometry {
        return when (geom) {
            is Polyline -> Polyline(rdp(geom.points, epsilon))
            is Polygon -> {
                val newRings = geom.rings.map { ring ->
                    val simplified = rdp(ring, epsilon)
                    if (simplified.size >= 3) {
                        if (simplified.first() != simplified.last()) simplified + simplified.first() else simplified
                    } else {
                        ring
                    }
                }
                Polygon(newRings)
            }
            else -> geom
        }
    }

    private fun rdp(points: List<Point>, epsilon: Double): List<Point> {
        if (points.size < 3) return points

        var maxDist = 0.0
        var index = 0
        val first = points.first()
        val last = points.last()

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], first, last)
            if (dist > maxDist) {
                maxDist = dist
                index = i
            }
        }

        return if (maxDist > epsilon) {
            val left = rdp(points.subList(0, index + 1), epsilon)
            val right = rdp(points.subList(index, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun perpendicularDistance(p: Point, a: Point, b: Point): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1e-12) return p.distanceTo(a)
        return kotlin.math.abs(dy * p.x - dx * p.y + b.x * a.y - b.y * a.x) / len
    }
}
