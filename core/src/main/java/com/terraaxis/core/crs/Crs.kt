package com.terraaxis.core.crs

enum class Hemisphere {
    NORTH,
    SOUTH
}

sealed interface CoordinateReferenceSystem {
    val epsgCode: Int
    val name: String
    val isGeographic: Boolean

    object WGS84 : CoordinateReferenceSystem {
        override val epsgCode: Int = 4326
        override val name: String = "GCS WGS 1984"
        override val isGeographic: Boolean = true
    }

    object WebMercator : CoordinateReferenceSystem {
        override val epsgCode: Int = 3857
        override val name: String = "WGS 1984 Web Mercator (Auxiliary Sphere)"
        override val isGeographic: Boolean = false
    }

    data class UTM(
        val zone: Int,
        val hemisphere: Hemisphere
    ) : CoordinateReferenceSystem {
        override val epsgCode: Int = if (hemisphere == Hemisphere.NORTH) 32600 + zone else 32700 + zone
        override val name: String = "WGS 1984 UTM Zone $zone${if (hemisphere == Hemisphere.NORTH) "N" else "S"}"
        override val isGeographic: Boolean = false
    }
}

data class UtmCoordinate(
    val zone: Int,
    val hemisphere: Hemisphere,
    val easting: Double,
    val northing: Double
) {
    fun format(): String =
        "${zone}${if (hemisphere == Hemisphere.NORTH) "N" else "S"} ${String.format("%.1f", easting)}m E, ${String.format("%.1f", northing)}m N"
}
