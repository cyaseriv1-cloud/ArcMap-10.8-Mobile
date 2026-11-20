import { BasemapId, type BasemapDefinition } from './basemap';

const RETRY_DELAY_MS = 8000;

export class TileManager {
  private readonly maxCacheSize = 420;
  private readonly memoryCache = new Map<string, HTMLImageElement>();
  private readonly inFlight = new Set<string>();
  private readonly failedAt = new Map<string, number>();

  public onTileLoaded: (() => void) | null = null;

  getTile(
    basemap: BasemapDefinition,
    z: number,
    x: number,
    y: number,
    isOverlay = false
  ): HTMLImageElement | null {
    const template = isOverlay ? basemap.overlayUrlTemplate : basemap.tileUrlTemplate;
    if (basemap.id === BasemapId.NONE || !template) return null;
    if (z < basemap.minZoom || z > basemap.maxZoom) return null;

    const key = `${basemap.id}_${isOverlay ? 'ov' : 'base'}_${z}_${x}_${y}`;
    const cached = this.memoryCache.get(key);
    if (cached) {
      // Refresh LRU order
      this.memoryCache.delete(key);
      this.memoryCache.set(key, cached);
      return cached;
    }

    if (this.inFlight.has(key)) {
      return null;
    }

    const lastFail = this.failedAt.get(key);
    if (lastFail !== undefined && Date.now() - lastFail < RETRY_DELAY_MS) {
      return null;
    }

    this.inFlight.add(key);
    this.failedAt.delete(key);
    this.loadTile(template, z, x, y, key, true);
    return null;
  }

  private loadTile(
    template: string,
    z: number,
    x: number,
    y: number,
    key: string,
    tryCors: boolean
  ): void {
    const url = template
      .replace('{z}', String(z))
      .replace('{x}', String(x))
      .replace('{y}', String(y));

    const img = new Image();
    if (tryCors) {
      img.crossOrigin = 'anonymous';
    }

    img.onload = () => {
      this.inFlight.delete(key);
      this.failedAt.delete(key);
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
        this.loadTile(template, z, x, y, key, false);
      } else {
        this.inFlight.delete(key);
        this.failedAt.set(key, Date.now());
      }
    };

    img.src = url;
  }

  clearMemory(): void {
    this.memoryCache.clear();
    this.failedAt.clear();
  }
}
