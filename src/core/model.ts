import { CRS_WGS84, type CoordinateReferenceSystem, ProjectionEngine } from './crs';
import {
  type BoundingBox,
  EMPTY_BBOX,
  type Geometry,
  GeometryType,
  bboxExpandBox,
  getGeometryBoundingBox,
  polygonCentroid,
} from './geometry';
import { type LayerSymbology, createSimpleSymbology } from './symbology';

export enum FieldType {
  STRING = 'STRING',
  INTEGER = 'INTEGER',
  DOUBLE = 'DOUBLE',
  DATE = 'DATE',
  BOOLEAN = 'BOOLEAN',
}

export interface Field {
  name: string;
  type: FieldType;
  alias: string;
  length: number;
}

export function makeField(
  name: string,
  type: FieldType,
  alias = name,
  length = 254
): Field {
  return { name, type, alias, length };
}

export type AttributeValue = string | number | boolean | null | undefined;

export interface Feature {
  id: number;
  geometry: Geometry | null;
  attributes: Record<string, AttributeValue>;
}

export function featureGetString(f: Feature, field: string): string | null {
  const v = f.attributes[field];
  return v === null || v === undefined ? null : String(v);
}

export function featureGetDouble(f: Feature, field: string): number | null {
  const v = f.attributes[field];
  if (typeof v === 'number') return Number.isFinite(v) ? v : null;
  if (typeof v === 'string') {
    const parsed = Number(v);
    return v.trim() !== '' && Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

export function featureGetBoolean(f: Feature, field: string): boolean | null {
  const v = f.attributes[field];
  if (typeof v === 'boolean') return v;
  if (typeof v === 'string') {
    const lower = v.toLowerCase();
    if (['true', 't', '1', 'yes', 'si'].includes(lower)) return true;
    if (['false', 'f', '0', 'no'].includes(lower)) return false;
  }
  if (typeof v === 'number') return v !== 0;
  return null;
}

export enum GeometryProperty {
  AREA = 'AREA',
  LENGTH_OR_PERIMETER = 'LENGTH_OR_PERIMETER',
  CENTROID_X = 'CENTROID_X',
  CENTROID_Y = 'CENTROID_Y',
}

export interface FieldStatistics {
  count: number;
  nullCount: number;
  min?: number;
  max?: number;
  sum?: number;
  mean?: number;
}

export interface VectorLayer {
  id: string;
  name: string;
  geometryType: GeometryType;
  isVisible: boolean;
  opacity: number;
  crs: CoordinateReferenceSystem;
  symbology: LayerSymbology;
  fields: Field[];
  features: Feature[];
  selectedFeatureIds: Set<number>;
  labelFieldName: string | null;
}

export function createVectorLayer(params: {
  id?: string;
  name: string;
  geometryType: GeometryType;
  isVisible?: boolean;
  opacity?: number;
  crs?: CoordinateReferenceSystem;
  symbology?: LayerSymbology;
  fields?: Field[];
  features?: Feature[];
  selectedFeatureIds?: Set<number>;
  labelFieldName?: string | null;
}): VectorLayer {
  return {
    id: params.id ?? crypto.randomUUID(),
    name: params.name,
    geometryType: params.geometryType,
    isVisible: params.isVisible ?? true,
    opacity: params.opacity ?? 1.0,
    crs: params.crs ?? CRS_WGS84,
    symbology: params.symbology ?? createSimpleSymbology(),
    fields: params.fields ?? [],
    features: params.features ?? [],
    selectedFeatureIds: params.selectedFeatureIds ?? new Set<number>(),
    labelFieldName: params.labelFieldName ?? null,
  };
}

export function getLayerBoundingBox(layer: VectorLayer): BoundingBox {
  let box = EMPTY_BBOX;
  for (const f of layer.features) {
    if (!f.geometry) continue;
    box = bboxExpandBox(box, getGeometryBoundingBox(f.geometry));
  }
  return box;
}

export function calculateFieldStatistics(layer: VectorLayer, fieldName: string): FieldStatistics {
  const field = layer.fields.find((f) => f.name.toLowerCase() === fieldName.toLowerCase());
  if (!field) return { count: 0, nullCount: layer.features.length };

  let count = 0;
  let nullCount = 0;
  let min = Number.MAX_VALUE;
  let max = -Number.MAX_VALUE;
  let sum = 0.0;

  for (const feat of layer.features) {
    const num = featureGetDouble(feat, field.name);
    if (num !== null) {
      count++;
      sum += num;
      if (num < min) min = num;
      if (num > max) max = num;
    } else {
      nullCount++;
    }
  }

  return count > 0
    ? { count, nullCount, min, max, sum, mean: sum / count }
    : { count: 0, nullCount };
}

export function calculateLayerGeometry(
  layer: VectorLayer,
  targetField: string,
  property: GeometryProperty
): VectorLayer {
  const existingField = layer.fields.find(
    (f) => f.name.toLowerCase() === targetField.toLowerCase()
  );
  const fields = existingField
    ? layer.fields
    : [...layer.fields, makeField(targetField, FieldType.DOUBLE)];
  const fieldName = existingField ? existingField.name : targetField;

  const features = layer.features.map((f) => {
    const geom = f.geometry;
    if (!geom) return f;

    let value: number | null = null;
    switch (property) {
      case GeometryProperty.AREA: {
        if (geom.geometryType === GeometryType.POLYGON && geom.rings.length > 0) {
          const outer = ProjectionEngine.geodesicPolygonAreaSquareMeters(geom.rings[0]);
          let holes = 0.0;
          for (let i = 1; i < geom.rings.length; i++) {
            holes += ProjectionEngine.geodesicPolygonAreaSquareMeters(geom.rings[i]);
          }
          value = Math.max(0.0, outer - holes);
        }
        break;
      }
      case GeometryProperty.LENGTH_OR_PERIMETER: {
        if (geom.geometryType === GeometryType.POLYLINE) {
          let totalMeters = 0.0;
          for (let i = 0; i < geom.points.length - 1; i++) {
            totalMeters += ProjectionEngine.geodesicDistanceMeters(
              geom.points[i],
              geom.points[i + 1]
            );
          }
          value = totalMeters;
        } else if (geom.geometryType === GeometryType.POLYGON) {
          let totalMeters = 0.0;
          for (const ring of geom.rings) {
            for (let i = 0; i < ring.length - 1; i++) {
              totalMeters += ProjectionEngine.geodesicDistanceMeters(ring[i], ring[i + 1]);
            }
            if (
              ring.length > 1 &&
              (ring[0].x !== ring[ring.length - 1].x || ring[0].y !== ring[ring.length - 1].y)
            ) {
              totalMeters += ProjectionEngine.geodesicDistanceMeters(
                ring[ring.length - 1],
                ring[0]
              );
            }
          }
          value = totalMeters;
        }
        break;
      }
      case GeometryProperty.CENTROID_X: {
        if (geom.geometryType === GeometryType.POINT) value = geom.x;
        else if (geom.geometryType === GeometryType.POLYGON) value = polygonCentroid(geom).x;
        else {
          const bb = getGeometryBoundingBox(geom);
          value = (bb.minX + bb.maxX) / 2.0;
        }
        break;
      }
      case GeometryProperty.CENTROID_Y: {
        if (geom.geometryType === GeometryType.POINT) value = geom.y;
        else if (geom.geometryType === GeometryType.POLYGON) value = polygonCentroid(geom).y;
        else {
          const bb = getGeometryBoundingBox(geom);
          value = (bb.minY + bb.maxY) / 2.0;
        }
        break;
      }
    }

    if (value === null) return f;
    return {
      ...f,
      attributes: {
        ...f.attributes,
        [fieldName]: Number(value.toFixed(4)),
      },
    };
  });

  return {
    ...layer,
    fields,
    features,
  };
}
