import React, { useState } from 'react';
import {
  ArrowLeft,
  ChevronDown,
  ChevronRight,
  CircleDot,
  Download,
  Filter,
  Folder,
  FolderOpen,
  Scissors,
  Sparkles,
  Wrench,
  X,
} from 'lucide-react';
import {
  AppLanguage,
  ArcMapTheme,
  ExportFileFormat,
  useArcMap,
} from '../state/ArcMapContext';

enum ToolboxTool {
  NONE = 'NONE',
  BUFFER = 'BUFFER',
  CLIP = 'CLIP',
  SIMPLIFY = 'SIMPLIFY',
  SELECT_BY_ATTRIBUTES = 'SELECT_BY_ATTRIBUTES',
  EXPORT_SHP = 'EXPORT_SHP',
}

export const ArcToolboxDialog: React.FC = () => {
  const {
    isToolboxVisible,
    toggleToolbox,
    layers,
    runBuffer,
    runClip,
    runSimplify,
    runSelectByAttributes,
    openExportDialog,
    theme,
    language,
  } = useArcMap();

  const [selectedTool, setSelectedTool] = useState<ToolboxTool>(ToolboxTool.NONE);
  const [selectedLayerId, setSelectedLayerId] = useState<string>('');
  const [bufferDistance, setBufferDistance] = useState('500');
  const [simplifyTolerance, setSimplifyTolerance] = useState('50');
  const [sqlWhereClause, setSqlWhereClause] = useState('POBLACION > 20000');

  const [expandAnalysis, setExpandAnalysis] = useState(true);
  const [expandSelection, setExpandSelection] = useState(true);
  const [expandCartography, setExpandCartography] = useState(false);
  const [expandConversion, setExpandConversion] = useState(false);

  if (!isToolboxVisible) return null;

  const isDark = theme === ArcMapTheme.DARK;
  const isEs = language === AppLanguage.ES;

  const targetLayer =
    layers.find((l) => l.id === selectedLayerId) ?? layers[0] ?? null;

  const handleClose = () => {
    setSelectedTool(ToolboxTool.NONE);
    toggleToolbox(false);
  };

  const handleExecute = () => {
    if (!targetLayer) {
      handleClose();
      return;
    }

    switch (selectedTool) {
      case ToolboxTool.BUFFER: {
        const dist = parseFloat(bufferDistance);
        runBuffer(targetLayer, Number.isFinite(dist) ? dist : 500);
        break;
      }
      case ToolboxTool.CLIP:
        runClip(targetLayer);
        break;
      case ToolboxTool.SIMPLIFY: {
        const tol = parseFloat(simplifyTolerance);
        runSimplify(targetLayer, Number.isFinite(tol) ? tol : 50);
        break;
      }
      case ToolboxTool.SELECT_BY_ATTRIBUTES:
        runSelectByAttributes(targetLayer, sqlWhereClause);
        break;
      case ToolboxTool.EXPORT_SHP:
        openExportDialog(targetLayer, ExportFileFormat.SHP_ZIP);
        break;
      default:
        break;
    }

    handleClose();
  };

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-3">
      <div
        className={`w-full max-w-md rounded-md border shadow-2xl flex flex-col max-h-[88vh] ${
          isDark
            ? 'bg-[#1E222B] border-[#384252] text-[#E6EDF3]'
            : 'bg-[#F7F6F2] border-[#B0B0B0] text-black'
        }`}
      >
        {/* Header */}
        <div
          className={`px-3.5 py-2.5 border-b flex items-center justify-between ${
            isDark ? 'border-[#384252]' : 'border-[#D0D0D0]'
          }`}
        >
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 rounded-[3px] bg-[#D32F2F] flex items-center justify-center">
              <Wrench className="w-3.5 h-3.5 text-white" />
            </div>
            <div>
              <h3 className="font-bold text-sm">ArcToolbox</h3>
              <p className="text-[11px] opacity-65">
                ArcGIS Desktop 10.8 Geoprocessing
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleClose}
            className="p-2 sm:p-1 rounded hover:bg-black/10 cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-3.5 flex flex-col gap-2.5 text-xs">
          {selectedTool === ToolboxTool.NONE ? (
            <>
              <div className="font-bold">
                {isEs
                  ? 'Cajas de herramientas disponibles:'
                  : 'Available toolboxes:'}
              </div>

              <ToolboxFolder
                title="Analysis Tools"
                isExpanded={expandAnalysis}
                isDark={isDark}
                onToggle={() => setExpandAnalysis((v) => !v)}
              >
                <ToolboxItem
                  name="Buffer"
                  subtitle="Proximity > Buffer (Área de influencia)"
                  icon={<CircleDot className="w-4 h-4 text-[#0079C1]" />}
                  isDark={isDark}
                  onClick={() => setSelectedTool(ToolboxTool.BUFFER)}
                />
                <ToolboxItem
                  name="Clip"
                  subtitle="Extract > Clip (Recortar por extensión)"
                  icon={<Scissors className="w-4 h-4 text-[#0079C1]" />}
                  isDark={isDark}
                  onClick={() => setSelectedTool(ToolboxTool.CLIP)}
                />
              </ToolboxFolder>

              <ToolboxFolder
                title="Selection Tools"
                isExpanded={expandSelection}
                isDark={isDark}
                onToggle={() => setExpandSelection((v) => !v)}
              >
                <ToolboxItem
                  name="Select by Attributes"
                  subtitle="Query > Selección SQL de registros"
                  icon={<Filter className="w-4 h-4 text-[#0079C1]" />}
                  isDark={isDark}
                  onClick={() => setSelectedTool(ToolboxTool.SELECT_BY_ATTRIBUTES)}
                />
              </ToolboxFolder>

              <ToolboxFolder
                title="Cartography Tools"
                isExpanded={expandCartography}
                isDark={isDark}
                onToggle={() => setExpandCartography((v) => !v)}
              >
                <ToolboxItem
                  name="Simplify Line"
                  subtitle="Generalization > Ramer-Douglas-Peucker"
                  icon={<Sparkles className="w-4 h-4 text-[#0079C1]" />}
                  isDark={isDark}
                  onClick={() => setSelectedTool(ToolboxTool.SIMPLIFY)}
                />
              </ToolboxFolder>

              <ToolboxFolder
                title="Conversion Tools"
                isExpanded={expandConversion}
                isDark={isDark}
                onToggle={() => setExpandConversion((v) => !v)}
              >
                <ToolboxItem
                  name="To Shapefile"
                  subtitle="To Shapefile > Exportar .shp / .dbf / .shx / .prj (ZIP)"
                  icon={<Download className="w-4 h-4 text-[#0079C1]" />}
                  isDark={isDark}
                  onClick={() => setSelectedTool(ToolboxTool.EXPORT_SHP)}
                />
              </ToolboxFolder>
            </>
          ) : (
            <div className="flex flex-col gap-3">
              <button
                type="button"
                onClick={() => setSelectedTool(ToolboxTool.NONE)}
                className="flex items-center gap-1.5 py-1.5 sm:py-0 text-[#0079C1] font-semibold text-xs hover:underline w-fit cursor-pointer"
              >
                <ArrowLeft className="w-4 h-4 sm:w-3.5 sm:h-3.5" />
                <span>
                  {isEs
                    ? 'Volver al árbol de ArcToolbox'
                    : 'Back to ArcToolbox tree'}
                </span>
              </button>

              <div
                className={`border-t ${isDark ? 'border-[#384252]' : 'border-gray-300'}`}
              />

              <div>
                <label className="font-bold block mb-1">
                  Input Features ({isEs ? 'Capa de entrada' : 'Input Layer'}):
                </label>
                <select
                  value={targetLayer?.id ?? ''}
                  onChange={(e) => setSelectedLayerId(e.target.value)}
                  className={`w-full px-2.5 py-1.5 rounded border text-xs ${
                    isDark
                      ? 'bg-[#282E3A] border-[#384252] text-white'
                      : 'bg-white border-gray-300 text-black'
                  }`}
                >
                  {layers.length === 0 ? (
                    <option value="">
                      {isEs ? 'Sin capas cargadas' : 'No layers loaded'}
                    </option>
                  ) : (
                    layers.map((l) => (
                      <option key={l.id} value={l.id}>
                        {l.name} ({l.features.length})
                      </option>
                    ))
                  )}
                </select>
              </div>

              {selectedTool === ToolboxTool.BUFFER && (
                <div>
                  <label className="font-semibold block mb-1">
                    Distance [value or field] (meters)
                  </label>
                  <input
                    type="number"
                    value={bufferDistance}
                    onChange={(e) => setBufferDistance(e.target.value)}
                    className={`w-full px-2.5 py-1.5 rounded border text-xs mb-1.5 ${
                      isDark
                        ? 'bg-[#282E3A] border-[#384252] text-white'
                        : 'bg-white border-gray-300 text-black'
                    }`}
                  />
                  <p className="text-[11px] opacity-70">
                    {isEs
                      ? 'Crea polígonos de zona de influencia a una distancia geodésica especificada alrededor de las entidades de entrada.'
                      : 'Creates buffer polygons around input features to a specified geodesic distance.'}
                  </p>
                </div>
              )}

              {selectedTool === ToolboxTool.CLIP && (
                <p className="text-[11px] opacity-80">
                  {isEs
                    ? 'Recorta las entidades de la capa seleccionada utilizando la extensión actual del mapa como capa de recorte.'
                    : 'Clips the selected layer features using the current visible map extent.'}
                </p>
              )}

              {selectedTool === ToolboxTool.SIMPLIFY && (
                <div>
                  <label className="font-semibold block mb-1">
                    Simplification Tolerance (meters)
                  </label>
                  <input
                    type="number"
                    value={simplifyTolerance}
                    onChange={(e) => setSimplifyTolerance(e.target.value)}
                    className={`w-full px-2.5 py-1.5 rounded border text-xs mb-1.5 ${
                      isDark
                        ? 'bg-[#282E3A] border-[#384252] text-white'
                        : 'bg-white border-gray-300 text-black'
                    }`}
                  />
                  <p className="text-[11px] opacity-70">
                    {isEs
                      ? 'Simplifica líneas y polígonos eliminando vértices innecesarios mediante el algoritmo cartográfico Ramer-Douglas-Peucker.'
                      : 'Simplifies lines and polygons by removing redundant vertices via Ramer-Douglas-Peucker.'}
                  </p>
                </div>
              )}

              {selectedTool === ToolboxTool.SELECT_BY_ATTRIBUTES && (
                <div>
                  <label className="font-semibold block mb-1">
                    WHERE Clause (SQL Expression)
                  </label>
                  <textarea
                    rows={3}
                    value={sqlWhereClause}
                    onChange={(e) => setSqlWhereClause(e.target.value)}
                    className={`w-full px-2.5 py-1.5 rounded border font-mono text-xs mb-1.5 ${
                      isDark
                        ? 'bg-[#282E3A] border-[#384252] text-white'
                        : 'bg-white border-gray-300 text-black'
                    }`}
                  />
                  <pre className="text-[10px] opacity-70 whitespace-pre-wrap font-sans">
                    {isEs
                      ? "Expresión SQL para seleccionar entidades. Ejemplos:\nPOBLACION > 20000\nCATEGORIA = 'Residencial'\nNOMBRE LIKE '%San%'"
                      : "SQL expression to select features. Examples:\nPOBLACION > 20000\nCATEGORIA = 'Residencial'\nNOMBRE LIKE '%San%'"}
                  </pre>
                </div>
              )}

              {selectedTool === ToolboxTool.EXPORT_SHP && (
                <p className="text-[11px] opacity-80">
                  {isEs
                    ? 'Exporta la capa completa como un paquete Shapefile ZIP (.shp, .shx, .dbf, .prj).'
                    : 'Exports the complete layer as a Shapefile ZIP bundle (.shp, .shx, .dbf, .prj).'}
                </p>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div
          className={`px-3.5 py-2.5 border-t flex justify-end gap-2 ${
            isDark ? 'border-[#384252]' : 'border-[#D0D0D0]'
          }`}
        >
          <button
            type="button"
            onClick={handleClose}
            className={`px-4 py-2.5 sm:px-3 sm:py-1.5 text-[13px] sm:text-xs rounded border cursor-pointer ${
              isDark
                ? 'border-[#384252] text-gray-300 hover:bg-[#282E3A]'
                : 'border-gray-300 text-gray-700 hover:bg-gray-100'
            }`}
          >
            {selectedTool === ToolboxTool.NONE
              ? isEs
                ? 'Cerrar'
                : 'Close'
              : isEs
                ? 'Cancelar'
                : 'Cancel'}
          </button>

          {selectedTool !== ToolboxTool.NONE && (
            <button
              type="button"
              disabled={!targetLayer}
              onClick={handleExecute}
              className="px-5 py-2.5 sm:px-4 sm:py-1.5 text-[13px] sm:text-xs rounded bg-[#0079C1] hover:bg-[#00639E] text-white font-bold disabled:opacity-40 cursor-pointer"
            >
              OK ({isEs ? 'Ejecutar' : 'Run'})
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

const ToolboxFolder: React.FC<{
  title: string;
  isExpanded: boolean;
  isDark: boolean;
  onToggle: () => void;
  children: React.ReactNode;
}> = ({ title, isExpanded, isDark, onToggle, children }) => (
  <div
    className={`rounded-[3px] border overflow-hidden ${
      isDark ? 'border-[#384252] bg-[#242933]' : 'border-[#D0D0D0] bg-[#FAFAFA]'
    }`}
  >
    <button
      type="button"
      onClick={onToggle}
      className="w-full px-2.5 py-2.5 sm:py-1.5 flex items-center gap-2 text-left cursor-pointer hover:bg-black/5"
    >
      {isExpanded ? (
        <FolderOpen className="w-4 h-4 text-[#FFB300] shrink-0" />
      ) : (
        <Folder className="w-4 h-4 text-[#FFB300] shrink-0" />
      )}
      <span className="font-bold text-xs flex-1">{title}</span>
      {isExpanded ? (
        <ChevronDown className="w-4 h-4 opacity-60" />
      ) : (
        <ChevronRight className="w-4 h-4 opacity-60" />
      )}
    </button>

    {isExpanded && (
      <div
        className={`border-t pl-4 pr-2 py-1 flex flex-col gap-0.5 ${
          isDark ? 'border-[#384252]' : 'border-[#E0E0E0]'
        }`}
      >
        {children}
      </div>
    )}
  </div>
);

const ToolboxItem: React.FC<{
  name: string;
  subtitle: string;
  icon: React.ReactNode;
  isDark: boolean;
  onClick: () => void;
}> = ({ name, subtitle, icon, isDark, onClick }) => (
  <button
    type="button"
    onClick={onClick}
    className={`w-full px-2.5 py-2.5 sm:px-2 sm:py-1.5 rounded flex items-center gap-2 text-left cursor-pointer ${
      isDark ? 'hover:bg-[#30363D]' : 'hover:bg-[#CCE8FF]'
    }`}
  >
    <span className="shrink-0">{icon}</span>
    <div className="min-w-0">
      <div className="font-semibold text-xs sm:text-[11px]">{name}</div>
      <div className="text-[11px] sm:text-[10px] opacity-65 truncate">{subtitle}</div>
    </div>
  </button>
);
