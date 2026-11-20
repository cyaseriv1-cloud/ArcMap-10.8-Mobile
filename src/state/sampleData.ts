import {
  GeometryType,
  makePoint,
  makePolygonFromRing,
  makePolyline,
} from '../core/geometry';
import {
  FieldType,
  type VectorLayer,
  createVectorLayer,
  makeField,
} from '../core/model';
import {
  Colors,
  createSimpleSymbology,
  makeColor,
} from '../core/symbology';

export const SampleData = {
  createSampleLayers(): VectorLayer[] {
    const centerLon = -71.9675;
    const centerLat = -13.532;

    // 1. Polygon Layer: Zonificación y Sectores
    const polygonLayer = createVectorLayer({
      name: 'Zonificación y Sectores',
      geometryType: GeometryType.POLYGON,
      labelFieldName: 'NOMBRE',
      symbology: {
        type: 'UniqueValues',
        fieldName: 'CATEGORIA',
        valueColors: {
          'Centro Histórico': makeColor(255, 152, 0, 0.6),
          Residencial: makeColor(33, 150, 243, 0.5),
          'Parque Ecológico': makeColor(76, 175, 80, 0.6),
          'Zona Comercial': makeColor(156, 39, 176, 0.5),
        },
        defaultFillColor: makeColor(158, 158, 158, 0.5),
        strokeColor: Colors.BLACK,
        strokeWidthDp: 2.0,
      },
      fields: [
        makeField('ID', FieldType.INTEGER),
        makeField('NOMBRE', FieldType.STRING),
        makeField('CATEGORIA', FieldType.STRING),
        makeField('POBLACION', FieldType.INTEGER),
        makeField('ESTADO', FieldType.STRING),
      ],
      features: [
        {
          id: 1,
          geometry: makePolygonFromRing([
            makePoint(centerLon - 0.02, centerLat + 0.01),
            makePoint(centerLon + 0.01, centerLat + 0.02),
            makePoint(centerLon + 0.02, centerLat - 0.01),
            makePoint(centerLon - 0.01, centerLat - 0.02),
            makePoint(centerLon - 0.02, centerLat + 0.01),
          ]),
          attributes: {
            ID: 1,
            NOMBRE: 'Sector Centro',
            CATEGORIA: 'Centro Histórico',
            POBLACION: 42500,
            ESTADO: 'Protegido',
          },
        },
        {
          id: 2,
          geometry: makePolygonFromRing([
            makePoint(centerLon + 0.02, centerLat + 0.03),
            makePoint(centerLon + 0.05, centerLat + 0.04),
            makePoint(centerLon + 0.06, centerLat + 0.01),
            makePoint(centerLon + 0.03, centerLat + 0.0),
            makePoint(centerLon + 0.02, centerLat + 0.03),
          ]),
          attributes: {
            ID: 2,
            NOMBRE: 'Sector Noreste',
            CATEGORIA: 'Residencial',
            POBLACION: 68000,
            ESTADO: 'Urbano',
          },
        },
        {
          id: 3,
          geometry: makePolygonFromRing([
            makePoint(centerLon - 0.04, centerLat - 0.01),
            makePoint(centerLon - 0.02, centerLat - 0.01),
            makePoint(centerLon - 0.01, centerLat - 0.04),
            makePoint(centerLon - 0.04, centerLat - 0.04),
            makePoint(centerLon - 0.04, centerLat - 0.01),
          ]),
          attributes: {
            ID: 3,
            NOMBRE: 'Valle Verde',
            CATEGORIA: 'Parque Ecológico',
            POBLACION: 1200,
            ESTADO: 'Reserva',
          },
        },
      ],
    });

    // 2. Polyline Layer: Red Vial y Vías Principales de Cusco
    const polylineLayer = createVectorLayer({
      name: 'Red Vial (Calles y Avenidas)',
      geometryType: GeometryType.POLYLINE,
      labelFieldName: 'NOMBRE',
      symbology: createSimpleSymbology({
        strokeColor: makeColor(211, 47, 47),
        strokeWidthDp: 2.8,
      }),
      fields: [
        makeField('ID', FieldType.INTEGER),
        makeField('NOMBRE', FieldType.STRING),
        makeField('TIPO', FieldType.STRING),
        makeField('LONG_KM', FieldType.DOUBLE),
      ],
      features: [
        {
          id: 1,
          geometry: makePolyline([
            makePoint(-71.99, -13.525),
            makePoint(-71.9675, -13.535),
            makePoint(-71.945, -13.5385),
            makePoint(-71.93, -13.542),
            makePoint(-71.91, -13.548),
          ]),
          attributes: {
            ID: 1,
            NOMBRE: 'Vía de Evitamiento',
            TIPO: 'Vía Rápida',
            LONG_KM: 12.4,
          },
        },
        {
          id: 2,
          geometry: makePolyline([
            makePoint(-71.95, -13.535),
            makePoint(-71.948, -13.538),
            makePoint(-71.947, -13.541),
          ]),
          attributes: {
            ID: 2,
            NOMBRE: 'Av. San Antonio',
            TIPO: 'Arteria Urbana',
            LONG_KM: 1.8,
          },
        },
        {
          id: 3,
          geometry: makePolyline([
            makePoint(-71.952, -13.5365),
            makePoint(-71.946, -13.5372),
            makePoint(-71.942, -13.5378),
          ]),
          attributes: {
            ID: 3,
            NOMBRE: 'Angamos',
            TIPO: 'Calle Principal',
            LONG_KM: 1.2,
          },
        },
        {
          id: 4,
          geometry: makePolyline([
            makePoint(-71.9465, -13.536),
            makePoint(-71.944, -13.539),
          ]),
          attributes: {
            ID: 4,
            NOMBRE: 'Av. La Amistad',
            TIPO: 'Calle Colectora',
            LONG_KM: 0.9,
          },
        },
        {
          id: 5,
          geometry: makePolyline([
            makePoint(-71.951, -13.538),
            makePoint(-71.947, -13.5385),
          ]),
          attributes: {
            ID: 5,
            NOMBRE: 'C. Mexico',
            TIPO: 'Calle Local',
            LONG_KM: 0.6,
          },
        },
        {
          id: 6,
          geometry: makePolyline([
            makePoint(-71.978, -13.518),
            makePoint(-71.974, -13.522),
            makePoint(-71.968, -13.528),
          ]),
          attributes: {
            ID: 6,
            NOMBRE: 'Av. El Sol',
            TIPO: 'Avenida Turística',
            LONG_KM: 2.1,
          },
        },
        {
          id: 7,
          geometry: makePolyline([
            makePoint(-71.965, -13.527),
            makePoint(-71.94, -13.53),
            makePoint(-71.915, -13.532),
          ]),
          attributes: {
            ID: 7,
            NOMBRE: 'Av. de la Cultura',
            TIPO: 'Arteria Metropolitana',
            LONG_KM: 8.5,
          },
        },
      ],
    });

    // 3. Point Layer: Puntos de Interés / Equipamientos (Cusco)
    const pointLayer = createVectorLayer({
      name: 'Puntos de Interés (POIs)',
      geometryType: GeometryType.POINT,
      labelFieldName: 'NOMBRE',
      symbology: createSimpleSymbology({
        fillColor: makeColor(0, 150, 136),
        strokeColor: Colors.WHITE,
        strokeWidthDp: 1.8,
        pointSizeDp: 7.0,
      }),
      fields: [
        makeField('ID', FieldType.INTEGER),
        makeField('NOMBRE', FieldType.STRING),
        makeField('CATEGORIA', FieldType.STRING),
        makeField('ELEVACION', FieldType.DOUBLE),
      ],
      features: [
        {
          id: 1,
          geometry: makePoint(-71.9785, -13.5165),
          attributes: {
            ID: 1,
            NOMBRE: 'Plaza Mayor',
            CATEGORIA: 'Patrimonio',
            ELEVACION: 3399.0,
          },
        },
        {
          id: 2,
          geometry: makePoint(-71.9815, -13.508),
          attributes: {
            ID: 2,
            NOMBRE: 'Sacsayhuamán',
            CATEGORIA: 'Arqueológico',
            ELEVACION: 3701.0,
          },
        },
        {
          id: 3,
          geometry: makePoint(-71.939, -13.536),
          attributes: {
            ID: 3,
            NOMBRE: 'Aeropuerto Int. Velasco Astete',
            CATEGORIA: 'Transporte',
            ELEVACION: 3310.0,
          },
        },
        {
          id: 4,
          geometry: makePoint(-71.9468, -13.5392),
          attributes: {
            ID: 4,
            NOMBRE: 'Mercado Uvima 7',
            CATEGORIA: 'Comercio',
            ELEVACION: 3320.0,
          },
        },
        {
          id: 5,
          geometry: makePoint(-71.949, -13.536),
          attributes: {
            ID: 5,
            NOMBRE: 'Apart Hotel Las Margaritas',
            CATEGORIA: 'Hotel',
            ELEVACION: 3325.0,
          },
        },
        {
          id: 6,
          geometry: makePoint(-71.9442, -13.541),
          attributes: {
            ID: 6,
            NOMBRE: 'Paradero Las Joyas',
            CATEGORIA: 'Paradero',
            ELEVACION: 3318.0,
          },
        },
        {
          id: 7,
          geometry: makePoint(-71.9515, -13.5425),
          attributes: {
            ID: 7,
            NOMBRE: 'Cancha Sintética Don Ignacio',
            CATEGORIA: 'Deportes',
            ELEVACION: 3315.0,
          },
        },
        {
          id: 8,
          geometry: makePoint(-71.765, -13.435),
          attributes: {
            ID: 8,
            NOMBRE: 'Pachatúsan (4,842m)',
            CATEGORIA: 'Montaña',
            ELEVACION: 4842.0,
          },
        },
        {
          id: 9,
          geometry: makePoint(-71.985, -13.298),
          attributes: {
            ID: 9,
            NOMBRE: 'Sahuasiray (5,818m)',
            CATEGORIA: 'Nevado',
            ELEVACION: 5818.0,
          },
        },
      ],
    });

    return [pointLayer, polylineLayer, polygonLayer];
  },
};
