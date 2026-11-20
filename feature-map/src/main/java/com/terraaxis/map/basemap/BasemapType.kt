package com.terraaxis.map.basemap

/**
 * Tipos de Mapas Base soportados al estilo ArcGIS Desktop 10.8 (Add Basemap):
 * - Esri World Imagery (Satélite de alta resolución)
 * - OpenStreetMap (Calles y manzanas de todo el mundo)
 * - CartoDB Dark Matter (Modo oscuro de alto contraste)
 * - CartoDB Positron (Gris cartográfico limpio)
 * - OpenTopoMap (Topografía, relieve y curvas de nivel)
 * - Ninguno (Lienzo en blanco clásico)
 */
enum class BasemapType(
    val id: String,
    val titleEs: String,
    val titleEn: String,
    val descriptionEs: String,
    val descriptionEn: String,
    val tileUrlTemplate: String,
    val minZoom: Int = 1,
    val maxZoom: Int = 19,
    val attribution: String = ""
) {
    NONE(
        id = "none",
        titleEs = "Sin mapa base (Fondo vectorial)",
        titleEn = "No Basemap (Vector Canvas)",
        descriptionEs = "Lienzo de dibujo cartográfico en blanco/oscuro tradicional",
        descriptionEn = "Blank classic cartographic paper background",
        tileUrlTemplate = "",
        minZoom = 1,
        maxZoom = 21,
        attribution = ""
    ),
    OPEN_STREET_MAP(
        id = "osm_standard",
        titleEs = "OpenStreetMap (Calles del Mundo)",
        titleEn = "OpenStreetMap (World Streets)",
        descriptionEs = "Red mundial de calles, avenidas, edificios y comercios",
        descriptionEn = "Collaborative worldwide street and road network",
        tileUrlTemplate = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        minZoom = 1,
        maxZoom = 19,
        attribution = "© OpenStreetMap contributors"
    ),
    ESRI_IMAGERY(
        id = "esri_satellite",
        titleEs = "Imágenes Satelitales (Esri)",
        titleEn = "Satellite Imagery (Esri)",
        descriptionEs = "Fotografía satelital global de alta resolución de Esri World Imagery",
        descriptionEn = "High-resolution global satellite imagery from Esri",
        tileUrlTemplate = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
        minZoom = 1,
        maxZoom = 19,
        attribution = "Esri, Maxar, Earthstar Geographics"
    ),
    CARTO_DARK(
        id = "carto_dark",
        titleEs = "CartoDB Dark Matter (Modo Oscuro)",
        titleEn = "CartoDB Dark Matter (Dark)",
        descriptionEs = "Mapa base urbano oscuro de alto contraste para análisis nocturno",
        descriptionEn = "High-contrast dark urban basemap",
        tileUrlTemplate = "https://a.basemaps.cartocdn.com/rastertiles/dark_all/{z}/{x}/{y}.png",
        minZoom = 1,
        maxZoom = 19,
        attribution = "© CARTO, © OpenStreetMap contributors"
    ),
    CARTO_POSITRON(
        id = "carto_positron",
        titleEs = "CartoDB Positron (Gris Cartográfico)",
        titleEn = "CartoDB Positron (Light)",
        descriptionEs = "Mapa base claro minimalista para resaltar capas SIG",
        descriptionEn = "Minimalist light basemap for GIS layer overlay",
        tileUrlTemplate = "https://a.basemaps.cartocdn.com/rastertiles/light_all/{z}/{x}/{y}.png",
        minZoom = 1,
        maxZoom = 19,
        attribution = "© CARTO, © OpenStreetMap contributors"
    ),
    OPEN_TOPO(
        id = "open_topo",
        titleEs = "OpenTopoMap (Topografía y Relieve)",
        titleEn = "OpenTopoMap (Topographic)",
        descriptionEs = "Relieve montañoso con curvas de nivel e hidrografía",
        descriptionEn = "Mountain relief with contour lines and elevation",
        tileUrlTemplate = "https://tile.opentopomap.org/{z}/{x}/{y}.png",
        minZoom = 1,
        maxZoom = 17,
        attribution = "© OpenTopoMap, © OpenStreetMap"
    )
}
