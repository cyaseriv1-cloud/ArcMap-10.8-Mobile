package com.terraaxis.core.crs

import com.terraaxis.core.geometry.Point
import kotlin.math.*

object ProjectionEngine {
    // WGS84 Ellipsoid constants
    private const val A = 6378137.0 // Semi-major axis
    private const val F = 1.0 / 298.257223563 // Flattening
    private const val B = A * (1.0 - F) // Semi-minor axis (~6356752.3142)
    private val E2 = (A * A - B * B) / (A * A) // First eccentricity squared
    private val E_PRIME2 = (A * A - B * B) / (B * B) // Second eccentricity squared
    private const val K0 = 0.9996 // UTM scale factor
    private const val FALSE_EASTING = 500000.0
    private const val FALSE_NORTHING_SOUTH = 10000000.0

    /**
     * Determines UTM Zone (1 to 60) for a given longitude in degrees.
     */
    fun calculateUtmZone(longitude: Double): Int {
        var lon = longitude
        while (lon > 180.0) lon -= 360.0
        while (lon < -180.0) lon += 360.0
        return ((lon + 180.0) / 6.0).toInt() + 1
    }

    /**
     * Converts WGS84 Geographic coordinates (lat, lon in degrees) to UTM (Easting, Northing).
     */
    fun wgs84ToUtm(latitude: Double, longitude: Double, forcedZone: Int? = null): UtmCoordinate {
        val latRad = Math.toRadians(latitude)
        val zone = forcedZone ?: calculateUtmZone(longitude)
        val hemisphere = if (latitude >= 0) Hemisphere.NORTH else Hemisphere.SOUTH

        val lonOrigin = (zone - 1) * 6 - 180 + 3
        val lonOriginRad = Math.toRadians(lonOrigin.toDouble())
        val deltaLonRad = Math.toRadians(longitude) - lonOriginRad

        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val tanLat = tan(latRad)

        val n = A / sqrt(1.0 - E2 * sinLat * sinLat)
        val t = tanLat * tanLat
        val c = E_PRIME2 * cosLat * cosLat
        val al = cosLat * deltaLonRad

        // Meridional arc distance
        val m = A * (
            (1.0 - E2 / 4.0 - 3.0 * E2 * E2 / 64.0 - 5.0 * E2 * E2 * E2 / 256.0) * latRad
            - (3.0 * E2 / 8.0 + 3.0 * E2 * E2 / 32.0 + 45.0 * E2 * E2 * E2 / 1024.0) * sin(2.0 * latRad)
            + (15.0 * E2 * E2 / 256.0 + 45.0 * E2 * E2 * E2 / 1024.0) * sin(4.0 * latRad)
            - (35.0 * E2 * E2 * E2 / 3072.0) * sin(6.0 * latRad)
        )

        val al2 = al * al
        val al3 = al2 * al
        val al4 = al2 * al2
        val al5 = al4 * al
        val al6 = al3 * al3

        var easting = K0 * n * (
            al + (1.0 - t + c) * al3 / 6.0
            + (5.0 - 18.0 * t + t * t + 72.0 * c - 58.0 * E_PRIME2) * al5 / 120.0
        ) + FALSE_EASTING

        var northing = K0 * (
            m + n * tanLat * (
                al2 / 2.0
                + (5.0 - t + 9.0 * c + 4.0 * c * c) * al4 / 24.0
                + (61.0 - 58.0 * t + t * t + 600.0 * c - 330.0 * E_PRIME2) * al6 / 720.0
            )
        )

        if (hemisphere == Hemisphere.SOUTH) {
            northing += FALSE_NORTHING_SOUTH
        }

        return UtmCoordinate(zone, hemisphere, easting, northing)
    }

    /**
     * Converts UTM coordinates to WGS84 Geographic coordinates (lat, lon in degrees).
     */
    fun utmToWgs84(utm: UtmCoordinate): Point {
        val x = utm.easting - FALSE_EASTING
        var y = utm.northing
        if (utm.hemisphere == Hemisphere.SOUTH) {
            y -= FALSE_NORTHING_SOUTH
        }

        val lonOrigin = (utm.zone - 1) * 6 - 180 + 3
        val lonOriginRad = Math.toRadians(lonOrigin.toDouble())

        val e1 = (1.0 - sqrt(1.0 - E2)) / (1.0 + sqrt(1.0 - E2))
        val m = y / K0
        val mu = m / (A * (1.0 - E2 / 4.0 - 3.0 * E2 * E2 / 64.0 - 5.0 * E2 * E2 * E2 / 256.0))

        val phi1Rad = mu + (3.0 * e1 / 2.0 - 27.0 * e1.pow(3) / 32.0) * sin(2.0 * mu) +
            (21.0 * e1 * e1 / 16.0 - 55.0 * e1.pow(4) / 32.0) * sin(4.0 * mu) +
            (151.0 * e1.pow(3) / 96.0) * sin(6.0 * mu) +
            (1097.0 * e1.pow(4) / 512.0) * sin(8.0 * mu)

        val sinPhi1 = sin(phi1Rad)
        val cosPhi1 = cos(phi1Rad)
        val tanPhi1 = tan(phi1Rad)

        val n1 = A / sqrt(1.0 - E2 * sinPhi1 * sinPhi1)
        val r1 = A * (1.0 - E2) / (1.0 - E2 * sinPhi1 * sinPhi1).pow(1.5)
        val d = x / (n1 * K0)

        val t1 = tanPhi1 * tanPhi1
        val c1 = E_PRIME2 * cosPhi1 * cosPhi1

        val d2 = d * d
        val d3 = d2 * d
        val d4 = d2 * d2
        val d5 = d4 * d
        val d6 = d3 * d3

        val latRad = phi1Rad - (n1 * tanPhi1 / r1) * (
            d2 / 2.0 -
            (5.0 + 3.0 * t1 + 10.0 * c1 - 4.0 * c1 * c1 - 9.0 * E_PRIME2) * d4 / 24.0 +
            (61.0 + 90.0 * t1 + 298.0 * c1 + 45.0 * t1 * t1 - 252.0 * E_PRIME2 - 3.0 * c1 * c1) * d6 / 720.0
        )

        val lonRad = lonOriginRad + (
            d - (1.0 + 2.0 * t1 + c1) * d3 / 6.0 +
            (5.0 - 2.0 * c1 + 28.0 * t1 - 3.0 * c1 * c1 + 8.0 * E_PRIME2 + 24.0 * t1 * t1) * d5 / 120.0
        ) / cosPhi1

        return Point(Math.toDegrees(lonRad), Math.toDegrees(latRad))
    }

    /**
     * WGS84 Geographic to EPSG:3857 (Web Mercator)
     */
    fun wgs84ToWebMercator(latitude: Double, longitude: Double): Point {
        val x = longitude * 20037508.34 / 180.0
        val clampedLat = latitude.coerceIn(-85.05112878, 85.05112878)
        val rad = Math.toRadians(clampedLat)
        val y = ln(tan(Math.PI / 4.0 + rad / 2.0)) * 20037508.34 / Math.PI
        return Point(x, y)
    }

    /**
     * EPSG:3857 (Web Mercator) to WGS84 Geographic
     */
    fun webMercatorToWgs84(x: Double, y: Double): Point {
        val lon = x * 180.0 / 20037508.34
        val lat = Math.toDegrees(2.0 * atan(exp(y * Math.PI / 20037508.34)) - Math.PI / 2.0)
        return Point(lon, lat)
    }

    /**
     * Computes geodesic distance in meters between two WGS84 points (Haversine formula).
     */
    fun geodesicDistanceMeters(p1: Point, p2: Point): Double {
        val earthRadius = 6371000.0 // meters
        val lat1Rad = Math.toRadians(p1.y)
        val lat2Rad = Math.toRadians(p2.y)
        val dLat = Math.toRadians(p2.y - p1.y)
        val dLon = Math.toRadians(p2.x - p1.x)

        val a = sin(dLat / 2.0).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2.0).pow(2)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return earthRadius * c
    }

    /**
     * Computes spherical polygon area in square meters on WGS84.
     */
    fun geodesicPolygonAreaSquareMeters(ring: List<Point>): Double {
        if (ring.size < 3) return 0.0
        val earthRadius = 6371000.0
        var total = 0.0
        val n = ring.size

        for (i in 0 until n) {
            val p1 = ring[i]
            val p2 = ring[(i + 1) % n]
            val lat1 = Math.toRadians(p1.y)
            val lat2 = Math.toRadians(p2.y)
            val lon1 = Math.toRadians(p1.x)
            val lon2 = Math.toRadians(p2.x)

            total += (lon2 - lon1) * (2.0 + sin(lat1) + sin(lat2))
        }

        val area = abs(total * earthRadius * earthRadius / 2.0)
        return area
    }
}
