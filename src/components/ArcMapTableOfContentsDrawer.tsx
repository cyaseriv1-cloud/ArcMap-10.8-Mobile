import React, { useState } from 'react';
import {
  ArrowDown,
  ArrowUp,
  CheckCircle2,
  Crosshair,
  Eye,
  Folder,
  Globe,
  Layers,
  List,
  Palette,
  Plus,
  Table2,
  Trash2,
  X,
} from 'lucide-react';
import { GeometryType, isBBoxEmpty } from '../core/geometry';
import { type VectorLayer, getLayerBoundingBox } from '../core/model';
import { colorToCss } from '../core/symbology';
import { AppLanguage, ArcMapTheme, useArcMap } from '../state/ArcMapContext';
import { LayerPropertiesDialog } from './LayerPropertiesDialog';

export const ArcMapTableOfContentsDrawer: React.FC = () => {
  const {
    isTocVisible,
    toggleToc,
    layers,
    activeLayer,
    setActiveLayer,
    toggleLayerVisibility,
    openAttributeTable,
    zoomToExtentOfLayer,
    moveLayerUp,
    moveLayerDown,
    setLayerOpacity,
    removeLayer,
    toggleCatalog,
    openBasemapDialog,
    openNewLayerDialog,
    clearAllSelections,
    theme,
    language,
  } = useArcMap();

  const [propertiesLayerId, setPropertiesLayerId] = useState<string | null>(null);
  const [tocTab, setTocTab] = useState<'order' | 'source' | 'visibility' | 'selection'>('order');

  if (!isTocVisible) return null;

  const isDark = theme === ArcMapTheme.DARK;
  const isEs = language === AppLanguage.ES;

  const propertiesLayer = layers.find((l) => l.id === propertiesLayerId) ?? null;

  const displayedLayers =
    tocTab === 'visibility'
      ? [...layers.filter((l) => l.isVisible), ...layers.filter((l) => !l.isVisible)]
      : tocTab === 'selection'
        ? layers.filter((l) => l.selectedFeatureIds.size > 0)
        : layers;

  return (
    <>
      <div
        className="fixed inset-0 bg-black/30 z-30 md:hidden"
        onClick={() => toggleToc(false)}
      />

      <aside
        className={`fixed md:relative left-0 top-0 bottom-0 w-[330px] max-w-[90vw] h-full flex flex-col p-2 border-r z-40 shrink-0 select-none ${
          isDark
            ? 'bg-[#1A1F26] border-[#30363D] text-[#E6EDF3]'
            : 'bg-[#F7F6F2] border-[#B0B0B0] text-black'
        }`}
      >
        {/* TOC Header */}
        <div className="bg-[#335E8A] text-white px-2.5 py-1.5 rounded-[2px] flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            <Layers className="w-4 h-4" />
            <span className="font-bold text-xs">Table of Contents</span>
          </div>
          <button
            type="button"
            onClick={() => toggleToc(false)}
            className="p-0.5 hover:bg-white/20 rounded cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Classic ArcMap TOC Tabs */}
        <div
          className={`flex items-center justify-around p-0.5 border mt-1 ${
            isDark ? 'bg-[#21262D] border-[#30363D]' : 'bg-[#EBE9E4] border-[#D0D0D0]'
          }`}
        >
          <button
            type="button"
            onClick={() => setTocTab('order')}
            className={`flex items-center gap-1 px-2 py-0.5 text-[11px] rounded-[2px] border cursor-pointer ${
              tocTab === 'order'
                ? isDark
                  ? 'bg-[#1A1F26] border-[#384252] font-bold text-white'
                  : 'bg-white border-[#B0B0B0] font-bold text-black'
                : isDark
                  ? 'border-transparent text-gray-400 hover:text-gray-200'
                  : 'border-transparent text-gray-600'
            }`}
          >
            <List className="w-3.5 h-3.5 text-[#0079C1]" />
            <span>Drawing Order</span>
          </button>

          <button
            type="button"
            title="List by Source"
            onClick={() => setTocTab('source')}
            className={`p-1 rounded cursor-pointer ${
              tocTab === 'source'
                ? 'text-[#0079C1]'
                : isDark
                  ? 'text-gray-400 hover:text-gray-200'
                  : 'text-gray-500'
            }`}
          >
            <Folder className="w-3.5 h-3.5" />
          </button>
          <button
            type="button"
            title="List by Visibility"
            onClick={() => setTocTab('visibility')}
            className={`p-1 rounded cursor-pointer ${
              tocTab === 'visibility'
                ? 'text-[#0079C1]'
                : isDark
                  ? 'text-gray-400 hover:text-gray-200'
                  : 'text-gray-500'
            }`}
          >
            <Eye className="w-3.5 h-3.5" />
          </button>
          <button
            type="button"
            title="List by Selection"
            onClick={() => setTocTab('selection')}
            className={`p-1 rounded cursor-pointer ${
              tocTab === 'selection'
                ? 'text-[#0079C1]'
                : isDark
                  ? 'text-gray-400 hover:text-gray-200'
                  : 'text-gray-500'
            }`}
          >
            <CheckCircle2 className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Data Frame: Layers */}
        <div
          className={`mt-1.5 px-2 py-1 rounded-[2px] flex items-center justify-between gap-1.5 ${
            isDark ? 'bg-[#21262D]' : 'bg-[#E8EEF5]'
          }`}
        >
          <div className="flex items-center gap-1.5 min-w-0">
            <Globe className="w-4 h-4 text-[#0079C1] shrink-0" />
            <span
              className={`font-bold text-xs truncate ${
                isDark ? 'text-[#90CAF9]' : 'text-[#1A237E]'
              }`}
            >
              {tocTab === 'source'
                ? isEs
                  ? 'Fuente y CRS'
                  : 'Source & CRS'
                : tocTab === 'visibility'
                  ? isEs
                    ? 'Por Visibilidad'
                    : 'By Visibility'
                  : tocTab === 'selection'
                    ? isEs
                      ? 'Con Selección'
                      : 'Selected Layers'
                    : 'Layers'}
            </span>
            <span className="text-[11px] opacity-65 shrink-0">
              ({displayedLayers.length} {isEs ? 'capas' : 'layers'})
            </span>
          </div>

          {tocTab === 'selection' && displayedLayers.length > 0 && (
            <button
              type="button"
              onClick={clearAllSelections}
              className="text-[10px] font-semibold text-[#0079C1] hover:underline shrink-0 cursor-pointer"
            >
              {isEs ? 'Limpiar' : 'Clear'}
            </button>
          )}
        </div>

        <div className={`my-1 border-t ${isDark ? 'border-[#30363D]' : 'border-[#B0B0B0]'}`} />

        {/* Layer Tree */}
        <div className="flex-1 overflow-y-auto flex flex-col gap-1.5 pr-0.5">
          {displayedLayers.length === 0 ? (
            <div className="text-center py-8 px-3 text-xs opacity-65">
              {tocTab === 'selection'
                ? isEs
                  ? 'Ninguna capa tiene entidades seleccionadas.'
                  : 'No layers currently have selected features.'
                : isEs
                  ? 'No hay capas vectoriales cargadas. Usa "Add Data" o carga los datos de muestra.'
                  : 'No vector layers loaded. Use "Add Data" or load sample data.'}
            </div>
          ) : (
            displayedLayers.map((layer) => {
              const origIndex = layers.findIndex((l) => l.id === layer.id);
              return (
                <ArcMapLayerTreeItem
                  key={layer.id}
                  layer={layer}
                  tocTab={tocTab}
                  isActive={activeLayer?.id === layer.id}
                  isFirst={origIndex <= 0}
                  isLast={origIndex === layers.length - 1}
                  isDark={isDark}
                  isEs={isEs}
                  onSelect={() => setActiveLayer(layer)}
                  onToggleVisibility={() => toggleLayerVisibility(layer.id)}
                  onOpenTable={() => {
                    openAttributeTable(layer);
                    if (window.innerWidth < 768) toggleToc(false);
                  }}
                  onOpenProperties={() => setPropertiesLayerId(layer.id)}
                  onZoomToLayer={() => {
                    const box = getLayerBoundingBox(layer);
                    if (!isBBoxEmpty(box)) {
                      zoomToExtentOfLayer(layer);
                    }
                  }}
                  onMoveUp={() => moveLayerUp(layer.id)}
                  onMoveDown={() => moveLayerDown(layer.id)}
                  onOpacityChange={(val) => setLayerOpacity(layer.id, val)}
                  onDelete={() => removeLayer(layer.id)}
                />
              );
            })
          )}
        </div>

        <div className={`my-1.5 border-t ${isDark ? 'border-[#30363D]' : 'border-[#B0B0B0]'}`} />

        {/* Bottom Action Buttons */}
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={() => {
              toggleCatalog(true);
            }}
            className="flex-1 h-8 bg-[#FFD54F] hover:bg-[#FFCA28] border border-[#B0B0B0] rounded-[3px] flex items-center justify-center gap-1 text-[11px] font-bold text-black cursor-pointer"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Add Data</span>
          </button>

          <button
            type="button"
            onClick={() => {
              openBasemapDialog();
            }}
            className={`flex-1 h-8 border rounded-[3px] flex items-center justify-center gap-1 text-[11px] font-bold cursor-pointer ${
              isDark
                ? 'bg-[#1E3A5F] hover:bg-[#254A78] border-[#0079C1] text-[#90CAF9]'
                : 'bg-[#E3F2FD] hover:bg-[#BBDEFB] border-[#0079C1] text-[#0079C1]'
            }`}
          >
            <Layers className="w-3.5 h-3.5" />
            <span>{isEs ? 'Mapa Base' : 'Basemap'}</span>
          </button>

          <button
            type="button"
            title={isEs ? 'Nueva Capa' : 'New Layer'}
            onClick={() => openNewLayerDialog()}
            className={`h-8 px-2 border rounded-[3px] flex items-center justify-center cursor-pointer ${
              isDark
                ? 'bg-[#21262D] border-[#384252] text-gray-200 hover:bg-[#30363D]'
                : 'bg-white border-[#B0B0B0] text-gray-800 hover:bg-gray-100'
            }`}
          >
            <Plus className="w-3.5 h-3.5" />
          </button>
        </div>
      </aside>

      {propertiesLayer && (
        <LayerPropertiesDialog
          layer={propertiesLayer}
          onClose={() => setPropertiesLayerId(null)}
        />
      )}
    </>
  );
};

const ArcMapLayerTreeItem: React.FC<{
  layer: VectorLayer;
  tocTab: 'order' | 'source' | 'visibility' | 'selection';
  isActive: boolean;
  isFirst: boolean;
  isLast: boolean;
  isDark: boolean;
  isEs: boolean;
  onSelect: () => void;
  onToggleVisibility: () => void;
  onOpenTable: () => void;
  onOpenProperties: () => void;
  onZoomToLayer: () => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
  onOpacityChange: (opacity: number) => void;
  onDelete: () => void;
}> = ({
  layer,
  tocTab,
  isActive,
  isFirst,
  isLast,
  isDark,
  isEs,
  onSelect,
  onToggleVisibility,
  onOpenTable,
  onOpenProperties,
  onZoomToLayer,
  onMoveUp,
  onMoveDown,
  onOpacityChange,
  onDelete,
}) => {
  const [expanded, setExpanded] = useState(false);

  const itemBg = isActive
    ? isDark
      ? 'bg-[#1E3A5F]/60 border-[#0079C1]'
      : 'bg-[#D4E4F7] border-[#0079C1]'
    : isDark
      ? 'bg-[#21262D] border-[#30363D]'
      : 'bg-white border-[#E0E0E0]';

  const legendText =
    tocTab === 'source'
      ? `EPSG:${layer.crs.epsgCode} · ${layer.fields.length} ${isEs ? 'campos' : 'fields'}${layer.labelFieldName ? ` · Lbl: ${layer.labelFieldName}` : ''}`
      : tocTab === 'visibility'
        ? layer.isVisible
          ? isEs
            ? 'Visible en el mapa'
            : 'Visible on map'
          : isEs
            ? 'Oculta en el mapa'
            : 'Hidden on map'
        : layer.symbology.type === 'UniqueValues'
          ? `${isEs ? 'Valores únicos por:' : 'Unique values:'} ${layer.symbology.fieldName}`
          : `${isEs ? 'Símbolo simple' : 'Simple symbol'} (${layer.geometryType.toLowerCase()})`;

  return (
    <div
      onClick={onSelect}
      className={`w-full rounded-[2px] border p-1.5 cursor-pointer transition-colors ${itemBg}`}
    >
      {/* Row 1: Checkbox + Name + Quick Actions */}
      <div className="flex items-center gap-1.5">
        <input
          type="checkbox"
          checked={layer.isVisible}
          onChange={(e) => {
            e.stopPropagation();
            onToggleVisibility();
          }}
          onClick={(e) => e.stopPropagation()}
          className="w-3.5 h-3.5 accent-[#0079C1] cursor-pointer shrink-0"
        />

        <div className="flex-1 min-w-0">
          <div className={`text-xs truncate ${isActive ? 'font-bold' : 'font-medium'}`}>
            {layer.name}
          </div>
          <div className="text-[10px] opacity-65 truncate">
            {layer.features.length} feat · {layer.geometryType}
            {layer.selectedFeatureIds.size > 0
              ? ` · ${layer.selectedFeatureIds.size} sel`
              : ''}
          </div>
        </div>

        <div className="flex items-center gap-0.5 shrink-0" onClick={(e) => e.stopPropagation()}>
          <button
            type="button"
            title={isEs ? 'Tabla de Atributos' : 'Attribute Table'}
            onClick={onOpenTable}
            className={`p-1 rounded cursor-pointer ${isDark ? 'hover:bg-white/10' : 'hover:bg-black/10'}`}
          >
            <Table2 className="w-3.5 h-3.5 text-[#0079C1]" />
          </button>
          <button
            type="button"
            title={isEs ? 'Simbología / Propiedades' : 'Symbology / Properties'}
            onClick={onOpenProperties}
            className={`p-1 rounded cursor-pointer ${isDark ? 'hover:bg-white/10' : 'hover:bg-black/10'}`}
          >
            <Palette className="w-3.5 h-3.5 text-[#AB47BC]" />
          </button>
          <button
            type="button"
            title={isEs ? 'Zoom a Capa' : 'Zoom to Layer'}
            onClick={onZoomToLayer}
            className={`p-1 rounded cursor-pointer ${isDark ? 'hover:bg-white/10' : 'hover:bg-black/10'}`}
          >
            <Crosshair className="w-3.5 h-3.5 text-[#4CAF50]" />
          </button>
          <button
            type="button"
            title={isEs ? 'Eliminar' : 'Remove'}
            onClick={onDelete}
            className={`p-1 rounded cursor-pointer ${isDark ? 'hover:bg-white/10' : 'hover:bg-black/10'}`}
          >
            <Trash2 className="w-3.5 h-3.5 text-[#EF5350]" />
          </button>
        </div>
      </div>

      {/* Row 2: Indented Symbol Swatch + Legend Description + Order/Opacity Toggle */}
      <div className="pl-5 pt-1 flex items-center justify-between gap-1.5">
        <div className="flex items-center gap-1.5 min-w-0">
          <ArcMapSymbolSwatch layer={layer} />
          <span className="text-[10px] opacity-75 truncate">{legendText}</span>
        </div>

        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            setExpanded((v) => !v);
          }}
          className="text-[10px] opacity-65 hover:opacity-100 px-1 cursor-pointer shrink-0"
        >
          {expanded ? '▲' : '▼'}
        </button>
      </div>

      {expanded && (
        <div
          className="mt-1.5 pt-1.5 border-t border-current/15 flex flex-col gap-1.5 pl-5"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-center gap-2 text-[10px]">
            <span className="w-20 shrink-0">
              {isEs ? 'Opacidad:' : 'Opacity:'} {Math.round(layer.opacity * 100)}%
            </span>
            <input
              type="range"
              min={10}
              max={100}
              value={Math.round(layer.opacity * 100)}
              onChange={(e) => onOpacityChange(Number(e.target.value) / 100)}
              className="flex-1 accent-[#0079C1] cursor-pointer"
            />
          </div>

          <div className="flex items-center gap-1 justify-end">
            <button
              type="button"
              disabled={isFirst}
              onClick={onMoveUp}
              className="flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] border border-current/20 rounded disabled:opacity-35 cursor-pointer"
            >
              <ArrowUp className="w-3 h-3" />
              <span>{isEs ? 'Subir' : 'Up'}</span>
            </button>
            <button
              type="button"
              disabled={isLast}
              onClick={onMoveDown}
              className="flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] border border-current/20 rounded disabled:opacity-35 cursor-pointer"
            >
              <ArrowDown className="w-3 h-3" />
              <span>{isEs ? 'Bajar' : 'Down'}</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

const ArcMapSymbolSwatch: React.FC<{ layer: VectorLayer }> = ({ layer }) => {
  const sym = layer.symbology;
  const primaryColor =
    sym.type === 'Simple'
      ? colorToCss(sym.fillColor)
      : colorToCss(Object.values(sym.valueColors)[0] ?? sym.defaultFillColor);
  const strokeColor = colorToCss(sym.strokeColor);

  if (
    layer.geometryType === GeometryType.POINT ||
    layer.geometryType === GeometryType.MULTI_POINT
  ) {
    return (
      <span
        className="w-3 h-3 rounded-full shrink-0 inline-block"
        style={{ backgroundColor: primaryColor, border: `1px solid ${strokeColor}` }}
      />
    );
  }
  if (
    layer.geometryType === GeometryType.POLYLINE ||
    layer.geometryType === GeometryType.MULTI_POLYLINE
  ) {
    return (
      <span
        className="w-4 h-[3px] shrink-0 inline-block"
        style={{ backgroundColor: strokeColor }}
      />
    );
  }
  return (
    <span
      className="w-3.5 h-3.5 shrink-0 inline-block"
      style={{ backgroundColor: primaryColor, border: `1px solid ${strokeColor}` }}
    />
  );
};
