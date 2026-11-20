export enum BasemapId {
  NONE = 'none',
  OPEN_STREET_MAP = 'osm_standard',
  ESRI_STREETS = 'esri_streets',
  ESRI_IMAGERY = 'esri_satellite',
  CARTO_DARK = 'carto_dark',
  CARTO_POSITRON = 'carto_positron',
  OPEN_TOPO = 'open_topo',
}

export interface BasemapDefinition {
  id: BasemapId;
  titleEs: string;
  titleEn: string;
  descriptionEs: string;
  descriptionEn: string;
  tileUrlTemplate: string;
  overlayUrlTemplate?: string;
  minZoom: number;
  maxZoom: number;
  attribution: string;
}

export const BASEMAPS: BasemapDefinition[] = [
  {
    id: BasemapId.NONE,
    titleEs: 'Sin mapa base (Fondo vectorial)',
    titleEn: 'No Basemap (Vector Canvas)',
    descriptionEs: 'Lienzo de dibujo cartográfico en blanco/oscuro tradicional',
    descriptionEn: 'Blank classic cartographic paper background',
    tileUrlTemplate: '',
    minZoom: 1,
    maxZoom: 21,
    attribution: '',
  },
  {
    id: BasemapId.OPEN_STREET_MAP,
    titleEs: 'OpenStreetMap (Calles del Mundo)',
    titleEn: 'OpenStreetMap (World Streets)',
    descriptionEs: 'Red mundial de calles, avenidas, edificios y comercios',
    descriptionEn: 'Collaborative worldwide street and road network',
    tileUrlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    minZoom: 1,
    maxZoom: 19,
    attribution: '© OpenStreetMap contributors',
  },
  {
    id: BasemapId.ESRI_STREETS,
    titleEs: 'Esri World Street Map (ArcGIS)',
    titleEn: 'Esri World Street Map (ArcGIS)',
    descriptionEs: 'Mapa oficial de calles y vías de ArcGIS Online sin clave API',
    descriptionEn: 'Official ArcGIS Online street and highway basemap',
    tileUrlTemplate:
      'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}',
    minZoom: 1,
    maxZoom: 19,
    attribution: 'Esri, HERE, Garmin, © OpenStreetMap',
  },
  {
    id: BasemapId.ESRI_IMAGERY,
    titleEs: 'Imágenes Satelitales (Esri)',
    titleEn: 'Satellite Imagery (Esri)',
    descriptionEs: 'Fotografía satelital global de alta resolución de Esri World Imagery',
    descriptionEn: 'High-resolution global satellite imagery from Esri',
    tileUrlTemplate:
      'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
    minZoom: 1,
    maxZoom: 19,
    attribution: 'Esri, Maxar, Earthstar Geographics',
  },
  {
    id: BasemapId.CARTO_DARK,
    titleEs: 'Esri Dark Gray Canvas (Modo Oscuro)',
    titleEn: 'Esri Dark Gray Canvas (Dark)',
    descriptionEs: 'Mapa base oscuro oficial de ArcGIS para alto contraste nocturno',
    descriptionEn: 'Official ArcGIS dark gray canvas basemap for night mode',
    tileUrlTemplate:
      'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}',
    overlayUrlTemplate:
      'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Reference/MapServer/tile/{z}/{y}/{x}',
    minZoom: 1,
    maxZoom: 16,
    attribution: 'Esri, HERE, Garmin, © OpenStreetMap',
  },
  {
    id: BasemapId.CARTO_POSITRON,
    titleEs: 'Esri Light Gray Canvas (Gris Cartográfico)',
    titleEn: 'Esri Light Gray Canvas (Light)',
    descriptionEs: 'Mapa base claro minimalista de ArcGIS para resaltar capas SIG',
    descriptionEn: 'Minimalist ArcGIS light gray canvas for GIS layer overlay',
    tileUrlTemplate:
      'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}',
    overlayUrlTemplate:
      'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Reference/MapServer/tile/{z}/{y}/{x}',
    minZoom: 1,
    maxZoom: 16,
    attribution: 'Esri, HERE, Garmin, © OpenStreetMap',
  },
  {
    id: BasemapId.OPEN_TOPO,
    titleEs: 'OpenTopoMap (Topografía y Relieve)',
    titleEn: 'OpenTopoMap (Topographic)',
    descriptionEs: 'Relieve montañoso con curvas de nivel e hidrografía',
    descriptionEn: 'Mountain relief with contour lines and elevation',
    tileUrlTemplate: 'https://tile.opentopomap.org/{z}/{x}/{y}.png',
    minZoom: 1,
    maxZoom: 17,
    attribution: '© OpenTopoMap, © OpenStreetMap',
  },
];

export function getBasemapById(id: BasemapId): BasemapDefinition {
  return BASEMAPS.find((b) => b.id === id) ?? BASEMAPS[0];
}
