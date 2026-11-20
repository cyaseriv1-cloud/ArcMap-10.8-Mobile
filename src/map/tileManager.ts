import { BasemapId, type BasemapDefinition } from './basemap';

export class TileManager {
  private readonly maxCacheSize = 350;
  private readonly memoryCache = new Map<string, HTMLImageElement>();
  private readonly inFlight = new Set<string>();
  private readonly failed = new Set<string>();

  public onTileLoaded: (() => void) | null = null;

  getTile(basemap: BasemapDefinition, z: number, x: number, y: number): HTMLImageElement | null {
    if (basemap.id === BasemapId.NONE || !basemap.tileUrlTemplate) return null;
    if (z < basemap.minZoom || z > basemap.maxZoom) return null;

    const key = `${basemap.id}_${z}_${x}_${y}`;
    const cached = this.memoryCache.get(key);
    if (cached) {
      // Refresh LRU order
      this.memoryCache.delete(key);
      this.memoryCache.set(key, cached);
      return cached;
    }

    if (this.inFlight.has(key) || this.failed.has(key)) {
      return null;
    }

    this.inFlight.add(key);
    this.loadTile(basemap, z, x, y, key, true);
    return null;
  }

  private loadTile(
    basemap: BasemapDefinition,
    z: number,
    x: number,
    y: number,
    key: string,
    tryCors: boolean
  ): void {
    const url = basemap.tileUrlTemplate
      .replace('{z}', String(z))
      .replace('{x}', String(x))
      .replace('{y}', String(y));

    const img = new Image();
    if (tryCors) {
      img.crossOrigin = 'anonymous';
    }

    img.onload = () => {
      this.inFlight.delete(key);
      if (this.memoryCache.size >= this.maxCacheSize) {
        const oldestKey = this.memoryCache.keys().next().value;
        if (oldestKey !== undefined) {
          this.memoryCache.delete(oldestKey);
        }
      }
      this.memoryCache.set(key, img);
      this.onTileLoaded?.();
    };

    img.onerror = () => {
      if (tryCors) {
        // Retry without crossOrigin if the tile server omits CORS headers
        this.loadTile(basemap, z, x, y, key, false);
      } else {
        this.inFlight.delete(key);
        this.failed.add(key);
      }
    };

    img.src = url;
  }

  clearMemory(): void {
    this.memoryCache.clear();
    this.failed.clear();
  }
}
