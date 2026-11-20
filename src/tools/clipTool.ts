import {
  type BoundingBox,
  type Geometry,
  GeometryType,
  type Point,
  type Polygon,
  type Polyline,
  bboxContains,
  bboxIntersects,
  getGeometryBoundingBox,
  makeMultiPoint,
  makeMultiPolygon,
  makeMultiPolyline,
  makePoint,
  makePolygonFromRing,
  makePolyline,
  pointsEqual,
} from '../core/geometry';
import { type Feature, type VectorLayer, createVectorLayer } from '../core/model';
import { createSimpleSymbology, makeColor } from '../core/symbology';

const INSIDE = 0;
const LEFT = 1;
const RIGHT = 2;
const BOTTOM = 4;
const TOP = 8;

export const ClipTool = {
  execute(
    inputLayer: VectorLayer,
    clipBox: BoundingBox,
    outputLayerName = `${inputLayer.name}_clip`
  ): VectorLayer {
    const outFeatures: Feature[] = [];
    let nextId = 1;

    for (const feat of inputLayer.features) {
      const geom = feat.geometry;
      if (!geom) continue;
      const clippedGeom = clipGeometry(geom, clipBox);
      if (!clippedGeom) continue;

      outFeatures.push({
        id: nextId++,
        geometry: clippedGeom,
        attributes: { ...feat.attributes },
      });
    }

    return createVectorLayer({
      name: outputLayerName,
      geometryType: inputLayer.geometryType,
      symbology: createSimpleSymbology({
        fillColor: makeColor(76, 175, 80, 0.5),
        strokeColor: makeColor(46, 125, 50),
        strokeWidthDp: 2.0,
      }),
      fields: inputLayer.fields.map((f) => ({ ...f })),
      features: outFeatures,
    });
  },
};

function clipGeometry(geom: Geometry, box: BoundingBox): Geometry | null {
  if (!bboxIntersects(getGeometryBoundingBox(geom), box)) return null;

  switch (geom.geometryType) {
    case GeometryType.POINT:
      return bboxContains(box, geom.x, geom.y) ? geom : null;
    case GeometryType.POLYLINE:
      return clipPolyline(geom, box);
    case GeometryType.POLYGON:
      return clipPolygon(geom, box);
    case GeometryType.MULTI_POINT: {
      const inside = geom.points.filter((pt) => bboxContains(box, pt.x, pt.y));
      return inside.length > 0 ? makeMultiPoint(inside) : null;
    }
    case GeometryType.MULTI_POLYLINE: {
      const lines = geom.polylines
        .map((l) => clipPolyline(l, box))
        .filter((l): l is Polyline => l !== null);
      return lines.length > 0 ? makeMultiPolyline(lines) : null;
    }
    case GeometryType.MULTI_POLYGON: {
      const polys = geom.polygons
        .map((p) => clipPolygon(p, box))
        .filter((p): p is Polygon => p !== null);
      return polys.length > 0 ? makeMultiPolygon(polys) : null;
    }
    default:
      return null;
  }
}

function computeOutCode(x: number, y: number, box: BoundingBox): number {
  let code = INSIDE;
  if (x < box.minX) code |= LEFT;
  else if (x > box.maxX) code |= RIGHT;
  if (y < box.minY) code |= BOTTOM;
  else if (y > box.maxY) code |= TOP;
  return code;
}

function clipPolyline(line: Polyline, box: BoundingBox): Polyline | null {
  const pts = line.points;
  if (pts.length < 2) return null;

  const resultPts: Point[] = [];

  for (let i = 0; i < pts.length - 1; i++) {
    let x0 = pts[i].x;
    let y0 = pts[i].y;
    let x1 = pts[i + 1].x;
    let y1 = pts[i + 1].y;

    let code0 = computeOutCode(x0, y0, box);
    let code1 = computeOutCode(x1, y1, box);
    let accept = false;

    while (true) {
      if ((code0 | code1) === 0) {
        accept = true;
        break;
      } else if ((code0 & code1) !== 0) {
        break;
      } else {
        let x = 0.0;
        let y = 0.0;
        const codeOut = code0 !== 0 ? code0 : code1;

        if ((codeOut & TOP) !== 0) {
          x = x0 + ((x1 - x0) * (box.maxY - y0)) / (y1 - y0);
          y = box.maxY;
        } else if ((codeOut & BOTTOM) !== 0) {
          x = x0 + ((x1 - x0) * (box.minY - y0)) / (y1 - y0);
          y = box.minY;
        } else if ((codeOut & RIGHT) !== 0) {
          y = y0 + ((y1 - y0) * (box.maxX - x0)) / (x1 - x0);
          x = box.maxX;
        } else if ((codeOut & LEFT) !== 0) {
          y = y0 + ((y1 - y0) * (box.minX - x0)) / (x1 - x0);
          x = box.minX;
        }

        if (codeOut === code0) {
          x0 = x;
          y0 = y;
          code0 = computeOutCode(x0, y0, box);
        } else {
          x1 = x;
          y1 = y;
          code1 = computeOutCode(x1, y1, box);
        }
      }
    }

    if (accept) {
      const p0 = makePoint(x0, y0);
      const p1 = makePoint(x1, y1);
      if (resultPts.length === 0 || !pointsEqual(resultPts[resultPts.length - 1], p0)) {
        resultPts.push(p0);
      }
      resultPts.push(p1);
    }
  }

  return resultPts.length >= 2 ? makePolyline(resultPts) : null;
}

function clipPolygon(polygon: Polygon, box: BoundingBox): Polygon | null {
  const extRing = polygon.rings[0] ?? [];
  if (extRing.length < 3) return null;

  let outputList = extRing;

  outputList = clipEdge(outputList, (p) => p.x >= box.minX);
  if (outputList.length < 3) return null;

  outputList = clipEdge(outputList, (p) => p.x <= box.maxX);
  if (outputList.length < 3) return null;

  outputList = clipEdge(outputList, (p) => p.y >= box.minY);
  if (outputList.length < 3) return null;

  outputList = clipEdge(outputList, (p) => p.y <= box.maxY);
  if (outputList.length < 3) return null;

  const closed = !pointsEqual(outputList[0], outputList[outputList.length - 1])
    ? [...outputList, outputList[0]]
    : outputList;

  return makePolygonFromRing(closed);
}

function clipEdge(ring: Point[], inside: (p: Point) => boolean): Point[] {
  const output: Point[] = [];
  if (ring.length === 0) return output;

  let prev = ring[ring.length - 1];
  for (const curr of ring) {
    const currInside = inside(curr);
    const prevInside = inside(prev);

    if (currInside) {
      if (!prevInside) {
        output.push(intersectEdge(prev, curr, inside));
      }
      output.push(curr);
    } else if (prevInside) {
      output.push(intersectEdge(prev, curr, inside));
    }
    prev = curr;
  }
  return output;
}

function intersectEdge(p1: Point, p2: Point, inside: (p: Point) => boolean): Point {
  let low = 0.0;
  let high = 1.0;
  const p1In = inside(p1);

  for (let i = 0; i < 16; i++) {
    const mid = (low + high) * 0.5;
    const midPt = makePoint(p1.x + mid * (p2.x - p1.x), p1.y + mid * (p2.y - p1.y));
    if (inside(midPt) === p1In) {
      low = mid;
    } else {
      high = mid;
    }
  }
  const t = (low + high) * 0.5;
  return makePoint(p1.x + t * (p2.x - p1.x), p1.y + t * (p2.y - p1.y));
}
