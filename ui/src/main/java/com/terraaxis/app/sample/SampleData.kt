package com.terraaxis.app.sample

import com.terraaxis.core.geometry.Point
import com.terraaxis.core.geometry.Polygon
import com.terraaxis.core.geometry.Polyline
import com.terraaxis.core.model.Feature
import com.terraaxis.core.model.Field
import com.terraaxis.core.model.FieldType
import com.terraaxis.core.model.VectorLayer
import com.terraaxis.core.geometry.GeometryType
import com.terraaxis.core.symbology.ColorRgba
import com.terraaxis.core.symbology.LayerSymbology

object SampleData {

    fun createSampleLayers(): List<VectorLayer> {
        val centerLon = -71.9675
        val centerLat = -13.5320

        // 1. Polygon Layer: Zonas y Sectores
        val polygonLayer = VectorLayer(
            name = "Zonificación y Sectores",
            geometryType = GeometryType.POLYGON,
            labelFieldName = "NOMBRE",
            symbology = LayerSymbology.UniqueValues(
                fieldName = "CATEGORIA",
                valueColors = mapOf(
                    "Centro Histórico" to ColorRgba(255, 152, 0, 0.6f),
                    "Residencial" to ColorRgba(33, 150, 243, 0.5f),
                    "Parque Ecológico" to ColorRgba(76, 175, 80, 0.6f),
                    "Zona Comercial" to ColorRgba(156, 39, 176, 0.5f)
                ),
                strokeColor = ColorRgba.BLACK,
                strokeWidthDp = 2.0f
            )
        )
        polygonLayer.fields.addAll(
            listOf(
                Field("ID", FieldType.INTEGER),
                Field("NOMBRE", FieldType.STRING),
                Field("CATEGORIA", FieldType.STRING),
                Field("POBLACION", FieldType.INTEGER),
                Field("ESTADO", FieldType.STRING)
            )
        )

        val poly1 = Polygon.fromSimpleRing(
            listOf(
                Point(centerLon - 0.02, centerLat + 0.01),
                Point(centerLon + 0.01, centerLat + 0.02),
                Point(centerLon + 0.02, centerLat - 0.01),
                Point(centerLon - 0.01, centerLat - 0.02),
                Point(centerLon - 0.02, centerLat + 0.01)
            )
        )
        val poly2 = Polygon.fromSimpleRing(
            listOf(
                Point(centerLon + 0.02, centerLat + 0.03),
                Point(centerLon + 0.05, centerLat + 0.04),
                Point(centerLon + 0.06, centerLat + 0.01),
                Point(centerLon + 0.03, centerLat + 0.00),
                Point(centerLon + 0.02, centerLat + 0.03)
            )
        )
        val poly3 = Polygon.fromSimpleRing(
            listOf(
                Point(centerLon - 0.04, centerLat - 0.01),
                Point(centerLon - 0.02, centerLat - 0.01),
                Point(centerLon - 0.01, centerLat - 0.04),
                Point(centerLon - 0.04, centerLat - 0.04),
                Point(centerLon - 0.04, centerLat - 0.01)
            )
        )

        polygonLayer.features.add(
            Feature(
                1L,
                poly1,
                mutableMapOf(
                    "ID" to 1,
                    "NOMBRE" to "Sector Centro",
                    "CATEGORIA" to "Centro Histórico",
                    "POBLACION" to 42500,
                    "ESTADO" to "Protegido"
                )
            )
        )
        polygonLayer.features.add(
            Feature(
                2L,
                poly2,
                mutableMapOf(
                    "ID" to 2,
                    "NOMBRE" to "Sector Noreste",
                    "CATEGORIA" to "Residencial",
                    "POBLACION" to 68000,
                    "ESTADO" to "Urbano"
                )
            )
        )
        polygonLayer.features.add(
            Feature(
                3L,
                poly3,
                mutableMapOf(
                    "ID" to 3,
                    "NOMBRE" to "Valle Verde",
                    "CATEGORIA" to "Parque Ecológico",
                    "POBLACION" to 1200,
                    "ESTADO" to "Reserva"
                )
            )
        )

        // 2. Polyline Layer: Red Vial y Vías Principales de Cusco
        val polylineLayer = VectorLayer(
            name = "Red Vial (Calles y Avenidas)",
            geometryType = GeometryType.POLYLINE,
            labelFieldName = "NOMBRE",
            symbology = LayerSymbology.Simple(
                strokeColor = ColorRgba(211, 47, 47), // Red Arterial
                strokeWidthDp = 2.8f
            )
        )
        polylineLayer.fields.addAll(
            listOf(
                Field("ID", FieldType.INTEGER),
                Field("NOMBRE", FieldType.STRING),
                Field("TIPO", FieldType.STRING),
                Field("LONG_KM", FieldType.DOUBLE)
            )
        )

        // Vía de Evitamiento (Cusco)
        val viaEvitamiento = Polyline(
            listOf(
                Point(-71.9900, -13.5250),
                Point(-71.9675, -13.5350),
                Point(-71.9450, -13.5385),
                Point(-71.9300, -13.5420),
                Point(-71.9100, -13.5480)
            )
        )
        // Av. San Antonio
        val avSanAntonio = Polyline(
            listOf(
                Point(-71.9500, -13.5350),
                Point(-71.9480, -13.5380),
                Point(-71.9470, -13.5410)
            )
        )
        // Calle Angamos
        val calleAngamos = Polyline(
            listOf(
                Point(-71.9520, -13.5365),
                Point(-71.9460, -13.5372),
                Point(-71.9420, -13.5378)
            )
        )
        // Av. La Amistad
        val avAmistad = Polyline(
            listOf(
                Point(-71.9465, -13.5360),
                Point(-71.9440, -13.5390)
            )
        )
        // Calle México
        val calleMexico = Polyline(
            listOf(
                Point(-71.9510, -13.5380),
                Point(-71.9470, -13.5385)
            )
        )
        // Av. El Sol (Centro Histórico)
        val avElSol = Polyline(
            listOf(
                Point(-71.9780, -13.5180),
                Point(-71.9740, -13.5220),
                Point(-71.9680, -13.5280)
            )
        )
        // Av. de la Cultura
        val avCultura = Polyline(
            listOf(
                Point(-71.9650, -13.5270),
                Point(-71.9400, -13.5300),
                Point(-71.9150, -13.5320)
            )
        )

        polylineLayer.features.add(Feature(1L, viaEvitamiento, mutableMapOf("ID" to 1, "NOMBRE" to "Vía de Evitamiento", "TIPO" to "Vía Rápida", "LONG_KM" to 12.4)))
        polylineLayer.features.add(Feature(2L, avSanAntonio, mutableMapOf("ID" to 2, "NOMBRE" to "Av. San Antonio", "TIPO" to "Arteria Urbana", "LONG_KM" to 1.8)))
        polylineLayer.features.add(Feature(3L, calleAngamos, mutableMapOf("ID" to 3, "NOMBRE" to "Angamos", "TIPO" to "Calle Principal", "LONG_KM" to 1.2)))
        polylineLayer.features.add(Feature(4L, avAmistad, mutableMapOf("ID" to 4, "NOMBRE" to "Av. La Amistad", "TIPO" to "Calle Colectora", "LONG_KM" to 0.9)))
        polylineLayer.features.add(Feature(5L, calleMexico, mutableMapOf("ID" to 5, "NOMBRE" to "C. Mexico", "TIPO" to "Calle Local", "LONG_KM" to 0.6)))
        polylineLayer.features.add(Feature(6L, avElSol, mutableMapOf("ID" to 6, "NOMBRE" to "Av. El Sol", "TIPO" to "Avenida Turística", "LONG_KM" to 2.1)))
        polylineLayer.features.add(Feature(7L, avCultura, mutableMapOf("ID" to 7, "NOMBRE" to "Av. de la Cultura", "TIPO" to "Arteria Metropolitana", "LONG_KM" to 8.5)))

        // 3. Point Layer: Puntos de Interés / Equipamientos (Cusco)
        val pointLayer = VectorLayer(
            name = "Puntos de Interés (POIs)",
            geometryType = GeometryType.POINT,
            labelFieldName = "NOMBRE",
            symbology = LayerSymbology.Simple(
                fillColor = ColorRgba(0, 150, 136), // Teal
                strokeColor = ColorRgba.WHITE,
                strokeWidthDp = 1.8f,
                pointSizeDp = 7.0f
            )
        )
        pointLayer.fields.addAll(
            listOf(
                Field("ID", FieldType.INTEGER),
                Field("NOMBRE", FieldType.STRING),
                Field("CATEGORIA", FieldType.STRING),
                Field("ELEVACION", FieldType.DOUBLE)
            )
        )

        pointLayer.features.add(Feature(1L, Point(-71.9785, -13.5165), mutableMapOf("ID" to 1, "NOMBRE" to "Plaza Mayor", "CATEGORIA" to "Patrimonio", "ELEVACION" to 3399.0)))
        pointLayer.features.add(Feature(2L, Point(-71.9815, -13.5080), mutableMapOf("ID" to 2, "NOMBRE" to "Sacsayhuamán", "CATEGORIA" to "Arqueológico", "ELEVACION" to 3701.0)))
        pointLayer.features.add(Feature(3L, Point(-71.9390, -13.5360), mutableMapOf("ID" to 3, "NOMBRE" to "Aeropuerto Int. Velasco Astete", "CATEGORIA" to "Transporte", "ELEVACION" to 3310.0)))
        pointLayer.features.add(Feature(4L, Point(-71.9468, -13.5392), mutableMapOf("ID" to 4, "NOMBRE" to "Mercado Uvima 7", "CATEGORIA" to "Comercio", "ELEVACION" to 3320.0)))
        pointLayer.features.add(Feature(5L, Point(-71.9490, -13.5360), mutableMapOf("ID" to 5, "NOMBRE" to "Apart Hotel Las Margaritas", "CATEGORIA" to "Hotel", "ELEVACION" to 3325.0)))
        pointLayer.features.add(Feature(6L, Point(-71.9442, -13.5410), mutableMapOf("ID" to 6, "NOMBRE" to "Paradero Las Joyas", "CATEGORIA" to "Paradero", "ELEVACION" to 3318.0)))
        pointLayer.features.add(Feature(7L, Point(-71.9515, -13.5425), mutableMapOf("ID" to 7, "NOMBRE" to "Cancha Sintética Don Ignacio", "CATEGORIA" to "Deportes", "ELEVACION" to 3315.0)))
        pointLayer.features.add(Feature(8L, Point(-71.7650, -13.4350), mutableMapOf("ID" to 8, "NOMBRE" to "Pachatúsan (4,842m)", "CATEGORIA" to "Montaña", "ELEVACION" to 4842.0)))
        pointLayer.features.add(Feature(9L, Point(-71.9850, -13.2980), mutableMapOf("ID" to 9, "NOMBRE" to "Sahuasiray (5,818m)", "CATEGORIA" to "Nevado", "ELEVACION" to 5818.0)))

        return listOf(pointLayer, polylineLayer, polygonLayer)
    }
}
