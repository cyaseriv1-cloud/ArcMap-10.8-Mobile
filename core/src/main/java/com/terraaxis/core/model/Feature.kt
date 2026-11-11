package com.terraaxis.core.model

import com.terraaxis.core.geometry.Geometry

data class Feature(
    val id: Long,
    var geometry: Geometry?,
    val attributes: MutableMap<String, Any?> = mutableMapOf()
) {
    fun getString(field: String): String? = attributes[field]?.toString()

    fun getInt(field: String): Int? = when (val value = attributes[field]) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    }

    fun getDouble(field: String): Double? = when (val value = attributes[field]) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }

    fun getBoolean(field: String): Boolean? = when (val value = attributes[field]) {
        is Boolean -> value
        is String -> value.lowercase() in listOf("true", "t", "1", "yes", "si")
        is Number -> value.toInt() != 0
        else -> null
    }

    fun setAttribute(field: String, value: Any?) {
        attributes[field] = value
    }

    fun copyDeep(): Feature {
        return Feature(
            id = id,
            geometry = geometry,
            attributes = HashMap(attributes)
        )
    }
}
