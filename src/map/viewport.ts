import { ProjectionEngine } from '../core/crs';
import { type BoundingBox, type Point, isBBoxEmpty, makePoint } from '../core/geometry';

export class MapViewport {
  public centerLon: number;
  public centerLat: number;
  public zoom: number;
  public screenWidth: number;
  public screenHeight: number;

  constructor(
    centerLon = -71.9675,
    centerLat = -13.532,
    zoom = 13.0,
    screenWidth = 1080,
    screenHeight = 720
  ) {
    this.centerLon = centerLon;
    this.centerLat = centerLat;
    this.zoom = zoom;
    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;
  }

  get scaleDenominator(): number {
    const latRad = (this.centerLat * Math.PI) / 180.0;
    const resolutionMetersPerPixel =
      (156543.03392 * Math.cos(latRad)) / Math.pow(2.0, this.zoom);
    const scale = resolutionMetersPerPixel / 0.000264583;
    return Math.max(100, Math.round(scale));
  }

  setScale(targetScale: number): void {
    if (targetScale <= 0) return;
    const latRad = (this.centerLat * Math.PI) / 180.0;
    const numerator = 156543.03392 * Math.cos(latRad);
    const denominator = targetScale * 0.000264583;
    if (denominator > 0.0) {
      const calculatedZoom = Math.log2(numerator / denominator);
      this.zoom = Math.max(2.0, Math.min(21.0, calculatedZoom));
    }
  }

  get metersPerPixel(): number {
    const worldSize = 256.0 * Math.pow(2.0, this.zoom);
    return 40075016.686 / worldSize;
  }

  get groundMetersPerPixel(): number {
    const latRad = (this.centerLat * Math.PI) / 180.0;
    return Math.max(1e-6, this.metersPerPixel * Math.cos(latRad));
  }

  geoToScreen(lon: number, lat: number): [number, number] {
    const mercCenter = ProjectionEngine.wgs84ToWebMercator(this.centerLat, this.centerLon);
    const mercPoint = ProjectionEngine.wgs84ToWebMercator(lat, lon);

    const mpp = this.metersPerPixel;
    const dx = (mercPoint.x - mercCenter.x) / mpp;
    const dy = (mercCenter.y - mercPoint.y) / mpp;

    return [this.screenWidth / 2 + dx, this.screenHeight / 2 + dy];
  }

  screenToGeo(screenX: number, screenY: number): Point {
    const mercCenter = ProjectionEngine.wgs84ToWebMercator(this.centerLat, this.centerLon);
    const mpp = this.metersPerPixel;

    const dx = (screenX - this.screenWidth / 2) * mpp;
    const dy = (this.screenHeight / 2 - screenY) * mpp;

    const pt = ProjectionEngine.webMercatorToWgs84(mercCenter.x + dx, mercCenter.y + dy);
    return makePoint(pt.x, pt.y);
  }

  zoomToExtent(box: BoundingBox, paddingFactor = 1.2): void {
    if (isBBoxEmpty(box)) return;
    this.centerLon = (box.minX + box.maxX) / 2.0;
    this.centerLat = (box.minY + box.maxY) / 2.0;

    const mercMin = ProjectionEngine.wgs84ToWebMercator(box.minY, box.minX);
    const mercMax = ProjectionEngine.wgs84ToWebMercator(box.maxY, box.maxX);
    const spanX = Math.abs(mercMax.x - mercMin.x) * paddingFactor;
    const spanY = Math.abs(mercMax.y - mercMin.y) * paddingFactor;

    if (spanX < 1e-6 && spanY < 1e-6) {
      this.zoom = 17.0;
      return;
    }

    const metersPerPixelX = spanX / Math.max(1, this.screenWidth);
    const metersPerPixelY = spanY / Math.max(1, this.screenHeight);
    const maxMetersPerPixel = Math.max(metersPerPixelX, metersPerPixelY);

    if (maxMetersPerPixel > 0.0) {
      const calculatedZoom = Math.log2(40075016.686 / (256.0 * maxMetersPerPixel));
      this.zoom = Math.max(2.0, Math.min(20.0, calculatedZoom));
    } else {
      this.zoom = 17.0;
    }
  }

  panByPixels(dx: number, dy: number): void {
    const newCenter = this.screenToGeo(this.screenWidth / 2 - dx, this.screenHeight / 2 - dy);
    this.centerLon = Math.max(-180, Math.min(180, newCenter.x));
    this.centerLat = Math.max(-85, Math.min(85, newCenter.y));
  }

  zoomBy(delta: number): void {
    this.zoom = Math.max(2.0, Math.min(21.0, this.zoom + delta));
  }

  zoomAround(focalX: number, focalY: number, delta: number): void {
    const focalGeo = this.screenToGeo(focalX, focalY);
    const focalMerc = ProjectionEngine.wgs84ToWebMercator(focalGeo.y, focalGeo.x);

    this.zoom = Math.max(2.0, Math.min(21.0, this.zoom + delta));

    const newMpp = this.metersPerPixel;
    const dx = (focalX - this.screenWidth / 2) * newMpp;
    const dy = (this.screenHeight / 2 - focalY) * newMpp;

    const newMercCenterX = focalMerc.x - dx;
    const newMercCenterY = focalMerc.y - dy;

    const newCenter = ProjectionEngine.webMercatorToWgs84(newMercCenterX, newMercCenterY);
    this.centerLon = Math.max(-180, Math.min(180, newCenter.x));
    this.centerLat = Math.max(-85, Math.min(85, newCenter.y));
  }
}
