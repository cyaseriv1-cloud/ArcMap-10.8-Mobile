import {
  type Geometry,
  GeometryType,
  type Point,
  type Polygon,
  type Polyline,
  makeMultiPolygon,
  makePoint,
  makePolygonFromRing,
  polygonCentroid,
} from '../core/geometry';
import {
  type Feature,
  FieldType,
  type VectorLayer,
  createVectorLayer,
  makeField,
} from '../core/model';
import { createSimpleSymbology, makeColor } from '../core/symbology';

const SEGMENTS_PER_CIRCLE = 32;

export const BufferTool = {
  execute(
    inputLayer: VectorLayer,
    distanceMeters: number,
    outputLayerName = `${inputLayer.name}_buffer_${Math.round(distanceMeters)}m`
  ): VectorLayer {
    const outFeatures: Feature[] = [];

    inputLayer.features.forEach((feat, idx) => {
      const geom = feat.geometry;
      if (!geom) return;
      const bufferedGeom = bufferGeometry(geom, distanceMeters);
      if (!bufferedGeom) return;

      outFeatures.push({
        id: idx + 1,
        geometry: bufferedGeom,
        attributes: {
          ...feat.attributes,
          BUFF_DIST: distanceMeters,
        },
      });
    });

    const outFields = inputLayer.fields.map((f) => ({ ...f }));
    if (!outFields.some((f) => f.name === 'BUFF_DIST')) {
      outFields.push(makeField('BUFF_DIST', FieldType.DOUBLE));
    }

    return createVectorLayer({
      name: outputLayerName,
      geometryType: GeometryType.POLYGON,
      symbology: createSimpleSymbology({
        fillColor: makeColor(255, 167, 38, 0.45),
        strokeColor: makeColor(239, 108, 0),
        strokeWidthDp: 2.0,
      }),
      fields: outFields,
      features: outFeatures,
    });
  },
};

function bufferGeometry(geom: Geometry, distanceMeters: number): Geometry | null {
  switch (geom.geometryType) {
    case GeometryType.POINT:
      return bufferPoint(geom, distanceMeters);
    case GeometryType.POLYLINE:
      return bufferPolyline(geom, distanceMeters);
    case GeometryType.POLYGON:
      return bufferPolygon(geom, distanceMeters);
    case GeometryType.MULTI_POINT:
      return makeMultiPolygon(geom.points.map((pt) => bufferPoint(pt, distanceMeters)));
    case GeometryType.MULTI_POLYLINE: {
      const polys = geom.polylines
        .map((line) => bufferPolyline(line, distanceMeters))
        .filter((p): p is Polygon => p !== null);
      return makeMultiPolygon(polys);
    }
    case GeometryType.MULTI_POLYGON: {
      const polys = geom.polygons
        .map((poly) => bufferPolygon(poly, distanceMeters))
        .filter((p): p is Polygon => p !== null);
      return makeMultiPolygon(polys);
    }
    default:
      return null;
  }
}

function bufferPoint(point: Point, distanceMeters: number): Polygon {
  const latRad = (point.y * Math.PI) / 180.0;
  const degLat = distanceMeters / 111320.0;
  const degLon = distanceMeters / (111320.0 * Math.max(0.001, Math.cos(latRad)));

  const ring: Point[] = [];
  for (let i = 0; i < SEGMENTS_PER_CIRCLE; i++) {
    const angle = (2.0 * Math.PI * i) / SEGMENTS_PER_CIRCLE;
    ring.push(makePoint(point.x + degLon * Math.cos(angle), point.y + degLat * Math.sin(angle)));
  }
  ring.push(ring[0]);
  return makePolygonFromRing(ring);
}

function bufferPolyline(polyline: Polyline, distanceMeters: number): Polygon | null {
  const pts = polyline.points;
  if (pts.length < 2) return null;

  const latAvg = pts.reduce((s, p) => s + p.y, 0) / pts.length;
  const latRad = (latAvg * Math.PI) / 180.0;
  const degLat = distanceMeters / 111320.0;
  const degLon = distanceMeters / (111320.0 * Math.max(0.001, Math.cos(latRad)));

  const leftSide: Point[] = [];
  const rightSide: Point[] = [];

  for (let i = 0; i < pts.length - 1; i++) {
    const p1 = pts[i];
    const p2 = pts[i + 1];
    const dx = p2.x - p1.x;
    const dy = p2.y - p1.y;
    const len = Math.sqrt(dx * dx + dy * dy);
    if (len < 1e-9) continue;

    const nx = (-dy / len) * degLon;
    const ny = (dx / len) * degLat;

    leftSide.push(makePoint(p1.x + nx, p1.y + ny));
    leftSide.push(makePoint(p2.x + nx, p2.y + ny));
    rightSide.push(makePoint(p1.x - nx, p1.y - ny));
    rightSide.push(makePoint(p2.x - nx, p2.y - ny));
  }

  if (leftSide.length === 0) return null;

  const ring: Point[] = [...leftSide, ...rightSide.reverse(), leftSide[0]];
  return makePolygonFromRing(ring);
}

function bufferPolygon(polygon: Polygon, distanceMeters: number): Polygon | null {
  const extRing = polygon.rings[0] ?? [];
  if (extRing.length < 3) return null;

  const latAvg = extRing.reduce((s, p) => s + p.y, 0) / extRing.length;
  const latRad = (latAvg * Math.PI) / 180.0;
  const degLat = distanceMeters / 111320.0;
  const degLon = distanceMeters / (111320.0 * Math.max(0.001, Math.cos(latRad)));

  const centroid = polygonCentroid(polygon);
  const expandedRing: Point[] = [];

  for (const pt of extRing) {
    const dx = pt.x - centroid.x;
    const dy = pt.y - centroid.y;
    const dist = Math.sqrt(dx * dx + dy * dy);
    if (dist < 1e-9) {
      expandedRing.push(pt);
    } else {
      const factor = (dist + Math.max(degLon, degLat)) / dist;
      expandedRing.push(makePoint(centroid.x + dx * factor, centroid.y + dy * factor));
    }
  }

  return makePolygonFromRing(expandedRing);
}
