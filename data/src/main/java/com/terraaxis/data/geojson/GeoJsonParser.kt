package com.terraaxis.data.geojson

import com.terraaxis.core.geometry.*
import com.terraaxis.core.model.Feature
import com.terraaxis.core.model.Field
import com.terraaxis.core.model.FieldType
import com.terraaxis.core.model.VectorLayer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object GeoJsonParser {

    fun parse(geoJsonString: String, layerName: String = "Layer"): VectorLayer {
        val root = JSONObject(geoJsonString)
        val type = root.optString("type", "")

        return when (type) {
            "FeatureCollection" -> parseFeatureCollection(root, layerName)
            "Feature" -> {
                val array = JSONArray().put(root)
                val dummyFc = JSONObject().put("type", "FeatureCollection").put("features", array)
                parseFeatureCollection(dummyFc, layerName)
            }
            else -> parseFeatureCollection(JSONObject().put("features", JSONArray()), layerName)
        }
    }

    fun parseFile(file: File, layerName: String = file.nameWithoutExtension): VectorLayer {
        return parse(file.readText(Charsets.UTF_8), layerName)
    }

    private fun parseFeatureCollection(fcJson: JSONObject, layerName: String): VectorLayer {
        val featuresArray = fcJson.optJSONArray("features") ?: JSONArray()
        val parsedFeatures = ArrayList<Feature>()
        val fieldsMap = LinkedHashMap<String, FieldType>()
        var dominantType: GeometryType = GeometryType.UNKNOWN

        for (i in 0 until featuresArray.length()) {
            val fObj = featuresArray.optJSONObject(i) ?: continue
            val id = if (fObj.has("id")) {
                fObj.optLong("id", (i + 1).toLong())
            } else {
                (i + 1).toLong()
            }

            val geomObj = fObj.optJSONObject("geometry")
            val geometry = if (geomObj != null) parseGeometry(geomObj) else null
            if (geometry != null && dominantType == GeometryType.UNKNOWN) {
                dominantType = geometry.geometryType
            }

            val attrs = HashMap<String, Any?>()
            val propsObj = fObj.optJSONObject("properties")
            if (propsObj != null) {
                val keys = propsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = if (propsObj.isNull(key)) null else propsObj.get(key)
                    attrs[key] = value

                    if (value != null && !fieldsMap.containsKey(key)) {
                        val inferredType = when (value) {
                            is Int, is Long -> FieldType.INTEGER
                            is Double, is Float -> FieldType.DOUBLE
                            is Boolean -> FieldType.BOOLEAN
                            else -> FieldType.STRING
                        }
                        fieldsMap[key] = inferredType
                    }
                }
            }

            parsedFeatures.add(Feature(id, geometry, attrs))
        }

        val fields = fieldsMap.map { (name, type) -> Field(name, type) }.toMutableList()
        val layer = VectorLayer(
            name = layerName,
            geometryType = if (dominantType != GeometryType.UNKNOWN) dominantType else GeometryType.POLYGON
        )
        layer.table.fields.addAll(fields)
        layer.table.features.addAll(parsedFeatures)
        return layer
    }

    private fun parseGeometry(geomObj: JSONObject): Geometry? {
        val type = geomObj.optString("type", "")
        val coords = geomObj.optJSONArray("coordinates") ?: return null

        return when (type) {
            "Point" -> {
                Point(coords.getDouble(0), coords.getDouble(1))
            }
            "LineString" -> {
                val pts = ArrayList<Point>()
                for (i in 0 until coords.length()) {
                    val ptArr = coords.getJSONArray(i)
                    pts.add(Point(ptArr.getDouble(0), ptArr.getDouble(1)))
                }
                Polyline(pts)
            }
            "Polygon" -> {
                val rings = ArrayList<List<Point>>()
                for (r in 0 until coords.length()) {
                    val ringArr = coords.getJSONArray(r)
                    val ringPts = ArrayList<Point>()
                    for (i in 0 until ringArr.length()) {
                        val ptArr = ringArr.getJSONArray(i)
                        ringPts.add(Point(ptArr.getDouble(0), ptArr.getDouble(1)))
                    }
                    rings.add(ringPts)
                }
                Polygon(rings)
            }
            "MultiPoint" -> {
                val pts = ArrayList<Point>()
                for (i in 0 until coords.length()) {
                    val ptArr = coords.getJSONArray(i)
                    pts.add(Point(ptArr.getDouble(0), ptArr.getDouble(1)))
                }
                MultiPoint(pts)
            }
            "MultiLineString" -> {
                val lines = ArrayList<Polyline>()
                for (l in 0 until coords.length()) {
                    val lineArr = coords.getJSONArray(l)
                    val pts = ArrayList<Point>()
                    for (i in 0 until lineArr.length()) {
                        val ptArr = lineArr.getJSONArray(i)
                        pts.add(Point(ptArr.getDouble(0), ptArr.getDouble(1)))
                    }
                    lines.add(Polyline(pts))
                }
                MultiPolyline(lines)
            }
            "MultiPolygon" -> {
                val polys = ArrayList<Polygon>()
                for (p in 0 until coords.length()) {
                    val polyArr = coords.getJSONArray(p)
                    val rings = ArrayList<List<Point>>()
                    for (r in 0 until polyArr.length()) {
                        val ringArr = polyArr.getJSONArray(r)
                        val ringPts = ArrayList<Point>()
                        for (i in 0 until ringArr.length()) {
                            val ptArr = ringArr.getJSONArray(i)
                            ringPts.add(Point(ptArr.getDouble(0), ptArr.getDouble(1)))
                        }
                        rings.add(ringPts)
                    }
                    polys.add(Polygon(rings))
                }
                MultiPolygon(polys)
            }
            else -> null
        }
    }

    /**
     * Exports a [VectorLayer] to standard GeoJSON format string.
     */
    fun exportToGeoJson(layer: VectorLayer): String {
        val root = JSONObject()
        root.put("type", "FeatureCollection")

        val featuresArr = JSONArray()
        for (f in layer.features) {
            val fObj = JSONObject()
            fObj.put("type", "Feature")
            fObj.put("id", f.id)

            val propsObj = JSONObject()
            for ((k, v) in f.attributes) {
                propsObj.put(k, v ?: JSONObject.NULL)
            }
            fObj.put("properties", propsObj)

            val geomObj = serializeGeometry(f.geometry)
            if (geomObj != null) {
                fObj.put("geometry", geomObj)
            } else {
                fObj.put("geometry", JSONObject.NULL)
            }
            featuresArr.put(fObj)
        }

        root.put("features", featuresArr)
        return root.toString(2)
    }

    private fun serializeGeometry(geometry: Geometry?): JSONObject? {
        if (geometry == null) return null
        val obj = JSONObject()

        when (geometry) {
            is Point -> {
                obj.put("type", "Point")
                obj.put("coordinates", JSONArray().put(geometry.x).put(geometry.y))
            }
            is Polyline -> {
                obj.put("type", "LineString")
                val ptsArr = JSONArray()
                for (p in geometry.points) {
                    ptsArr.put(JSONArray().put(p.x).put(p.y))
                }
                obj.put("coordinates", ptsArr)
            }
            is Polygon -> {
                obj.put("type", "Polygon")
                val ringsArr = JSONArray()
                for (ring in geometry.rings) {
                    val ringArr = JSONArray()
                    for (p in ring) {
                        ringArr.put(JSONArray().put(p.x).put(p.y))
                    }
                    ringsArr.put(ringArr)
                }
                obj.put("coordinates", ringsArr)
            }
            is MultiPoint -> {
                obj.put("type", "MultiPoint")
                val ptsArr = JSONArray()
                for (p in geometry.points) {
                    ptsArr.put(JSONArray().put(p.x).put(p.y))
                }
                obj.put("coordinates", ptsArr)
            }
            is MultiPolyline -> {
                obj.put("type", "MultiLineString")
                val linesArr = JSONArray()
                for (line in geometry.polylines) {
                    val ptsArr = JSONArray()
                    for (p in line.points) {
                        ptsArr.put(JSONArray().put(p.x).put(p.y))
                    }
                    linesArr.put(ptsArr)
                }
                obj.put("coordinates", linesArr)
            }
            is MultiPolygon -> {
                obj.put("type", "MultiPolygon")
                val polysArr = JSONArray()
                for (poly in geometry.polygons) {
                    val ringsArr = JSONArray()
                    for (ring in poly.rings) {
                        val ringArr = JSONArray()
                        for (p in ring) {
                            ringArr.put(JSONArray().put(p.x).put(p.y))
                        }
                        ringsArr.put(ringArr)
                    }
                    polysArr.put(ringsArr)
                }
                obj.put("coordinates", polysArr)
            }
            else -> return null
        }
        return obj
    }
}
