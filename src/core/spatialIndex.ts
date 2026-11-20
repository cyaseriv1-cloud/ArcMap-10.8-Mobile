import {
  type BoundingBox,
  GeometryType,
  type Point,
  bboxContains,
  bboxIntersects,
  getGeometryBoundingBox,
  isBBoxEmpty,
  pointDistanceTo,
  polygonContainsPoint,
} from './geometry';
import type { Feature } from './model';

export class SpatialIndex {
  private readonly cellSizeDegrees: number;
  private readonly grid = new Map<string, Feature[]>();

  constructor(cellSizeDegrees = 0.05) {
    this.cellSizeDegrees = cellSizeDegrees;
  }

  clear(): void {
    this.grid.clear();
  }

  build(features: ReadonlyArray<Feature>): void {
    this.clear();
    for (const f of features) {
      this.insert(f);
    }
  }

  private cellCoord(v: number): number {
    return Math.floor(v / this.cellSizeDegrees);
  }

  insert(feature: Feature): void {
    const geom = feature.geometry;
    if (!geom) return;
    const box = getGeometryBoundingBox(geom);
    if (isBBoxEmpty(box)) return;

    const minCellX = this.cellCoord(box.minX);
    const maxCellX = this.cellCoord(box.maxX);
    const minCellY = this.cellCoord(box.minY);
    const maxCellY = this.cellCoord(box.maxY);

    for (let cx = minCellX; cx <= maxCellX; cx++) {
      for (let cy = minCellY; cy <= maxCellY; cy++) {
        const key = `${cx},${cy}`;
        let cell = this.grid.get(key);
        if (!cell) {
          cell = [];
          this.grid.set(key, cell);
        }
        cell.push(feature);
      }
    }
  }

  query(box: BoundingBox): Feature[] {
    if (isBBoxEmpty(box)) return [];

    const minCellX = this.cellCoord(box.minX);
    const maxCellX = this.cellCoord(box.maxX);
    const minCellY = this.cellCoord(box.minY);
    const maxCellY = this.cellCoord(box.maxY);

    const seen = new Set<Feature>();
    const candidates: Feature[] = [];

    for (let cx = minCellX; cx <= maxCellX; cx++) {
      for (let cy = minCellY; cy <= maxCellY; cy++) {
        const list = this.grid.get(`${cx},${cy}`);
        if (!list) continue;
        for (const f of list) {
          if (seen.has(f)) continue;
          const geom = f.geometry;
          if (geom && bboxIntersects(getGeometryBoundingBox(geom), box)) {
            seen.add(f);
            candidates.push(f);
          }
        }
      }
    }

    return candidates;
  }

  hitTest(touchPoint: Point, toleranceDegrees: number): Feature | null {
    const searchBox: BoundingBox = {
      minX: touchPoint.x - toleranceDegrees,
      minY: touchPoint.y - toleranceDegrees,
      maxX: touchPoint.x + toleranceDegrees,
      maxY: touchPoint.y + toleranceDegrees,
    };
    const candidates = this.query(searchBox);

    let bestPointFeature: Feature | null = null;
    let bestPointDist = Number.MAX_VALUE;

    let bestLineFeature: Feature | null = null;
    let bestLineDist = Number.MAX_VALUE;

    let foundPolygonFeature: Feature | null = null;
    let bestPolygonArea = Number.MAX_VALUE;

    for (const f of candidates) {
      const geom = f.geometry;
      if (!geom) continue;

      switch (geom.geometryType) {
        case GeometryType.POINT: {
          const d = pointDistanceTo(geom, touchPoint);
          if (d <= toleranceDegrees && d < bestPointDist) {
            bestPointDist = d;
            bestPointFeature = f;
          }
          break;
        }
        case GeometryType.MULTI_POINT: {
          for (const pt of geom.points) {
            const d = pointDistanceTo(pt, touchPoint);
            if (d <= toleranceDegrees && d < bestPointDist) {
              bestPointDist = d;
              bestPointFeature = f;
            }
          }
          break;
        }
        case GeometryType.POLYLINE: {
          const d = this.distancePointToPolyline(touchPoint, geom.points);
          if (d <= toleranceDegrees && d < bestLineDist) {
            bestLineDist = d;
            bestLineFeature = f;
          }
          break;
        }
        case GeometryType.MULTI_POLYLINE: {
          for (const line of geom.polylines) {
            const d = this.distancePointToPolyline(touchPoint, line.points);
            if (d <= toleranceDegrees && d < bestLineDist) {
              bestLineDist = d;
              bestLineFeature = f;
            }
          }
          break;
        }
        case GeometryType.POLYGON: {
          if (polygonContainsPoint(geom, touchPoint)) {
            const bb = getGeometryBoundingBox(geom);
            const area = (bb.maxX - bb.minX) * (bb.maxY - bb.minY);
            if (area < bestPolygonArea) {
              bestPolygonArea = area;
              foundPolygonFeature = f;
            }
          }
          break;
        }
        case GeometryType.MULTI_POLYGON: {
          const hitPoly = geom.polygons.find((p) => polygonContainsPoint(p, touchPoint));
          if (hitPoly) {
            const bb = getGeometryBoundingBox(hitPoly);
            const area = (bb.maxX - bb.minX) * (bb.maxY - bb.minY);
            if (area < bestPolygonArea) {
              bestPolygonArea = area;
              foundPolygonFeature = f;
            }
          }
          break;
        }
        default: {
          const bb = getGeometryBoundingBox(geom);
          if (bboxContains(bb, touchPoint.x, touchPoint.y)) {
            const area = (bb.maxX - bb.minX) * (bb.maxY - bb.minY);
            if (area < bestPolygonArea) {
              bestPolygonArea = area;
              foundPolygonFeature = f;
            }
          }
          break;
        }
      }
    }

    return bestPointFeature ?? bestLineFeature ?? foundPolygonFeature;
  }

  private distancePointToPolyline(p: Point, points: Point[]): number {
    if (points.length < 2) return Number.MAX_VALUE;
    let minDist = Number.MAX_VALUE;
    for (let i = 0; i < points.length - 1; i++) {
      const d = this.distancePointToSegment(p, points[i], points[i + 1]);
      if (d < minDist) minDist = d;
    }
    return minDist;
  }

  private distancePointToSegment(p: Point, a: Point, b: Point): number {
    const dx = b.x - a.x;
    const dy = b.y - a.y;
    const lenSq = dx * dx + dy * dy;
    if (lenSq < 1e-12) return pointDistanceTo(p, a);

    const t = Math.max(0.0, Math.min(1.0, ((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq));
    const projX = a.x + t * dx;
    const projY = a.y + t * dy;
    const distX = p.x - projX;
    const distY = p.y - projY;
    return Math.sqrt(distX * distX + distY * distY);
  }
}
