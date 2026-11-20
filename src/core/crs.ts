export enum Hemisphere {
  NORTH = 'NORTH',
  SOUTH = 'SOUTH',
}

export type CoordinateReferenceSystem =
  | { kind: 'WGS84'; epsgCode: 4326; name: 'GCS WGS 1984'; isGeographic: true }
  | {
      kind: 'WebMercator';
      epsgCode: 3857;
      name: 'WGS 1984 Web Mercator (Auxiliary Sphere)';
      isGeographic: false;
    }
  | {
      kind: 'UTM';
      zone: number;
      hemisphere: Hemisphere;
      epsgCode: number;
      name: string;
      isGeographic: false;
    };

export const CRS_WGS84: CoordinateReferenceSystem = {
  kind: 'WGS84',
  epsgCode: 4326,
  name: 'GCS WGS 1984',
  isGeographic: true,
};

export const CRS_WEB_MERCATOR: CoordinateReferenceSystem = {
  kind: 'WebMercator',
  epsgCode: 3857,
  name: 'WGS 1984 Web Mercator (Auxiliary Sphere)',
  isGeographic: false,
};

export function createUtmCrs(zone: number, hemisphere: Hemisphere): CoordinateReferenceSystem {
  return {
    kind: 'UTM',
    zone,
    hemisphere,
    epsgCode: hemisphere === Hemisphere.NORTH ? 32600 + zone : 32700 + zone,
    name: `WGS 1984 UTM Zone ${zone}${hemisphere === Hemisphere.NORTH ? 'N' : 'S'}`,
    isGeographic: false,
  };
}

export interface UtmCoordinate {
  zone: number;
  hemisphere: Hemisphere;
  easting: number;
  northing: number;
}

export function formatUtm(utm: UtmCoordinate): string {
  const hem = utm.hemisphere === Hemisphere.NORTH ? 'N' : 'S';
  return `${utm.zone}${hem} ${utm.easting.toFixed(1)}m E, ${utm.northing.toFixed(1)}m N`;
}

// WGS84 Ellipsoid constants
const A = 6378137.0;
const F = 1.0 / 298.257223563;
const B = A * (1.0 - F);
const E2 = (A * A - B * B) / (A * A);
const E_PRIME2 = (A * A - B * B) / (B * B);
const K0 = 0.9996;
const FALSE_EASTING = 500000.0;
const FALSE_NORTHING_SOUTH = 10000000.0;

function toRadians(deg: number): number {
  return (deg * Math.PI) / 180.0;
}

function toDegrees(rad: number): number {
  return (rad * 180.0) / Math.PI;
}

export const ProjectionEngine = {
  calculateUtmZone(longitude: number): number {
    let lon = longitude;
    while (lon > 180.0) lon -= 360.0;
    while (lon < -180.0) lon += 360.0;
    return Math.floor((lon + 180.0) / 6.0) + 1;
  },

  wgs84ToUtm(latitude: number, longitude: number, forcedZone?: number): UtmCoordinate {
    const latRad = toRadians(latitude);
    const zone = forcedZone ?? ProjectionEngine.calculateUtmZone(longitude);
    const hemisphere = latitude >= 0 ? Hemisphere.NORTH : Hemisphere.SOUTH;

    const lonOrigin = (zone - 1) * 6 - 180 + 3;
    const lonOriginRad = toRadians(lonOrigin);
    const deltaLonRad = toRadians(longitude) - lonOriginRad;

    const sinLat = Math.sin(latRad);
    const cosLat = Math.cos(latRad);
    const tanLat = Math.tan(latRad);

    const n = A / Math.sqrt(1.0 - E2 * sinLat * sinLat);
    const t = tanLat * tanLat;
    const c = E_PRIME2 * cosLat * cosLat;
    const al = cosLat * deltaLonRad;

    const m =
      A *
      ((1.0 - E2 / 4.0 - (3.0 * E2 * E2) / 64.0 - (5.0 * E2 * E2 * E2) / 256.0) * latRad -
        ((3.0 * E2) / 8.0 + (3.0 * E2 * E2) / 32.0 + (45.0 * E2 * E2 * E2) / 1024.0) *
          Math.sin(2.0 * latRad) +
        ((15.0 * E2 * E2) / 256.0 + (45.0 * E2 * E2 * E2) / 1024.0) * Math.sin(4.0 * latRad) -
        ((35.0 * E2 * E2 * E2) / 3072.0) * Math.sin(6.0 * latRad));

    const al2 = al * al;
    const al3 = al2 * al;
    const al4 = al2 * al2;
    const al5 = al4 * al;
    const al6 = al3 * al3;

    const easting =
      K0 *
        n *
        (al +
          ((1.0 - t + c) * al3) / 6.0 +
          ((5.0 - 18.0 * t + t * t + 72.0 * c - 58.0 * E_PRIME2) * al5) / 120.0) +
      FALSE_EASTING;

    let northing =
      K0 *
      (m +
        n *
          tanLat *
          (al2 / 2.0 +
            ((5.0 - t + 9.0 * c + 4.0 * c * c) * al4) / 24.0 +
            ((61.0 - 58.0 * t + t * t + 600.0 * c - 330.0 * E_PRIME2) * al6) / 720.0));

    if (hemisphere === Hemisphere.SOUTH) {
      northing += FALSE_NORTHING_SOUTH;
    }

    return { zone, hemisphere, easting, northing };
  },

  utmToWgs84(utm: UtmCoordinate): { x: number; y: number } {
    const x = utm.easting - FALSE_EASTING;
    let y = utm.northing;
    if (utm.hemisphere === Hemisphere.SOUTH) {
      y -= FALSE_NORTHING_SOUTH;
    }

    const lonOrigin = (utm.zone - 1) * 6 - 180 + 3;
    const lonOriginRad = toRadians(lonOrigin);

    const e1 = (1.0 - Math.sqrt(1.0 - E2)) / (1.0 + Math.sqrt(1.0 - E2));
    const m = y / K0;
    const mu = m / (A * (1.0 - E2 / 4.0 - (3.0 * E2 * E2) / 64.0 - (5.0 * E2 * E2 * E2) / 256.0));

    const phi1Rad =
      mu +
      ((3.0 * e1) / 2.0 - (27.0 * Math.pow(e1, 3)) / 32.0) * Math.sin(2.0 * mu) +
      ((21.0 * e1 * e1) / 16.0 - (55.0 * Math.pow(e1, 4)) / 32.0) * Math.sin(4.0 * mu) +
      ((151.0 * Math.pow(e1, 3)) / 96.0) * Math.sin(6.0 * mu) +
      ((1097.0 * Math.pow(e1, 4)) / 512.0) * Math.sin(8.0 * mu);

    const sinPhi1 = Math.sin(phi1Rad);
    const cosPhi1 = Math.cos(phi1Rad);
    const tanPhi1 = Math.tan(phi1Rad);

    const n1 = A / Math.sqrt(1.0 - E2 * sinPhi1 * sinPhi1);
    const r1 = (A * (1.0 - E2)) / Math.pow(1.0 - E2 * sinPhi1 * sinPhi1, 1.5);
    const d = x / (n1 * K0);

    const t1 = tanPhi1 * tanPhi1;
    const c1 = E_PRIME2 * cosPhi1 * cosPhi1;

    const d2 = d * d;
    const d3 = d2 * d;
    const d4 = d2 * d2;
    const d5 = d4 * d;
    const d6 = d3 * d3;

    const latRad =
      phi1Rad -
      ((n1 * tanPhi1) / r1) *
        (d2 / 2.0 -
          ((5.0 + 3.0 * t1 + 10.0 * c1 - 4.0 * c1 * c1 - 9.0 * E_PRIME2) * d4) / 24.0 +
          ((61.0 +
            90.0 * t1 +
            298.0 * c1 +
            45.0 * t1 * t1 -
            252.0 * E_PRIME2 -
            3.0 * c1 * c1) *
            d6) /
            720.0);

    const lonRad =
      lonOriginRad +
      (d -
        ((1.0 + 2.0 * t1 + c1) * d3) / 6.0 +
        ((5.0 - 2.0 * c1 + 28.0 * t1 - 3.0 * c1 * c1 + 8.0 * E_PRIME2 + 24.0 * t1 * t1) * d5) /
          120.0) /
        cosPhi1;

    return { x: toDegrees(lonRad), y: toDegrees(latRad) };
  },

  wgs84ToWebMercator(latitude: number, longitude: number): { x: number; y: number } {
    const x = (longitude * 20037508.34) / 180.0;
    const clampedLat = Math.max(-85.05112878, Math.min(85.05112878, latitude));
    const rad = toRadians(clampedLat);
    const y = (Math.log(Math.tan(Math.PI / 4.0 + rad / 2.0)) * 20037508.34) / Math.PI;
    return { x, y };
  },

  webMercatorToWgs84(x: number, y: number): { x: number; y: number } {
    const lon = (x * 180.0) / 20037508.34;
    const lat = toDegrees(2.0 * Math.atan(Math.exp((y * Math.PI) / 20037508.34)) - Math.PI / 2.0);
    return { x: lon, y: lat };
  },

  geodesicDistanceMeters(p1: { x: number; y: number }, p2: { x: number; y: number }): number {
    const earthRadius = 6371000.0;
    const lat1Rad = toRadians(p1.y);
    const lat2Rad = toRadians(p2.y);
    const dLat = toRadians(p2.y - p1.y);
    const dLon = toRadians(p2.x - p1.x);

    const a =
      Math.pow(Math.sin(dLat / 2.0), 2) +
      Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.pow(Math.sin(dLon / 2.0), 2);
    const c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    return earthRadius * c;
  },

  geodesicPolygonAreaSquareMeters(ring: ReadonlyArray<{ x: number; y: number }>): number {
    if (ring.length < 3) return 0.0;
    const earthRadius = 6371000.0;
    let total = 0.0;
    const n = ring.length;

    for (let i = 0; i < n; i++) {
      const p1 = ring[i];
      const p2 = ring[(i + 1) % n];
      const lat1 = toRadians(p1.y);
      const lat2 = toRadians(p2.y);
      const lon1 = toRadians(p1.x);
      const lon2 = toRadians(p2.x);

      total += (lon2 - lon1) * (2.0 + Math.sin(lat1) + Math.sin(lat2));
    }

    return Math.abs((total * earthRadius * earthRadius) / 2.0);
  },
};
