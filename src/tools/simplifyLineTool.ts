import {
  type Geometry,
  GeometryType,
  type Point,
  makePolygon,
  makePolyline,
  pointDistanceTo,
  pointsEqual,
} from '../core/geometry';
import { type Feature, type VectorLayer, createVectorLayer } from '../core/model';

export const SimplifyLineTool = {
  execute(
    inputLayer: VectorLayer,
    toleranceDegrees = 0.001,
    outputLayerName = `${inputLayer.name}_simplified`
  ): VectorLayer {
    const outFeatures: Feature[] = [];
    let nextId = 1;

    for (const feat of inputLayer.features) {
      if (!feat.geometry) continue;
      const simplified = simplifyGeometry(feat.geometry, toleranceDegrees);
      outFeatures.push({
        id: nextId++,
        geometry: simplified,
        attributes: { ...feat.attributes },
      });
    }

    return createVectorLayer({
      name: outputLayerName,
      geometryType: inputLayer.geometryType,
      symbology: inputLayer.symbology,
      fields: inputLayer.fields.map((f) => ({ ...f })),
      features: outFeatures,
    });
  },
};

function simplifyGeometry(geom: Geometry, epsilon: number): Geometry {
  if (geom.geometryType === GeometryType.POLYLINE) {
    return makePolyline(rdp(geom.points, epsilon));
  }
  if (geom.geometryType === GeometryType.POLYGON) {
    const newRings = geom.rings.map((ring) => {
      const simplified = rdp(ring, epsilon);
      if (simplified.length >= 3) {
        return !pointsEqual(simplified[0], simplified[simplified.length - 1])
          ? [...simplified, simplified[0]]
          : simplified;
      }
      return ring;
    });
    return makePolygon(newRings);
  }
  return geom;
}

function rdp(points: Point[], epsilon: number): Point[] {
  if (points.length < 3) return points;

  let maxDist = 0.0;
  let index = 0;
  const first = points[0];
  const last = points[points.length - 1];

  for (let i = 1; i < points.length - 1; i++) {
    const dist = perpendicularDistance(points[i], first, last);
    if (dist > maxDist) {
      maxDist = dist;
      index = i;
    }
  }

  if (maxDist > epsilon) {
    const left = rdp(points.slice(0, index + 1), epsilon);
    const right = rdp(points.slice(index), epsilon);
    return [...left.slice(0, -1), ...right];
  }
  return [first, last];
}

function perpendicularDistance(p: Point, a: Point, b: Point): number {
  const dx = b.x - a.x;
  const dy = b.y - a.y;
  const len = Math.sqrt(dx * dx + dy * dy);
  if (len < 1e-12) return pointDistanceTo(p, a);
  return Math.abs(dy * p.x - dx * p.y + b.x * a.y - b.y * a.x) / len;
}
