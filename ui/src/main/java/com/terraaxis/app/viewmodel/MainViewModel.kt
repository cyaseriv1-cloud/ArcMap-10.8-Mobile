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
import com.terraaxis.tools.query.AttributeQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class MainViewModel : ViewModel() {

    private val _project = MutableStateFlow(MapProject("TerraAxis Project"))
    val project: StateFlow<MapProject> = _project.asStateFlow()

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
                // In pan mode, single tap can dismiss identify
                _identifiedFeature.value = null
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
        val tolerance = 0.003 * (20.0 - viewport.zoom).coerceAtLeast(1.0)
        for (layer in _layers.value) {
            if (!layer.isVisible) continue
            val index = SpatialIndex()
            index.build(layer.features)
            val found = index.hitTest(tapPoint, tolerance)
            if (found != null) {
                _identifiedFeature.value = Pair(layer, found)
                _statusMessage.value = "Identificado: ${layer.name} (ID: ${found.id})"
                return
            }
        }
        _identifiedFeature.value = null
        _statusMessage.value = "Ninguna entidad en esa posición"
    }

    private fun performSelect(tapPoint: Point) {
        val tolerance = 0.003 * (20.0 - viewport.zoom).coerceAtLeast(1.0)
        val targetLayer = _activeLayer.value ?: _layers.value.firstOrNull() ?: return
        val index = SpatialIndex()
        index.build(targetLayer.features)
        val found = index.hitTest(tapPoint, tolerance)

        if (found != null) {
            if (targetLayer.selectedFeatureIds.contains(found.id)) {
                targetLayer.deselectFeature(found.id)
            } else {
                targetLayer.selectFeature(found.id, append = true)
            }
            _statusMessage.value = "${targetLayer.selectedFeatureIds.size} entidades seleccionadas en ${targetLayer.name}"
        } else {
            targetLayer.clearSelection()
            _statusMessage.value = "Selección limpiada"
        }
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
        updateRender()
    }

    fun zoomOut() {
        viewport.zoomBy(-1.0)
        _mapScale.value = viewport.scaleDenominator
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

    // ArcToolbox Executions
    fun runBuffer(layer: VectorLayer, distanceMeters: Double) {
        val buffered = BufferTool.execute(layer, distanceMeters)
        val currentList = _layers.value.toMutableList()
        currentList.add(0, buffered) // Add on top
        _layers.value = currentList
        _activeLayer.value = buffered
        _statusMessage.value = "Buffer de ${distanceMeters.toInt()}m generado con éxito"
        updateRender()
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
        val currentList = _layers.value.toMutableList()
        currentList.add(0, clipped)
        _layers.value = currentList
        _activeLayer.value = clipped
        _statusMessage.value = "Capa recortada por la extensión actual"
        updateRender()
    }

    fun runSelectByAttributes(layer: VectorLayer, whereClause: String) {
        val count = AttributeQuery.selectByAttributes(layer, whereClause)
        _statusMessage.value = "$count entidades seleccionadas con: $whereClause"
        updateRender()
    }

    fun runCalculateGeometry(layer: VectorLayer, fieldName: String, property: GeometryProperty) {
        layer.table.calculateGeometry(fieldName, property)
        _statusMessage.value = "Geometría calculada en el campo $fieldName"
        updateRender()
    }

    fun updateRender() {
        mapRenderTrigger.value = System.currentTimeMillis()
    }
}
