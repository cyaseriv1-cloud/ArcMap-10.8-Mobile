package com.terraaxis.core.model

import com.terraaxis.core.geometry.Point
import com.terraaxis.core.geometry.Polygon
import com.terraaxis.core.geometry.Polyline

data class FieldStatistics(
    val count: Int,
    val nullCount: Int,
    val min: Double? = null,
    val max: Double? = null,
    val sum: Double? = null,
    val mean: Double? = null
)

class AttributeTable(
    val fields: MutableList<Field> = mutableListOf(),
    val features: MutableList<Feature> = mutableListOf()
) {
    val rowCount: Int get() = features.size
    val columnCount: Int get() = fields.size

    fun getField(name: String): Field? = fields.find { it.name.equals(name, ignoreCase = true) }

    fun addField(field: Field) {
        if (getField(field.name) == null) {
            fields.add(field)
        }
    }

    fun removeField(fieldName: String) {
        fields.removeAll { it.name.equals(fieldName, ignoreCase = true) }
        features.forEach { it.attributes.remove(fieldName) }
    }

    fun sort(fieldName: String, ascending: Boolean = true) {
        val field = getField(fieldName) ?: return
        features.sortWith { a, b ->
            val valA = a.attributes[field.name]
            val valB = b.attributes[field.name]
            when {
                valA == null && valB == null -> 0
                valA == null -> if (ascending) -1 else 1
                valB == null -> if (ascending) 1 else -1
                valA is Comparable<*> && valB is Comparable<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val comp = (valA as Comparable<Any>).compareTo(valB as Any)
                    if (ascending) comp else -comp
                }
                else -> {
                    val comp = valA.toString().compareTo(valB.toString())
                    if (ascending) comp else -comp
                }
            }
        }
    }

    fun filter(query: (Feature) -> Boolean): List<Feature> {
        return features.filter(query)
    }

    fun calculateStatistics(fieldName: String): FieldStatistics {
        val field = getField(fieldName) ?: return FieldStatistics(0, features.size)
        var count = 0
        var nullCount = 0
        var min = Double.MAX_VALUE
        var max = -Double.MAX_VALUE
        var sum = 0.0

        for (feat in features) {
            val num = feat.getDouble(field.name)
            if (num != null) {
                count++
                sum += num
                if (num < min) min = num
                if (num > max) max = num
            } else {
                nullCount++
            }
        }

        return if (count > 0) {
            FieldStatistics(count, nullCount, min, max, sum, sum / count)
        } else {
            FieldStatistics(0, nullCount)
        }
    }

    /**
     * ArcMap-like "Calculate Geometry":
     * Computes area, length, or centroid coordinates directly into an attribute field!
     */
    fun calculateGeometry(targetField: String, property: GeometryProperty) {
        val field = getField(targetField) ?: run {
            val newField = Field(targetField, FieldType.DOUBLE)
            addField(newField)
            newField
        }

        for (f in features) {
            val geom = f.geometry ?: continue
            val value: Double? = when (property) {
                GeometryProperty.AREA -> when (geom) {
                    is Polygon -> geom.areaPlanar()
                    else -> null
                }
                GeometryProperty.LENGTH_OR_PERIMETER -> when (geom) {
                    is Polyline -> geom.lengthPlanar()
                    is Polygon -> geom.perimeterPlanar()
                    else -> null
                }
                GeometryProperty.CENTROID_X -> when (geom) {
                    is Point -> geom.x
                    is Polygon -> geom.centroid().x
                    else -> geom.boundingBox.centerX
                }
                GeometryProperty.CENTROID_Y -> when (geom) {
                    is Point -> geom.y
                    is Polygon -> geom.centroid().y
                    else -> geom.boundingBox.centerY
                }
            }
            if (value != null) {
                f.setAttribute(field.name, value)
            }
        }
    }
}

enum class GeometryProperty {
    AREA,
    LENGTH_OR_PERIMETER,
    CENTROID_X,
    CENTROID_Y
}
