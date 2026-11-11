package com.terraaxis.core.symbology

import com.terraaxis.core.model.Feature

data class ColorRgba(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Float = 1.0f
) {
    val hexColorString: String
        get() = String.format("#%02X%02X%02X", red, green, blue)

    fun toArgbInt(): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return (a shl 24) or (red shl 16) or (green shl 8) or blue
    }

    companion object {
        val RED = ColorRgba(230, 0, 0)
        val GREEN = ColorRgba(76, 175, 80)
        val BLUE = ColorRgba(33, 150, 243)
        val ORANGE = ColorRgba(255, 152, 0)
        val PURPLE = ColorRgba(156, 39, 176)
        val CYAN = ColorRgba(0, 255, 255) // ArcGIS selection color
        val BLACK = ColorRgba(0, 0, 0)
        val WHITE = ColorRgba(255, 255, 255)
        val GRAY = ColorRgba(158, 158, 158)
        val TRANSPARENT = ColorRgba(0, 0, 0, 0.0f)

        val DEFAULT_PALETTE = listOf(
            ColorRgba(66, 133, 244),
            ColorRgba(234, 67, 53),
            ColorRgba(251, 188, 5),
            ColorRgba(52, 168, 83),
            ColorRgba(171, 71, 188),
            ColorRgba(0, 172, 193),
            ColorRgba(255, 112, 67),
            ColorRgba(141, 110, 99)
        )
    }
}

enum class MarkerShape {
    CIRCLE,
    SQUARE,
    TRIANGLE,
    CROSS
}

sealed interface LayerSymbology {
    fun getFillColor(feature: Feature): ColorRgba
    fun getStrokeColor(feature: Feature): ColorRgba
    fun getStrokeWidth(feature: Feature): Float

    data class Simple(
        val fillColor: ColorRgba = ColorRgba.BLUE.copy(alpha = 0.5f),
        val strokeColor: ColorRgba = ColorRgba.BLUE,
        val strokeWidthDp: Float = 2.0f,
        val pointSizeDp: Float = 8.0f,
        val markerShape: MarkerShape = MarkerShape.CIRCLE
    ) : LayerSymbology {
        override fun getFillColor(feature: Feature): ColorRgba = fillColor
        override fun getStrokeColor(feature: Feature): ColorRgba = strokeColor
        override fun getStrokeWidth(feature: Feature): Float = strokeWidthDp
    }

    /**
     * ArcGIS "Unique Values" symbology renderer:
     * maps distinct attribute values of [fieldName] to distinct colors.
     */
    data class UniqueValues(
        val fieldName: String,
        val valueColors: Map<String, ColorRgba>,
        val defaultFillColor: ColorRgba = ColorRgba.GRAY.copy(alpha = 0.5f),
        val strokeColor: ColorRgba = ColorRgba.BLACK,
        val strokeWidthDp: Float = 1.5f
    ) : LayerSymbology {
        override fun getFillColor(feature: Feature): ColorRgba {
            val key = feature.getString(fieldName) ?: return defaultFillColor
            return valueColors[key] ?: defaultFillColor
        }

        override fun getStrokeColor(feature: Feature): ColorRgba = strokeColor
        override fun getStrokeWidth(feature: Feature): Float = strokeWidthDp

        companion object {
            fun createFromFeatures(
                fieldName: String,
                features: List<Feature>,
                palette: List<ColorRgba> = ColorRgba.DEFAULT_PALETTE
            ): UniqueValues {
                val distinctValues = features.mapNotNull { it.getString(fieldName) }.distinct().take(palette.size)
                val map = distinctValues.mapIndexed { index, value ->
                    value to palette[index % palette.size].copy(alpha = 0.7f)
                }.toMap()
                return UniqueValues(fieldName, map)
            }
        }
    }
}
