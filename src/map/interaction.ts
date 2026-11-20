import { ProjectionEngine } from '../core/crs';
import type { Point } from '../core/geometry';

export enum MapToolMode {
  PAN = 'PAN',
  IDENTIFY = 'IDENTIFY',
  SELECT = 'SELECT',
  MEASURE_DISTANCE = 'MEASURE_DISTANCE',
  MEASURE_AREA = 'MEASURE_AREA',
  DIGITIZE_POINT = 'DIGITIZE_POINT',
  DIGITIZE_LINE = 'DIGITIZE_LINE',
  DIGITIZE_POLYGON = 'DIGITIZE_POLYGON',
}

export class MeasureSession {
  public points: Point[] = [];
  public isArea = false;

  constructor(points: Point[] = [], isArea = false) {
    this.points = points;
    this.isArea = isArea;
  }

  addPoint(p: Point): MeasureSession {
    return new MeasureSession([...this.points, p], this.isArea);
  }

  removeLastPoint(): MeasureSession {
    if (this.points.length === 0) return this;
    return new MeasureSession(this.points.slice(0, -1), this.isArea);
  }

  clear(): MeasureSession {
    return new MeasureSession([], this.isArea);
  }

  withAreaMode(isArea: boolean): MeasureSession {
    return new MeasureSession([...this.points], isArea);
  }

  get totalDistanceMeters(): number {
    if (this.points.length < 2) return 0.0;
    let dist = 0.0;
    for (let i = 0; i < this.points.length - 1; i++) {
      dist += ProjectionEngine.geodesicDistanceMeters(this.points[i], this.points[i + 1]);
    }
    if (this.isArea && this.points.length >= 3) {
      dist += ProjectionEngine.geodesicDistanceMeters(
        this.points[this.points.length - 1],
        this.points[0]
      );
    }
    return dist;
  }

  get areaSquareMeters(): number {
    if (this.points.length < 3) return 0.0;
    return ProjectionEngine.geodesicPolygonAreaSquareMeters(this.points);
  }

  formatDistance(): string {
    const d = this.totalDistanceMeters;
    return d >= 1000.0 ? `${(d / 1000.0).toFixed(3)} km` : `${d.toFixed(1)} m`;
  }

  formatArea(): string {
    const a = this.areaSquareMeters;
    if (a >= 1000000.0) return `${(a / 1000000.0).toFixed(3)} km²`;
    if (a >= 10000.0) return `${(a / 10000.0).toFixed(2)} ha`;
    return `${a.toFixed(1)} m²`;
  }
}

export class DigitizeSession {
  public points: Point[] = [];
  public targetLayerId: string | null = null;
  public isPolygon = false;

  constructor(points: Point[] = [], targetLayerId: string | null = null, isPolygon = false) {
    this.points = points;
    this.targetLayerId = targetLayerId;
    this.isPolygon = isPolygon;
  }

  addPoint(p: Point): DigitizeSession {
    return new DigitizeSession([...this.points, p], this.targetLayerId, this.isPolygon);
  }

  removeLast(): DigitizeSession {
    if (this.points.length === 0) return this;
    return new DigitizeSession(this.points.slice(0, -1), this.targetLayerId, this.isPolygon);
  }

  clear(): DigitizeSession {
    return new DigitizeSession([], null, this.isPolygon);
  }
}
