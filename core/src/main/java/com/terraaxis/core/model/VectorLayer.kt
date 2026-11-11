package com.terraaxis.core.model

import com.terraaxis.core.crs.CoordinateReferenceSystem
import com.terraaxis.core.geometry.BoundingBox
import com.terraaxis.core.geometry.GeometryType
import com.terraaxis.core.symbology.LayerSymbology
import java.util.UUID

data class VectorLayer(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    val geometryType: GeometryType,
    var isVisible: Boolean = true,
    var opacity: Float = 1.0f,
    val crs: CoordinateReferenceSystem = CoordinateReferenceSystem.WGS84,
    var symbology: LayerSymbology = LayerSymbology.Simple(),
    val table: AttributeTable = AttributeTable(),
    val selectedFeatureIds: MutableSet<Long> = mutableSetOf(),
    var labelFieldName: String? = null
) {
    val features: MutableList<Feature> get() = table.features
    val fields: MutableList<Field> get() = table.fields

    val boundingBox: BoundingBox
        get() {
            var box = BoundingBox.EMPTY
            for (f in features) {
                val geom = f.geometry ?: continue
                box = box.expandToInclude(geom.boundingBox)
            }
            return box
        }

    fun selectFeature(id: Long, append: Boolean = false) {
        if (!append) selectedFeatureIds.clear()
        selectedFeatureIds.add(id)
    }

    fun deselectFeature(id: Long) {
        selectedFeatureIds.remove(id)
    }

    fun clearSelection() {
        selectedFeatureIds.clear()
    }

    fun selectAll() {
        selectedFeatureIds.clear()
        selectedFeatureIds.addAll(features.map { it.id })
    }

    fun getSelectedFeatures(): List<Feature> =
        features.filter { selectedFeatureIds.contains(it.id) }
}
