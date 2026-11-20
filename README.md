# 🗺️ ArcMap 10.8 (ArcGIS Desktop Mobile)

Versión móvil nativa para Android inspirada en la suite oficial **Esri ArcGIS Desktop 10.8 (ArcMap, ArcCatalog y ArcToolbox)**.

---

## 📱 Instalación Inmediata
El instalador APK listo para tu teléfono se encuentra en esta misma carpeta:
* **[ArcMap-10.8-debug.apk](file:///d:/CURSOS%203/AGENTE%20ANTIGRAVITY/ArcMap_10.8_Mobile/ArcMap-10.8-debug.apk)** (16.1 MB)

---

## 🚀 Capacidades y Funcionalidades Integradas

### 1. Mapas Base Mundiales en Vivo (*Add Basemap*)
* **OpenStreetMap (OSM):** Red vial global de calles, avenidas, comercios y números de casa.
* **Esri World Imagery:** Fotografía satelital global de alta resolución de Esri.
* **CartoDB Dark Matter:** Mapa base urbano oscuro de alto contraste para análisis nocturno.
* **CartoDB Positron:** Mapa base claro y minimalista para resaltar capas temáticas de SIG.
* **OpenTopoMap:** Relieve y curvas de nivel topográficas.
* **Caché Offline:** Los mapas visitados se almacenan en el teléfono para usarse sin internet en campo.

### 2. Buscador y Geocodificador Global (Lupa 🔍)
* **📍 Entidades Locales:** Búsqueda y zoom a capas vectoriales cargadas.
* **🌍 Ciudades del Mundo:** Conectado a OpenStreetMap Nominatim. Busca cualquier ciudad (*París, Tokio, Madrid, Lima, Nueva York, etc.*) y vuela instantáneamente a ella con descarga de teselas en tiempo real.

### 3. GPS en Tiempo Real
* Integración directa con los sensores GPS satelital y de red del dispositivo.
* Botón de centrado con **punto azul y halo de precisión animado** (estilo Google Maps y ArcGIS Field Maps).

### 4. Interfaz Clásica ArcGIS Desktop 10.8
* **Menús completos en Español e Inglés:** `Archivo`, `Edición`, `Ver`, `Selección`, `Geoprocesamiento`, `Ventanas`, `Ayuda`.
* **Tema Claro y Tema Oscuro:** Selector ☀️ / 🌙 en la barra superior.
* **Tabla de Contenidos (TOC):** Árbol jerárquico de capas con simbología cartográfica original.
* **ArcCatalog:** Explorador de almacenamiento para importar Shapefiles (`.shp`, `.dbf`) y GeoJSON.
* **ArcToolbox:** Geoprocesamiento con herramientas como Buffer, Clip, Simplificación y Selección por Atributos.
* **Layout View:** Conmutador entre vista de datos y composición cartográfica con membrete, norte, escala y leyenda.

---

## 📂 Estructura de Módulos del Proyecto
* `:core` - Geometrías (Point, Polyline, Polygon), proyecciones (WGS84, Web Mercator, UTM), índices espaciales R-Tree y modelos de atributos.
* `:data` - Parseador de ESRI Shapefile binario (`.shp`, `.shx`, `.dbf`), GeoJSON y gestión de proyectos.
* `:feature-map` - Motor de renderizado en Canvas nativo a 60 fps, visor de teselas XYZ y sesiones de interacción.
* `:feature-tools` - Algoritmos de geoprocesamiento (Buffer, Clip, Simplificación Douglas-Peucker).
* `:ui` - Interfaz completa en Jetpack Compose con barras de herramientas, TOC, diálogos y vistas de mapa.
