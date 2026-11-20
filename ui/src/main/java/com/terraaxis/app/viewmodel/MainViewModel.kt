package com.terraaxis.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terraaxis.app.sample.SampleData
import com.terraaxis.core.crs.ProjectionEngine
import com.terraaxis.core.crs.UtmCoordinate
import com.terraaxis.core.geometry.BoundingBox
import com.terraaxis.core.geometry.GeometryType
import com.terraaxis.core.geometry.Point
import com.terraaxis.core.geometry.Polygon
import com.terraaxis.core.geometry.Polyline
import com.terraaxis.core.model.Feature
import com.terraaxis.core.model.GeometryProperty
import com.terraaxis.core.model.VectorLayer
import com.terraaxis.core.spatial.SpatialIndex
import com.terraaxis.data.project.MapProject
import com.terraaxis.map.basemap.BasemapType
import com.terraaxis.map.interaction.DigitizeSession
import com.terraaxis.map.interaction.MapToolMode
import com.terraaxis.map.interaction.MeasureSession
import com.terraaxis.map.viewport.MapViewport
import com.terraaxis.tools.analysis.BufferTool
import com.terraaxis.tools.analysis.ClipTool
import com.terraaxis.tools.generalization.SimplifyLineTool
import com.terraaxis.tools.query.AttributeQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class ArcMapViewMode {
    DATA_VIEW,
    LAYOUT_VIEW
}

enum class ArcMapTheme {
    LIGHT,
    DARK
}

enum class AppLanguage {
    ES,
    EN
}

data class WorldPlaceResult(
    val displayName: String,
    val lat: Double,
    val lon: Double,
    val type: String,
    val category: String
)

class MainViewModel : ViewModel() {

    private val _theme = MutableStateFlow(ArcMapTheme.LIGHT)
    val theme: StateFlow<ArcMapTheme> = _theme.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.ES)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _currentBasemap = MutableStateFlow(BasemapType.OPEN_STREET_MAP)
    val currentBasemap: StateFlow<BasemapType> = _currentBasemap.asStateFlow()

    private val _isBasemapDialogVisible = MutableStateFlow(false)
    val isBasemapDialogVisible: StateFlow<Boolean> = _isBasemapDialogVisible.asStateFlow()

    private val _worldSearchResults = MutableStateFlow<List<WorldPlaceResult>>(emptyList())
    val worldSearchResults: StateFlow<List<WorldPlaceResult>> = _worldSearchResults.asStateFlow()

    private val _isSearchingWorld = MutableStateFlow(false)
    val isSearchingWorld: StateFlow<Boolean> = _isSearchingWorld.asStateFlow()

    private val _gpsLocation = MutableStateFlow<Point?>(null)
    val gpsLocation: StateFlow<Point?> = _gpsLocation.asStateFlow()

    private val _gpsAccuracy = MutableStateFlow<Float?>(null)
    val gpsAccuracy: StateFlow<Float?> = _gpsAccuracy.asStateFlow()

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    private val _project = MutableStateFlow(MapProject("ArcMap Document (Untitled.mxd)"))
    val project: StateFlow<MapProject> = _project.asStateFlow()

    private val _viewMode = MutableStateFlow(ArcMapViewMode.DATA_VIEW)
    val viewMode: StateFlow<ArcMapViewMode> = _viewMode.asStateFlow()

    private val _isCatalogVisible = MutableStateFlow(false)
    val isCatalogVisible: StateFlow<Boolean> = _isCatalogVisible.asStateFlow()

    private val _layoutTitle = MutableStateFlow("MAPA GENERAL DE COBERTURA")
    val layoutTitle: StateFlow<String> = _layoutTitle.asStateFlow()

    private val _isEditorActive = MutableStateFlow(false)
    val isEditorActive: StateFlow<Boolean> = _isEditorActive.asStateFlow()

    private val _layers = MutableStateFlow<List<VectorLayer>>(emptyList())
    val layers: StateFlow<List<VectorLayer>> = _layers.asStateFlow()

    private val _activeLayer = MutableStateFlow<VectorLayer?>(null)
    val activeLayer: StateFlow<VectorLayer?> = _activeLayer.asStateFlow()

    private val _activeTool = MutableStateFlow(MapToolMode.PAN)
    val activeTool: StateFlow<MapToolMode> = _activeTool.asStateFlow()

    val viewport = MapViewport(centerLon = -71.9675, centerLat = -13.5320, zoom = 13.0)

    private val _measureSession = MutableStateFlow(MeasureSession())
    val measureSession: StateFlow<MeasureSession> = _measureSession.asStateFlow()

    private val _digitizeSession = MutableStateFlow(DigitizeSession())
    val digitizeSession: StateFlow<DigitizeSession> = _digitizeSession.asStateFlow()

    private val _identifiedFeature = MutableStateFlow<Pair<VectorLayer, Feature>?>(null)
    val identifiedFeature: StateFlow<Pair<VectorLayer, Feature>?> = _identifiedFeature.asStateFlow()

    private val _selectedLayerForTable = MutableStateFlow<VectorLayer?>(null)
    val selectedLayerForTable: StateFlow<VectorLayer?> = _selectedLayerForTable.asStateFlow()

    private val _isAttributeTableVisible = MutableStateFlow(false)
    val isAttributeTableVisible: StateFlow<Boolean> = _isAttributeTableVisible.asStateFlow()

    private val _isToolboxVisible = MutableStateFlow(false)
    val isToolboxVisible: StateFlow<Boolean> = _isToolboxVisible.asStateFlow()

    private val _isTocVisible = MutableStateFlow(false)
    val isTocVisible: StateFlow<Boolean> = _isTocVisible.asStateFlow()

    private val _cursorUtm = MutableStateFlow(ProjectionEngine.wgs84ToUtm(viewport.centerLat, viewport.centerLon))
    val cursorUtm: StateFlow<UtmCoordinate> = _cursorUtm.asStateFlow()

    private val _mapScale = MutableStateFlow(viewport.scaleDenominator)
    val mapScale: StateFlow<Long> = _mapScale.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>("Listo")
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Key to trigger map canvas recomposition
    val mapRenderTrigger = MutableStateFlow(0L)

    init {
        // ⚠️ Iniciamos con mapa VACÍO — el usuario añade capas según necesite.
        // Los datos de muestra se cargan solo desde Archivo > Cargar muestra (Cusco).
        _layers.value = emptyList()
        _statusMessage.value = "Listo · Selecciona un Mapa Base para comenzar"
    }

    fun loadSampleData() {
        val sample = SampleData.createSampleLayers()
        _layers.value = sample
        _activeLayer.value = sample.firstOrNull()
        updateRender()
        _statusMessage.value = "Capas de muestra cargadas (${sample.size} capas)"
    }

    fun setTool(tool: MapToolMode) {
        _activeTool.value = tool
        if (tool == MapToolMode.MEASURE_DISTANCE || tool == MapToolMode.MEASURE_AREA) {
            _measureSession.value = MeasureSession()
        }
        if (tool == MapToolMode.DIGITIZE_POINT || tool == MapToolMode.DIGITIZE_LINE || tool == MapToolMode.DIGITIZE_POLYGON) {
            val session = DigitizeSession()
            session.targetLayerId = _activeLayer.value?.id
            _digitizeSession.value = session
        }
        _statusMessage.value = "Herramienta activa: ${tool.name}"
        updateRender()
    }

    fun handleMapTap(screenX: Float, screenY: Float) {
        val geoPoint = viewport.screenToGeo(screenX, screenY)
        updateCoordinates(geoPoint.x, geoPoint.y)

        when (_activeTool.value) {
            MapToolMode.PAN -> {
                // In pan mode, tap only updates cursor coordinates and clears identify popup if open
                if (_identifiedFeature.value != null) {
                    _identifiedFeature.value = null
                    updateRender()
                }
            }
            MapToolMode.IDENTIFY -> {
                performIdentify(geoPoint)
            }
            MapToolMode.SELECT -> {
                performSelect(geoPoint)
            }
            MapToolMode.MEASURE_DISTANCE, MapToolMode.MEASURE_AREA -> {
                _measureSession.value.addPoint(geoPoint)
                updateRender()
            }
            MapToolMode.DIGITIZE_POINT -> {
                addPointFeature(geoPoint)
            }
            MapToolMode.DIGITIZE_LINE, MapToolMode.DIGITIZE_POLYGON -> {
                _digitizeSession.value.addPoint(geoPoint)
                updateRender()
            }
        }
    }

    private fun performIdentify(tapPoint: Point) {
        // 28-pixel finger touch tolerance in geographic degrees
        val tolerance = (viewport.metersPerPixel * 28.0) / 111320.0

        for (layer in _layers.value) {
            if (!layer.isVisible) continue
            val index = SpatialIndex()
            index.build(layer.features)
            val found = index.hitTest(tapPoint, tolerance)
            if (found != null) {
                _identifiedFeature.value = Pair(layer, found)
                _activeLayer.value = layer
                _statusMessage.value = "Identificado: ${layer.name} (ID: ${found.id})"
                updateRender()
                return
            }
        }
        _identifiedFeature.value = null
        _statusMessage.value = "Ninguna entidad en esa posición"
        updateRender()
    }

    private fun performSelect(tapPoint: Point) {
        val tolerance = (viewport.metersPerPixel * 28.0) / 111320.0

        // 1. Try active layer first
        val targetLayer = _activeLayer.value
        if (targetLayer != null && targetLayer.isVisible) {
            val index = SpatialIndex()
            index.build(targetLayer.features)
            val found = index.hitTest(tapPoint, tolerance)
            if (found != null) {
                if (targetLayer.selectedFeatureIds.contains(found.id)) {
                    targetLayer.deselectFeature(found.id)
                } else {
                    targetLayer.selectFeature(found.id, append = true)
                }
                _statusMessage.value = "${targetLayer.selectedFeatureIds.size} seleccionados en ${targetLayer.name}"
                updateRender()
                return
            }
        }

        // 2. If nothing found in active layer, search all other visible layers
        for (layer in _layers.value) {
            if (!layer.isVisible || layer.id == targetLayer?.id) continue
            val index = SpatialIndex()
            index.build(layer.features)
            val found = index.hitTest(tapPoint, tolerance)
            if (found != null) {
                _activeLayer.value = layer
                layer.selectFeature(found.id, append = false)
                _statusMessage.value = "1 seleccionado en ${layer.name} (ID: ${found.id})"
                updateRender()
                return
            }
        }

        // 3. Nothing hit: clear all selections
        _layers.value.forEach { it.clearSelection() }
        _statusMessage.value = "Selección limpiada"
        updateRender()
    }

    fun completeDigitizing() {
        val session = _digitizeSession.value
        val pts = session.points
        val target = _layers.value.find { it.id == session.targetLayerId } ?: _activeLayer.value

        if (target != null && pts.isNotEmpty()) {
            val nextId = (target.features.maxOfOrNull { it.id } ?: 0L) + 1L
            val geom = when (_activeTool.value) {
                MapToolMode.DIGITIZE_LINE -> if (pts.size >= 2) Polyline(ArrayList(pts)) else null
                MapToolMode.DIGITIZE_POLYGON -> if (pts.size >= 3) {
                    val closed = if (pts.first() != pts.last()) pts + pts.first() else pts
                    Polygon.fromSimpleRing(ArrayList(closed))
                } else null
                else -> null
            }

            if (geom != null) {
                target.features.add(Feature(nextId, geom, mutableMapOf("ID" to nextId, "NOMBRE" to "Nueva Entidad $nextId")))
                _statusMessage.value = "Entidad creada en ${target.name}"
            }
        }
        session.clear()
        setTool(MapToolMode.PAN)
        updateRender()
    }

    private fun addPointFeature(p: Point) {
        val target = _activeLayer.value ?: return
        val nextId = (target.features.maxOfOrNull { it.id } ?: 0L) + 1L
        target.features.add(Feature(nextId, p, mutableMapOf("ID" to nextId, "NOMBRE" to "Punto $nextId")))
        _statusMessage.value = "Punto creado en ${target.name}"
        updateRender()
    }

    fun updateCoordinates(lon: Double, lat: Double) {
        _cursorUtm.value = ProjectionEngine.wgs84ToUtm(lat, lon)
        _mapScale.value = viewport.scaleDenominator
    }

    fun zoomIn() {
        viewport.zoomBy(1.0)
        _mapScale.value = viewport.scaleDenominator
        updateCoordinates(viewport.centerLon, viewport.centerLat)
        updateRender()
    }

    fun zoomOut() {
        viewport.zoomBy(-1.0)
        _mapScale.value = viewport.scaleDenominator
        updateCoordinates(viewport.centerLon, viewport.centerLat)
        updateRender()
    }

    fun zoomAround(focalX: Float, focalY: Float, delta: Double) {
        viewport.zoomAround(focalX, focalY, delta)
        _mapScale.value = viewport.scaleDenominator
        updateCoordinates(viewport.centerLon, viewport.centerLat)
        updateRender()
    }

    fun zoomFullExtent() {
        var overallBox = BoundingBox.EMPTY
        for (l in _layers.value) {
            if (l.isVisible) {
                overallBox = overallBox.expandToInclude(l.boundingBox)
            }
        }
        if (overallBox != BoundingBox.EMPTY) {
            viewport.zoomToExtent(overallBox)
            _mapScale.value = viewport.scaleDenominator
            updateCoordinates(viewport.centerLon, viewport.centerLat)
            updateRender()
            _statusMessage.value = "Extensión completa"
        }
    }

    fun toggleLayerVisibility(layerId: String) {
        _layers.value.find { it.id == layerId }?.let {
            it.isVisible = !it.isVisible
            updateRender()
        }
    }

    fun setLayerOpacity(layerId: String, opacity: Float) {
        _layers.value.find { it.id == layerId }?.let {
            it.opacity = opacity.coerceIn(0.05f, 1.0f)
            updateRender()
        }
    }

    fun moveLayerUp(layerId: String) {
        val list = _layers.value.toMutableList()
        val index = list.indexOfFirst { it.id == layerId }
        if (index > 0) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _layers.value = list
            updateRender()
        }
    }

    fun moveLayerDown(layerId: String) {
        val list = _layers.value.toMutableList()
        val index = list.indexOfFirst { it.id == layerId }
        if (index >= 0 && index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _layers.value = list
            updateRender()
        }
    }

    fun removeLayer(layerId: String) {
        val list = _layers.value.toMutableList()
        list.removeAll { it.id == layerId }
        _layers.value = list
        if (_activeLayer.value?.id == layerId) {
            _activeLayer.value = list.firstOrNull()
        }
        updateRender()
        _statusMessage.value = "Capa eliminada"
    }

    fun addLayer(layer: VectorLayer) {
        val list = _layers.value.toMutableList()
        list.add(0, layer)
        _layers.value = list
        _activeLayer.value = layer
        updateRender()
        _statusMessage.value = "Capa añadida: ${layer.name}"
    }

    fun setActiveLayer(layer: VectorLayer) {
        _activeLayer.value = layer
    }

    fun openAttributeTable(layer: VectorLayer) {
        _selectedLayerForTable.value = layer
        _isAttributeTableVisible.value = true
    }

    fun closeAttributeTable() {
        _isAttributeTableVisible.value = false
    }

    fun toggleToolbox(visible: Boolean) {
        _isToolboxVisible.value = visible
    }

    fun toggleToc(visible: Boolean) {
        _isTocVisible.value = visible
    }

    fun dismissIdentify() {
        _identifiedFeature.value = null
    }

    fun toggleTheme() {
        val newTheme = if (_theme.value == ArcMapTheme.LIGHT) ArcMapTheme.DARK else ArcMapTheme.LIGHT
        _theme.value = newTheme
        // Auto-sync mapa base con el tema
        val currentBm = _currentBasemap.value
        if (newTheme == ArcMapTheme.DARK &&
            (currentBm == BasemapType.OPEN_STREET_MAP || currentBm == BasemapType.CARTO_POSITRON)) {
            _currentBasemap.value = BasemapType.CARTO_DARK
        } else if (newTheme == ArcMapTheme.LIGHT && currentBm == BasemapType.CARTO_DARK) {
            _currentBasemap.value = BasemapType.OPEN_STREET_MAP
        }
        _statusMessage.value = if (newTheme == ArcMapTheme.DARK) "Tema Oscuro activado" else "Tema Claro activado"
        updateRender()
    }

    fun toggleLanguage() {
        _language.value = if (_language.value == AppLanguage.ES) AppLanguage.EN else AppLanguage.ES
        _statusMessage.value = if (_language.value == AppLanguage.ES) "Idioma: Español" else "Language: English"
    }

    fun toggleSearch(visible: Boolean) {
        _isSearchVisible.value = visible
    }

    fun updateGpsLocation(lon: Double, lat: Double, accuracy: Float?) {
        _gpsLocation.value = Point(lon, lat)
        _gpsAccuracy.value = accuracy
        updateCoordinates(lon, lat)
        updateRender()
    }

    fun centerOnGps() {
        val gps = _gpsLocation.value
        if (gps != null) {
            viewport.centerLon = gps.x
            viewport.centerLat = gps.y
            viewport.zoom = 17.0 // Zoom de calle detallado
            updateCoordinates(viewport.centerLon, viewport.centerLat)
            _statusMessage.value = "GPS: Centrado en tu ubicación (±${_gpsAccuracy.value?.toInt() ?: "?"}m)"
            updateRender()
        } else {
            _statusMessage.value = "🛰️ Buscando señal GPS... Por favor espera unos segundos"
            updateRender()
        }
    }

    // Captura el punto GPS actual y lo agrega como entidad a la capa activa
    fun captureGpsPoint() {
        val gps = _gpsLocation.value
        if (gps == null) {
            _statusMessage.value = "Sin señal GPS todavía. Espera unos segundos."
            return
        }
        val target = _activeLayer.value ?: run {
            // Si no hay capa activa, crear una automáticamente para puntos de campo
            val fieldLayer = VectorLayer(
                name = "Puntos de Campo GPS",
                geometryType = com.terraaxis.core.geometry.GeometryType.POINT
            )
            fieldLayer.fields.addAll(listOf(
                com.terraaxis.core.model.Field("ID", com.terraaxis.core.model.FieldType.INTEGER),
                com.terraaxis.core.model.Field("NOMBRE", com.terraaxis.core.model.FieldType.STRING),
                com.terraaxis.core.model.Field("PRECISION_M", com.terraaxis.core.model.FieldType.DOUBLE),
                com.terraaxis.core.model.Field("FECHA", com.terraaxis.core.model.FieldType.STRING),
                com.terraaxis.core.model.Field("LONGITUD", com.terraaxis.core.model.FieldType.DOUBLE),
                com.terraaxis.core.model.Field("LATITUD", com.terraaxis.core.model.FieldType.DOUBLE)
            ))
            val list = _layers.value.toMutableList()
            list.add(0, fieldLayer)
            _layers.value = list
            _activeLayer.value = fieldLayer
            fieldLayer
        }
        if (target.geometryType != com.terraaxis.core.geometry.GeometryType.POINT) {
            _statusMessage.value = "La capa activa '${target.name}' no es de tipo punto. Selecciona una capa de puntos en el TOC."
            return
        }
        val nextId = (target.features.maxOfOrNull { it.id } ?: 0L) + 1L
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val timestamp = sdf.format(java.util.Date())
        val attrs = mutableMapOf<String, Any?>(
            "ID" to nextId,
            "NOMBRE" to "GPS-$nextId",
            "PRECISION_M" to (_gpsAccuracy.value?.toDouble() ?: 0.0),
            "FECHA" to timestamp,
            "LONGITUD" to gps.x,
            "LATITUD" to gps.y
        )
        target.features.add(Feature(nextId, gps, attrs))
        _statusMessage.value = "✅ Punto GPS-$nextId capturado en '${target.name}' (±${_gpsAccuracy.value?.toInt() ?: "?"}m)"
        updateRender()
    }

    fun searchPlaces(query: String): List<Pair<VectorLayer, Feature>> {
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<Pair<VectorLayer, Feature>>()
        val clean = query.trim().lowercase()

        for (layer in _layers.value) {
            if (!layer.isVisible) continue
            for (feat in layer.features) {
                val matches = feat.attributes.values.any { it?.toString()?.lowercase()?.contains(clean) == true }
                if (matches) {
                    results.add(Pair(layer, feat))
                }
            }
        }
        return results
    }

    fun zoomToFeature(feature: Feature) {
        val geom = feature.geometry ?: return
        if (geom is Point) {
            viewport.centerLon = geom.x
            viewport.centerLat = geom.y
            viewport.zoom = 16.5
        } else {
            viewport.zoomToExtent(geom.boundingBox)
        }
        updateCoordinates(viewport.centerLon, viewport.centerLat)
        updateRender()
        _statusMessage.value = "Navegado a: ${feature.getString("NOMBRE") ?: "Entidad ${feature.id}"}"
    }

    fun setViewMode(mode: ArcMapViewMode) {
        _viewMode.value = mode
        _statusMessage.value = if (mode == ArcMapViewMode.DATA_VIEW) "Modo: Data View (Edición Geográfica)" else "Modo: Layout View (Composición de Impresión)"
        updateRender()
    }

    fun toggleCatalog(visible: Boolean) {
        _isCatalogVisible.value = visible
    }

    fun setLayoutTitle(title: String) {
        _layoutTitle.value = title
        updateRender()
    }

    fun toggleEditor(active: Boolean) {
        _isEditorActive.value = active
        if (!active && (_activeTool.value == MapToolMode.DIGITIZE_POINT || _activeTool.value == MapToolMode.DIGITIZE_LINE || _activeTool.value == MapToolMode.DIGITIZE_POLYGON)) {
            setTool(MapToolMode.PAN)
        }
        _statusMessage.value = if (active) "Editor iniciado: Listo para digitalizar" else "Editor detenido"
    }

    fun loadShapefile(shpFile: File): Boolean {
        return try {
            val layer = com.terraaxis.data.shapefile.ShapefileReader.read(shpFile)
            addLayer(layer)
            zoomFullExtent()
            _statusMessage.value = "Shapefile cargado: ${layer.name} (${layer.features.size} entidades)"
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _statusMessage.value = "Error al leer Shapefile: ${e.message}"
            false
        }
    }

    fun loadGeoJsonFile(geoJsonFile: File): Boolean {
        return try {
            val layer = com.terraaxis.data.geojson.GeoJsonParser.parseFile(geoJsonFile)
            addLayer(layer)
            zoomFullExtent()
            _statusMessage.value = "GeoJSON cargado: ${layer.name} (${layer.features.size} entidades)"
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _statusMessage.value = "Error al leer GeoJSON: ${e.message}"
            false
        }
    }

    fun zoomToScale(scaleDenominator: Long) {
        if (scaleDenominator <= 0) return
        viewport.setScale(scaleDenominator)
        _mapScale.value = viewport.scaleDenominator
        updateCoordinates(viewport.centerLon, viewport.centerLat)
        updateRender()
    }

    // ArcToolbox Executions
    fun runBuffer(layer: VectorLayer, distanceMeters: Double) {
        val buffered = BufferTool.execute(layer, distanceMeters)
        addLayer(buffered)
        _statusMessage.value = "Buffer de ${distanceMeters.toInt()}m generado con éxito"
    }

    fun runClip(layer: VectorLayer) {
        // Clip against current map viewport extent
        val pTopLeft = viewport.screenToGeo(0f, 0f)
        val pBottomRight = viewport.screenToGeo(viewport.screenWidth, viewport.screenHeight)
        val clipBox = BoundingBox(
            minOf(pTopLeft.x, pBottomRight.x),
            minOf(pTopLeft.y, pBottomRight.y),
            maxOf(pTopLeft.x, pBottomRight.x),
            maxOf(pTopLeft.y, pBottomRight.y)
        )
        val clipped = ClipTool.execute(layer, clipBox)
        addLayer(clipped)
        _statusMessage.value = "Capa recortada por la extensión actual"
    }

    fun runSimplify(layer: VectorLayer, toleranceMeters: Double) {
        // Tolerance in degrees roughly toleranceMeters / 111320
        val degTolerance = toleranceMeters / 111320.0
        val simplified = SimplifyLineTool.execute(layer, degTolerance)
        addLayer(simplified)
        _statusMessage.value = "Capa simplificada (Ramer-Douglas-Peucker ${toleranceMeters}m)"
    }

    fun runSelectByAttributes(layer: VectorLayer, whereClause: String) {
        val count = AttributeQuery.selectByAttributes(layer, whereClause)
        _statusMessage.value = "$count entidades seleccionadas con: $whereClause"
        updateRender()
    }

    fun runCalculateGeometry(layer: VectorLayer, fieldName: String, property: GeometryProperty) {
        layer.table.calculateGeometry(fieldName, property)
        // Forzar re-emisión del StateFlow para que Compose recomponga la tabla de atributos
        _layers.value = _layers.value.toMutableList()
        // También refrescar la capa seleccionada en la tabla
        if (_selectedLayerForTable.value?.id == layer.id) {
            _selectedLayerForTable.value = null
            _selectedLayerForTable.value = layer
        }
        _statusMessage.value = "Geometría calculada en el campo $fieldName (${layer.features.size} registros)"
        updateRender()
    }

    fun refreshLayerTable(layer: VectorLayer) {
        // Fuerza re-render de la tabla de atributos para la capa dada
        _layers.value = _layers.value.toMutableList()
        if (_selectedLayerForTable.value?.id == layer.id) {
            val current = _selectedLayerForTable.value
            _selectedLayerForTable.value = null
            _selectedLayerForTable.value = current
        }
        updateRender()
    }

    fun setLayerSymbology(layerId: String, symbology: com.terraaxis.core.symbology.LayerSymbology) {
        val list = _layers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == layerId }
        if (idx >= 0) {
            list[idx] = list[idx].copy(symbology = symbology)
            _layers.value = list
            if (_activeLayer.value?.id == layerId) _activeLayer.value = list[idx]
            updateRender()
            _statusMessage.value = "Simbología actualizada"
        }
    }

    fun setLayerLabelField(layerId: String, fieldName: String?) {
        val list = _layers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == layerId }
        if (idx >= 0) {
            list[idx] = list[idx].copy(labelFieldName = fieldName)
            _layers.value = list
            if (_activeLayer.value?.id == layerId) _activeLayer.value = list[idx]
            updateRender()
            _statusMessage.value = if (fieldName != null) "Etiquetas activadas: $fieldName" else "Etiquetas desactivadas"
        }
    }

    fun updateRender() {
        mapRenderTrigger.value = System.currentTimeMillis()
    }

    // ── Funciones de Mapa Base (Basemaps) ──────────────────────────────────────
    fun setBasemap(type: BasemapType) {
        _currentBasemap.value = type
        _statusMessage.value = if (_language.value == AppLanguage.ES) "Mapa base activo: ${type.titleEs}" else "Active basemap: ${type.titleEn}"
        updateRender()
    }

    fun openBasemapDialog() {
        _isBasemapDialogVisible.value = true
    }

    fun closeBasemapDialog() {
        _isBasemapDialogVisible.value = false
    }

    // ── Geocodificador y Navegación Mundial ───────────────────────────────────
    fun zoomToLocation(lat: Double, lon: Double, zoomLevel: Double = 14.5) {
        viewport.centerLat = lat
        viewport.centerLon = lon
        viewport.zoom = zoomLevel
        updateCoordinates(lon, lat)
        updateRender()
        _statusMessage.value = if (_language.value == AppLanguage.ES) "Navegando a: %.4f, %.4f".format(lat, lon) else "Navigated to: %.4f, %.4f".format(lat, lon)
    }

    fun searchWorldPlaces(query: String) {
        val q = query.trim()
        if (q.length < 2) {
            _worldSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearchingWorld.value = true
            try {
                val encoded = withContext(Dispatchers.IO) {
                    URLEncoder.encode(q, "UTF-8")
                }
                val urlStr = "https://nominatim.openstreetmap.org/search?format=json&q=$encoded&limit=8&addressdetails=1"
                val list = withContext(Dispatchers.IO) {
                    val url = URL(urlStr)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 6000
                        readTimeout = 7000
                        setRequestProperty("User-Agent", "ArcMapMobile/10.8 (com.terraaxis.app; Android GIS)")
                        setRequestProperty("Accept-Language", if (_language.value == AppLanguage.ES) "es,en" else "en")
                    }
                    if (conn.responseCode in 200..299) {
                        val jsonStr = conn.inputStream.use { it.bufferedReader().readText() }
                        val arr = JSONArray(jsonStr)
                        val results = mutableListOf<WorldPlaceResult>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val name = obj.optString("display_name", "")
                            val lat = obj.optDouble("lat", 0.0)
                            val lon = obj.optDouble("lon", 0.0)
                            val type = obj.optString("type", "")
                            val cat = obj.optString("class", "")
                            results.add(WorldPlaceResult(name, lat, lon, type, cat))
                        }
                        results
                    } else {
                        emptyList()
                    }
                }
                _worldSearchResults.value = list
            } catch (_: Exception) {
                _worldSearchResults.value = emptyList()
            } finally {
                _isSearchingWorld.value = false
            }
        }
    }

    fun clearWorldSearchResults() {
        _worldSearchResults.value = emptyList()
    }

    // ── Limpiar todas las capas ──────────────────────────────────────────────
    fun clearAllLayers() {
        _layers.value = emptyList()
        _activeLayer.value = null
        _identifiedFeature.value = null
        _isAttributeTableVisible.value = false
        _statusMessage.value = if (_language.value == AppLanguage.ES)
            "✓ Todas las capas eliminadas. Mapa limpio." else "✓ All layers cleared."
        updateRender()
    }

    // ── Exportar capa activa a GeoJSON (en Downloads/) ───────────────────────
    fun exportActiveLayerAsGeoJson() {
        val layer = _activeLayer.value ?: run {
            _statusMessage.value = "Sin capa activa para exportar."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                dir.mkdirs()
                val safeName = layer.name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                val outFile = java.io.File(dir, "$safeName.geojson")
                val features = layer.features.joinToString(",\n") { feat ->
                    val geom = feat.geometry
                    val geomJson = when (geom) {
                        is Point -> """{"type":"Point","coordinates":[${geom.x},${geom.y}]}"""
                        is Polyline -> {
                            val coords = geom.points.joinToString(",") { "[${it.x},${it.y}]" }
                            """{"type":"LineString","coordinates":[$coords]}"""
                        }
                        is Polygon -> {
                            val ring = geom.rings.firstOrNull() ?: emptyList()
                            val coords = ring.joinToString(",") { "[${it.x},${it.y}]" }
                            """{"type":"Polygon","coordinates":[[$coords]]}"""
                        }
                        else -> "null"
                    }
                    val attrsJson = feat.attributes.entries.joinToString(",") { (k, v) ->
                        val valStr = when (v) {
                            is Number -> v.toString()
                            null -> "null"
                            else -> "\"${v.toString().replace("\"", "\\\"")}\""
                        }
                        "\"$k\":$valStr"
                    }
                    """{"type":"Feature","geometry":$geomJson,"properties":{$attrsJson}}"""
                }
                val geoJson = """{"type":"FeatureCollection","features":[$features]}"""
                outFile.writeText(geoJson)
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "✓ Exportado: Downloads/${outFile.name} (${layer.features.size} entidades)"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Error al exportar: ${e.message}"
                }
            }
        }
    }
}