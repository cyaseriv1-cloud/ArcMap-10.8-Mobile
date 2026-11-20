import {
  type BoundingBox,
  type Geometry,
  GeometryType,
  type Point,
  getGeometryBoundingBox,
  isBBoxEmpty,
  pointsEqual,
} from '../core/geometry';
import {
  type Field,
  FieldType,
  type VectorLayer,
  featureGetBoolean,
  featureGetDouble,
  makeField,
} from '../core/model';
import { getLayerBoundingBox } from '../core/model';

const WGS84_PRJ =
  'GEOGCS["GCS_WGS_1984",DATUM["D_WGS_1984",SPHEROID["WGS_1984",6378137.0,298.257223563]],PRIMEM["Greenwich",0.0],UNIT["Degree",0.0174532925199433]]';

export interface ShapefileBundle {
  shp: Uint8Array;
  shx: Uint8Array;
  dbf: Uint8Array;
  prj: Uint8Array;
}

export const ShapefileWriter = {
  buildFiles(layer: VectorLayer): ShapefileBundle {
    let shpShapeType = 1;
    if (
      layer.geometryType === GeometryType.POLYLINE ||
      layer.geometryType === GeometryType.MULTI_POLYLINE
    ) {
      shpShapeType = 3;
    } else if (
      layer.geometryType === GeometryType.POLYGON ||
      layer.geometryType === GeometryType.MULTI_POLYGON
    ) {
      shpShapeType = 5;
    } else if (layer.geometryType === GeometryType.MULTI_POINT) {
      shpShapeType = 8;
    }

    const shpRecordChunks: Uint8Array[] = [];
    const shxRecordChunks: Uint8Array[] = [];
    const bbox = getLayerBoundingBox(layer);
    let currentOffsetWords = 50;

    layer.features.forEach((feature, index) => {
      const recordNum = index + 1;
      const recordContent = buildShpRecordContent(feature.geometry, shpShapeType);
      const contentLengthWords = recordContent.byteLength / 2;

      const shxEntry = new ArrayBuffer(8);
      const shxView = new DataView(shxEntry);
      shxView.setInt32(0, currentOffsetWords, false);
      shxView.setInt32(4, contentLengthWords, false);
      shxRecordChunks.push(new Uint8Array(shxEntry));

      const shpHeader = new ArrayBuffer(8);
      const shpView = new DataView(shpHeader);
      shpView.setInt32(0, recordNum, false);
      shpView.setInt32(4, contentLengthWords, false);
      shpRecordChunks.push(new Uint8Array(shpHeader));
      shpRecordChunks.push(recordContent);

      currentOffsetWords += 4 + contentLengthWords;
    });

    const shpRecordsLen = shpRecordChunks.reduce((acc, c) => acc + c.byteLength, 0);
    const shxRecordsLen = shxRecordChunks.reduce((acc, c) => acc + c.byteLength, 0);

    const shpTotalWords = 50 + shpRecordsLen / 2;
    const shxTotalWords = 50 + shxRecordsLen / 2;

    const shp = concatBytes([
      buildShapeHeader(shpTotalWords, shpShapeType, bbox),
      ...shpRecordChunks,
    ]);
    const shx = concatBytes([
      buildShapeHeader(shxTotalWords, shpShapeType, bbox),
      ...shxRecordChunks,
    ]);
    const dbf = buildDbf(layer);
    const prj = new TextEncoder().encode(WGS84_PRJ);

    return { shp, shx, dbf, prj };
  },

  buildZipBlob(layer: VectorLayer, baseName: string): Blob {
    const bundle = ShapefileWriter.buildFiles(layer);
    const safeName = baseName.replace(/[^a-zA-Z0-9_-]/g, '_') || 'layer';
    const zipBytes = createUncompressedZip([
      { name: `${safeName}.shp`, data: bundle.shp },
      { name: `${safeName}.shx`, data: bundle.shx },
      { name: `${safeName}.dbf`, data: bundle.dbf },
      { name: `${safeName}.prj`, data: bundle.prj },
    ]);
    return new Blob([zipBytes.buffer as ArrayBuffer], { type: 'application/zip' });
  },
};

function buildShapeHeader(
  fileLengthWords: number,
  shapeType: number,
  box: BoundingBox
): Uint8Array {
  const buf = new ArrayBuffer(100);
  const view = new DataView(buf);

  view.setInt32(0, 9994, false);
  view.setInt32(24, fileLengthWords, false);
  view.setInt32(28, 1000, true);
  view.setInt32(32, shapeType, true);

  const empty = isBBoxEmpty(box);
  view.setFloat64(36, empty ? 0.0 : box.minX, true);
  view.setFloat64(44, empty ? 0.0 : box.minY, true);
  view.setFloat64(52, empty ? 0.0 : box.maxX, true);
  view.setFloat64(60, empty ? 0.0 : box.maxY, true);

  return new Uint8Array(buf);
}

function buildShpRecordContent(geom: Geometry | null, shapeType: number): Uint8Array {
  if (!geom) {
    const buf = new ArrayBuffer(4);
    new DataView(buf).setInt32(0, 0, true);
    return new Uint8Array(buf);
  }

  if (shapeType === 1) {
    const pt =
      geom.geometryType === GeometryType.POINT
        ? geom
        : geom.geometryType === GeometryType.MULTI_POINT && geom.points.length > 0
          ? geom.points[0]
          : { x: 0, y: 0 };
    const buf = new ArrayBuffer(20);
    const view = new DataView(buf);
    view.setInt32(0, 1, true);
    view.setFloat64(4, pt.x, true);
    view.setFloat64(12, pt.y, true);
    return new Uint8Array(buf);
  }

  if (shapeType === 3 || shapeType === 5) {
    const parts: Point[][] = [];
    const pushClosedRing = (ring: Point[]) => {
      if (ring.length === 0) return;
      const closed =
        ring.length >= 2 && !pointsEqual(ring[0], ring[ring.length - 1])
          ? [...ring, ring[0]]
          : ring;
      parts.push(closed);
    };

    if (geom.geometryType === GeometryType.POLYLINE) parts.push(geom.points);
    else if (geom.geometryType === GeometryType.MULTI_POLYLINE) {
      geom.polylines.forEach((l) => parts.push(l.points));
    } else if (geom.geometryType === GeometryType.POLYGON) {
      geom.rings.forEach(pushClosedRing);
    } else if (geom.geometryType === GeometryType.MULTI_POLYGON) {
      geom.polygons.forEach((poly) => poly.rings.forEach(pushClosedRing));
    }

    const numParts = parts.length;
    const numPoints = parts.reduce((sum, p) => sum + p.length, 0);
    const box = getGeometryBoundingBox(geom);

    const totalSize = 4 + 32 + 4 + 4 + numParts * 4 + numPoints * 16;
    const buf = new ArrayBuffer(totalSize);
    const view = new DataView(buf);

    let pos = 0;
    view.setInt32(pos, shapeType, true);
    pos += 4;
    view.setFloat64(pos, box.minX, true);
    view.setFloat64(pos + 8, box.minY, true);
    view.setFloat64(pos + 16, box.maxX, true);
    view.setFloat64(pos + 24, box.maxY, true);
    pos += 32;
    view.setInt32(pos, numParts, true);
    view.setInt32(pos + 4, numPoints, true);
    pos += 8;

    let pointIndex = 0;
    for (const part of parts) {
      view.setInt32(pos, pointIndex, true);
      pos += 4;
      pointIndex += part.length;
    }

    for (const part of parts) {
      for (const p of part) {
        view.setFloat64(pos, p.x, true);
        view.setFloat64(pos + 8, p.y, true);
        pos += 16;
      }
    }

    return new Uint8Array(buf);
  }

  if (shapeType === 8 && geom.geometryType === GeometryType.MULTI_POINT) {
    const pts = geom.points;
    const box = getGeometryBoundingBox(geom);
    const totalSize = 4 + 32 + 4 + pts.length * 16;
    const buf = new ArrayBuffer(totalSize);
    const view = new DataView(buf);
    let pos = 0;
    view.setInt32(pos, 8, true);
    pos += 4;
    view.setFloat64(pos, box.minX, true);
    view.setFloat64(pos + 8, box.minY, true);
    view.setFloat64(pos + 16, box.maxX, true);
    view.setFloat64(pos + 24, box.maxY, true);
    pos += 32;
    view.setInt32(pos, pts.length, true);
    pos += 4;
    for (const p of pts) {
      view.setFloat64(pos, p.x, true);
      view.setFloat64(pos + 8, p.y, true);
      pos += 16;
    }
    return new Uint8Array(buf);
  }

  const buf = new ArrayBuffer(4);
  new DataView(buf).setInt32(0, 0, true);
  return new Uint8Array(buf);
}

function getFieldDbfLength(f: Field): number {
  switch (f.type) {
    case FieldType.INTEGER:
      return 10;
    case FieldType.DOUBLE:
      return 19;
    case FieldType.BOOLEAN:
      return 1;
    case FieldType.DATE:
      return 8;
    default:
      return Math.max(1, Math.min(254, f.length));
  }
}

function buildDbf(layer: VectorLayer): Uint8Array {
  const fields =
    layer.fields.length > 0 ? layer.fields : [makeField('ID', FieldType.INTEGER)];
  const recordCount = layer.features.length;

  let recordLength = 1;
  for (const f of fields) {
    recordLength += getFieldDbfLength(f);
  }

  const headerLength = 32 + fields.length * 32 + 1;
  const totalBytes = headerLength + recordCount * recordLength + 1;
  const out = new Uint8Array(totalBytes);
  const view = new DataView(out.buffer);

  const now = new Date();
  out[0] = 0x03;
  out[1] = Math.max(0, Math.min(255, now.getFullYear() - 1900));
  out[2] = now.getMonth() + 1;
  out[3] = now.getDate();
  view.setInt32(4, recordCount, true);
  view.setInt16(8, headerLength, true);
  view.setInt16(10, recordLength, true);

  let offset = 32;
  for (const f of fields) {
    const cleanName = f.name
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^\x20-\x7e]/g, '_')
      .slice(0, 10);
    for (let i = 0; i < cleanName.length; i++) {
      out[offset + i] = cleanName.charCodeAt(i) & 0x7f;
    }

    let typeChar = 'C';
    let len = Math.max(1, Math.min(254, f.length));
    let dec = 0;
    if (f.type === FieldType.INTEGER) {
      typeChar = 'N';
      len = 10;
    } else if (f.type === FieldType.DOUBLE) {
      typeChar = 'N';
      len = 19;
      dec = 6;
    } else if (f.type === FieldType.BOOLEAN) {
      typeChar = 'L';
      len = 1;
    } else if (f.type === FieldType.DATE) {
      typeChar = 'D';
      len = 8;
    }

    out[offset + 11] = typeChar.charCodeAt(0);
    out[offset + 16] = len;
    out[offset + 17] = dec;
    offset += 32;
  }

  out[offset] = 0x0d;
  offset += 1;

  for (const feature of layer.features) {
    out[offset] = 0x20;
    offset += 1;

    for (const f of fields) {
      const fieldLen = getFieldDbfLength(f);
      const value = feature.attributes[f.name];
      let formatted = '';

      if (f.type === FieldType.INTEGER) {
        const d = featureGetDouble(feature, f.name);
        formatted =
          d !== null && Number.isFinite(d)
            ? String(Math.trunc(d)).padStart(fieldLen, ' ')
            : ' '.repeat(fieldLen);
      } else if (f.type === FieldType.DOUBLE) {
        const d = featureGetDouble(feature, f.name);
        formatted = d !== null ? d.toFixed(6).padStart(fieldLen, ' ') : ' '.repeat(fieldLen);
      } else if (f.type === FieldType.BOOLEAN) {
        const b = featureGetBoolean(feature, f.name);
        formatted = b === true ? 'T' : b === false ? 'F' : ' ';
      } else {
        formatted = (value !== null && value !== undefined ? String(value) : '').padEnd(
          fieldLen,
          ' '
        );
      }

      const clipped = formatted.slice(0, fieldLen);
      for (let i = 0; i < fieldLen; i++) {
        out[offset + i] = i < clipped.length ? clipped.charCodeAt(i) & 0xff : 0x20;
      }
      offset += fieldLen;
    }
  }

  out[offset] = 0x1a;
  return out;
}

function concatBytes(chunks: Uint8Array[]): Uint8Array {
  const total = chunks.reduce((sum, c) => sum + c.byteLength, 0);
  const res = new Uint8Array(total);
  let pos = 0;
  for (const c of chunks) {
    res.set(c, pos);
    pos += c.byteLength;
  }
  return res;
}

// CRC32 table for uncompressed ZIP archive
const CRC_TABLE = (() => {
  const table = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) {
      c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    }
    table[n] = c >>> 0;
  }
  return table;
})();

function crc32(data: Uint8Array): number {
  let crc = 0xffffffff;
  for (let i = 0; i < data.length; i++) {
    crc = CRC_TABLE[(crc ^ data[i]) & 0xff] ^ (crc >>> 8);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function createUncompressedZip(entries: { name: string; data: Uint8Array }[]): Uint8Array {
  const encoder = new TextEncoder();
  const localParts: Uint8Array[] = [];
  const centralParts: Uint8Array[] = [];
  let offset = 0;

  for (const entry of entries) {
    const nameBytes = encoder.encode(entry.name);
    const crc = crc32(entry.data);
    const size = entry.data.byteLength;

    const localHeader = new ArrayBuffer(30 + nameBytes.length);
    const lv = new DataView(localHeader);
    lv.setUint32(0, 0x04034b50, true);
    lv.setUint16(4, 20, true);
    lv.setUint16(6, 0, true);
    lv.setUint16(8, 0, true);
    lv.setUint16(10, 0, true);
    lv.setUint16(12, 0, true);
    lv.setUint32(14, crc, true);
    lv.setUint32(18, size, true);
    lv.setUint32(22, size, true);
    lv.setUint16(26, nameBytes.length, true);
    lv.setUint16(28, 0, true);
    new Uint8Array(localHeader, 30).set(nameBytes);

    localParts.push(new Uint8Array(localHeader), entry.data);

    const centralHeader = new ArrayBuffer(46 + nameBytes.length);
    const cv = new DataView(centralHeader);
    cv.setUint32(0, 0x02014b50, true);
    cv.setUint16(4, 20, true);
    cv.setUint16(6, 20, true);
    cv.setUint16(8, 0, true);
    cv.setUint16(10, 0, true);
    cv.setUint16(12, 0, true);
    cv.setUint16(14, 0, true);
    cv.setUint32(16, crc, true);
    cv.setUint32(20, size, true);
    cv.setUint32(24, size, true);
    cv.setUint16(28, nameBytes.length, true);
    cv.setUint16(30, 0, true);
    cv.setUint16(32, 0, true);
    cv.setUint16(34, 0, true);
    cv.setUint16(36, 0, true);
    cv.setUint32(38, 0, true);
    cv.setUint32(42, offset, true);
    new Uint8Array(centralHeader, 46).set(nameBytes);

    centralParts.push(new Uint8Array(centralHeader));
    offset += localHeader.byteLength + size;
  }

  const centralSize = centralParts.reduce((s, p) => s + p.byteLength, 0);
  const eocd = new ArrayBuffer(22);
  const ev = new DataView(eocd);
  ev.setUint32(0, 0x06054b50, true);
  ev.setUint16(4, 0, true);
  ev.setUint16(6, 0, true);
  ev.setUint16(8, entries.length, true);
  ev.setUint16(10, entries.length, true);
  ev.setUint32(12, centralSize, true);
  ev.setUint32(16, offset, true);
  ev.setUint16(20, 0, true);

  return concatBytes([...localParts, ...centralParts, new Uint8Array(eocd)]);
}
