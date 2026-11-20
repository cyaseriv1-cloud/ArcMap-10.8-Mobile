import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { ProjectionEngine, type UtmCoordinate } from '../core/crs';
import {
  EMPTY_BBOX,
  type Geometry,
  GeometryType,
  type Point,
  bboxExpandBox,
  getGeometryBoundingBox,
  isBBoxEmpty,
  makePoint,
  makePolygonFromRing,
  makePolyline,
  pointsEqual,
} from '../core/geometry';
import {
  type AttributeValue,
  type Feature,
  type Field,
  FieldType,
  type GeometryProperty,
  type VectorLayer,
  calculateLayerGeometry,
  createVectorLayer,
  getLayerBoundingBox,
  makeField,
} from '../core/model';
import { SpatialIndex } from '../core/spatialIndex';
import type { LayerSymbology } from '../core/symbology';
import { GeoJsonParser } from '../data/geoJsonParser';
import { ShapefileReader } from '../data/shapefileReader';
import { ShapefileWriter } from '../data/shapefileWriter';
import { BasemapId } from '../map/basemap';
import { DigitizeSession, MapToolMode, MeasureSession } from '../map/interaction';
import { TileManager } from '../map/tileManager';
import { MapViewport } from '../map/viewport';
import { AttributeQuery } from '../tools/attributeQuery';
import { BufferTool } from '../tools/bufferTool';
import { ClipTool } from '../tools/clipTool';
import { SimplifyLineTool } from '../tools/simplifyLineTool';
import { SampleData } from './sampleData';

export enum ArcMapViewMode {
  DATA_VIEW = 'DATA_VIEW',
  LAYOUT_VIEW = 'LAYOUT_VIEW',
}

export enum ArcMapTheme {
  LIGHT = 'LIGHT',
  DARK = 'DARK',
}

export enum AppLanguage {
  ES = 'ES',
  EN = 'EN',
}

export interface WorldPlaceResult {
  displayName: string;
  lat: number;
  lon: number;
  type: string;
  category: string;
}

export interface PendingFeatureDraft {
  layerId: string;
  geometry: Geometry;
  nextId: number;
  isPointCapture: boolean;
  prefillAttributes?: Record<string, AttributeValue>;
}

function getExpectedGeometryForTool(tool: MapToolMode): GeometryType | null {
  switch (tool) {
    case MapToolMode.DIGITIZE_POINT:
      return GeometryType.POINT;
    case MapToolMode.DIGITIZE_LINE:
      return GeometryType.POLYLINE;
    case MapToolMode.DIGITIZE_POLYGON:
      return GeometryType.POLYGON;
    default:
      return null;
  }
}

function isLayerCompatibleWithTool(tool: MapToolMode, layer: VectorLayer | null): boolean {
  if (!layer) return false;
  switch (tool) {
    case MapToolMode.DIGITIZE_POINT:
      return (
        layer.geometryType === GeometryType.POINT ||
        layer.geometryType === GeometryType.MULTI_POINT
      );
    case MapToolMode.DIGITIZE_LINE:
      return (
        layer.geometryType === GeometryType.POLYLINE ||
        layer.geometryType === GeometryType.MULTI_POLYLINE
      );
    case MapToolMode.DIGITIZE_POLYGON:
      return (
        layer.geometryType === GeometryType.POLYGON ||
        layer.geometryType === GeometryType.MULTI_POLYGON
      );
    default:
      return true;
  }
}

interface ArcMapContextValue {
  theme: ArcMapTheme;
  language: AppLanguage;
  currentBasemap: BasemapId;
  isBasemapDialogVisible: boolean;
  worldSearchResults: WorldPlaceResult[];
  isSearchingWorld: boolean;
  gpsLocation: Point | null;
  gpsAccuracy: number | null;
  isSearchVisible: boolean;
  viewMode: ArcMapViewMode;
  isCatalogVisible: boolean;
  isNewLayerDialogVisible: boolean;
  newLayerDefaultGeomType: GeometryType;
  pendingFeatureDraft: PendingFeatureDraft | null;
  layoutTitle: string;
  isEditorActive: boolean;
  layers: VectorLayer[];
  activeLayer: VectorLayer | null;
  activeTool: MapToolMode;
  viewport: MapViewport;
  tileManager: TileManager;
  measureSession: MeasureSession;
  digitizeSession: DigitizeSession;
  identifiedFeature: { layer: VectorLayer; feature: Feature } | null;
  selectedLayerForTable: VectorLayer | null;
  isAttributeTableVisible: boolean;
  isToolboxVisible: boolean;
  isTocVisible: boolean;
  cursorUtm: UtmCoordinate;
  mapScale: number;
  statusMessage: string;
  renderVersion: number;

  updateRender: () => void;
  loadSampleData: () => void;
  setTool: (tool: MapToolMode) => void;
  handleMapTap: (screenX: number, screenY: number) => void;
  undoMeasurePoint: () => void;
  clearMeasureSession: () => void;
  undoDigitizePoint: () => void;
  clearDigitizeSession: () => void;
  completeDigitizing: () => void;
  commitPendingFeature: (attributes: Record<string, AttributeValue>) => void;
  cancelPendingFeature: () => void;
  openNewLayerDialog: (defaultGeom?: GeometryType, resumeTool?: MapToolMode | null) => void;
  closeNewLayerDialog: () => void;
  createNewLayer: (name: string, geometryType: GeometryType, fields: Field[]) => void;
  updateFeatureAttributes: (
    layerId: string,
    featureId: number,
    attributes: Record<string, AttributeValue>
  ) => void;
  deleteFeature: (layerId: string, featureId: number) => void;
  deleteSelectedFeatures: (layerId: string) => void;
  addFieldToLayer: (layerId: string, fieldName: string, fieldType: FieldType) => boolean;
  updateCoordinates: (lon: number, lat: number) => void;
  zoomIn: () => void;
  zoomOut: () => void;
  zoomAround: (focalX: number, focalY: number, delta: number) => void;
  zoomFullExtent: () => void;
  zoomToExtentOfLayer: (layer: VectorLayer) => void;
  zoomToFeature: (feature: Feature) => void;
  zoomToLocation: (lat: number, lon: number, zoomLevel?: number) => void;
  zoomToScale: (scaleDenominator: number) => void;
  toggleLayerVisibility: (layerId: string) => void;
  setLayerOpacity: (layerId: string, opacity: number) => void;
  moveLayerUp: (layerId: string) => void;
  moveLayerDown: (layerId: string) => void;
  removeLayer: (layerId: string) => void;
  addLayer: (layer: VectorLayer) => void;
  setActiveLayer: (layer: VectorLayer) => void;
  openAttributeTable: (layer: VectorLayer) => void;
  closeAttributeTable: () => void;
  toggleToolbox: (visible: boolean) => void;
  toggleToc: (visible: boolean) => void;
  toggleCatalog: (visible: boolean) => void;
  toggleSearch: (visible: boolean) => void;
  openBasemapDialog: () => void;
  closeBasemapDialog: () => void;
  setBasemap: (type: BasemapId) => void;
  dismissIdentify: () => void;
  toggleTheme: () => void;
  toggleLanguage: () => void;
  setViewMode: (mode: ArcMapViewMode) => void;
  setLayoutTitle: (title: string) => void;
  toggleEditor: (active: boolean) => void;
  clearAllSelections: () => void;
  clearAllLayers: () => void;
  selectFeatureInLayer: (layerId: string, featureId: number, append?: boolean) => void;
  deselectFeatureInLayer: (layerId: string, featureId: number) => void;
  selectAllInLayer: (layerId: string) => void;
  clearLayerSelection: (layerId: string) => void;
  requestGpsAndCenter: () => void;
  requestGpsAndCapture: () => void;
  searchPlaces: (query: string) => Array<{ layer: VectorLayer; feature: Feature }>;
  searchWorldPlaces: (query: string) => void;
  clearWorldSearchResults: () => void;
  runBuffer: (layer: VectorLayer, distanceMeters: number) => void;
  runClip: (layer: VectorLayer) => void;
  runSimplify: (layer: VectorLayer, toleranceMeters: number) => void;
  runSelectByAttributes: (layer: VectorLayer, whereClause: string) => void;
  runCalculateGeometry: (layer: VectorLayer, fieldName: string, property: GeometryProperty) => void;
  setLayerSymbology: (layerId: string, symbology: LayerSymbology) => void;
  setLayerLabelField: (layerId: string, fieldName: string | null) => void;
  loadGeoJsonText: (name: string, text: string) => boolean;
  loadShapefileBuffers: (
    name: string,
    shpBuffer: ArrayBuffer,
    dbfBuffer?: ArrayBuffer | null
  ) => boolean;
  exportActiveLayerAsGeoJson: () => void;
  exportLayerAsShapefile: (layer: VectorLayer) => Promise<void>;
}

const ArcMapContext = createContext<ArcMapContextValue | null>(null);

export const ArcMapProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const viewportRef = useRef<MapViewport>(new MapViewport(-71.9675, -13.532, 13.0));
  const tileManagerRef = useRef<TileManager>(new TileManager());
  const searchAbortRef = useRef<AbortController | null>(null);
  const gpsWatchIdRef = useRef<number | null>(null);

  const [theme, setTheme] = useState<ArcMapTheme>(ArcMapTheme.LIGHT);
  const [language, setLanguage] = useState<AppLanguage>(AppLanguage.ES);
  const [currentBasemap, setCurrentBasemap] = useState<BasemapId>(BasemapId.OPEN_STREET_MAP);
  const [isBasemapDialogVisible, setIsBasemapDialogVisible] = useState(false);
  const [worldSearchResults, setWorldSearchResults] = useState<WorldPlaceResult[]>([]);
  const [isSearchingWorld, setIsSearchingWorld] = useState(false);
  const [gpsLocation, setGpsLocation] = useState<Point | null>(null);
  const [gpsAccuracy, setGpsAccuracy] = useState<number | null>(null);
  const [isSearchVisible, setIsSearchVisible] = useState(false);
  const [viewMode, setViewModeState] = useState<ArcMapViewMode>(ArcMapViewMode.DATA_VIEW);
  const [isCatalogVisible, setIsCatalogVisible] = useState(false);

  const [isNewLayerDialogVisible, setIsNewLayerDialogVisible] = useState(false);
  const [newLayerDefaultGeomType, setNewLayerDefaultGeomType] = useState<GeometryType>(
    GeometryType.POLYGON
  );
  const [pendingDigitizeToolAfterCreate, setPendingDigitizeToolAfterCreate] =
    useState<MapToolMode | null>(null);
  const [pendingFeatureDraft, setPendingFeatureDraft] = useState<PendingFeatureDraft | null>(
    null
  );

  const [layoutTitle, setLayoutTitleState] = useState('MAPA GENERAL DE COBERTURA');
  const [isEditorActive, setIsEditorActive] = useState(false);
  const [layers, setLayers] = useState<VectorLayer[]>([]);
  const [activeLayerId, setActiveLayerId] = useState<string | null>(null);
  const [activeTool, setActiveTool] = useState<MapToolMode>(MapToolMode.PAN);

  const [measureSession, setMeasureSession] = useState<MeasureSession>(
    () => new MeasureSession()
  );
  const [digitizeSession, setDigitizeSession] = useState<DigitizeSession>(
    () => new DigitizeSession()
  );

  const [identifiedFeatureIds, setIdentifiedFeatureIds] = useState<{
    layerId: string;
    featureId: number;
  } | null>(null);
  const [tableLayerId, setTableLayerId] = useState<string | null>(null);
  const [isAttributeTableVisible, setIsAttributeTableVisible] = useState(false);
  const [isToolboxVisible, setIsToolboxVisible] = useState(false);
  const [isTocVisible, setIsTocVisible] = useState(false);
  const [cursorUtm, setCursorUtm] = useState<UtmCoordinate>(() =>
    ProjectionEngine.wgs84ToUtm(viewportRef.current.centerLat, viewportRef.current.centerLon)
  );
  const [mapScale, setMapScale] = useState<number>(viewportRef.current.scaleDenominator);
  const [statusMessage, setStatusMessage] = useState<string>(
    'Listo · Selecciona un Mapa Base para comenzar'
  );
  const [renderVersion, setRenderVersion] = useState(0);

  const updateRender = useCallback(() => {
    setRenderVersion((v) => v + 1);
  }, []);

  useEffect(() => {
    tileManagerRef.current.onTileLoaded = () => {
      setRenderVersion((v) => v + 1);
    };
    return () => {
      tileManagerRef.current.onTileLoaded = null;
      if (gpsWatchIdRef.current !== null && 'geolocation' in navigator) {
        navigator.geolocation.clearWatch(gpsWatchIdRef.current);
        gpsWatchIdRef.current = null;
      }
      searchAbortRef.current?.abort();
    };
  }, []);

  const activeLayer = useMemo(
    () => layers.find((l) => l.id === activeLayerId) ?? null,
    [layers, activeLayerId]
  );

  const selectedLayerForTable = useMemo(
    () => layers.find((l) => l.id === tableLayerId) ?? null,
    [layers, tableLayerId]
  );

  const identifiedFeature = useMemo(() => {
    if (!identifiedFeatureIds) return null;
    const l = layers.find((layer) => layer.id === identifiedFeatureIds.layerId);
    if (!l) return null;
    const f = l.features.find((feat) => feat.id === identifiedFeatureIds.featureId);
    if (!f) return null;
    return { layer: l, feature: f };
  }, [layers, identifiedFeatureIds]);

  const updateCoordinates = useCallback((lon: number, lat: number) => {
    setCursorUtm(ProjectionEngine.wgs84ToUtm(lat, lon));
    setMapScale(viewportRef.current.scaleDenominator);
  }, []);

  const addLayer = useCallback(
    (layer: VectorLayer) => {
      setLayers((prev) => [layer, ...prev]);
      setActiveLayerId(layer.id);
      setStatusMessage(`Capa añadida: ${layer.name}`);
      updateRender();
    },
    [updateRender]
  );

  const openNewLayerDialog = useCallback(
    (defaultGeom = GeometryType.POLYGON, resumeTool: MapToolMode | null = null) => {
      setNewLayerDefaultGeomType(defaultGeom);
      setPendingDigitizeToolAfterCreate(resumeTool);
      setIsNewLayerDialogVisible(true);
    },
    []
  );

  const closeNewLayerDialog = useCallback(() => {
    setIsNewLayerDialogVisible(false);
    setPendingDigitizeToolAfterCreate(null);
  }, []);

  const createNewLayer = useCallback(
    (name: string, geometryType: GeometryType, fields: Field[]) => {
      const cleanName = name.trim() || `Capa Nueva ${layers.length + 1}`;
      const normalizedFields =
        fields.length > 0
          ? fields
          : [makeField('ID', FieldType.INTEGER), makeField('NOMBRE', FieldType.STRING)];

      const newLayer = createVectorLayer({
        name: cleanName,
        geometryType,
        fields: normalizedFields,
      });

      setLayers((prev) => [newLayer, ...prev]);
      setActiveLayerId(newLayer.id);
      setIsNewLayerDialogVisible(false);

      if (
        pendingDigitizeToolAfterCreate &&
        isLayerCompatibleWithTool(pendingDigitizeToolAfterCreate, newLayer)
      ) {
        const resume = pendingDigitizeToolAfterCreate;
        setActiveTool(resume);
        setDigitizeSession(
          new DigitizeSession(
            [],
            newLayer.id,
            resume === MapToolMode.DIGITIZE_POLYGON
          )
        );
        setStatusMessage(`Capa '${cleanName}' creada. Listo para digitalizar.`);
      } else {
        setStatusMessage(`Capa '${cleanName}' creada (${geometryType})`);
      }

      setPendingDigitizeToolAfterCreate(null);
      updateRender();
    },
    [layers.length, pendingDigitizeToolAfterCreate, updateRender]
  );

  const loadSampleData = useCallback(() => {
    const sample = SampleData.createSampleLayers();
    setLayers(sample);
    setActiveLayerId(sample[0]?.id ?? null);
    viewportRef.current.centerLon = -71.9675;
    viewportRef.current.centerLat = -13.532;
    viewportRef.current.zoom = 13.0;
    updateCoordinates(-71.9675, -13.532);
    setStatusMessage(`Capas de muestra cargadas (${sample.length} capas)`);
    updateRender();
  }, [updateCoordinates, updateRender]);

  const setTool = useCallback(
    (tool: MapToolMode) => {
      const expectedGeom = getExpectedGeometryForTool(tool);
      if (expectedGeom !== null && !isLayerCompatibleWithTool(tool, activeLayer)) {
        const msg = !activeLayer
          ? language === AppLanguage.ES
            ? 'No hay capa activa. Crea una capa compatible para digitalizar.'
            : 'No active layer. Create a compatible layer to digitize.'
          : language === AppLanguage.ES
            ? `La capa '${activeLayer.name}' (${activeLayer.geometryType}) no es compatible. Crea una capa ${expectedGeom}.`
            : `Layer '${activeLayer.name}' (${activeLayer.geometryType}) is incompatible. Create a ${expectedGeom} layer.`;
        setStatusMessage(msg);
        openNewLayerDialog(expectedGeom, tool);
        return;
      }

      const wasMeasuring =
        activeTool === MapToolMode.MEASURE_DISTANCE ||
        activeTool === MapToolMode.MEASURE_AREA;
      const isNowMeasuring =
        tool === MapToolMode.MEASURE_DISTANCE || tool === MapToolMode.MEASURE_AREA;

      setActiveTool(tool);

      if (isNowMeasuring) {
        const areaMode = tool === MapToolMode.MEASURE_AREA;
        setMeasureSession((prev) =>
          wasMeasuring ? prev.withAreaMode(areaMode) : new MeasureSession([], areaMode)
        );
      }

      if (
        tool === MapToolMode.DIGITIZE_POINT ||
        tool === MapToolMode.DIGITIZE_LINE ||
        tool === MapToolMode.DIGITIZE_POLYGON
      ) {
        setDigitizeSession(
          new DigitizeSession(
            [],
            activeLayer?.id ?? null,
            tool === MapToolMode.DIGITIZE_POLYGON
          )
        );
      }

      setStatusMessage(`Herramienta activa: ${tool}`);
      updateRender();
    },
    [activeLayer, activeTool, language, openNewLayerDialog, updateRender]
  );

  const undoMeasurePoint = useCallback(() => {
    setMeasureSession((prev) => prev.removeLastPoint());
    updateRender();
  }, [updateRender]);

  const clearMeasureSession = useCallback(() => {
    setMeasureSession((prev) => prev.clear());
    updateRender();
  }, [updateRender]);

  const undoDigitizePoint = useCallback(() => {
    setDigitizeSession((prev) => prev.removeLast());
    updateRender();
  }, [updateRender]);

  const clearDigitizeSession = useCallback(() => {
    setDigitizeSession((prev) => new DigitizeSession([], prev.targetLayerId, prev.isPolygon));
    updateRender();
  }, [updateRender]);

  const performIdentify = useCallback(
    (tapPoint: Point) => {
      const tolerance = (viewportRef.current.metersPerPixel * 24.0) / 111320.0;
      for (const layer of layers) {
        if (!layer.isVisible) continue;
        const index = new SpatialIndex();
        index.build(layer.features);
        const found = index.hitTest(tapPoint, tolerance);
        if (found) {
          setIdentifiedFeatureIds({ layerId: layer.id, featureId: found.id });
          setActiveLayerId(layer.id);
          setStatusMessage(`Identificado: ${layer.name} (ID: ${found.id})`);
          updateRender();
          return;
        }
      }
      setIdentifiedFeatureIds(null);
      setStatusMessage('Ninguna entidad en esa posición');
      updateRender();
    },
    [layers, updateRender]
  );

  const performSelect = useCallback(
    (tapPoint: Point) => {
      const tolerance = (viewportRef.current.metersPerPixel * 24.0) / 111320.0;

      if (activeLayer && activeLayer.isVisible) {
        const index = new SpatialIndex();
        index.build(activeLayer.features);
        const found = index.hitTest(tapPoint, tolerance);
        if (found) {
          const nextSel = new Set(activeLayer.selectedFeatureIds);
          if (nextSel.has(found.id)) {
            nextSel.delete(found.id);
          } else {
            nextSel.add(found.id);
          }
          setLayers((prev) =>
            prev.map((l) =>
              l.id === activeLayer.id ? { ...l, selectedFeatureIds: nextSel } : l
            )
          );
          setStatusMessage(`${nextSel.size} seleccionados en ${activeLayer.name}`);
          updateRender();
          return;
        }
      }

      for (const layer of layers) {
        if (!layer.isVisible || layer.id === activeLayer?.id) continue;
        const index = new SpatialIndex();
        index.build(layer.features);
        const found = index.hitTest(tapPoint, tolerance);
        if (found) {
          setActiveLayerId(layer.id);
          const nextSel = new Set<number>([found.id]);
          setLayers((prev) =>
            prev.map((l) =>
              l.id === layer.id ? { ...l, selectedFeatureIds: nextSel } : l
            )
          );
          setStatusMessage(`1 seleccionado en ${layer.name} (ID: ${found.id})`);
          updateRender();
          return;
        }
      }

      setLayers((prev) =>
        prev.map((l) =>
          l.selectedFeatureIds.size > 0
            ? { ...l, selectedFeatureIds: new Set<number>() }
            : l
        )
      );
      setStatusMessage('Selección limpiada');
      updateRender();
    },
    [activeLayer, layers, updateRender]
  );

  const beginPointCaptureDraft = useCallback(
    (p: Point) => {
      if (!isLayerCompatibleWithTool(MapToolMode.DIGITIZE_POINT, activeLayer)) {
        openNewLayerDialog(GeometryType.POINT, MapToolMode.DIGITIZE_POINT);
        return;
      }
      const target = activeLayer!;
      const nextId = target.features.reduce((max, f) => Math.max(max, f.id), 0) + 1;
      setPendingFeatureDraft({
        layerId: target.id,
        geometry: p,
        nextId,
        isPointCapture: true,
      });
    },
    [activeLayer, openNewLayerDialog]
  );

  const handleMapTap = useCallback(
    (screenX: number, screenY: number) => {
      const geoPoint = viewportRef.current.screenToGeo(screenX, screenY);
      updateCoordinates(geoPoint.x, geoPoint.y);

      switch (activeTool) {
        case MapToolMode.PAN:
          if (identifiedFeatureIds) {
            setIdentifiedFeatureIds(null);
            updateRender();
          }
          break;
        case MapToolMode.IDENTIFY:
          performIdentify(geoPoint);
          break;
        case MapToolMode.SELECT:
          performSelect(geoPoint);
          break;
        case MapToolMode.MEASURE_DISTANCE:
        case MapToolMode.MEASURE_AREA:
          setMeasureSession((prev) => prev.addPoint(geoPoint));
          updateRender();
          break;
        case MapToolMode.DIGITIZE_POINT:
          beginPointCaptureDraft(geoPoint);
          break;
        case MapToolMode.DIGITIZE_LINE:
        case MapToolMode.DIGITIZE_POLYGON: {
          if (!isLayerCompatibleWithTool(activeTool, activeLayer)) {
            const expected = getExpectedGeometryForTool(activeTool) ?? GeometryType.POLYGON;
            openNewLayerDialog(expected, activeTool);
            return;
          }
          setDigitizeSession((prev) => {
            const next = prev.addPoint(geoPoint);
            next.targetLayerId = activeLayer?.id ?? prev.targetLayerId;
            next.isPolygon = activeTool === MapToolMode.DIGITIZE_POLYGON;
            return next;
          });
          updateRender();
          break;
        }
      }
    },
    [
      activeLayer,
      activeTool,
      beginPointCaptureDraft,
      identifiedFeatureIds,
      openNewLayerDialog,
      performIdentify,
      performSelect,
      updateCoordinates,
      updateRender,
    ]
  );

  const completeDigitizing = useCallback(() => {
    const pts = digitizeSession.points;
    const target = layers.find((l) => l.id === digitizeSession.targetLayerId) ?? activeLayer;

    if (!target || !isLayerCompatibleWithTool(activeTool, target)) {
      const expected = getExpectedGeometryForTool(activeTool) ?? GeometryType.POLYGON;
      openNewLayerDialog(expected, activeTool);
      return;
    }

    let geom: Geometry | null = null;
    if (activeTool === MapToolMode.DIGITIZE_LINE && pts.length >= 2) {
      geom = makePolyline([...pts]);
    } else if (activeTool === MapToolMode.DIGITIZE_POLYGON && pts.length >= 3) {
      const closed = !pointsEqual(pts[0], pts[pts.length - 1]) ? [...pts, pts[0]] : [...pts];
      geom = makePolygonFromRing(closed);
    }

    if (!geom) return;

    const nextId = target.features.reduce((max, f) => Math.max(max, f.id), 0) + 1;
    setPendingFeatureDraft({
      layerId: target.id,
      geometry: geom,
      nextId,
      isPointCapture: false,
    });
  }, [activeLayer, activeTool, digitizeSession, layers, openNewLayerDialog]);

  const commitPendingFeature = useCallback(
    (attributes: Record<string, AttributeValue>) => {
      if (!pendingFeatureDraft) return;
      const { layerId, geometry, nextId, isPointCapture } = pendingFeatureDraft;

      const newFeat: Feature = {
        id: nextId,
        geometry,
        attributes: {
          ID: nextId,
          ...attributes,
        },
      };

      setLayers((prev) =>
        prev.map((l) =>
          l.id === layerId
            ? {
                ...l,
                features: [...l.features, newFeat],
              }
            : l
        )
      );

      const targetName = layers.find((l) => l.id === layerId)?.name ?? 'Capa';
      setPendingFeatureDraft(null);
      if (!isPointCapture) {
        setDigitizeSession(new DigitizeSession());
        setActiveTool(MapToolMode.PAN);
      }
      setStatusMessage(`✓ Entidad #${nextId} guardada en '${targetName}'`);
      updateRender();
    },
    [layers, pendingFeatureDraft, updateRender]
  );

  const cancelPendingFeature = useCallback(() => {
    setPendingFeatureDraft(null);
  }, []);

  const updateFeatureAttributes = useCallback(
    (layerId: string, featureId: number, attributes: Record<string, AttributeValue>) => {
      setLayers((prev) =>
        prev.map((l) =>
          l.id === layerId
            ? {
                ...l,
                features: l.features.map((f) =>
                  f.id === featureId ? { ...f, attributes: { ...attributes } } : f
                ),
              }
            : l
        )
      );
      setStatusMessage(`✓ Atributos actualizados (ID: ${featureId})`);
      updateRender();
    },
    [updateRender]
  );

  const deleteFeature = useCallback(
    (layerId: string, featureId: number) => {
      setLayers((prev) =>
        prev.map((l) => {
          if (l.id !== layerId) return l;
          const nextSel = new Set(l.selectedFeatureIds);
          nextSel.delete(featureId);
          return {
            ...l,
            selectedFeatureIds: nextSel,
            features: l.features.filter((f) => f.id !== featureId),
          };
        })
      );
      if (
        identifiedFeatureIds &&
        identifiedFeatureIds.layerId === layerId &&
        identifiedFeatureIds.featureId === featureId
      ) {
        setIdentifiedFeatureIds(null);
      }
      setStatusMessage(`Entidad #${featureId} eliminada`);
      updateRender();
    },
    [identifiedFeatureIds, updateRender]
  );

  const deleteSelectedFeatures = useCallback(
    (layerId: string) => {
      const target = layers.find((l) => l.id === layerId);
      if (!target || target.selectedFeatureIds.size === 0) return;
      const count = target.selectedFeatureIds.size;
      const toRemove = new Set(target.selectedFeatureIds);

      setLayers((prev) =>
        prev.map((l) =>
          l.id === layerId
            ? {
                ...l,
                selectedFeatureIds: new Set<number>(),
                features: l.features.filter((f) => !toRemove.has(f.id)),
              }
            : l
        )
      );
      if (
        identifiedFeatureIds &&
        identifiedFeatureIds.layerId === layerId &&
        toRemove.has(identifiedFeatureIds.featureId)
      ) {
        setIdentifiedFeatureIds(null);
      }
      setStatusMessage(`${count} entidades eliminadas`);
      updateRender();
    },
    [identifiedFeatureIds, layers, updateRender]
  );

  const addFieldToLayer = useCallback(
    (layerId: string, fieldName: string, fieldType: FieldType): boolean => {
      const clean = fieldName.trim();
      if (!clean) return false;
      const target = layers.find((l) => l.id === layerId);
      if (!target) return false;
      if (target.fields.some((f) => f.name.toLowerCase() === clean.toLowerCase())) {
        setStatusMessage(`El campo '${clean}' ya existe`);
        return false;
      }

      const newField = makeField(clean, fieldType);
      setLayers((prev) =>
        prev.map((l) =>
          l.id === layerId
            ? {
                ...l,
                fields: [...l.fields, newField],
                features: l.features.map((f) => ({
                  ...f,
                  attributes: {
                    ...f.attributes,
                    [clean]: f.attributes[clean] ?? null,
                  },
                })),
              }
            : l
        )
      );
      setStatusMessage(`✓ Campo '${clean}' añadido a ${target.name}`);
      updateRender();
      return true;
    },
    [layers, updateRender]
  );

  const zoomIn = useCallback(() => {
    viewportRef.current.zoomBy(1.0);
    updateCoordinates(viewportRef.current.centerLon, viewportRef.current.centerLat);
    updateRender();
  }, [updateCoordinates, updateRender]);

  const zoomOut = useCallback(() => {
    viewportRef.current.zoomBy(-1.0);
    updateCoordinates(viewportRef.current.centerLon, viewportRef.current.centerLat);
    updateRender();
  }, [updateCoordinates, updateRender]);

  const zoomAround = useCallback(
    (focalX: number, focalY: number, delta: number) => {
      viewportRef.current.zoomAround(focalX, focalY, delta);
      updateCoordinates(viewportRef.current.centerLon, viewportRef.current.centerLat);
      updateRender();
    },
    [updateCoordinates, updateRender]
  );

  const zoomFullExtent = useCallback(() => {
    let overallBox = EMPTY_BBOX;
    for (const l of layers) {
      if (l.isVisible) {
        overallBox = bboxExpandBox(overallBox, getLayerBoundingBox(l));
      }
    }
    if (!isBBoxEmpty(overallBox)) {
      viewportRef.current.zoomToExtent(overallBox);
      updateCoordinates(viewportRef.current.centerLon, viewportRef.current.centerLat);
      updateRender();
      setStatusMessage('Extensión completa');
    }
  }, [layers, updateCoordinates, updateRender]);

  const zoomToExtentOfLayer = useCallback(
    (layer: VectorLayer) => {
      const box = getLayerBoundingBox(layer);
      if (!isBBoxEmpty(box)) {
        viewportRef.current.zoomToExtent(box);
        updateCoordinates(viewportRef.current.centerLon, viewportRef.current.centerLat);
        updateRender();
      }
    },
    [updateCoordinates, updateRender]
  );

  const zoomToFeature = useCallback(
    (feature: Feature) => {
      const geom = feature.geometry;
      if (!geom) return;
      if (geom.geometryType === GeometryType.POINT) {
        viewportRef.current.centerLon = geom.x;
        viewportRef.current.centerLat = geom.y;
        viewportRef.current.zoom = 16.5;
      } else {
        viewportRef.current.zoomToExtent(getGeometryBoundingBox(geom));
      }
      updateCoordinates(viewportRef.current.centerLon, viewportRef.current.centerLat);
      updateRender();
      const label = feature.attributes['NOMBRE'];
      setStatusMessage(`Navegado a: ${label ? String(label) : `Entidad ${feature.id}`}`);
    },
    [updateCoordinates, updateRender]
  );

  const zoomToLocation = useCallback(
    (lat: number, lon: number, zoomLevel = 14.5) => {
      viewportRef.current.centerLat = lat;
      viewportRef.current.centerLon = lon;
      viewportRef.current.zoom = zoomLevel;
      updateCoordinates(lon, lat);
      updateRender();
      setStatusMessage(
        language === AppLanguage.ES
          ? `Navegando a: ${lat.toFixed(4)}, ${lon.toFixed(4)}`
          : `Navigated to: ${lat.toFixed(4)}, ${lon.toFixed(4)}`
      );
    },
    [language, updateCoordinates, updateRender]
  );

  const zoomToScale = useCallback(
    (scaleDenominator: number) => {
      if (scaleDenominator <= 0) return;
      viewportRef.current.setScale(scaleDenominator);
      updateCoordinates(viewportRef.current.centerLon, viewportRef.current.centerLat);
      updateRender();
    },
    [updateCoordinates, updateRender]
  );

  const toggleLayerVisibility = useCallback(
    (layerId: string) => {
      setLayers((prev) =>
        prev.map((l) => (l.id === layerId ? { ...l, isVisible: !l.isVisible } : l))
      );
      updateRender();
    },
    [updateRender]
  );

  const setLayerOpacity = useCallback(
    (layerId: string, opacity: number) => {
      const clamped = Math.max(0.05, Math.min(1.0, opacity));
      setLayers((prev) => prev.map((l) => (l.id === layerId ? { ...l, opacity: clamped } : l)));
      updateRender();
    },
    [updateRender]
  );

  const moveLayerUp = useCallback(
    (layerId: string) => {
      setLayers((prev) => {
        const idx = prev.findIndex((l) => l.id === layerId);
        if (idx <= 0) return prev;
        const copy = [...prev];
        const [item] = copy.splice(idx, 1);
        copy.splice(idx - 1, 0, item);
        return copy;
      });
      updateRender();
    },
    [updateRender]
  );

  const moveLayerDown = useCallback(
    (layerId: string) => {
      setLayers((prev) => {
        const idx = prev.findIndex((l) => l.id === layerId);
        if (idx < 0 || idx >= prev.length - 1) return prev;
        const copy = [...prev];
        const [item] = copy.splice(idx, 1);
        copy.splice(idx + 1, 0, item);
        return copy;
      });
      updateRender();
    },
    [updateRender]
  );

  const removeLayer = useCallback(
    (layerId: string) => {
      setLayers((prev) => {
        const next = prev.filter((l) => l.id !== layerId);
        if (activeLayerId === layerId) {
          setActiveLayerId(next[0]?.id ?? null);
        }
        return next;
      });
      setStatusMessage('Capa eliminada');
      updateRender();
    },
    [activeLayerId, updateRender]
  );

  const setActiveLayer = useCallback((layer: VectorLayer) => {
    setActiveLayerId(layer.id);
  }, []);

  const openAttributeTable = useCallback((layer: VectorLayer) => {
    setTableLayerId(layer.id);
    setIsAttributeTableVisible(true);
  }, []);

  const closeAttributeTable = useCallback(() => {
    setIsAttributeTableVisible(false);
  }, []);

  const toggleTheme = useCallback(() => {
    setTheme((prev) => {
      const next = prev === ArcMapTheme.LIGHT ? ArcMapTheme.DARK : ArcMapTheme.LIGHT;
      setCurrentBasemap((bm) => {
        if (
          next === ArcMapTheme.DARK &&
          (bm === BasemapId.OPEN_STREET_MAP ||
            bm === BasemapId.ESRI_STREETS ||
            bm === BasemapId.CARTO_POSITRON)
        ) {
          return BasemapId.CARTO_DARK;
        }
        if (next === ArcMapTheme.LIGHT && bm === BasemapId.CARTO_DARK) {
          return BasemapId.OPEN_STREET_MAP;
        }
        return bm;
      });
      setStatusMessage(
        next === ArcMapTheme.DARK ? 'Tema Oscuro activado' : 'Tema Claro activado'
      );
      return next;
    });
    updateRender();
  }, [updateRender]);

  const toggleLanguage = useCallback(() => {
    setLanguage((prev) => {
      const next = prev === AppLanguage.ES ? AppLanguage.EN : AppLanguage.ES;
      setStatusMessage(next === AppLanguage.ES ? 'Idioma: Español' : 'Language: English');
      return next;
    });
  }, []);

  const setBasemap = useCallback(
    (type: BasemapId) => {
      setCurrentBasemap(type);
      setStatusMessage(
        language === AppLanguage.ES ? `Mapa base activo: ${type}` : `Active basemap: ${type}`
      );
      updateRender();
    },
    [language, updateRender]
  );

  const setViewMode = useCallback(
    (mode: ArcMapViewMode) => {
      setViewModeState(mode);
      setStatusMessage(
        mode === ArcMapViewMode.DATA_VIEW
          ? 'Modo: Data View (Edición Geográfica)'
          : 'Modo: Layout View (Composición de Impresión)'
      );
      updateRender();
    },
    [updateRender]
  );

  const setLayoutTitle = useCallback(
    (title: string) => {
      setLayoutTitleState(title);
      updateRender();
    },
    [updateRender]
  );

  const toggleEditor = useCallback(
    (active: boolean) => {
      setIsEditorActive(active);
      if (
        !active &&
        (activeTool === MapToolMode.DIGITIZE_POINT ||
          activeTool === MapToolMode.DIGITIZE_LINE ||
          activeTool === MapToolMode.DIGITIZE_POLYGON)
      ) {
        setActiveTool(MapToolMode.PAN);
        setDigitizeSession(new DigitizeSession());
      }
      setStatusMessage(
        active ? 'Editor iniciado: Listo para digitalizar' : 'Editor detenido'
      );
      updateRender();
    },
    [activeTool, updateRender]
  );

  const clearAllSelections = useCallback(() => {
    setLayers((prev) =>
      prev.map((l) =>
        l.selectedFeatureIds.size > 0
          ? { ...l, selectedFeatureIds: new Set<number>() }
          : l
      )
    );
    setStatusMessage('Selección limpiada');
    updateRender();
  }, [updateRender]);

  const clearAllLayers = useCallback(() => {
    setLayers([]);
    setActiveLayerId(null);
    setIdentifiedFeatureIds(null);
    setIsAttributeTableVisible(false);
    setStatusMessage(
      language === AppLanguage.ES
        ? '✓ Todas las capas eliminadas. Mapa limpio.'
        : '✓ All layers cleared.'
    );
    updateRender();
  }, [language, updateRender]);

  const selectFeatureInLayer = useCallback(
    (layerId: string, featureId: number, append = false) => {
      setLayers((prev) =>
        prev.map((l) => {
          if (l.id !== layerId) return l;
          const nextSel = append ? new Set(l.selectedFeatureIds) : new Set<number>();
          nextSel.add(featureId);
          return { ...l, selectedFeatureIds: nextSel };
        })
      );
      updateRender();
    },
    [updateRender]
  );

  const deselectFeatureInLayer = useCallback(
    (layerId: string, featureId: number) => {
      setLayers((prev) =>
        prev.map((l) => {
          if (l.id !== layerId || !l.selectedFeatureIds.has(featureId)) return l;
          const nextSel = new Set(l.selectedFeatureIds);
          nextSel.delete(featureId);
          return { ...l, selectedFeatureIds: nextSel };
        })
      );
      updateRender();
    },
    [updateRender]
  );

  const selectAllInLayer = useCallback(
    (layerId: string) => {
      setLayers((prev) =>
        prev.map((l) =>
          l.id === layerId
            ? { ...l, selectedFeatureIds: new Set(l.features.map((f) => f.id)) }
            : l
        )
      );
      updateRender();
    },
    [updateRender]
  );

  const clearLayerSelection = useCallback(
    (layerId: string) => {
      setLayers((prev) =>
        prev.map((l) =>
          l.id === layerId && l.selectedFeatureIds.size > 0
            ? { ...l, selectedFeatureIds: new Set<number>() }
            : l
        )
      );
      updateRender();
    },
    [updateRender]
  );

  const ensureGpsWatchStarted = useCallback(() => {
    if (!('geolocation' in navigator) || gpsWatchIdRef.current !== null) return;
    gpsWatchIdRef.current = navigator.geolocation.watchPosition(
      (pos) => {
        setGpsLocation(makePoint(pos.coords.longitude, pos.coords.latitude));
        setGpsAccuracy(pos.coords.accuracy);
      },
      () => {
        // Watch failures are handled gracefully on explicit actions
      },
      { enableHighAccuracy: true, maximumAge: 5000 }
    );
  }, []);

  const requestGpsAndCenter = useCallback(() => {
    if (!('geolocation' in navigator)) {
      setStatusMessage('GPS no soportado en este navegador');
      return;
    }
    ensureGpsWatchStarted();
    setStatusMessage('🛰️ Buscando señal GPS...');
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const lon = pos.coords.longitude;
        const lat = pos.coords.latitude;
        const acc = pos.coords.accuracy;
        setGpsLocation(makePoint(lon, lat));
        setGpsAccuracy(acc);
        viewportRef.current.centerLon = lon;
        viewportRef.current.centerLat = lat;
        viewportRef.current.zoom = 17.0;
        updateCoordinates(lon, lat);
        setStatusMessage(`GPS: Centrado en tu ubicación (±${Math.round(acc)}m)`);
        updateRender();
      },
      (err) => {
        setStatusMessage(`GPS no disponible: ${err.message}`);
      },
      { enableHighAccuracy: true, timeout: 8000 }
    );
  }, [ensureGpsWatchStarted, updateCoordinates, updateRender]);

  const requestGpsAndCapture = useCallback(() => {
    const capturePoint = (lon: number, lat: number, acc: number | null) => {
      const gps = makePoint(lon, lat);
      setGpsLocation(gps);
      setGpsAccuracy(acc);
      const target = activeLayer;

      if (!target || target.geometryType !== GeometryType.POINT) {
        const existingGpsLayer = layers.find(
          (l) => l.name === 'Puntos de Campo GPS' && l.geometryType === GeometryType.POINT
        );
        if (existingGpsLayer) {
          const nextId =
            existingGpsLayer.features.reduce((max, f) => Math.max(max, f.id), 0) + 1;
          const newFeat: Feature = {
            id: nextId,
            geometry: gps,
            attributes: {
              ID: nextId,
              NOMBRE: `GPS-${nextId}`,
              PRECISION_M: acc ?? 0,
              FECHA: new Date().toISOString().slice(0, 19).replace('T', ' '),
              LONGITUD: lon,
              LATITUD: lat,
            },
          };
          setLayers((prev) =>
            prev.map((l) =>
              l.id === existingGpsLayer.id
                ? { ...l, features: [...l.features, newFeat] }
                : l
            )
          );
          setActiveLayerId(existingGpsLayer.id);
          setStatusMessage(
            `✅ Punto GPS-${nextId} capturado en '${existingGpsLayer.name}' (±${acc ? Math.round(acc) : '?'}m)`
          );
          updateRender();
          return;
        }

        const fieldLayer = createVectorLayer({
          name: 'Puntos de Campo GPS',
          geometryType: GeometryType.POINT,
          fields: [
            makeField('ID', FieldType.INTEGER),
            makeField('NOMBRE', FieldType.STRING),
            makeField('PRECISION_M', FieldType.DOUBLE),
            makeField('FECHA', FieldType.STRING),
            makeField('LONGITUD', FieldType.DOUBLE),
            makeField('LATITUD', FieldType.DOUBLE),
          ],
        });
        const nextId = 1;
        fieldLayer.features.push({
          id: nextId,
          geometry: gps,
          attributes: {
            ID: nextId,
            NOMBRE: `GPS-${nextId}`,
            PRECISION_M: acc ?? 0,
            FECHA: new Date().toISOString().slice(0, 19).replace('T', ' '),
            LONGITUD: lon,
            LATITUD: lat,
          },
        });
        addLayer(fieldLayer);
        setStatusMessage(
          `✅ Punto GPS-${nextId} capturado en '${fieldLayer.name}' (±${acc ? Math.round(acc) : '?'}m)`
        );
        return;
      }

      const nextId = target.features.reduce((max, f) => Math.max(max, f.id), 0) + 1;
      setPendingFeatureDraft({
        layerId: target.id,
        geometry: gps,
        nextId,
        isPointCapture: true,
        prefillAttributes: {
          ID: nextId,
          NOMBRE: `GPS-${nextId}`,
          PRECISION_M: acc ?? 0,
          FECHA: new Date().toISOString().slice(0, 19).replace('T', ' '),
          LONGITUD: Number(lon.toFixed(6)),
          LATITUD: Number(lat.toFixed(6)),
        },
      });
      setStatusMessage(
        `🛰️ Ubicación GPS (±${acc ? Math.round(acc) : '?'}m) lista para guardar en '${target.name}'`
      );
    };

    ensureGpsWatchStarted();
    if (gpsLocation) {
      capturePoint(gpsLocation.x, gpsLocation.y, gpsAccuracy);
      return;
    }

    if (!('geolocation' in navigator)) {
      setStatusMessage('Sin señal GPS todavía.');
      return;
    }

    setStatusMessage('🛰️ Capturando posición GPS...');
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        capturePoint(pos.coords.longitude, pos.coords.latitude, pos.coords.accuracy);
      },
      (err) => {
        setStatusMessage(`Sin señal GPS: ${err.message}`);
      },
      { enableHighAccuracy: true, timeout: 8000 }
    );
  }, [activeLayer, addLayer, ensureGpsWatchStarted, gpsAccuracy, gpsLocation, updateRender]);

  const searchPlaces = useCallback(
    (query: string) => {
      if (!query.trim()) return [];
      const clean = query.trim().toLowerCase();
      const results: Array<{ layer: VectorLayer; feature: Feature }> = [];
      for (const layer of layers) {
        if (!layer.isVisible) continue;
        for (const feature of layer.features) {
          const matches = Object.values(feature.attributes).some(
            (v) => v !== null && v !== undefined && String(v).toLowerCase().includes(clean)
          );
          if (matches) results.push({ layer, feature });
        }
      }
      return results;
    },
    [layers]
  );

  const searchWorldPlaces = useCallback(
    (query: string) => {
      const q = query.trim();
      if (q.length < 2) {
        setWorldSearchResults([]);
        return;
      }

      searchAbortRef.current?.abort();
      const controller = new AbortController();
      searchAbortRef.current = controller;

      setIsSearchingWorld(true);
      const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(q)}&limit=8&addressdetails=1`;

      fetch(url, {
        signal: controller.signal,
        headers: {
          'Accept-Language': language === AppLanguage.ES ? 'es,en' : 'en',
        },
      })
        .then((res) => (res.ok ? res.json() : []))
        .then((arr: Array<Record<string, unknown>>) => {
          const parsed: WorldPlaceResult[] = arr.map((obj) => ({
            displayName: String(obj.display_name ?? ''),
            lat: Number(obj.lat ?? 0),
            lon: Number(obj.lon ?? 0),
            type: String(obj.type ?? ''),
            category: String(obj.class ?? ''),
          }));
          setWorldSearchResults(parsed);
        })
        .catch(() => {
          if (!controller.signal.aborted) {
            setWorldSearchResults([]);
          }
        })
        .finally(() => {
          if (!controller.signal.aborted) {
            setIsSearchingWorld(false);
          }
        });
    },
    [language]
  );

  const clearWorldSearchResults = useCallback(() => {
    setWorldSearchResults([]);
  }, []);

  const runBuffer = useCallback(
    (layer: VectorLayer, distanceMeters: number) => {
      const buffered = BufferTool.execute(layer, distanceMeters);
      addLayer(buffered);
      setStatusMessage(`Buffer de ${Math.round(distanceMeters)}m generado con éxito`);
    },
    [addLayer]
  );

  const runClip = useCallback(
    (layer: VectorLayer) => {
      const pTopLeft = viewportRef.current.screenToGeo(0, 0);
      const pBottomRight = viewportRef.current.screenToGeo(
        viewportRef.current.screenWidth,
        viewportRef.current.screenHeight
      );
      const clipBox = {
        minX: Math.min(pTopLeft.x, pBottomRight.x),
        minY: Math.min(pTopLeft.y, pBottomRight.y),
        maxX: Math.max(pTopLeft.x, pBottomRight.x),
        maxY: Math.max(pTopLeft.y, pBottomRight.y),
      };
      const clipped = ClipTool.execute(layer, clipBox);
      addLayer(clipped);
      setStatusMessage('Capa recortada por la extensión actual');
    },
    [addLayer]
  );

  const runSimplify = useCallback(
    (layer: VectorLayer, toleranceMeters: number) => {
      const degTolerance = toleranceMeters / 111320.0;
      const simplified = SimplifyLineTool.execute(layer, degTolerance);
      addLayer(simplified);
      setStatusMessage(`Capa simplificada (Ramer-Douglas-Peucker ${toleranceMeters}m)`);
    },
    [addLayer]
  );

  const runSelectByAttributes = useCallback(
    (layer: VectorLayer, whereClause: string) => {
      const count = AttributeQuery.selectByAttributes(layer, whereClause);
      setStatusMessage(`${count} entidades seleccionadas con: ${whereClause}`);
      updateRender();
    },
    [updateRender]
  );

  const runCalculateGeometry = useCallback(
    (layer: VectorLayer, fieldName: string, property: GeometryProperty) => {
      const updated = calculateLayerGeometry(layer, fieldName, property);
      setLayers((prev) => prev.map((l) => (l.id === layer.id ? updated : l)));
      setStatusMessage(
        `Geometría calculada en el campo ${fieldName} (${layer.features.length} registros)`
      );
      updateRender();
    },
    [updateRender]
  );

  const setLayerSymbology = useCallback(
    (layerId: string, symbology: LayerSymbology) => {
      setLayers((prev) => prev.map((l) => (l.id === layerId ? { ...l, symbology } : l)));
      setStatusMessage('Simbología actualizada');
      updateRender();
    },
    [updateRender]
  );

  const setLayerLabelField = useCallback(
    (layerId: string, fieldName: string | null) => {
      setLayers((prev) =>
        prev.map((l) => (l.id === layerId ? { ...l, labelFieldName: fieldName } : l))
      );
      setStatusMessage(fieldName ? `Etiquetas activadas: ${fieldName}` : 'Etiquetas desactivadas');
      updateRender();
    },
    [updateRender]
  );

  const loadGeoJsonText = useCallback(
    (name: string, text: string): boolean => {
      try {
        const layer = GeoJsonParser.parse(text, name);
        addLayer(layer);
        const box = getLayerBoundingBox(layer);
        if (!isBBoxEmpty(box)) {
          viewportRef.current.zoomToExtent(box);
          updateCoordinates(viewportRef.current.centerLon, viewportRef.current.centerLat);
        }
        setStatusMessage(`GeoJSON cargado: ${layer.name} (${layer.features.length} entidades)`);
        updateRender();
        return true;
      } catch (err) {
        setStatusMessage(
          `Error al leer GeoJSON: ${err instanceof Error ? err.message : 'Desconocido'}`
        );
        return false;
      }
    },
    [addLayer, updateCoordinates, updateRender]
  );

  const loadShapefileBuffers = useCallback(
    (name: string, shpBuffer: ArrayBuffer, dbfBuffer?: ArrayBuffer | null): boolean => {
      try {
        const layer = ShapefileReader.readBuffers(name, shpBuffer, dbfBuffer);
        addLayer(layer);
        const box = getLayerBoundingBox(layer);
        if (!isBBoxEmpty(box)) {
          viewportRef.current.zoomToExtent(box);
          updateCoordinates(viewportRef.current.centerLon, viewportRef.current.centerLat);
        }
        setStatusMessage(`Shapefile cargado: ${layer.name} (${layer.features.length} entidades)`);
        updateRender();
        return true;
      } catch (err) {
        setStatusMessage(
          `Error al leer Shapefile: ${err instanceof Error ? err.message : 'Desconocido'}`
        );
        return false;
      }
    },
    [addLayer, updateCoordinates, updateRender]
  );

  const exportActiveLayerAsGeoJson = useCallback(() => {
    if (!activeLayer) {
      setStatusMessage('Sin capa activa para exportar.');
      return;
    }
    const geoJson = GeoJsonParser.exportToGeoJson(activeLayer);
    const safeName = activeLayer.name.replace(/[^a-zA-Z0-9_-]/g, '_') || 'layer';
    triggerDownload(
      new Blob([geoJson], { type: 'application/geo+json' }),
      `${safeName}.geojson`
    );
    setStatusMessage(
      `✓ Exportado: ${safeName}.geojson (${activeLayer.features.length} entidades)`
    );
  }, [activeLayer]);

  const exportLayerAsShapefile = useCallback(async (layer: VectorLayer) => {
    try {
      setStatusMessage(`Exportando Shapefile '${layer.name}'...`);
      await new Promise<void>((resolve) => setTimeout(resolve, 0));
      const safeName = layer.name.replace(/[^a-zA-Z0-9_-]/g, '_') || 'layer';
      const zipBlob = ShapefileWriter.buildZipBlob(layer, safeName);
      triggerDownload(zipBlob, `${safeName}_shapefile.zip`);
      setStatusMessage(
        `✓ Shapefile exportado: ${safeName}_shapefile.zip (.shp, .shx, .dbf, .prj)`
      );
    } catch (err) {
      setStatusMessage(
        `Error al exportar Shapefile: ${err instanceof Error ? err.message : 'Desconocido'}`
      );
    }
  }, []);

  const value: ArcMapContextValue = {
    theme,
    language,
    currentBasemap,
    isBasemapDialogVisible,
    worldSearchResults,
    isSearchingWorld,
    gpsLocation,
    gpsAccuracy,
    isSearchVisible,
    viewMode,
    isCatalogVisible,
    isNewLayerDialogVisible,
    newLayerDefaultGeomType,
    pendingFeatureDraft,
    layoutTitle,
    isEditorActive,
    layers,
    activeLayer,
    activeTool,
    viewport: viewportRef.current,
    tileManager: tileManagerRef.current,
    measureSession,
    digitizeSession,
    identifiedFeature,
    selectedLayerForTable,
    isAttributeTableVisible,
    isToolboxVisible,
    isTocVisible,
    cursorUtm,
    mapScale,
    statusMessage,
    renderVersion,

    updateRender,
    loadSampleData,
    setTool,
    handleMapTap,
    undoMeasurePoint,
    clearMeasureSession,
    undoDigitizePoint,
    clearDigitizeSession,
    completeDigitizing,
    commitPendingFeature,
    cancelPendingFeature,
    openNewLayerDialog,
    closeNewLayerDialog,
    createNewLayer,
    updateFeatureAttributes,
    deleteFeature,
    deleteSelectedFeatures,
    addFieldToLayer,
    updateCoordinates,
    zoomIn,
    zoomOut,
    zoomAround,
    zoomFullExtent,
    zoomToExtentOfLayer,
    zoomToFeature,
    zoomToLocation,
    zoomToScale,
    toggleLayerVisibility,
    setLayerOpacity,
    moveLayerUp,
    moveLayerDown,
    removeLayer,
    addLayer,
    setActiveLayer,
    openAttributeTable,
    closeAttributeTable,
    toggleToolbox: setIsToolboxVisible,
    toggleToc: setIsTocVisible,
    toggleCatalog: setIsCatalogVisible,
    toggleSearch: setIsSearchVisible,
    openBasemapDialog: () => setIsBasemapDialogVisible(true),
    closeBasemapDialog: () => setIsBasemapDialogVisible(false),
    setBasemap,
    dismissIdentify: () => setIdentifiedFeatureIds(null),
    toggleTheme,
    toggleLanguage,
    setViewMode,
    setLayoutTitle,
    toggleEditor,
    clearAllSelections,
    clearAllLayers,
    selectFeatureInLayer,
    deselectFeatureInLayer,
    selectAllInLayer,
    clearLayerSelection,
    requestGpsAndCenter,
    requestGpsAndCapture,
    searchPlaces,
    searchWorldPlaces,
    clearWorldSearchResults,
    runBuffer,
    runClip,
    runSimplify,
    runSelectByAttributes,
    runCalculateGeometry,
    setLayerSymbology,
    setLayerLabelField,
    loadGeoJsonText,
    loadShapefileBuffers,
    exportActiveLayerAsGeoJson,
    exportLayerAsShapefile,
  };

  return <ArcMapContext.Provider value={value}>{children}</ArcMapContext.Provider>;
};

export function useArcMap(): ArcMapContextValue {
  const ctx = useContext(ArcMapContext);
  if (!ctx) throw new Error('useArcMap must be used within ArcMapProvider');
  return ctx;
}

declare global {
  interface Window {
    AndroidBridge?: {
      saveBase64File: (filename: string, mimeType: string, base64Data: string) => void;
    };
    __arcmapHandleBack?: () => boolean;
  }
}

function triggerDownload(blob: Blob, filename: string): void {
  if (window.AndroidBridge?.saveBase64File) {
    const reader = new FileReader();
    reader.onloadend = () => {
      const result = typeof reader.result === 'string' ? reader.result : '';
      const base64 = result.includes(',') ? result.split(',')[1] : result;
      if (base64) {
        window.AndroidBridge?.saveBase64File(
          filename,
          blob.type || 'application/octet-stream',
          base64
        );
      }
    };
    reader.readAsDataURL(blob);
    return;
  }

  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
