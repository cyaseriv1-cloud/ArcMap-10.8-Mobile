import React, { useEffect, useRef, useState } from 'react';
import {
  Compass,
  Eraser,
  FilePlus2,
  FolderOpen,
  Globe,
  Info,
  Layers,
  Map as MapIcon,
  MapPinPlus,
  Moon,
  MousePointerClick,
  PenTool,
  Plus,
  Printer,
  Ruler,
  Search,
  Share2,
  SlidersHorizontal,
  SquareDashedMousePointer,
  Sun,
  Trash2,
  Waypoints,
  Wrench,
  ZoomIn,
  ZoomOut,
  Maximize2,
  Hand,
  Scissors,
  CircleDot,
  Filter,
} from 'lucide-react';
import { MapToolMode } from '../map/interaction';
import { AppLanguage, ArcMapTheme, ArcMapViewMode, useArcMap } from '../state/ArcMapContext';

type MenuKey = 'file' | 'edit' | 'view' | 'selection' | 'geoprocessing' | 'windows' | null;

export const ArcMapClassicTopBar: React.FC = () => {
  const {
    activeTool,
    isEditorActive,
    viewMode,
    theme,
    language,
    setTool,
    toggleLanguage,
    toggleTheme,
    toggleToc,
    isTocVisible,
    toggleToolbox,
    toggleCatalog,
    toggleSearch,
    openBasemapDialog,
    openNewLayerDialog,
    requestGpsAndCapture,
    loadSampleData,
    clearAllLayers,
    exportActiveLayerAsGeoJson,
    setViewMode,
    toggleEditor,
    clearAllSelections,
    zoomIn,
    zoomOut,
    zoomFullExtent,
  } = useArcMap();

  const [activeMenu, setActiveMenu] = useState<MenuKey>(null);
  const menuContainerRef = useRef<HTMLDivElement>(null);

  const isDark = theme === ArcMapTheme.DARK;
  const isEs = language === AppLanguage.ES;

  useEffect(() => {
    const onOutside = (e: MouseEvent | TouchEvent) => {
      if (menuContainerRef.current && !menuContainerRef.current.contains(e.target as Node)) {
        setActiveMenu(null);
      }
    };
    document.addEventListener('mousedown', onOutside);
    document.addEventListener('touchstart', onOutside, { passive: true });
    return () => {
      document.removeEventListener('mousedown', onOutside);
      document.removeEventListener('touchstart', onOutside);
    };
  }, []);

  const menuDefs: { key: Exclude<MenuKey, null>; label: string }[] = isEs
    ? [
        { key: 'file', label: 'Archivo' },
        { key: 'edit', label: 'Edición' },
        { key: 'view', label: 'Ver' },
        { key: 'selection', label: 'Selección' },
        { key: 'geoprocessing', label: 'Geoprocesam.' },
        { key: 'windows', label: 'Ventanas' },
      ]
    : [
        { key: 'file', label: 'File' },
        { key: 'edit', label: 'Edit' },
        { key: 'view', label: 'View' },
        { key: 'selection', label: 'Selection' },
        { key: 'geoprocessing', label: 'Geoprocessing' },
        { key: 'windows', label: 'Windows' },
      ];

  const titleBarBg = isDark ? 'bg-[#161B22]' : 'bg-[#335E8A]';
  const menuBarBg = isDark ? 'bg-[#21262D]' : 'bg-[#F0EFEA]';
  const toolbarBg = isDark ? 'bg-[#1A1F26]' : 'bg-[#EBE9E4]';
  const borderColor = isDark ? 'border-[#30363D]' : 'border-[#B0B0B0]';
  const textColor = isDark ? 'text-[#E6EDF3]' : 'text-black';
  const dropdownBg = isDark ? 'bg-[#21262D] border-[#384252] text-[#E6EDF3]' : 'bg-white border-[#B0B0B0] text-black';
  const dropdownHover = isDark ? 'hover:bg-[#30363D]' : 'hover:bg-[#CCE8FF]';

  return (
    <header
      ref={menuContainerRef}
      className={`w-full ${toolbarBg} border-b ${borderColor} select-none z-30 relative`}
    >
      {/* 1. Classic ArcMap Title Bar */}
      <div className={`w-full ${titleBarBg} px-2 py-1 flex items-center justify-between gap-2`}>
        <div className="flex items-center gap-1.5 min-w-0">
          <Globe className="w-4 h-4 text-white shrink-0" />
          <span className="text-white font-bold text-xs truncate">
            {isEs
              ? 'Sin título - ArcMap 10.8 (Esri Mobile)'
              : 'Untitled - ArcMap 10.8 (Esri Mobile)'}
          </span>
        </div>

        <div className="flex items-center gap-1.5 shrink-0">
          <button
            type="button"
            onClick={toggleLanguage}
            title={isEs ? 'Switch to English' : 'Cambiar a Español'}
            className="bg-white/20 hover:bg-white/30 text-white font-bold text-[10px] px-1.5 py-0.5 rounded-[2px] cursor-pointer whitespace-nowrap"
          >
            {isEs ? 'ES' : 'EN'}
          </button>

          <button
            type="button"
            onClick={toggleTheme}
            title={isEs ? 'Cambiar Tema' : 'Toggle Theme'}
            className="p-1 rounded-[2px] hover:bg-white/15 cursor-pointer"
          >
            {isDark ? (
              <Sun className="w-3.5 h-3.5 text-[#FFD54F]" />
            ) : (
              <Moon className="w-3.5 h-3.5 text-white" />
            )}
          </button>

          <span
            className={`text-white font-bold text-[9px] px-1.5 py-0.5 rounded-[2px] whitespace-nowrap ${
              viewMode === ArcMapViewMode.DATA_VIEW ? 'bg-[#0079C1]' : 'bg-[#E65100]'
            }`}
          >
            {viewMode === ArcMapViewMode.DATA_VIEW ? 'DATA' : 'LAYOUT'}
          </span>
        </div>
      </div>

      {/* 2. Classic Menu Bar */}
      <div className={`w-full ${menuBarBg} border-b ${borderColor} px-1 py-0.5 flex items-center flex-wrap gap-0.5`}>
        {menuDefs.map((m) => {
          const alignRightOnMobile =
            m.key === 'selection' || m.key === 'geoprocessing' || m.key === 'windows';
          return (
          <div key={m.key} className="relative">
            <button
              type="button"
              onClick={() => setActiveMenu((prev) => (prev === m.key ? null : m.key))}
              className={`px-2 py-1 sm:py-0.5 text-xs rounded-[2px] cursor-pointer whitespace-nowrap ${textColor} ${
                activeMenu === m.key
                  ? isDark
                    ? 'bg-[#30363D] font-bold'
                    : 'bg-[#CCE8FF] font-bold'
                  : isDark
                    ? 'hover:bg-[#30363D]'
                    : 'hover:bg-[#E0DFDA]'
              }`}
            >
              {m.label}
            </button>

            {activeMenu === m.key && (
              <div
                className={`absolute ${
                  alignRightOnMobile ? 'right-0 sm:left-0 sm:right-auto' : 'left-0'
                } top-full mt-0.5 min-w-[235px] max-w-[92vw] border shadow-lg rounded-[3px] py-1 z-50 text-xs ${dropdownBg}`}
              >
                {m.key === 'file' && (
                  <>
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<FilePlus2 className="w-4 h-4 text-[#0079C1]" />}
                      label={isEs ? 'Nueva Capa Vectorial...' : 'New Vector Layer...'}
                      onClick={() => {
                        setActiveMenu(null);
                        openNewLayerDialog();
                      }}
                    />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Plus className="w-4 h-4 text-[#F57F17]" />}
                      label={isEs ? 'Añadir Datos... (Shapefile)' : 'Add Data... (Shapefile)'}
                      onClick={() => {
                        setActiveMenu(null);
                        toggleCatalog(true);
                      }}
                    />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Layers className="w-4 h-4 text-[#0079C1]" />}
                      label={
                        isEs
                          ? 'Añadir mapa base... (Satélite/OSM)'
                          : 'Add Basemap... (Satellite/OSM)'
                      }
                      onClick={() => {
                        setActiveMenu(null);
                        openBasemapDialog();
                      }}
                    />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<MapPinPlus className="w-4 h-4 text-[#2E7D32]" />}
                      label={
                        isEs ? 'Capturar Punto GPS Actual' : 'Capture Current GPS Point'
                      }
                      onClick={() => {
                        setActiveMenu(null);
                        requestGpsAndCapture();
                      }}
                    />
                    <MenuDivider isDark={isDark} />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Compass className="w-4 h-4 text-[#607D8B]" />}
                      label={isEs ? 'Cargar Muestra Esri (Cusco)' : 'Load Esri Sample Data'}
                      onClick={() => {
                        setActiveMenu(null);
                        loadSampleData();
                      }}
                    />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Trash2 className="w-4 h-4 text-[#E53935]" />}
                      label={isEs ? 'Limpiar todas las capas' : 'Clear all layers'}
                      onClick={() => {
                        setActiveMenu(null);
                        clearAllLayers();
                      }}
                    />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Share2 className="w-4 h-4 text-[#1565C0]" />}
                      label={isEs ? 'Exportar capa como GeoJSON' : 'Export layer as GeoJSON'}
                      onClick={() => {
                        setActiveMenu(null);
                        exportActiveLayerAsGeoJson();
                      }}
                    />
                    <MenuDivider isDark={isDark} />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Printer className="w-4 h-4" />}
                      label={isEs ? 'Vista de Composición (Layout)' : 'Layout View (Sheet)'}
                      onClick={() => {
                        setActiveMenu(null);
                        setViewMode(ArcMapViewMode.LAYOUT_VIEW);
                      }}
                    />
                  </>
                )}

                {m.key === 'edit' && (
                  <>
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<PenTool className="w-4 h-4" />}
                      label={
                        isEditorActive
                          ? isEs
                            ? 'Detener Edición'
                            : 'Stop Editing'
                          : isEs
                            ? 'Iniciar Edición'
                            : 'Start Editing'
                      }
                      onClick={() => {
                        setActiveMenu(null);
                        toggleEditor(!isEditorActive);
                      }}
                    />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Eraser className="w-4 h-4" />}
                      label={isEs ? 'Limpiar Selección' : 'Clear Selection'}
                      onClick={() => {
                        setActiveMenu(null);
                        clearAllSelections();
                      }}
                    />
                  </>
                )}

                {m.key === 'view' && (
                  <>
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<MapIcon className="w-4 h-4" />}
                      label={isEs ? 'Vista de Datos (Mapa)' : 'Data View (Map)'}
                      onClick={() => {
                        setActiveMenu(null);
                        setViewMode(ArcMapViewMode.DATA_VIEW);
                      }}
                    />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Printer className="w-4 h-4" />}
                      label={
                        isEs ? 'Vista de Composición (Lámina)' : 'Layout View (Paper Sheet)'
                      }
                      onClick={() => {
                        setActiveMenu(null);
                        setViewMode(ArcMapViewMode.LAYOUT_VIEW);
                      }}
                    />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Maximize2 className="w-4 h-4" />}
                      label={isEs ? 'Extensión Completa' : 'Full Extent'}
                      onClick={() => {
                        setActiveMenu(null);
                        zoomFullExtent();
                      }}
                    />
                  </>
                )}

                {m.key === 'selection' && (
                  <>
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Filter className="w-4 h-4" />}
                      label={
                        isEs
                          ? 'Seleccionar por Atributos (SQL)...'
                          : 'Select by Attributes...'
                      }
                      onClick={() => {
                        setActiveMenu(null);
                        toggleToolbox(true);
                      }}
                    />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Eraser className="w-4 h-4" />}
                      label={
                        isEs ? 'Deseleccionar Entidades' : 'Clear Selected Features'
                      }
                      onClick={() => {
                        setActiveMenu(null);
                        clearAllSelections();
                      }}
                    />
                  </>
                )}

                {m.key === 'geoprocessing' && (
                  <>
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Wrench className="w-4 h-4 text-[#D32F2F]" />}
                      label="ArcToolbox..."
                      onClick={() => {
                        setActiveMenu(null);
                        toggleToolbox(true);
                      }}
                    />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<CircleDot className="w-4 h-4" />}
                      label="Buffer (Área de Influencia)"
                      onClick={() => {
                        setActiveMenu(null);
                        toggleToolbox(true);
                      }}
                    />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Scissors className="w-4 h-4" />}
                      label="Clip (Recortar)"
                      onClick={() => {
                        setActiveMenu(null);
                        toggleToolbox(true);
                      }}
                    />
                  </>
                )}

                {m.key === 'windows' && (
                  <>
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Layers className="w-4 h-4" />}
                      label={
                        isEs ? 'Tabla de Contenidos (TOC)' : 'Table of Contents'
                      }
                      onClick={() => {
                        setActiveMenu(null);
                        toggleToc(true);
                      }}
                    />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<FolderOpen className="w-4 h-4 text-[#F57F17]" />}
                      label="ArcCatalog"
                      onClick={() => {
                        setActiveMenu(null);
                        toggleCatalog(true);
                      }}
                    />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Wrench className="w-4 h-4 text-[#D32F2F]" />}
                      label="ArcToolbox"
                      onClick={() => {
                        setActiveMenu(null);
                        toggleToolbox(true);
                      }}
                    />
                    <MenuItem
                      hoverClass={dropdownHover}
                      icon={<Search className="w-4 h-4 text-[#0079C1]" />}
                      label={
                        isEs
                          ? 'Buscar (Find / Geocodificador)'
                          : 'Find (Places / Streets)'
                      }
                      onClick={() => {
                        setActiveMenu(null);
                        toggleSearch(true);
                      }}
                    />
                  </>
                )}
              </div>
            )}
          </div>
          );
        })}
      </div>

      {/* 3. Standard & Tools Toolbar (horizontally scrollable with touch-friendly labels on mobile) */}
      <div className="w-full px-1.5 py-1 flex items-center flex-nowrap overflow-x-auto no-scrollbar sm:flex-wrap sm:overflow-visible gap-1">
        {/* Classic Add Data (+) */}
        <button
          type="button"
          onClick={() => toggleCatalog(true)}
          className="h-[42px] sm:h-7 flex flex-col sm:flex-row items-center justify-center gap-0.5 sm:gap-1 bg-[#FFD54F] hover:bg-[#FFCA28] text-black font-bold text-[10px] sm:text-[11px] px-2.5 sm:px-2 py-1 rounded-[3px] shadow-xs border border-[#D4A72C] shrink-0 cursor-pointer whitespace-nowrap"
          title="Add Data (ArcCatalog)"
        >
          <Plus className="w-4 h-4 sm:w-3.5 sm:h-3.5 text-black" strokeWidth={2.75} />
          <span>{isEs ? 'Datos' : 'Add Data'}</span>
        </button>

        {/* Classic Basemap */}
        <button
          type="button"
          onClick={openBasemapDialog}
          className={`h-[42px] sm:h-7 flex flex-col sm:flex-row items-center justify-center gap-0.5 sm:gap-1 font-bold text-[10px] sm:text-[11px] px-2.5 sm:px-2 py-1 rounded-[3px] shadow-xs border shrink-0 cursor-pointer whitespace-nowrap ${
            isDark
              ? 'bg-[#1E3A5F] hover:bg-[#254A78] text-white border-[#2E598F]'
              : 'bg-[#E3F2FD] hover:bg-[#BBDEFB] text-[#0D47A1] border-[#90CAF9]'
          }`}
          title={isEs ? 'Añadir Mapa Base' : 'Add Basemap'}
        >
          <Layers className="w-4 h-4 sm:w-3.5 sm:h-3.5 text-[#0079C1]" />
          <span>{isEs ? 'Base' : 'Basemap'}</span>
        </button>

        {/* Table of Contents (placed early on mobile for quick layer access) */}
        <ToolbarIconButton
          title="Table of Contents"
          mobileLabel={isEs ? 'Capas' : 'Layers'}
          isSelected={isTocVisible}
          isDark={isDark}
          onClick={() => toggleToc(!isTocVisible)}
        >
          <SlidersHorizontal className="w-4 h-4 text-[#0079C1]" />
        </ToolbarIconButton>

        {/* Search / Find */}
        <ToolbarIconButton
          title={isEs ? 'Buscar Calles y Lugares' : 'Find Places & Streets'}
          mobileLabel={isEs ? 'Buscar' : 'Find'}
          isDark={isDark}
          onClick={() => toggleSearch(true)}
        >
          <Search className="w-4 h-4 text-[#0079C1]" />
        </ToolbarIconButton>

        {/* ArcCatalog */}
        <ToolbarIconButton
          title="ArcCatalog"
          mobileLabel="Catálogo"
          isDark={isDark}
          onClick={() => toggleCatalog(true)}
        >
          <FolderOpen className="w-4 h-4 text-[#F57F17]" />
        </ToolbarIconButton>

        {/* ArcToolbox */}
        <ToolbarIconButton
          title="ArcToolbox"
          mobileLabel="Toolbox"
          isDark={isDark}
          onClick={() => toggleToolbox(true)}
        >
          <Wrench className="w-4 h-4 text-[#D32F2F]" />
        </ToolbarIconButton>

        <div className={`w-px h-7 sm:h-5 mx-0.5 shrink-0 ${isDark ? 'bg-[#30363D]' : 'bg-[#B0B0B0]'}`} />

        {/* Map Tools */}
        <ToolbarIconButton
          title="Pan"
          mobileLabel={isEs ? 'Mover' : 'Pan'}
          isSelected={activeTool === MapToolMode.PAN}
          isDark={isDark}
          onClick={() => setTool(MapToolMode.PAN)}
        >
          <Hand className="w-4 h-4" />
        </ToolbarIconButton>

        <ToolbarIconButton
          title="Select Features"
          mobileLabel={isEs ? 'Selec.' : 'Select'}
          isSelected={activeTool === MapToolMode.SELECT}
          isDark={isDark}
          onClick={() => setTool(MapToolMode.SELECT)}
        >
          <MousePointerClick
            className={`w-4 h-4 ${
              activeTool === MapToolMode.SELECT ? 'text-[#00B8D4]' : ''
            }`}
          />
        </ToolbarIconButton>

        <ToolbarIconButton
          title={isEs ? 'Limpiar Selección (Deseleccionar)' : 'Clear Selection'}
          mobileLabel={isEs ? 'Desel.' : 'Clear'}
          isDark={isDark}
          onClick={clearAllSelections}
        >
          <Eraser className="w-4 h-4" />
        </ToolbarIconButton>

        <ToolbarIconButton
          title="Identify"
          mobileLabel="Info"
          isSelected={activeTool === MapToolMode.IDENTIFY}
          isDark={isDark}
          onClick={() => setTool(MapToolMode.IDENTIFY)}
        >
          <Info className="w-4 h-4 text-[#0079C1]" />
        </ToolbarIconButton>

        <ToolbarIconButton
          title={isEs ? 'Medir Distancia / Área' : 'Measure Distance / Area'}
          mobileLabel={isEs ? 'Medir' : 'Measure'}
          isSelected={
            activeTool === MapToolMode.MEASURE_DISTANCE ||
            activeTool === MapToolMode.MEASURE_AREA
          }
          isDark={isDark}
          onClick={() =>
            setTool(
              activeTool === MapToolMode.MEASURE_DISTANCE
                ? MapToolMode.MEASURE_AREA
                : MapToolMode.MEASURE_DISTANCE
            )
          }
        >
          <Ruler className="w-4 h-4 text-[#E91E63]" />
        </ToolbarIconButton>

        <ToolbarIconButton
          title="Zoom In"
          mobileLabel="Zoom+"
          isDark={isDark}
          onClick={zoomIn}
        >
          <ZoomIn className="w-4 h-4" />
        </ToolbarIconButton>

        <ToolbarIconButton
          title="Zoom Out"
          mobileLabel="Zoom-"
          isDark={isDark}
          onClick={zoomOut}
        >
          <ZoomOut className="w-4 h-4" />
        </ToolbarIconButton>

        <ToolbarIconButton
          title="Full Extent"
          mobileLabel={isEs ? 'Todo' : 'Extent'}
          isDark={isDark}
          onClick={zoomFullExtent}
        >
          <Maximize2 className="w-4 h-4" />
        </ToolbarIconButton>

        <div className={`w-px h-7 sm:h-5 mx-0.5 shrink-0 ${isDark ? 'bg-[#30363D]' : 'bg-[#B0B0B0]'}`} />

        {/* Editor Toggle */}
        <ToolbarIconButton
          title="Editor"
          mobileLabel={isEs ? 'Editar' : 'Editor'}
          isSelected={isEditorActive}
          isDark={isDark}
          onClick={() => toggleEditor(!isEditorActive)}
        >
          <PenTool
            className={`w-4 h-4 ${
              isEditorActive ? 'text-[#4CAF50]' : isDark ? 'text-gray-300' : 'text-[#555]'
            }`}
          />
        </ToolbarIconButton>

        {isEditorActive && (
          <>
            <ToolbarIconButton
              title={isEs ? '+Punto' : '+Point'}
              mobileLabel={isEs ? '+Punto' : '+Point'}
              isSelected={activeTool === MapToolMode.DIGITIZE_POINT}
              isDark={isDark}
              onClick={() => setTool(MapToolMode.DIGITIZE_POINT)}
            >
              <MapPinPlus className="w-4 h-4" />
            </ToolbarIconButton>
            <ToolbarIconButton
              title={isEs ? '+Línea' : '+Line'}
              mobileLabel={isEs ? '+Línea' : '+Line'}
              isSelected={activeTool === MapToolMode.DIGITIZE_LINE}
              isDark={isDark}
              onClick={() => setTool(MapToolMode.DIGITIZE_LINE)}
            >
              <Waypoints className="w-4 h-4" />
            </ToolbarIconButton>
            <ToolbarIconButton
              title={isEs ? '+Polígono' : '+Polygon'}
              mobileLabel={isEs ? '+Políg.' : '+Poly'}
              isSelected={activeTool === MapToolMode.DIGITIZE_POLYGON}
              isDark={isDark}
              onClick={() => setTool(MapToolMode.DIGITIZE_POLYGON)}
            >
              <SquareDashedMousePointer className="w-4 h-4" />
            </ToolbarIconButton>
            <ToolbarIconButton
              title={isEs ? 'Borrar Entidad al Tocar' : 'Delete Feature on Tap'}
              mobileLabel={isEs ? 'Borrar' : 'Delete'}
              isSelected={activeTool === MapToolMode.DELETE_FEATURE}
              isDark={isDark}
              onClick={() => setTool(MapToolMode.DELETE_FEATURE)}
            >
              <Trash2
                className={`w-4 h-4 ${
                  activeTool === MapToolMode.DELETE_FEATURE ? 'text-[#E53935]' : 'text-[#D32F2F]'
                }`}
              />
            </ToolbarIconButton>
          </>
        )}
      </div>
    </header>
  );
};

const MenuItem: React.FC<{
  icon: React.ReactNode;
  label: string;
  hoverClass: string;
  onClick: () => void;
}> = ({ icon, label, hoverClass, onClick }) => (
  <button
    type="button"
    onClick={onClick}
    className={`w-full flex items-center gap-2.5 px-3 py-2.5 sm:py-1.5 text-[13px] sm:text-xs text-left cursor-pointer ${hoverClass}`}
  >
    <span className="shrink-0">{icon}</span>
    <span className="truncate">{label}</span>
  </button>
);

const MenuDivider: React.FC<{ isDark: boolean }> = ({ isDark }) => (
  <div className={`my-1 border-t ${isDark ? 'border-[#384252]' : 'border-[#E0E0E0]'}`} />
);

const ToolbarIconButton: React.FC<{
  title: string;
  mobileLabel?: string;
  isSelected?: boolean;
  isDark: boolean;
  onClick: () => void;
  children: React.ReactNode;
}> = ({ title, mobileLabel, isSelected = false, isDark, onClick, children }) => {
  const bgClass = isSelected
    ? isDark
      ? 'bg-[#1E3A5F] border-[#0079C1] text-[#64B5F6]'
      : 'bg-[#B0D0F0] border-[#0079C1] text-[#005A9E]'
    : isDark
      ? 'border-transparent text-gray-200 hover:bg-[#2A303C]'
      : 'border-transparent text-gray-800 hover:bg-[#DCDAD5]';

  return (
    <button
      type="button"
      title={title}
      onClick={onClick}
      className={`min-w-[42px] h-[42px] px-1.5 flex flex-col items-center justify-center gap-0.5 sm:flex-row sm:min-w-0 sm:w-7 sm:h-7 sm:px-0 rounded-[3px] border shrink-0 cursor-pointer transition-colors ${bgClass}`}
    >
      {children}
      {mobileLabel && (
        <span className="sm:hidden text-[9px] leading-none font-medium whitespace-nowrap">
          {mobileLabel}
        </span>
      )}
    </button>
  );
};
