package com.terraaxis.app.viewmodel

import androidx.lifecycle.ViewModel
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
import com.terraaxis.map.interaction.DigitizeSession
import com.terraaxis.map.interaction.MapToolMode
import com.terraaxis.map.interaction.MeasureSession
import com.terraaxis.map.viewport.MapViewport
import com.terraaxis.tools.analysis.BufferTool
import com.terraaxis.tools.analysis.ClipTool
import com.terraaxis.tools.generalization.SimplifyLineTool
import com.terraaxis.tools.query.AttributeQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class ArcMapViewMode {
    DATA_VIEW,
    LAYOUT_VIEW
}

class MainViewModel : ViewModel() {

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
        loadSampleData()
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
}