import { GeometryType } from '../core/geometry';
import { type VectorLayer, createVectorLayer } from '../core/model';
import {
  colorToHex,
  createSimpleSymbology,
  hexToColorRgba,
} from '../core/symbology';
import { GeoJsonParser } from './geoJsonParser';

export interface MapProject {
  name: string;
  centerLon: number;
  centerLat: number;
  zoomLevel: number;
  crsEpsg: number;
  layers: VectorLayer[];
}

export function createDefaultProject(name = 'ArcMap Document (Untitled.mxd)'): MapProject {
  return {
    name,
    centerLon: -71.9675,
    centerLat: -13.532,
    zoomLevel: 13.0,
    crsEpsg: 4326,
    layers: [],
  };
}

export const ProjectManager = {
  serializeProject(project: MapProject): string {
    const layers = project.layers.map((layer) => {
      const s = layer.symbology;
      const symbology =
        s.type === 'Simple'
          ? {
              type: 'Simple',
              fillColor: colorToHex(s.fillColor),
              fillAlpha: s.fillColor.alpha,
              strokeColor: colorToHex(s.strokeColor),
              strokeWidth: s.strokeWidthDp,
              pointSize: s.pointSizeDp,
            }
          : {
              type: 'UniqueValues',
              fieldName: s.fieldName,
              strokeColor: colorToHex(s.strokeColor),
              strokeWidth: s.strokeWidthDp,
              valueColors: Object.fromEntries(
                Object.entries(s.valueColors).map(([k, v]) => [k, colorToHex(v)])
              ),
            };

      return {
        id: layer.id,
        name: layer.name,
        geometryType: layer.geometryType,
        isVisible: layer.isVisible,
        opacity: layer.opacity,
        labelFieldName: layer.labelFieldName,
        symbology,
        data: JSON.parse(GeoJsonParser.exportToGeoJson(layer)),
      };
    });

    return JSON.stringify(
      {
        version: 1,
        name: project.name,
        centerLon: project.centerLon,
        centerLat: project.centerLat,
        zoomLevel: project.zoomLevel,
        crsEpsg: project.crsEpsg,
        layers,
      },
      null,
      2
    );
  },

  deserializeProject(jsonText: string): MapProject {
    const root = JSON.parse(jsonText) as Record<string, unknown>;
    const project: MapProject = {
      name: typeof root.name === 'string' ? root.name : 'Untitled Map',
      centerLon: typeof root.centerLon === 'number' ? root.centerLon : -71.9675,
      centerLat: typeof root.centerLat === 'number' ? root.centerLat : -13.532,
      zoomLevel: typeof root.zoomLevel === 'number' ? root.zoomLevel : 12.0,
      crsEpsg: typeof root.crsEpsg === 'number' ? root.crsEpsg : 4326,
      layers: [],
    };

    const layersArr = Array.isArray(root.layers) ? root.layers : [];
    for (let i = 0; i < layersArr.length; i++) {
      const lObj = layersArr[i] as Record<string, unknown>;
      const name = typeof lObj.name === 'string' ? lObj.name : `Layer ${i}`;
      const dataObj = lObj.data as Record<string, unknown> | undefined;

      const layer = dataObj
        ? GeoJsonParser.parse(JSON.stringify(dataObj), name)
        : createVectorLayer({
            name,
            geometryType: (lObj.geometryType as GeometryType) ?? GeometryType.POLYGON,
          });

      layer.isVisible = typeof lObj.isVisible === 'boolean' ? lObj.isVisible : true;
      layer.opacity = typeof lObj.opacity === 'number' ? lObj.opacity : 1.0;
      layer.labelFieldName =
        typeof lObj.labelFieldName === 'string' ? lObj.labelFieldName : null;

      const symObj = lObj.symbology as Record<string, unknown> | undefined;
      if (symObj && symObj.type === 'Simple') {
        const fillHex = typeof symObj.fillColor === 'string' ? symObj.fillColor : '#2196F3';
        const fillAlpha = typeof symObj.fillAlpha === 'number' ? symObj.fillAlpha : 0.5;
        const strokeHex = typeof symObj.strokeColor === 'string' ? symObj.strokeColor : '#2196F3';
        const strokeWidth = typeof symObj.strokeWidth === 'number' ? symObj.strokeWidth : 2.0;
        const pointSize = typeof symObj.pointSize === 'number' ? symObj.pointSize : 8.0;

        layer.symbology = createSimpleSymbology({
          fillColor: hexToColorRgba(fillHex, fillAlpha),
          strokeColor: hexToColorRgba(strokeHex, 1.0),
          strokeWidthDp: strokeWidth,
          pointSizeDp: pointSize,
        });
      }

      project.layers.push(layer);
    }

    return project;
  },
};
