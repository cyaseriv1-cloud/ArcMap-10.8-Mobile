package com.terraaxis.data.shapefile

import com.terraaxis.core.geometry.*
import com.terraaxis.core.model.Feature
import com.terraaxis.core.model.Field
import com.terraaxis.core.model.FieldType
import com.terraaxis.core.model.VectorLayer
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ShapefileReader {

    fun read(shpFile: File): VectorLayer {
        val baseName = shpFile.nameWithoutExtension
        val parent = shpFile.parentFile ?: File(".")
        val dbfFile = File(parent, "$baseName.dbf")

        val (geomType, geometries) = readShp(shpFile)
        val (fields, attributeRecords) = if (dbfFile.exists()) {
            readDbf(dbfFile)
        } else {
            Pair(emptyList<Field>(), emptyList<Map<String, Any?>>())
        }

        val features = mutableListOf<Feature>()
        val count = maxOf(geometries.size, attributeRecords.size)

        for (i in 0 until count) {
            val geom = geometries.getOrNull(i)
            val attrs = if (i < attributeRecords.size) {
                attributeRecords[i].toMutableMap()
            } else {
                mutableMapOf()
            }
            features.add(Feature((i + 1).toLong(), geom, attrs))
        }

        val layer = VectorLayer(
            name = baseName,
            geometryType = geomType
        )
        layer.table.fields.addAll(fields)
        layer.table.features.addAll(features)
        return layer
    }

    private fun readShp(file: File): Pair<GeometryType, List<Geometry?>> {
        val bytes = file.readBytes()
        if (bytes.size < 100) return Pair(GeometryType.UNKNOWN, emptyList())

        val bbBig = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val fileCode = bbBig.getInt(0)
        if (fileCode != 9994) {
            // Not a valid shapefile
            return Pair(GeometryType.UNKNOWN, emptyList())
        }

        val bbLittle = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val shapeTypeInt = bbLittle.getInt(32)
        val dominantType = when (shapeTypeInt) {
            1 -> GeometryType.POINT
            3 -> GeometryType.POLYLINE
            5 -> GeometryType.POLYGON
            8 -> GeometryType.MULTI_POINT
            else -> GeometryType.UNKNOWN
        }

        val geometries = mutableListOf<Geometry?>()
        var offset = 100

        while (offset + 8 <= bytes.size) {
            bbBig.position(offset)
            val recordNumber = bbBig.getInt()
            val contentLengthWords = bbBig.getInt()
            val contentLengthBytes = contentLengthWords * 2

            offset += 8
            if (offset + contentLengthBytes > bytes.size) break

            bbLittle.position(offset)
            val recordShapeType = bbLittle.getInt()

            val geom: Geometry? = when (recordShapeType) {
                1 -> { // Point
                    val x = bbLittle.getDouble()
                    val y = bbLittle.getDouble()
                    Point(x, y)
                }
                3, 5 -> { // PolyLine (3) or Polygon (5)
                    // Skip bounding box (4 doubles = 32 bytes)
                    bbLittle.position(bbLittle.position() + 32)
                    val numParts = bbLittle.getInt()
                    val numPoints = bbLittle.getInt()

                    val partIndices = IntArray(numParts)
                    for (p in 0 until numParts) {
                        partIndices[p] = bbLittle.getInt()
                    }

                    val allPoints = mutableListOf<Point>()
                    for (pt in 0 until numPoints) {
                        val x = bbLittle.getDouble()
                        val y = bbLittle.getDouble()
                        allPoints.add(Point(x, y))
                    }

                    if (recordShapeType == 3) {
                        // Polyline
                        val lines = mutableListOf<Polyline>()
                        for (p in 0 until numParts) {
                            val start = partIndices[p]
                            val end = if (p + 1 < numParts) partIndices[p + 1] else numPoints
                            if (start < end && end <= allPoints.size) {
                                lines.add(Polyline(allPoints.subList(start, end)))
                            }
                        }
                        if (lines.size == 1) lines.first() else MultiPolyline(lines)
                    } else {
                        // Polygon
                        val rings = mutableListOf<List<Point>>()
                        for (p in 0 until numParts) {
                            val start = partIndices[p]
                            val end = if (p + 1 < numParts) partIndices[p + 1] else numPoints
                            if (start < end && end <= allPoints.size) {
                                rings.add(allPoints.subList(start, end))
                            }
                        }
                        Polygon(rings)
                    }
                }
                else -> null
            }

            geometries.add(geom)
            offset += contentLengthBytes
        }

        return Pair(dominantType, geometries)
    }

    private fun readDbf(file: File): Pair<List<Field>, List<Map<String, Any?>>> {
        val bytes = file.readBytes()
        if (bytes.size < 32) return Pair(emptyList(), emptyList())

        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val recordCount = bb.getInt(4)
        val headerLength = bb.getShort(8).toInt()
        val recordLength = bb.getShort(10).toInt()

        val fields = mutableListOf<DbfFieldDesc>()
        var offset = 32
        while (offset < headerLength && bytes[offset].toInt() != 0x0D) {
            if (offset + 32 > bytes.size) break
            val nameBytes = bytes.copyOfRange(offset, offset + 11)
            val nullIdx = nameBytes.indexOf(0.toByte())
            val rawName = String(
                nameBytes,
                0,
                if (nullIdx >= 0) nullIdx else nameBytes.size,
                Charsets.US_ASCII
            ).trim()
            val typeChar = bytes[offset + 11].toInt().toChar()
            val fieldLength = bytes[offset + 16].toInt() and 0xFF
            val decimalCount = bytes[offset + 17].toInt() and 0xFF

            val fieldType = when (typeChar) {
                'N', 'F' -> if (decimalCount > 0) FieldType.DOUBLE else FieldType.INTEGER
                'L' -> FieldType.BOOLEAN
                'D' -> FieldType.DATE
                else -> FieldType.STRING
            }

            fields.add(DbfFieldDesc(rawName, fieldType, typeChar, fieldLength, decimalCount))
            offset += 32
        }

        val records = mutableListOf<Map<String, Any?>>()
        var recordOffset = headerLength

        for (r in 0 until recordCount) {
            if (recordOffset + recordLength > bytes.size) break
            val deleteFlag = bytes[recordOffset].toInt().toChar()
            if (deleteFlag != '*') { // '*' means deleted
                val rowAttrs = mutableMapOf<String, Any?>()
                var fieldOffset = recordOffset + 1
                for (f in fields) {
                    val rawVal = String(bytes, fieldOffset, f.length, Charsets.ISO_8859_1).trim()
                    val value: Any? = when (f.type) {
                        FieldType.INTEGER -> rawVal.toIntOrNull()
                        FieldType.DOUBLE -> rawVal.toDoubleOrNull()
                        FieldType.BOOLEAN -> when (rawVal.uppercase()) {
                            "T", "Y", "1" -> true
                            "F", "N", "0" -> false
                            else -> null
                        }
                        else -> rawVal.ifEmpty { null }
                    }
                    rowAttrs[f.name] = value
                    fieldOffset += f.length
                }
                records.add(rowAttrs)
            }
            recordOffset += recordLength
        }

        val publicFields = fields.map { Field(it.name, it.type, length = it.length) }
        return Pair(publicFields, records)
    }

    private data class DbfFieldDesc(
        val name: String,
        val type: FieldType,
        val typeChar: Char,
        val length: Int,
        val decimalCount: Int
    )
}
