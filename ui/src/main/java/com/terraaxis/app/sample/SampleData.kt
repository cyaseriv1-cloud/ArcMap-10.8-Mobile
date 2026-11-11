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

        // 2. Polyline Layer: Red Vial y Vías Principales
        val polylineLayer = VectorLayer(
            name = "Red Vial Principal",
            geometryType = GeometryType.POLYLINE,
            labelFieldName = "NOMBRE",
            symbology = LayerSymbology.Simple(
                strokeColor = ColorRgba(211, 47, 47), // Red
                strokeWidthDp = 3.0f
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

        val line1 = Polyline(
            listOf(
                Point(centerLon - 0.05, centerLat + 0.02),
                Point(centerLon - 0.01, centerLat + 0.01),
                Point(centerLon + 0.02, centerLat - 0.01),
                Point(centerLon + 0.06, centerLat - 0.03)
            )
        )
        val line2 = Polyline(
            listOf(
                Point(centerLon - 0.02, centerLat - 0.04),
                Point(centerLon - 0.01, centerLat - 0.01),
                Point(centerLon + 0.01, centerLat + 0.03)
            )
        )

        polylineLayer.features.add(
            Feature(
                1L,
                line1,
                mutableMapOf(
                    "ID" to 1,
                    "NOMBRE" to "Av. El Sol",
                    "TIPO" to "Arteria Principal",
                    "LONG_KM" to 14.2
                )
            )
        )
        polylineLayer.features.add(
            Feature(
                2L,
                line2,
                mutableMapOf(
                    "ID" to 2,
                    "NOMBRE" to "Vía de Evitamiento",
                    "TIPO" to "Vía Rápida",
                    "LONG_KM" to 8.6
                )
            )
        )

        // 3. Point Layer: Puntos de Interés / Estaciones
        val pointLayer = VectorLayer(
            name = "Estaciones y POIs",
            geometryType = GeometryType.POINT,
            labelFieldName = "NOMBRE",
            symbology = LayerSymbology.Simple(
                fillColor = ColorRgba(0, 150, 136), // Teal
                strokeColor = ColorRgba.WHITE,
                strokeWidthDp = 2.0f,
                pointSizeDp = 7.0f
            )
        )
        pointLayer.fields.addAll(
            listOf(
                Field("ID", FieldType.INTEGER),
                Field("NOMBRE", FieldType.STRING),
                Field("ELEVACION", FieldType.DOUBLE),
                Field("TEMPERATURA", FieldType.DOUBLE)
            )
        )

        pointLayer.features.add(
            Feature(
                1L,
                Point(centerLon, centerLat),
                mutableMapOf(
                    "ID" to 1,
                    "NOMBRE" to "Plaza Mayor",
                    "ELEVACION" to 3399.0,
                    "TEMPERATURA" to 16.5
                )
            )
        )
        pointLayer.features.add(
            Feature(
                2L,
                Point(centerLon - 0.015, centerLat + 0.012),
                mutableMapOf(
                    "ID" to 2,
                    "NOMBRE" to "Sacsayhuamán",
                    "ELEVACION" to 3701.0,
                    "TEMPERATURA" to 14.0
                )
            )
        )
        pointLayer.features.add(
            Feature(
                3L,
                Point(centerLon + 0.035, centerLat - 0.015),
                mutableMapOf(
                    "ID" to 3,
                    "NOMBRE" to "Aeropuerto",
                    "ELEVACION" to 3310.0,
                    "TEMPERATURA" to 17.2
                )
            )
        )

        return listOf(pointLayer, polylineLayer, polygonLayer)
    }
}
