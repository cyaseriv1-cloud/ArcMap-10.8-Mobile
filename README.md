# 🗺️ ArcMap 10.8 (ArcGIS Desktop Web)

Aplicación web SIG inspirada en la suite oficial **Esri ArcGIS Desktop 10.8 (ArcMap, ArcCatalog y ArcToolbox)**, construida con **React, TypeScript, Vite y Tailwind CSS**.

---

## 🚀 Capacidades y Funcionalidades Integradas

### 1. Mapas Base Mundiales en Vivo (*Add Basemap*)
* **OpenStreetMap (OSM):** Red vial global de calles, avenidas, edificios y comercios.
* **Esri World Imagery:** Fotografía satelital global de alta resolución de Esri.
* **CartoDB Dark Matter:** Mapa base urbano oscuro de alto contraste para análisis nocturno.
* **CartoDB Positron:** Mapa base claro y minimalista para resaltar capas temáticas SIG.
* **OpenTopoMap:** Relieve y curvas de nivel topográficas.
* **Sin Mapa Base:** Lienzo cartográfico vectorial con cuadrícula de coordenadas.

### 2. Buscador y Geocodificador Global (Find 🔍)
* **📍 Entidades Locales:** Búsqueda y zoom a capas vectoriales cargadas.
* **🌍 Ciudades del Mundo:** Conectado a OpenStreetMap Nominatim para buscar cualquier ciudad o dirección y centrar el mapa en tiempo real.

### 3. GPS en Tiempo Real (Web Geolocation)
* Integración con la API de geolocalización del navegador.
* Botón de centrado con punto azul y halo de precisión, y botón de captura directa de puntos GPS como entidades vectoriales.

### 4. Interfaz Clásica ArcGIS Desktop 10.8
* **Menús en Español e Inglés:** `Archivo`, `Edición`, `Ver`, `Selección`, `Geoprocesamiento`, `Ventanas`.
* **Tema Claro y Tema Oscuro:** Selector ☀️ / 🌙 en la barra superior con sincronización automática de mapa base.
* **Tabla de Contenidos (TOC):** Árbol jerárquico de capas con visibilidad, opacidad, orden de dibujo, propiedades de simbología (simple / valores únicos) y etiquetas.
* **ArcCatalog (Add Data):** Importación de Shapefiles binarios (`.shp` + `.dbf`), GeoJSON (`.geojson`/`.json`) y paquete de muestra Esri (Cusco).
* **ArcToolbox:** Geoprocesamiento con Buffer, Clip, Simplificación (Ramer-Douglas-Peucker), Selección por Atributos (SQL) y Exportación a Shapefile ZIP (`.shp`, `.shx`, `.dbf`, `.prj`).
* **Tabla de Atributos & Calculate Geometry:** Ordenación, búsqueda, selección y cálculo geodésico de área, longitud/perímetro y centroides.
* **Layout View:** Conmutador entre vista de datos y lámina cartográfica con título editable, leyenda, proyección UTM, escala y exportación a imagen PNG.

---

## 📂 Estructura del Proyecto
* `src/core/` - Geometrías (`Point`, `Polyline`, `Polygon`, multiparte), proyecciones (`WGS84`, `Web Mercator`, `UTM`), índice espacial de grilla y simbología.
* `src/data/` - Parseador/escritor de ESRI Shapefile binario (`.shp`, `.shx`, `.dbf`, `.prj`), GeoJSON y gestor de proyectos.
* `src/map/` - Motor de renderizado en HTML5 Canvas, gestor de teselas XYZ (`TileManager`), viewport y modos de interacción.
* `src/tools/` - Algoritmos de geoprocesamiento (`BufferTool`, `ClipTool`, `SimplifyLineTool`, `AttributeQuery`).
* `src/state/` - Estado global de la aplicación (`ArcMapContext`) y datos de muestra (`sampleData`).
* `src/components/` - Interfaz completa en React con barras de herramientas clásicas, TOC, diálogos, tabla de atributos y vista de lámina.
