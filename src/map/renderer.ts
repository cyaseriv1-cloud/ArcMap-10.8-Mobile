import { ProjectionEngine } from '../core/crs';
import {
  type Geometry,
  GeometryType,
  type Point,
  type Polygon,
  type Polyline,
  getGeometryBoundingBox,
  polygonCentroid,
} from '../core/geometry';
import type { VectorLayer } from '../core/model';
import {
  type LayerSymbology,
  MarkerShape,
  colorToCss,
  getSymbologyFillColor,
  getSymbologyStrokeColor,
  getSymbologyStrokeWidth,
} from '../core/symbology';
import { type BasemapDefinition, BasemapId } from './basemap';
import type { DigitizeSession, MeasureSession } from './interaction';
import type { TileManager } from './tileManager';
import type { MapViewport } from './viewport';

export interface RenderOptions {
  ctx: CanvasRenderingContext2D;
  viewport: MapViewport;
  layers: VectorLayer[];
  measureSession?: MeasureSession | null;
  digitizeSession?: DigitizeSession | null;
  isDarkMode?: boolean;
  gpsLocation?: Point | null;
  gpsAccuracy?: number | null;
  basemap?: BasemapDefinition;
  tileManager?: TileManager | null;
}

export class MapCanvasRenderer {
  render(opts: RenderOptions): void {
    const {
      ctx,
      viewport,
      layers,
      measureSession = null,
      digitizeSession = null,
      isDarkMode = false,
      gpsLocation = null,
      gpsAccuracy = null,
      basemap,
      tileManager = null,
    } = opts;

    const w = viewport.screenWidth;
    const h = viewport.screenHeight;
    if (w <= 0 || h <= 0) return;

    ctx.save();

    // Background paper/slate
    ctx.fillStyle = isDarkMode ? 'rgb(22, 26, 33)' : 'rgb(246, 247, 248)';
    ctx.fillRect(0, 0, w, h);

    const isSatellite = basemap?.id === BasemapId.ESRI_IMAGERY;

    // 1. Basemap tiles
    if (basemap && basemap.id !== BasemapId.NONE && tileManager) {
      this.drawBasemap(ctx, viewport, basemap, tileManager, isDarkMode);
    }

    // 2. Cartographic coordinate grid (graticule)
    this.drawGraticule(ctx, viewport, isDarkMode, isSatellite);

    // 3. Vector layers (reverse order: index 0 drawn last / on top)
    for (let i = layers.length - 1; i >= 0; i--) {
      const layer = layers[i];
      if (!layer.isVisible) continue;
      this.drawLayer(ctx, viewport, layer, isDarkMode);
    }

    // 4. GPS Location Indicator
    if (gpsLocation) {
      this.drawGpsLocation(ctx, viewport, gpsLocation, gpsAccuracy);
    }

    // 5. Measure Overlay
    if (measureSession && measureSession.points.length > 0) {
      this.drawMeasureOverlay(ctx, viewport, measureSession);
    }

    // 6. Digitize Overlay
    if (digitizeSession && digitizeSession.points.length > 0) {
      this.drawDigitizeOverlay(ctx, viewport, digitizeSession);
    }

    // 7. Scale bar & North arrow
    this.drawScaleBar(ctx, viewport, isDarkMode, isSatellite);
    this.drawNorthArrow(ctx, viewport, isDarkMode, isSatellite);

    ctx.restore();
  }

  private drawBasemap(
    ctx: CanvasRenderingContext2D,
    viewport: MapViewport,
    basemap: BasemapDefinition,
    tileManager: TileManager,
    isDarkMode: boolean
  ): void {
    const z = Math.max(
      basemap.minZoom,
      Math.min(basemap.maxZoom, Math.floor(viewport.zoom))
    );
    const nTiles = 1 << z;
    const worldSizeMeters = 40075016.686;
    const halfWorld = 20037508.34;
    const tileMeters = worldSizeMeters / nTiles;

    const screenW = viewport.screenWidth;
    const screenH = viewport.screenHeight;
    const mpp = viewport.metersPerPixel;
    if (mpp <= 0) return;

    const mercCenter = ProjectionEngine.wgs84ToWebMercator(
      viewport.centerLat,
      viewport.centerLon
    );

    const minMercX = mercCenter.x + (0 - screenW / 2) * mpp;
    const maxMercX = mercCenter.x + (screenW - screenW / 2) * mpp;
    const maxMercY = mercCenter.y - (0 - screenH / 2) * mpp;
    const minMercY = mercCenter.y - (screenH - screenH / 2) * mpp;

    const minTileX = Math.max(0, Math.min(nTiles - 1, Math.floor((minMercX + halfWorld) / tileMeters)));
    const maxTileX = Math.max(0, Math.min(nTiles - 1, Math.floor((maxMercX + halfWorld) / tileMeters)));
    const minTileY = Math.max(0, Math.min(nTiles - 1, Math.floor((halfWorld - maxMercY) / tileMeters)));
    const maxTileY = Math.max(0, Math.min(nTiles - 1, Math.floor((halfWorld - minMercY) / tileMeters)));

    for (let tx = minTileX; tx <= maxTileX; tx++) {
      for (let ty = minTileY; ty <= maxTileY; ty++) {
        const tileLeftMercX = -halfWorld + tx * tileMeters;
        const tileRightMercX = -halfWorld + (tx + 1) * tileMeters;
        const tileTopMercY = halfWorld - ty * tileMeters;
        const tileBottomMercY = halfWorld - (ty + 1) * tileMeters;

        const left = screenW / 2 + (tileLeftMercX - mercCenter.x) / mpp;
        const right = screenW / 2 + (tileRightMercX - mercCenter.x) / mpp;
        const top = screenH / 2 + (mercCenter.y - tileTopMercY) / mpp;
        const bottom = screenH / 2 + (mercCenter.y - tileBottomMercY) / mpp;

        const img = tileManager.getTile(basemap, z, tx, ty, false);
        if (img && img.complete && img.naturalWidth > 0) {
          // Slight 0.6px overlap avoids hairline seams between tiles at fractional zoom
          ctx.drawImage(img, left, top, right - left + 0.6, bottom - top + 0.6);
        }
        if (basemap.overlayUrlTemplate) {
          const ovImg = tileManager.getTile(basemap, z, tx, ty, true);
          if (ovImg && ovImg.complete && ovImg.naturalWidth > 0) {
            ctx.drawImage(ovImg, left, top, right - left + 0.6, bottom - top + 0.6);
          }
        }
      }
    }

    if (basemap.attribution) {
      ctx.save();
      ctx.font = 'bold 9.5px sans-serif';
      const metrics = ctx.measureText(basemap.attribution);
      const padX = 5;
      const boxW = Math.min(metrics.width + padX * 2, Math.max(80, screenW - 92));
      const rectLeft = 18;
      const rectTop = screenH - 20;
      ctx.fillStyle = isDarkMode ? 'rgba(22, 26, 33, 0.8)' : 'rgba(255, 255, 255, 0.82)';
      ctx.fillRect(rectLeft, rectTop, boxW, 15);
      ctx.fillStyle = isDarkMode ? 'rgb(200, 210, 225)' : 'rgb(50, 50, 50)';
      ctx.fillText(basemap.attribution, rectLeft + padX, rectTop + 11, boxW - padX * 2);
      ctx.restore();
    }
  }

  private drawGpsLocation(
    ctx: CanvasRenderingContext2D,
    viewport: MapViewport,
    loc: Point,
    accuracy: number | null
  ): void {
    const [sx, sy] = viewport.geoToScreen(loc.x, loc.y);
    if (sx < -200 || sx > viewport.screenWidth + 200 || sy < -200 || sy > viewport.screenHeight + 200) {
      return;
    }

    const latRad = (loc.y * Math.PI) / 180.0;
    const groundMpp = Math.max(1e-6, viewport.metersPerPixel * Math.cos(latRad));
    const radiusPx =
      accuracy !== null && accuracy > 0
        ? Math.max(16, Math.min(200, accuracy / groundMpp))
        : 28;

    ctx.save();
    // Accuracy halo
    ctx.beginPath();
    ctx.arc(sx, sy, radiusPx, 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(0, 121, 193, 0.18)';
    ctx.fill();
    ctx.lineWidth = 1.5;
    ctx.strokeStyle = 'rgba(0, 121, 193, 0.48)';
    ctx.stroke();

    // White border
    ctx.beginPath();
    ctx.arc(sx, sy, 9, 0, Math.PI * 2);
    ctx.fillStyle = '#FFFFFF';
    ctx.fill();

    // ESRI Blue dot
    ctx.beginPath();
    ctx.arc(sx, sy, 6.5, 0, Math.PI * 2);
    ctx.fillStyle = 'rgb(0, 121, 193)';
    ctx.fill();

    // White center glint
    ctx.beginPath();
    ctx.arc(sx, sy, 2, 0, Math.PI * 2);
    ctx.fillStyle = '#FFFFFF';
    ctx.fill();
    ctx.restore();
  }

  private drawGraticule(
    ctx: CanvasRenderingContext2D,
    viewport: MapViewport,
    isDarkMode: boolean,
    isSatellite: boolean
  ): void {
    const step = this.calculateGridStep(viewport.zoom);
    const pTopLeft = viewport.screenToGeo(0, 0);
    const pBottomRight = viewport.screenToGeo(viewport.screenWidth, viewport.screenHeight);

    const minLon = Math.min(pTopLeft.x, pBottomRight.x);
    const maxLon = Math.max(pTopLeft.x, pBottomRight.x);
    const minLat = Math.min(pTopLeft.y, pBottomRight.y);
    const maxLat = Math.max(pTopLeft.y, pBottomRight.y);

    const minLabelSpacingPx = 90;

    ctx.save();
    if (isSatellite) {
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.14)';
      ctx.fillStyle = 'rgb(225, 225, 225)';
    } else if (isDarkMode) {
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.14)';
      ctx.fillStyle = 'rgb(160, 175, 190)';
    } else {
      ctx.strokeStyle = 'rgba(0, 0, 0, 0.16)';
      ctx.fillStyle = 'rgb(95, 95, 95)';
    }
    ctx.lineWidth = 1;
    ctx.font = '10px monospace';

    let lon = Math.floor(minLon / step) * step;
    let lastLonLabelX = -999;
    let guard = 0;
    while (lon <= maxLon + step * 0.5 && guard++ < 80) {
      const [sx] = viewport.geoToScreen(lon, viewport.centerLat);
      if (sx >= 0 && sx <= viewport.screenWidth) {
        ctx.beginPath();
        ctx.moveTo(sx, 0);
        ctx.lineTo(sx, viewport.screenHeight);
        ctx.stroke();

        const avoidsNorthArrow = sx < viewport.screenWidth - 78;
        if (avoidsNorthArrow && sx - lastLonLabelX >= minLabelSpacingPx) {
          ctx.fillText(this.formatCoord(lon, false), sx + 4, 16);
          lastLonLabelX = sx;
        }
      }
      lon += step;
    }

    let lat = Math.floor(minLat / step) * step;
    let lastLatLabelY = viewport.screenHeight + 999;
    guard = 0;
    while (lat <= maxLat + step * 0.5 && guard++ < 80) {
      const [, sy] = viewport.geoToScreen(viewport.centerLon, lat);
      if (sy >= 0 && sy <= viewport.screenHeight) {
        ctx.beginPath();
        ctx.moveTo(0, sy);
        ctx.lineTo(viewport.screenWidth, sy);
        ctx.stroke();

        const avoidsScaleBar = sy < viewport.screenHeight - 64;
        if (avoidsScaleBar && sy > 24 && lastLatLabelY - sy >= minLabelSpacingPx) {
          ctx.fillText(this.formatCoord(lat, true), 6, sy - 4);
          lastLatLabelY = sy;
        }
      }
      lat += step;
    }

    ctx.restore();
  }

  private formatCoord(value: number, isLat: boolean): string {
    const dir = isLat ? (value >= 0 ? 'N' : 'S') : value >= 0 ? 'E' : 'W';
    return `${Math.abs(value).toFixed(3)}°${dir}`;
  }

  private calculateGridStep(zoom: number): number {
    if (zoom >= 16) return 0.005;
    if (zoom >= 14) return 0.01;
    if (zoom >= 12) return 0.02;
    if (zoom >= 10) return 0.05;
    if (zoom >= 8) return 0.2;
    if (zoom >= 6) return 0.5;
    return 2.0;
  }

  private drawLayer(
    ctx: CanvasRenderingContext2D,
    viewport: MapViewport,
    layer: VectorLayer,
    isDarkMode: boolean
  ): void {
    const symbology = layer.symbology;

    for (const feature of layer.features) {
      const geom = feature.geometry;
      if (!geom) continue;
      const isSelected = layer.selectedFeatureIds.has(feature.id);

      const fillColor = getSymbologyFillColor(symbology, feature);
      const strokeColor = getSymbologyStrokeColor(symbology);
      const strokeWidth = getSymbologyStrokeWidth(symbology);

      const fillStyle = colorToCss(fillColor, layer.opacity);
      const strokeStyle = colorToCss(strokeColor, layer.opacity);
      const lineWidth = Math.max(1, strokeWidth * 1.3);

      switch (geom.geometryType) {
        case GeometryType.POINT:
          this.drawPoint(ctx, viewport, geom, symbology, fillStyle, strokeStyle, lineWidth, isSelected);
          break;
        case GeometryType.POLYLINE:
          this.drawPolyline(ctx, viewport, geom, strokeStyle, lineWidth, isSelected);
          break;
        case GeometryType.POLYGON:
          this.drawPolygon(ctx, viewport, geom, fillStyle, strokeStyle, lineWidth, isSelected);
          break;
        case GeometryType.MULTI_POINT:
          for (const pt of geom.points) {
            this.drawPoint(ctx, viewport, pt, symbology, fillStyle, strokeStyle, lineWidth, isSelected);
          }
          break;
        case GeometryType.MULTI_POLYLINE:
          for (const line of geom.polylines) {
            this.drawPolyline(ctx, viewport, line, strokeStyle, lineWidth, isSelected);
          }
          break;
        case GeometryType.MULTI_POLYGON:
          for (const poly of geom.polygons) {
            this.drawPolygon(ctx, viewport, poly, fillStyle, strokeStyle, lineWidth, isSelected);
          }
          break;
      }

      if (layer.labelFieldName) {
        const raw = feature.attributes[layer.labelFieldName];
        if (raw !== null && raw !== undefined && String(raw).trim() !== '') {
          this.drawLabel(ctx, viewport, geom, String(raw), isDarkMode);
        }
      }
    }
  }

  private drawPoint(
    ctx: CanvasRenderingContext2D,
    viewport: MapViewport,
    point: Point,
    symbology: LayerSymbology,
    fillStyle: string,
    strokeStyle: string,
    lineWidth: number,
    isSelected: boolean
  ): void {
    const [sx, sy] = viewport.geoToScreen(point.x, point.y);
    if (sx < -60 || sx > viewport.screenWidth + 60 || sy < -60 || sy > viewport.screenHeight + 60) {
      return;
    }

    const baseRadius = symbology.type === 'Simple' ? Math.max(4, symbology.pointSizeDp * 1.05) : 7.5;
    const shape = symbology.type === 'Simple' ? symbology.markerShape : MarkerShape.CIRCLE;

    ctx.save();
    ctx.fillStyle = fillStyle;
    ctx.strokeStyle = strokeStyle;
    ctx.lineWidth = lineWidth;

    ctx.beginPath();
    if (shape === MarkerShape.SQUARE) {
      ctx.rect(sx - baseRadius, sy - baseRadius, baseRadius * 2, baseRadius * 2);
      ctx.fill();
      ctx.stroke();
    } else if (shape === MarkerShape.TRIANGLE) {
      ctx.moveTo(sx, sy - baseRadius);
      ctx.lineTo(sx + baseRadius, sy + baseRadius);
      ctx.lineTo(sx - baseRadius, sy + baseRadius);
      ctx.closePath();
      ctx.fill();
      ctx.stroke();
    } else if (shape === MarkerShape.CROSS) {
      ctx.moveTo(sx - baseRadius, sy);
      ctx.lineTo(sx + baseRadius, sy);
      ctx.moveTo(sx, sy - baseRadius);
      ctx.lineTo(sx, sy + baseRadius);
      ctx.stroke();
    } else {
      ctx.arc(sx, sy, baseRadius, 0, Math.PI * 2);
      ctx.fill();
      ctx.stroke();
    }

    if (isSelected) {
      ctx.beginPath();
      ctx.arc(sx, sy, baseRadius + 5, 0, Math.PI * 2);
      ctx.strokeStyle = 'rgba(0, 200, 255, 0.35)';
      ctx.lineWidth = 7;
      ctx.stroke();

      ctx.beginPath();
      ctx.arc(sx, sy, baseRadius + 3, 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(0, 255, 255, 0.5)';
      ctx.fill();
      ctx.strokeStyle = 'rgb(0, 255, 255)';
      ctx.lineWidth = 3;
      ctx.stroke();
    }

    ctx.restore();
  }

  private drawPolyline(
    ctx: CanvasRenderingContext2D,
    viewport: MapViewport,
    polyline: Polyline,
    strokeStyle: string,
    lineWidth: number,
    isSelected: boolean
  ): void {
    const pts = polyline.points;
    if (pts.length < 2) return;

    ctx.save();
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';
    ctx.beginPath();
    const [startX, startY] = viewport.geoToScreen(pts[0].x, pts[0].y);
    ctx.moveTo(startX, startY);
    for (let i = 1; i < pts.length; i++) {
      const [px, py] = viewport.geoToScreen(pts[i].x, pts[i].y);
      ctx.lineTo(px, py);
    }

    ctx.strokeStyle = strokeStyle;
    ctx.lineWidth = lineWidth;
    ctx.stroke();

    if (isSelected) {
      ctx.strokeStyle = 'rgba(0, 200, 255, 0.35)';
      ctx.lineWidth = lineWidth + 7;
      ctx.stroke();

      ctx.strokeStyle = 'rgb(0, 255, 255)';
      ctx.lineWidth = lineWidth + 2.5;
      ctx.stroke();
    }
    ctx.restore();
  }

  private drawPolygon(
    ctx: CanvasRenderingContext2D,
    viewport: MapViewport,
    polygon: Polygon,
    fillStyle: string,
    strokeStyle: string,
    lineWidth: number,
    isSelected: boolean
  ): void {
    const rings = polygon.rings;
    if (rings.length === 0 || rings[0].length < 3) return;

    ctx.save();
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';
    ctx.beginPath();

    for (const ring of rings) {
      if (ring.length < 3) continue;
      const [startX, startY] = viewport.geoToScreen(ring[0].x, ring[0].y);
      ctx.moveTo(startX, startY);
      for (let i = 1; i < ring.length; i++) {
        const [px, py] = viewport.geoToScreen(ring[i].x, ring[i].y);
        ctx.lineTo(px, py);
      }
      ctx.closePath();
    }

    ctx.fillStyle = fillStyle;
    ctx.fill('evenodd');
    ctx.strokeStyle = strokeStyle;
    ctx.lineWidth = lineWidth;
    ctx.stroke();

    if (isSelected) {
      ctx.fillStyle = 'rgba(0, 255, 255, 0.5)';
      ctx.fill('evenodd');
      ctx.strokeStyle = 'rgba(0, 200, 255, 0.35)';
      ctx.lineWidth = 8;
      ctx.stroke();
      ctx.strokeStyle = 'rgb(0, 255, 255)';
      ctx.lineWidth = 3.5;
      ctx.stroke();
    }
    ctx.restore();
  }

  private drawLabel(
    ctx: CanvasRenderingContext2D,
    viewport: MapViewport,
    geom: Geometry,
    text: string,
    isDarkMode: boolean
  ): void {
    let lx = 0;
    let ly = 0;

    if (geom.geometryType === GeometryType.POINT) {
      const [px, py] = viewport.geoToScreen(geom.x, geom.y);
      lx = px + 10;
      ly = py - 8;
    } else if (geom.geometryType === GeometryType.POLYGON) {
      const c = polygonCentroid(geom);
      const [cx, cy] = viewport.geoToScreen(c.x, c.y);
      lx = cx;
      ly = cy;
    } else if (geom.geometryType === GeometryType.POLYLINE) {
      const mid = geom.points[Math.floor(geom.points.length / 2)] ?? geom.points[0];
      if (!mid) return;
      const [mx, my] = viewport.geoToScreen(mid.x, mid.y);
      lx = mx;
      ly = my - 6;
    } else {
      const bb = getGeometryBoundingBox(geom);
      const [bx, by] = viewport.geoToScreen((bb.minX + bb.maxX) / 2, (bb.minY + bb.maxY) / 2);
      lx = bx;
      ly = by;
    }

    if (
      lx >= -100 &&
      lx <= viewport.screenWidth + 100 &&
      ly >= -100 &&
      ly <= viewport.screenHeight + 100
    ) {
      ctx.save();
      ctx.font = 'bold 11px sans-serif';
      ctx.lineJoin = 'round';
      ctx.lineCap = 'round';
      ctx.lineWidth = 3.5;
      ctx.strokeStyle = isDarkMode ? 'rgb(22, 26, 33)' : '#FFFFFF';
      ctx.fillStyle = isDarkMode ? 'rgb(240, 240, 240)' : 'rgb(20, 20, 20)';
      ctx.strokeText(text, lx, ly);
      ctx.fillText(text, lx, ly);
      ctx.restore();
    }
  }

  private drawMeasureOverlay(
    ctx: CanvasRenderingContext2D,
    viewport: MapViewport,
    session: MeasureSession
  ): void {
    const pts = session.points;
    if (pts.length === 0) return;

    ctx.save();
    if (session.isArea && pts.length >= 3) {
      ctx.beginPath();
      const [fx, fy] = viewport.geoToScreen(pts[0].x, pts[0].y);
      ctx.moveTo(fx, fy);
      for (let i = 1; i < pts.length; i++) {
        const [px, py] = viewport.geoToScreen(pts[i].x, pts[i].y);
        ctx.lineTo(px, py);
      }
      ctx.closePath();
      ctx.fillStyle = 'rgba(233, 30, 99, 0.16)';
      ctx.fill();
    }

    if (pts.length >= 2) {
      ctx.beginPath();
      const [sx0, sy0] = viewport.geoToScreen(pts[0].x, pts[0].y);
      ctx.moveTo(sx0, sy0);
      for (let i = 1; i < pts.length; i++) {
        const [px, py] = viewport.geoToScreen(pts[i].x, pts[i].y);
        ctx.lineTo(px, py);
      }
      if (session.isArea && pts.length >= 3) {
        ctx.closePath();
      }
      ctx.strokeStyle = 'rgb(233, 30, 99)';
      ctx.lineWidth = 3;
      ctx.setLineDash([10, 6]);
      ctx.stroke();
      ctx.setLineDash([]);
    }

    for (const p of pts) {
      const [px, py] = viewport.geoToScreen(p.x, p.y);
      ctx.beginPath();
      ctx.arc(px, py, 8, 0, Math.PI * 2);
      ctx.fillStyle = 'rgb(233, 30, 99)';
      ctx.fill();

      ctx.beginPath();
      ctx.arc(px, py, 5, 0, Math.PI * 2);
      ctx.fillStyle = '#FFFFFF';
      ctx.fill();

      ctx.beginPath();
      ctx.arc(px, py, 2, 0, Math.PI * 2);
      ctx.fillStyle = 'rgb(233, 30, 99)';
      ctx.fill();
    }
    ctx.restore();
  }

  private drawDigitizeOverlay(
    ctx: CanvasRenderingContext2D,
    viewport: MapViewport,
    session: DigitizeSession
  ): void {
    const pts = session.points;
    if (pts.length === 0) return;

    ctx.save();
    if (session.isPolygon && pts.length >= 3) {
      ctx.beginPath();
      const [fx, fy] = viewport.geoToScreen(pts[0].x, pts[0].y);
      ctx.moveTo(fx, fy);
      for (let i = 1; i < pts.length; i++) {
        const [px, py] = viewport.geoToScreen(pts[i].x, pts[i].y);
        ctx.lineTo(px, py);
      }
      ctx.closePath();
      ctx.fillStyle = 'rgba(0, 121, 193, 0.22)';
      ctx.fill();
    }

    if (pts.length >= 2) {
      ctx.beginPath();
      const [sx0, sy0] = viewport.geoToScreen(pts[0].x, pts[0].y);
      ctx.moveTo(sx0, sy0);
      for (let i = 1; i < pts.length; i++) {
        const [px, py] = viewport.geoToScreen(pts[i].x, pts[i].y);
        ctx.lineTo(px, py);
      }
      ctx.strokeStyle = 'rgb(0, 121, 193)';
      ctx.lineWidth = 2.5;
      ctx.setLineDash([]);
      ctx.stroke();

      if (session.isPolygon && pts.length >= 3) {
        const [lx, ly] = viewport.geoToScreen(
          pts[pts.length - 1].x,
          pts[pts.length - 1].y
        );
        ctx.beginPath();
        ctx.moveTo(lx, ly);
        ctx.lineTo(sx0, sy0);
        ctx.strokeStyle = 'rgba(0, 121, 193, 0.75)';
        ctx.lineWidth = 2;
        ctx.setLineDash([6, 4]);
        ctx.stroke();
        ctx.setLineDash([]);
      }
    }

    for (const p of pts) {
      const [px, py] = viewport.geoToScreen(p.x, p.y);
      ctx.fillStyle = 'rgba(0, 121, 193, 0.35)';
      ctx.strokeStyle = 'rgb(0, 121, 193)';
      ctx.lineWidth = 1.5;
      ctx.fillRect(px - 5, py - 5, 10, 10);
      ctx.strokeRect(px - 5, py - 5, 10, 10);
    }
    ctx.restore();
  }

  private drawScaleBar(
    ctx: CanvasRenderingContext2D,
    viewport: MapViewport,
    isDarkMode: boolean,
    isSatellite: boolean
  ): void {
    const groundMpp = viewport.groundMetersPerPixel;
    if (groundMpp <= 0) return;

    const targetPixels = 120;
    const rawMeters = targetPixels * groundMpp;
    const niceMeters = this.getNiceScaleDistance(rawMeters);
    const barWidthPx = niceMeters / groundMpp;

    const startX = 18;
    const startY = viewport.screenHeight - 36;
    const barHeight = 7;
    const halfW = barWidthPx / 2;

    const lightInk = isDarkMode || isSatellite;
    const blackCol = lightInk ? 'rgb(240, 240, 240)' : '#000000';
    const whiteCol = lightInk ? 'rgb(30, 36, 46)' : '#FFFFFF';

    ctx.save();
    ctx.fillStyle = blackCol;
    ctx.fillRect(startX, startY, halfW, barHeight);

    ctx.fillStyle = whiteCol;
    ctx.fillRect(startX + halfW, startY, halfW, barHeight);

    ctx.strokeStyle = blackCol;
    ctx.lineWidth = 1.2;
    ctx.strokeRect(startX, startY, barWidthPx, barHeight);

    ctx.beginPath();
    ctx.moveTo(startX + halfW, startY);
    ctx.lineTo(startX + halfW, startY + barHeight);
    ctx.stroke();

    const label =
      niceMeters >= 1000 ? `${Math.round(niceMeters / 1000)} km` : `${Math.round(niceMeters)} m`;

    ctx.font = 'bold 11px sans-serif';
    ctx.lineWidth = 3;
    ctx.lineJoin = 'round';
    ctx.strokeStyle = lightInk ? 'rgba(0, 0, 0, 0.78)' : 'rgba(255, 255, 255, 0.85)';
    ctx.fillStyle = blackCol;
    ctx.strokeText('0', startX - 2, startY - 5);
    ctx.fillText('0', startX - 2, startY - 5);
    ctx.strokeText(label, startX + barWidthPx - 12, startY - 5);
    ctx.fillText(label, startX + barWidthPx - 12, startY - 5);
    ctx.restore();
  }

  private getNiceScaleDistance(rawMeters: number): number {
    const exponent = Math.floor(Math.log10(Math.max(1e-6, rawMeters)));
    const pow10 = Math.pow(10, exponent);
    const fraction = rawMeters / pow10;
    let niceFraction = 10;
    if (fraction < 1.5) niceFraction = 1;
    else if (fraction < 3.5) niceFraction = 2;
    else if (fraction < 7.5) niceFraction = 5;
    return niceFraction * pow10;
  }

  private drawNorthArrow(
    ctx: CanvasRenderingContext2D,
    viewport: MapViewport,
    isDarkMode: boolean,
    isSatellite: boolean
  ): void {
    const cx = viewport.screenWidth - 36;
    const cy = 54;
    const size = 16;

    const lightInk = isDarkMode || isSatellite;
    const blackCol = lightInk ? 'rgb(240, 240, 240)' : '#000000';
    const whiteCol = lightInk ? 'rgb(30, 36, 46)' : '#FFFFFF';

    ctx.save();
    ctx.beginPath();
    ctx.moveTo(cx, cy - size);
    ctx.lineTo(cx - size * 0.45, cy + size * 0.7);
    ctx.lineTo(cx, cy + size * 0.35);
    ctx.closePath();
    ctx.fillStyle = blackCol;
    ctx.fill();

    ctx.beginPath();
    ctx.moveTo(cx, cy - size);
    ctx.lineTo(cx + size * 0.45, cy + size * 0.7);
    ctx.lineTo(cx, cy + size * 0.35);
    ctx.closePath();
    ctx.fillStyle = whiteCol;
    ctx.fill();
    ctx.strokeStyle = blackCol;
    ctx.lineWidth = 1.2;
    ctx.stroke();

    ctx.font = 'bold 12px sans-serif';
    ctx.lineWidth = 3;
    ctx.lineJoin = 'round';
    ctx.strokeStyle = lightInk ? 'rgba(0, 0, 0, 0.78)' : 'rgba(255, 255, 255, 0.85)';
    ctx.fillStyle = blackCol;
    ctx.strokeText('N', cx - 4.5, cy - size - 4);
    ctx.fillText('N', cx - 4.5, cy - size - 4);
    ctx.restore();
  }
}
