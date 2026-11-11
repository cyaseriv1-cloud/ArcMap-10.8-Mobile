package com.terraaxis.data.shapefile

import com.terraaxis.core.geometry.*
import com.terraaxis.core.model.Field
import com.terraaxis.core.model.FieldType
import com.terraaxis.core.model.VectorLayer
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ShapefileWriter {

    private const val WGS84_PRJ = "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]]"

    fun write(layer: VectorLayer, targetDirectory: File, baseName: String = layer.name): List<File> {
        targetDirectory.mkdirs()
        val shpFile = File(targetDirectory, "$baseName.shp")
        val shxFile = File(targetDirectory, "$baseName.shx")
        val dbfFile = File(targetDirectory, "$baseName.dbf")
        val prjFile = File(targetDirectory, "$baseName.prj")

        val shpShapeType = when (layer.geometryType) {
            GeometryType.POINT -> 1
            GeometryType.POLYLINE, GeometryType.MULTI_POLYLINE -> 3
            GeometryType.POLYGON, GeometryType.MULTI_POLYGON -> 5
            GeometryType.MULTI_POINT -> 8
            else -> 1
        }

        // 1. Build records for SHP and SHX
        val shpRecordsBytes = ByteArrayOutputStream()
        val shxRecordsBytes = ByteArrayOutputStream()

        val bbox = layer.boundingBox
        var currentOffsetWords = 50 // 100 bytes / 2

        for ((index, feature) in layer.features.withIndex()) {
            val recordNum = index + 1
            val geom = feature.geometry

            val recordContent = buildShpRecordContent(geom, shpShapeType)
            val contentLengthWords = recordContent.size / 2

            // Write SHX entry (Offset in 16-bit words, Content Length in words)
            val shxEntry = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
            shxEntry.putInt(currentOffsetWords)
            shxEntry.putInt(contentLengthWords)
            shxRecordsBytes.write(shxEntry.array())

            // Write SHP Record Header (Record Number, Content Length in words)
            val shpHeader = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
            shpHeader.putInt(recordNum)
            shpHeader.putInt(contentLengthWords)
            shpRecordsBytes.write(shpHeader.array())
            shpRecordsBytes.write(recordContent)

            currentOffsetWords += 4 + contentLengthWords
        }

        val shpTotalWords = 50 + (shpRecordsBytes.size() / 2)
        val shxTotalWords = 50 + (shxRecordsBytes.size() / 2)

        // Write SHP file
        shpFile.outputStream().use { fos ->
            writeShapeHeader(fos, shpTotalWords, shpShapeType, bbox)
            shpRecordsBytes.writeTo(fos)
        }

        // Write SHX file
        shxFile.outputStream().use { fos ->
            writeShapeHeader(fos, shxTotalWords, shpShapeType, bbox)
            shxRecordsBytes.writeTo(fos)
        }

        // Write DBF file
        writeDbf(layer, dbfFile)

        // Write PRJ file
        prjFile.writeText(WGS84_PRJ, Charsets.US_ASCII)

        return listOf(shpFile, shxFile, dbfFile, prjFile)
    }

    private fun writeShapeHeader(
        out: java.io.OutputStream,
        fileLengthWords: Int,
        shapeType: Int,
        box: BoundingBox
    ) {
        val header = ByteBuffer.allocate(100)

        // Big-endian part (bytes 0 to 27)
        header.order(ByteOrder.BIG_ENDIAN)
        header.putInt(9994) // File Code
        header.putInt(0) // Unused 1
        header.putInt(0) // Unused 2
        header.putInt(0) // Unused 3
        header.putInt(0) // Unused 4
        header.putInt(0) // Unused 5
        header.putInt(fileLengthWords)

        // Little-endian part (bytes 28 to 99)
        header.order(ByteOrder.LITTLE_ENDIAN)
        header.putInt(1000) // Version 1000
        header.putInt(shapeType)

        val minX = if (box == BoundingBox.EMPTY) 0.0 else box.minX
        val minY = if (box == BoundingBox.EMPTY) 0.0 else box.minY
        val maxX = if (box == BoundingBox.EMPTY) 0.0 else box.maxX
        val maxY = if (box == BoundingBox.EMPTY) 0.0 else box.maxY

        header.putDouble(minX)
        header.putDouble(minY)
        header.putDouble(maxX)
        header.putDouble(maxY)
        header.putDouble(0.0) // Zmin
        header.putDouble(0.0) // Zmax
        header.putDouble(0.0) // Mmin
        header.putDouble(0.0) // Mmax

        out.write(header.array())
    }

    private fun buildShpRecordContent(geom: Geometry?, shapeType: Int): ByteArray {
        if (geom == null) {
            val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            buf.putInt(0) // Null shape
            return buf.array()
        }

        return when (shapeType) {
            1 -> { // Point
                val pt = geom as? Point ?: Point(0.0, 0.0)
                val buf = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
                buf.putInt(1)
                buf.putDouble(pt.x)
                buf.putDouble(pt.y)
                buf.array()
            }
            3, 5 -> { // Polyline or Polygon
                val parts = mutableListOf<List<Point>>()
                when (geom) {
                    is Polyline -> parts.add(geom.points)
                    is MultiPolyline -> parts.addAll(geom.polylines.map { it.points })
                    is Polygon -> parts.addAll(geom.rings)
                    is MultiPolygon -> parts.addAll(geom.polygons.flatMap { it.rings })
                    else -> {}
                }

                val numParts = parts.size
                val numPoints = parts.sumOf { it.size }
                val box = geom.boundingBox

                val totalSize = 4 + 32 + 4 + 4 + (numParts * 4) + (numPoints * 16)
                val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
                buf.putInt(shapeType)
                buf.putDouble(box.minX)
                buf.putDouble(box.minY)
                buf.putDouble(box.maxX)
                buf.putDouble(box.maxY)
                buf.putInt(numParts)
                buf.putInt(numPoints)

                var pointIndex = 0
                for (part in parts) {
                    buf.putInt(pointIndex)
                    pointIndex += part.size
                }

                for (part in parts) {
                    for (p in part) {
                        buf.putDouble(p.x)
                        buf.putDouble(p.y)
                    }
                }

                buf.array()
            }
            else -> {
                val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                buf.putInt(0)
                buf.array()
            }
        }
    }

    private fun writeDbf(layer: VectorLayer, dbfFile: File) {
        val fields = layer.fields
        val recordCount = layer.features.size

        // Calculate record length: 1 byte for delete flag + sum of field lengths
        var recordLength = 1
        for (f in fields) {
            recordLength += getFieldDbfLength(f)
        }

        val headerLength = 32 + (fields.size * 32) + 1 // +1 for 0x0D terminator

        dbfFile.outputStream().use { fos ->
            val dos = DataOutputStream(fos)
            val header = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
            header.put(0x03.toByte()) // dBASE III
            header.put(24.toByte()) // YY (2024 -> 24)
            header.put(11.toByte()) // MM
            header.put(11.toByte()) // DD
            header.putInt(recordCount)
            header.putShort(headerLength.toShort())
            header.putShort(recordLength.toShort())
            fos.write(header.array())

            // Field Descriptors
            for (f in fields) {
                val fBuf = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
                val nameBytes = f.name.take(10).toByteArray(Charsets.US_ASCII)
                val namePadded = ByteArray(11)
                System.arraycopy(nameBytes, 0, namePadded, 0, nameBytes.size)
                fBuf.put(namePadded)

                val (typeChar, len, dec) = when (f.type) {
                    FieldType.INTEGER -> Triple('N'.code.toByte(), 10.toByte(), 0.toByte())
                    FieldType.DOUBLE -> Triple('N'.code.toByte(), 19.toByte(), 6.toByte())
                    FieldType.BOOLEAN -> Triple('L'.code.toByte(), 1.toByte(), 0.toByte())
                    FieldType.DATE -> Triple('D'.code.toByte(), 8.toByte(), 0.toByte())
                    else -> Triple('C'.code.toByte(), f.length.coerceIn(1, 254).toByte(), 0.toByte())
                }

                fBuf.put(typeChar)
                fBuf.putInt(0) // Address
                fBuf.put(len)
                fBuf.put(dec)
                fos.write(fBuf.array())
            }

            // Terminator
            fos.write(0x0D)

            // Records
            for (feature in layer.features) {
                fos.write(0x20) // ' ' not deleted
                for (f in fields) {
                    val fieldLen = getFieldDbfLength(f)
                    val value = feature.attributes[f.name]
                    val formatted = when (f.type) {
                        FieldType.INTEGER -> (value?.toString() ?: "").padStart(fieldLen, ' ')
                        FieldType.DOUBLE -> {
                            val d = feature.getDouble(f.name)
                            if (d != null) String.format(java.util.Locale.US, "%19.6f", d) else "".padStart(fieldLen, ' ')
                        }
                        FieldType.BOOLEAN -> when (feature.getBoolean(f.name)) {
                            true -> "T"
                            false -> "F"
                            null -> " "
                        }
                        else -> (value?.toString() ?: "").padEnd(fieldLen, ' ')
                    }
                    val recordBytes = formatted.take(fieldLen).toByteArray(Charsets.ISO_8859_1)
                    val padded = ByteArray(fieldLen) { ' '.code.toByte() }
                    System.arraycopy(recordBytes, 0, padded, 0, minOf(recordBytes.size, fieldLen))
                    fos.write(padded)
                }
            }

            // End of File marker
            fos.write(0x1A)
        }
    }

    private fun getFieldDbfLength(f: Field): Int = when (f.type) {
        FieldType.INTEGER -> 10
        FieldType.DOUBLE -> 19
        FieldType.BOOLEAN -> 1
        FieldType.DATE -> 8
        else -> f.length.coerceIn(1, 254)
    }
}
