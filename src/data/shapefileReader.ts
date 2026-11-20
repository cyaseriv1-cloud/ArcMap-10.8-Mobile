import {
  type Geometry,
  GeometryType,
  type Point,
  type Polyline,
  makeMultiPoint,
  makeMultiPolyline,
  makePoint,
  makePolygon,
  makePolyline,
} from '../core/geometry';
import {
  type AttributeValue,
  type Feature,
  type Field,
  FieldType,
  type VectorLayer,
  createVectorLayer,
  makeField,
} from '../core/model';

interface DbfFieldDesc {
  name: string;
  type: FieldType;
  typeChar: string;
  length: number;
  decimalCount: number;
}

export const ShapefileReader = {
  readBuffers(
    baseName: string,
    shpBuffer: ArrayBuffer,
    dbfBuffer?: ArrayBuffer | null
  ): VectorLayer {
    const { geomType, geometries } = readShp(shpBuffer);
    const { fields, attributeRecords } = dbfBuffer
      ? readDbf(dbfBuffer)
      : { fields: [] as Field[], attributeRecords: [] as Record<string, AttributeValue>[] };

    const features: Feature[] = [];
    const count = Math.max(geometries.length, attributeRecords.length);

    for (let i = 0; i < count; i++) {
      const geom = geometries[i] ?? null;
      const attrs: Record<string, AttributeValue> =
        i < attributeRecords.length ? { ...attributeRecords[i] } : {};
      features.push({ id: i + 1, geometry: geom, attributes: attrs });
    }

    return createVectorLayer({
      name: baseName,
      geometryType: geomType !== GeometryType.UNKNOWN ? geomType : GeometryType.POLYGON,
      fields,
      features,
    });
  },
};

function readShp(buffer: ArrayBuffer): {
  geomType: GeometryType;
  geometries: (Geometry | null)[];
} {
  if (buffer.byteLength < 100) {
    return { geomType: GeometryType.UNKNOWN, geometries: [] };
  }

  const view = new DataView(buffer);
  const fileCode = view.getInt32(0, false); // Big-endian
  if (fileCode !== 9994) {
    return { geomType: GeometryType.UNKNOWN, geometries: [] };
  }

  const shapeTypeInt = view.getInt32(32, true); // Little-endian
  let dominantType = GeometryType.UNKNOWN;
  if (shapeTypeInt === 1 || shapeTypeInt === 11 || shapeTypeInt === 21) {
    dominantType = GeometryType.POINT;
  } else if (shapeTypeInt === 3 || shapeTypeInt === 13 || shapeTypeInt === 23) {
    dominantType = GeometryType.POLYLINE;
  } else if (shapeTypeInt === 5 || shapeTypeInt === 15 || shapeTypeInt === 25) {
    dominantType = GeometryType.POLYGON;
  } else if (shapeTypeInt === 8 || shapeTypeInt === 18 || shapeTypeInt === 28) {
    dominantType = GeometryType.MULTI_POINT;
  }

  const geometries: (Geometry | null)[] = [];
  let offset = 100;

  while (offset + 8 <= buffer.byteLength) {
    const contentLengthWords = view.getInt32(offset + 4, false); // Big-endian
    const contentLengthBytes = contentLengthWords * 2;
    offset += 8;

    if (contentLengthBytes < 4 || offset + contentLengthBytes > buffer.byteLength) break;

    const recordEnd = offset + contentLengthBytes;
    let pos = offset;
    const recordShapeType = view.getInt32(pos, true);
    pos += 4;

    let geom: Geometry | null = null;
    if (recordShapeType === 0) {
      geom = null;
    } else if (recordShapeType === 1 || recordShapeType === 11 || recordShapeType === 21) {
      if (pos + 16 <= recordEnd) {
        const x = view.getFloat64(pos, true);
        const y = view.getFloat64(pos + 8, true);
        geom = makePoint(x, y);
      }
    } else if (recordShapeType === 8 || recordShapeType === 18 || recordShapeType === 28) {
      // MultiPoint / MultiPointZ / MultiPointM: 32-byte bbox + int32 numPoints
      if (pos + 36 <= recordEnd) {
        pos += 32;
        const numPoints = Math.max(0, view.getInt32(pos, true));
        pos += 4;
        const pts: Point[] = [];
        for (let pt = 0; pt < numPoints && pos + 16 <= recordEnd; pt++) {
          const x = view.getFloat64(pos, true);
          const y = view.getFloat64(pos + 8, true);
          pos += 16;
          pts.push(makePoint(x, y));
        }
        geom = pts.length > 0 ? makeMultiPoint(pts) : null;
      }
    } else if (
      recordShapeType === 3 ||
      recordShapeType === 13 ||
      recordShapeType === 23 ||
      recordShapeType === 5 ||
      recordShapeType === 15 ||
      recordShapeType === 25
    ) {
      if (pos + 40 <= recordEnd) {
        // Skip bounding box (4 doubles = 32 bytes)
        pos += 32;
        const numParts = Math.max(0, view.getInt32(pos, true));
        const numPoints = Math.max(0, view.getInt32(pos + 4, true));
        pos += 8;

        const partIndices: number[] = [];
        for (let p = 0; p < numParts && pos + 4 <= recordEnd; p++) {
          partIndices.push(view.getInt32(pos, true));
          pos += 4;
        }

        const allPoints: Point[] = [];
        for (let pt = 0; pt < numPoints && pos + 16 <= recordEnd; pt++) {
          const x = view.getFloat64(pos, true);
          const y = view.getFloat64(pos + 8, true);
          pos += 16;
          allPoints.push(makePoint(x, y));
        }

        const isPolyline =
          recordShapeType === 3 || recordShapeType === 13 || recordShapeType === 23;

        if (isPolyline) {
          const lines: Polyline[] = [];
          for (let p = 0; p < partIndices.length; p++) {
            const start = partIndices[p];
            const end = p + 1 < partIndices.length ? partIndices[p + 1] : allPoints.length;
            if (start >= 0 && start < end && end <= allPoints.length) {
              lines.push(makePolyline(allPoints.slice(start, end)));
            }
          }
          geom =
            lines.length === 0 ? null : lines.length === 1 ? lines[0] : makeMultiPolyline(lines);
        } else {
          const rings: Point[][] = [];
          for (let p = 0; p < partIndices.length; p++) {
            const start = partIndices[p];
            const end = p + 1 < partIndices.length ? partIndices[p + 1] : allPoints.length;
            if (start >= 0 && start < end && end <= allPoints.length) {
              rings.push(allPoints.slice(start, end));
            }
          }
          geom = rings.length > 0 ? makePolygon(rings) : null;
        }
      }
    }

    geometries.push(geom);
    offset += contentLengthBytes;
  }

  return { geomType: dominantType, geometries };
}

function readDbf(buffer: ArrayBuffer): {
  fields: Field[];
  attributeRecords: Record<string, AttributeValue>[];
} {
  if (buffer.byteLength < 32) return { fields: [], attributeRecords: [] };

  const bytes = new Uint8Array(buffer);
  const view = new DataView(buffer);
  const recordCount = view.getInt32(4, true);
  const headerLength = view.getInt16(8, true);
  const recordLength = view.getInt16(10, true);

  const asciiDecoder = new TextDecoder('ascii');
  const latin1Decoder = new TextDecoder('iso-8859-1');

  const dbfFields: DbfFieldDesc[] = [];
  let offset = 32;
  while (offset < headerLength && bytes[offset] !== 0x0d) {
    if (offset + 32 > buffer.byteLength) break;
    const nameSlice = bytes.slice(offset, offset + 11);
    const nullIdx = nameSlice.indexOf(0);
    const rawName = asciiDecoder
      .decode(nullIdx >= 0 ? nameSlice.slice(0, nullIdx) : nameSlice)
      .trim();
    const typeChar = String.fromCharCode(bytes[offset + 11]);
    const fieldLength = bytes[offset + 16] & 0xff;
    const decimalCount = bytes[offset + 17] & 0xff;

    let fieldType = FieldType.STRING;
    if (typeChar === 'N' || typeChar === 'F') {
      fieldType = decimalCount > 0 ? FieldType.DOUBLE : FieldType.INTEGER;
    } else if (typeChar === 'L') {
      fieldType = FieldType.BOOLEAN;
    } else if (typeChar === 'D') {
      fieldType = FieldType.DATE;
    }

    dbfFields.push({ name: rawName, type: fieldType, typeChar, length: fieldLength, decimalCount });
    offset += 32;
  }

  const records: Record<string, AttributeValue>[] = [];
  let recordOffset = headerLength;

  for (let r = 0; r < recordCount; r++) {
    if (recordOffset + recordLength > buffer.byteLength) break;
    const deleteFlag = String.fromCharCode(bytes[recordOffset]);
    if (deleteFlag !== '*') {
      const rowAttrs: Record<string, AttributeValue> = {};
      let fieldOffset = recordOffset + 1;
      for (const f of dbfFields) {
        const rawVal = latin1Decoder
          .decode(bytes.slice(fieldOffset, fieldOffset + f.length))
          .trim();
        let value: AttributeValue = null;
        if (f.type === FieldType.INTEGER) {
          const parsed = parseInt(rawVal, 10);
          value = rawVal !== '' && !Number.isNaN(parsed) ? parsed : null;
        } else if (f.type === FieldType.DOUBLE) {
          const parsed = parseFloat(rawVal);
          value = rawVal !== '' && !Number.isNaN(parsed) ? parsed : null;
        } else if (f.type === FieldType.BOOLEAN) {
          const upper = rawVal.toUpperCase();
          if (upper === 'T' || upper === 'Y' || upper === '1') value = true;
          else if (upper === 'F' || upper === 'N' || upper === '0') value = false;
        } else {
          value = rawVal.length > 0 ? rawVal : null;
        }
        rowAttrs[f.name] = value;
        fieldOffset += f.length;
      }
      records.push(rowAttrs);
    }
    recordOffset += recordLength;
  }

  const fields = dbfFields.map((f) => makeField(f.name, f.type, f.name, f.length));
  return { fields, attributeRecords: records };
}
