export enum GeometryType {
  POINT = 'POINT',
  POLYLINE = 'POLYLINE',
  POLYGON = 'POLYGON',
  MULTI_POINT = 'MULTI_POINT',
  MULTI_POLYLINE = 'MULTI_POLYLINE',
  MULTI_POLYGON = 'MULTI_POLYGON',
  UNKNOWN = 'UNKNOWN',
}

export interface BoundingBox {
  minX: number;
  minY: number;
  maxX: number;
  maxY: number;
}

export const EMPTY_BBOX: BoundingBox = {
  minX: Number.MAX_VALUE,
  minY: Number.MAX_VALUE,
  maxX: -Number.MAX_VALUE,
  maxY: -Number.MAX_VALUE,
};

export function isBBoxEmpty(box: BoundingBox): boolean {
  return box.minX > box.maxX || box.minY > box.maxY;
}

export function bboxWidth(box: BoundingBox): number {
  return box.maxX - box.minX;
}

export function bboxHeight(box: BoundingBox): number {
  return box.maxY - box.minY;
}

export function bboxCenterX(box: BoundingBox): number {
  return (box.minX + box.maxX) / 2.0;
}

export function bboxCenterY(box: BoundingBox): number {
  return (box.minY + box.maxY) / 2.0;
}

export function bboxContains(box: BoundingBox, x: number, y: number): boolean {
  return x >= box.minX && x <= box.maxX && y >= box.minY && y <= box.maxY;
}

export function bboxIntersects(a: BoundingBox, b: BoundingBox): boolean {
  return a.minX <= b.maxX && a.maxX >= b.minX && a.minY <= b.maxY && a.maxY >= b.minY;
}

export function bboxExpandPoint(box: BoundingBox, p: Point): BoundingBox {
  if (isBBoxEmpty(box)) {
    return { minX: p.x, minY: p.y, maxX: p.x, maxY: p.y };
  }
  return {
    minX: Math.min(box.minX, p.x),
    minY: Math.min(box.minY, p.y),
    maxX: Math.max(box.maxX, p.x),
    maxY: Math.max(box.maxY, p.y),
  };
}

export function bboxExpandBox(a: BoundingBox, b: BoundingBox): BoundingBox {
  if (isBBoxEmpty(b)) return a;
  if (isBBoxEmpty(a)) return b;
  return {
    minX: Math.min(a.minX, b.minX),
    minY: Math.min(a.minY, b.minY),
    maxX: Math.max(a.maxX, b.maxX),
    maxY: Math.max(a.maxY, b.maxY),
  };
}

export function bboxFromPoints(points: ReadonlyArray<Point>): BoundingBox {
  if (points.length === 0) return EMPTY_BBOX;
  let minX = Number.MAX_VALUE;
  let minY = Number.MAX_VALUE;
  let maxX = -Number.MAX_VALUE;
  let maxY = -Number.MAX_VALUE;
  for (const p of points) {
    if (p.x < minX) minX = p.x;
    if (p.y < minY) minY = p.y;
    if (p.x > maxX) maxX = p.x;
    if (p.y > maxY) maxY = p.y;
  }
  return { minX, minY, maxX, maxY };
}

export interface Point {
  geometryType: GeometryType.POINT;
  x: number;
  y: number;
  z?: number;
  m?: number;
}

export interface Polyline {
  geometryType: GeometryType.POLYLINE;
  points: Point[];
}

export interface Polygon {
  geometryType: GeometryType.POLYGON;
  rings: Point[][];
}

export interface MultiPoint {
  geometryType: GeometryType.MULTI_POINT;
  points: Point[];
}

export interface MultiPolyline {
  geometryType: GeometryType.MULTI_POLYLINE;
  polylines: Polyline[];
}

export interface MultiPolygon {
  geometryType: GeometryType.MULTI_POLYGON;
  polygons: Polygon[];
}

export type Geometry =
  | Point
  | Polyline
  | Polygon
  | MultiPoint
  | MultiPolyline
  | MultiPolygon;

export function makePoint(x: number, y: number, z?: number, m?: number): Point {
  return { geometryType: GeometryType.POINT, x, y, z, m };
}

export function makePolyline(points: Point[]): Polyline {
  return { geometryType: GeometryType.POLYLINE, points };
}

export function makePolygon(rings: Point[][]): Polygon {
  return { geometryType: GeometryType.POLYGON, rings };
}

export function makePolygonFromRing(points: Point[]): Polygon {
  return { geometryType: GeometryType.POLYGON, rings: [points] };
}

export function makeMultiPoint(points: Point[]): MultiPoint {
  return { geometryType: GeometryType.MULTI_POINT, points };
}

export function makeMultiPolyline(polylines: Polyline[]): MultiPolyline {
  return { geometryType: GeometryType.MULTI_POLYLINE, polylines };
}

export function makeMultiPolygon(polygons: Polygon[]): MultiPolygon {
  return { geometryType: GeometryType.MULTI_POLYGON, polygons };
}

export function pointDistanceTo(a: Point, b: Point): number {
  const dx = b.x - a.x;
  const dy = b.y - a.y;
  return Math.sqrt(dx * dx + dy * dy);
}

export function pointsEqual(a: Point, b: Point): boolean {
  return a.x === b.x && a.y === b.y;
}

export function getGeometryBoundingBox(geom: Geometry): BoundingBox {
  switch (geom.geometryType) {
    case GeometryType.POINT:
      return { minX: geom.x, minY: geom.y, maxX: geom.x, maxY: geom.y };
    case GeometryType.POLYLINE:
    case GeometryType.MULTI_POINT:
      return bboxFromPoints(geom.points);
    case GeometryType.POLYGON:
      return bboxFromPoints(geom.rings.flat());
    case GeometryType.MULTI_POLYLINE:
      return geom.polylines.reduce(
        (acc, line) => bboxExpandBox(acc, getGeometryBoundingBox(line)),
        EMPTY_BBOX
      );
    case GeometryType.MULTI_POLYGON:
      return geom.polygons.reduce(
        (acc, poly) => bboxExpandBox(acc, getGeometryBoundingBox(poly)),
        EMPTY_BBOX
      );
  }
}

export function polylineLengthPlanar(polyline: Polyline): number {
  const pts = polyline.points;
  if (pts.length < 2) return 0.0;
  let total = 0.0;
  for (let i = 0; i < pts.length - 1; i++) {
    total += pointDistanceTo(pts[i], pts[i + 1]);
  }
  return total;
}

function ringArea(points: Point[]): number {
  if (points.length < 3) return 0.0;
  let area = 0.0;
  const n = points.length;
  for (let i = 0; i < n; i++) {
    const j = (i + 1) % n;
    area += points[i].x * points[j].y;
    area -= points[j].x * points[i].y;
  }
  return area * 0.5;
}

function isPointInRing(p: Point, ring: Point[]): boolean {
  let inside = false;
  const n = ring.length;
  let j = n - 1;
  for (let i = 0; i < n; i++) {
    const pi = ring[i];
    const pj = ring[j];
    if (
      pi.y > p.y !== pj.y > p.y &&
      p.x < ((pj.x - pi.x) * (p.y - pi.y)) / (pj.y - pi.y) + pi.x
    ) {
      inside = !inside;
    }
    j = i;
  }
  return inside;
}

export function polygonAreaPlanar(polygon: Polygon): number {
  if (polygon.rings.length === 0) return 0.0;
  const outerArea = Math.abs(ringArea(polygon.rings[0]));
  let holesArea = 0.0;
  for (let i = 1; i < polygon.rings.length; i++) {
    holesArea += Math.abs(ringArea(polygon.rings[i]));
  }
  return Math.max(0.0, outerArea - holesArea);
}

export function polygonContainsPoint(polygon: Polygon, p: Point): boolean {
  const box = getGeometryBoundingBox(polygon);
  if (!bboxContains(box, p.x, p.y)) return false;
  const extRing = polygon.rings[0] ?? [];
  if (!isPointInRing(p, extRing)) return false;
  for (let i = 1; i < polygon.rings.length; i++) {
    if (isPointInRing(p, polygon.rings[i])) return false;
  }
  return true;
}

export function polygonCentroid(polygon: Polygon): Point {
  const ring = polygon.rings[0] ?? [];
  if (ring.length === 0) return makePoint(0, 0);
  let signedArea = 0.0;
  let cx = 0.0;
  let cy = 0.0;
  const n = ring.length;
  for (let i = 0; i < n; i++) {
    const p0 = ring[i];
    const p1 = ring[(i + 1) % n];
    const a = p0.x * p1.y - p1.x * p0.y;
    signedArea += a;
    cx += (p0.x + p1.x) * a;
    cy += (p0.y + p1.y) * a;
  }
  signedArea *= 0.5;
  if (Math.abs(signedArea) < 1e-9) {
    const avgX = ring.reduce((sum, p) => sum + p.x, 0) / ring.length;
    const avgY = ring.reduce((sum, p) => sum + p.y, 0) / ring.length;
    return makePoint(avgX, avgY);
  }
  cx /= 6.0 * signedArea;
  cy /= 6.0 * signedArea;
  return makePoint(cx, cy);
}
