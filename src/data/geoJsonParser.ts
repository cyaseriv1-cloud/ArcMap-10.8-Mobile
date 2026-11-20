import {
  type Geometry,
  GeometryType,
  type Point,
  type Polygon,
  type Polyline,
  makeMultiPoint,
  makeMultiPolygon,
  makeMultiPolyline,
  makePoint,
  makePolygon,
  makePolyline,
} from '../core/geometry';
import {
  type AttributeValue,
  type Feature,
  type Field,
  FieldType,
  type VectorLayer,
  createVectorLayer,
  makeField,
} from '../core/model';

export const GeoJsonParser = {
  parse(geoJsonString: String | string, layerName = 'Layer'): VectorLayer {
    const root = JSON.parse(String(geoJsonString)) as Record<string, unknown>;
    const type = typeof root.type === 'string' ? root.type : '';

    if (type === 'FeatureCollection') {
      return parseFeatureCollection(root, layerName);
    }
    if (type === 'Feature') {
      return parseFeatureCollection({ type: 'FeatureCollection', features: [root] }, layerName);
    }
    return parseFeatureCollection({ type: 'FeatureCollection', features: [] }, layerName);
  },

  exportToGeoJson(layer: VectorLayer): string {
    const features = layer.features.map((f) => ({
      type: 'Feature',
      id: f.id,
      properties: { ...f.attributes },
      geometry: serializeGeometry(f.geometry),
    }));

    return JSON.stringify(
      {
        type: 'FeatureCollection',
        features,
      },
      null,
      2
    );
  },
};

function parseFeatureCollection(fcJson: Record<string, unknown>, layerName: string): VectorLayer {
  const featuresArray = Array.isArray(fcJson.features) ? fcJson.features : [];
  const parsedFeatures: Feature[] = [];
  const fieldsMap = new Map<string, FieldType>();
  let dominantType: GeometryType = GeometryType.UNKNOWN;

  for (let i = 0; i < featuresArray.length; i++) {
    const fObj = featuresArray[i] as Record<string, unknown> | null;
    if (!fObj || typeof fObj !== 'object') continue;

    const rawId = fObj.id;
    const id = typeof rawId === 'number' && Number.isFinite(rawId) ? rawId : i + 1;

    const geomObj = fObj.geometry as Record<string, unknown> | null | undefined;
    const geometry = geomObj ? parseGeometry(geomObj) : null;
    if (geometry && dominantType === GeometryType.UNKNOWN) {
      dominantType = geometry.geometryType;
    }

    const attrs: Record<string, AttributeValue> = {};
    const propsObj = fObj.properties as Record<string, unknown> | null | undefined;
    if (propsObj && typeof propsObj === 'object') {
      for (const [key, value] of Object.entries(propsObj)) {
        if (value === null || value === undefined) {
          attrs[key] = null;
        } else if (
          typeof value === 'string' ||
          typeof value === 'number' ||
          typeof value === 'boolean'
        ) {
          attrs[key] = value;
        } else {
          attrs[key] = JSON.stringify(value);
        }

        if (value !== null && value !== undefined && !fieldsMap.has(key)) {
          let inferredType = FieldType.STRING;
          if (typeof value === 'number') {
            inferredType = Number.isInteger(value) ? FieldType.INTEGER : FieldType.DOUBLE;
          } else if (typeof value === 'boolean') {
            inferredType = FieldType.BOOLEAN;
          }
          fieldsMap.set(key, inferredType);
        }
      }
    }

    parsedFeatures.push({ id, geometry, attributes: attrs });
  }

  const fields: Field[] = Array.from(fieldsMap.entries()).map(([name, type]) =>
    makeField(name, type)
  );

  return createVectorLayer({
    name: layerName,
    geometryType: dominantType !== GeometryType.UNKNOWN ? dominantType : GeometryType.POLYGON,
    fields,
    features: parsedFeatures,
  });
}

function parseGeometry(geomObj: Record<string, unknown>): Geometry | null {
  const type = typeof geomObj.type === 'string' ? geomObj.type : '';
  const coords = geomObj.coordinates as unknown;
  if (!Array.isArray(coords)) return null;

  switch (type) {
    case 'Point': {
      const c = coords as number[];
      if (c.length < 2) return null;
      return makePoint(Number(c[0]), Number(c[1]));
    }
    case 'LineString': {
      const pts: Point[] = (coords as number[][]).map((c) => makePoint(Number(c[0]), Number(c[1])));
      return makePolyline(pts);
    }
    case 'Polygon': {
      const rings: Point[][] = (coords as number[][][]).map((ring) =>
        ring.map((c) => makePoint(Number(c[0]), Number(c[1])))
      );
      return makePolygon(rings);
    }
    case 'MultiPoint': {
      const pts: Point[] = (coords as number[][]).map((c) => makePoint(Number(c[0]), Number(c[1])));
      return makeMultiPoint(pts);
    }
    case 'MultiLineString': {
      const lines: Polyline[] = (coords as number[][][]).map((line) =>
        makePolyline(line.map((c) => makePoint(Number(c[0]), Number(c[1]))))
      );
      return makeMultiPolyline(lines);
    }
    case 'MultiPolygon': {
      const polys: Polygon[] = (coords as number[][][][]).map((poly) =>
        makePolygon(
          poly.map((ring) => ring.map((c) => makePoint(Number(c[0]), Number(c[1]))))
        )
      );
      return makeMultiPolygon(polys);
    }
    default:
      return null;
  }
}

function serializeGeometry(geometry: Geometry | null): Record<string, unknown> | null {
  if (!geometry) return null;
  switch (geometry.geometryType) {
    case GeometryType.POINT:
      return { type: 'Point', coordinates: [geometry.x, geometry.y] };
    case GeometryType.POLYLINE:
      return { type: 'LineString', coordinates: geometry.points.map((p) => [p.x, p.y]) };
    case GeometryType.POLYGON:
      return {
        type: 'Polygon',
        coordinates: geometry.rings.map((ring) => ring.map((p) => [p.x, p.y])),
      };
    case GeometryType.MULTI_POINT:
      return { type: 'MultiPoint', coordinates: geometry.points.map((p) => [p.x, p.y]) };
    case GeometryType.MULTI_POLYLINE:
      return {
        type: 'MultiLineString',
        coordinates: geometry.polylines.map((line) => line.points.map((p) => [p.x, p.y])),
      };
    case GeometryType.MULTI_POLYGON:
      return {
        type: 'MultiPolygon',
        coordinates: geometry.polygons.map((poly) =>
          poly.rings.map((ring) => ring.map((p) => [p.x, p.y]))
        ),
      };
    default:
      return null;
  }
}
