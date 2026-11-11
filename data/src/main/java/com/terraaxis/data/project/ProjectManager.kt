package com.terraaxis.data.project

import com.terraaxis.core.crs.CoordinateReferenceSystem
import com.terraaxis.core.geometry.GeometryType
import com.terraaxis.core.model.VectorLayer
import com.terraaxis.core.symbology.ColorRgba
import com.terraaxis.core.symbology.LayerSymbology
import com.terraaxis.data.geojson.GeoJsonParser
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class MapProject(
    var name: String = "Untitled Map",
    var centerLon: Double = -71.9675, // Default Cusco / Peru coordinates
    var centerLat: Double = -13.5320,
    var zoomLevel: Float = 12.0f,
    var crsEpsg: Int = 4326,
    val layers: MutableList<VectorLayer> = mutableListOf()
)

object ProjectManager {

    fun saveProject(project: MapProject, file: File) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("name", project.name)
        root.put("centerLon", project.centerLon)
        root.put("centerLat", project.centerLat)
        root.put("zoomLevel", project.zoomLevel.toDouble())
        root.put("crsEpsg", project.crsEpsg)

        val layersArr = JSONArray()
        for (layer in project.layers) {
            val lObj = JSONObject()
            lObj.put("id", layer.id)
            lObj.put("name", layer.name)
            lObj.put("geometryType", layer.geometryType.name)
            lObj.put("isVisible", layer.isVisible)
            lObj.put("opacity", layer.opacity.toDouble())
            lObj.put("labelFieldName", layer.labelFieldName ?: JSONObject.NULL)

            // Save symbology
            val symObj = JSONObject()
            when (val s = layer.symbology) {
                is LayerSymbology.Simple -> {
                    symObj.put("type", "Simple")
                    symObj.put("fillColor", s.fillColor.hexColorString)
                    symObj.put("fillAlpha", s.fillColor.alpha.toDouble())
                    symObj.put("strokeColor", s.strokeColor.hexColorString)
                    symObj.put("strokeWidth", s.strokeWidthDp.toDouble())
                    symObj.put("pointSize", s.pointSizeDp.toDouble())
                }
                is LayerSymbology.UniqueValues -> {
                    symObj.put("type", "UniqueValues")
                    symObj.put("fieldName", s.fieldName)
                    symObj.put("strokeColor", s.strokeColor.hexColorString)
                    symObj.put("strokeWidth", s.strokeWidthDp.toDouble())
                    val colorsObj = JSONObject()
                    s.valueColors.forEach { (k, v) ->
                        colorsObj.put(k, v.hexColorString)
                    }
                    symObj.put("valueColors", colorsObj)
                }
            }
            lObj.put("symbology", symObj)

            // Save GeoJSON data of layer
            val geoJsonStr = GeoJsonParser.exportToGeoJson(layer)
            lObj.put("data", JSONObject(geoJsonStr))

            layersArr.put(lObj)
        }

        root.put("layers", layersArr)
        file.parentFile?.mkdirs()
        file.writeText(root.toString(2), Charsets.UTF_8)
    }

    fun loadProject(file: File): MapProject {
        val text = file.readText(Charsets.UTF_8)
        val root = JSONObject(text)

        val project = MapProject(
            name = root.optString("name", file.nameWithoutExtension),
            centerLon = root.optDouble("centerLon", -71.9675),
            centerLat = root.optDouble("centerLat", -13.5320),
            zoomLevel = root.optDouble("zoomLevel", 12.0).toFloat(),
            crsEpsg = root.optInt("crsEpsg", 4326)
        )

        val layersArr = root.optJSONArray("layers") ?: JSONArray()
        for (i in 0 until layersArr.length()) {
            val lObj = layersArr.getJSONObject(i)
            val name = lObj.optString("name", "Layer $i")
            val dataObj = lObj.optJSONObject("data")

            val layer = if (dataObj != null) {
                GeoJsonParser.parse(dataObj.toString(), name)
            } else {
                VectorLayer(
                    name = name,
                    geometryType = GeometryType.valueOf(lObj.optString("geometryType", "POLYGON"))
                )
            }

            layer.isVisible = lObj.optBoolean("isVisible", true)
            layer.opacity = lObj.optDouble("opacity", 1.0).toFloat()
            if (lObj.has("labelFieldName") && !lObj.isNull("labelFieldName")) {
                layer.labelFieldName = lObj.getString("labelFieldName")
            }

            val symObj = lObj.optJSONObject("symbology")
            if (symObj != null) {
                val symType = symObj.optString("type", "Simple")
                if (symType == "Simple") {
                    val fillHex = symObj.optString("fillColor", "#2196F3")
                    val fillAlpha = symObj.optDouble("fillAlpha", 0.5).toFloat()
                    val strokeHex = symObj.optString("strokeColor", "#2196F3")
                    val strokeWidth = symObj.optDouble("strokeWidth", 2.0).toFloat()
                    val pointSize = symObj.optDouble("pointSize", 8.0).toFloat()

                    layer.symbology = LayerSymbology.Simple(
                        fillColor = parseHexColor(fillHex).copy(alpha = fillAlpha),
                        strokeColor = parseHexColor(strokeHex),
                        strokeWidthDp = strokeWidth,
                        pointSizeDp = pointSize
                    )
                }
            }

            project.layers.add(layer)
        }

        return project
    }

    private fun parseHexColor(hex: String): ColorRgba {
        val clean = hex.removePrefix("#")
        return try {
            val r = clean.substring(0, 2).toInt(16)
            val g = clean.substring(2, 4).toInt(16)
            val b = clean.substring(4, 6).toInt(16)
            ColorRgba(r, g, b)
        } catch (e: Exception) {
            ColorRgba.BLUE
        }
    }
}
