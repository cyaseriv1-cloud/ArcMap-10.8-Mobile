import React, { useEffect, useMemo, useState } from 'react';
import { Download, FolderOpen, Share2, X } from 'lucide-react';
import {
  AppLanguage,
  ArcMapTheme,
  ExportFileFormat,
  useArcMap,
} from '../state/ArcMapContext';

function buildDefaultFilename(layerName: string, format: ExportFileFormat): string {
  const cleanStem =
    layerName
      .trim()
      .replace(/[^a-zA-Z0-9_-]/g, '_')
      .replace(/_+/g, '_')
      .replace(/^_|_$/g, '') || 'capa_export';
  return format === ExportFileFormat.SHP_ZIP
    ? `${cleanStem}_shapefile.zip`
    : `${cleanStem}.geojson`;
}

export const ExportLayerDialog: React.FC = () => {
  const {
    theme,
    language,
    layers,
    activeLayer,
    exportDialogState,
    closeExportDialog,
    executeLayerExport,
  } = useArcMap();

  const [selectedLayerId, setSelectedLayerId] = useState<string>('');
  const [format, setFormat] = useState<ExportFileFormat>(ExportFileFormat.SHP_ZIP);
  const [filename, setFilename] = useState<string>('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const selectedLayer = useMemo(
    () =>
      layers.find((l) => l.id === selectedLayerId) ??
      layers.find((l) => l.id === exportDialogState.layerId) ??
      activeLayer ??
      layers[0] ??
      null,
    [activeLayer, exportDialogState.layerId, layers, selectedLayerId]
  );

  useEffect(() => {
    if (!exportDialogState.isOpen) return;
    const initialLayer =
      layers.find((l) => l.id === exportDialogState.layerId) ??
      activeLayer ??
      layers[0] ??
      null;
    const initialFormat = exportDialogState.format;
    setSelectedLayerId(initialLayer?.id ?? '');
    setFormat(initialFormat);
    setFilename(initialLayer ? buildDefaultFilename(initialLayer.name, initialFormat) : '');
    setErrorMsg(null);
  }, [
    activeLayer,
    exportDialogState.format,
    exportDialogState.isOpen,
    exportDialogState.layerId,
    layers,
  ]);

  if (!exportDialogState.isOpen) return null;

  const isDark = theme === ArcMapTheme.DARK;
  const isEs = language === AppLanguage.ES;
  const isAndroidNative =
    typeof window !== 'undefined' && Boolean(window.AndroidBridge);

  const handleLayerChange = (newLayerId: string) => {
    setSelectedLayerId(newLayerId);
    const target = layers.find((l) => l.id === newLayerId);
    if (target) {
      setFilename(buildDefaultFilename(target.name, format));
    }
    setErrorMsg(null);
  };

  const handleFormatChange = (nextFormat: ExportFileFormat) => {
    setFormat(nextFormat);
    const targetExt = nextFormat === ExportFileFormat.SHP_ZIP ? '.zip' : '.geojson';
    const currentStem = filename
      .trim()
      .replace(/\.(zip|geojson|json)$/i, '')
      .trim();
    if (currentStem) {
      setFilename(`${currentStem}${targetExt}`);
    } else if (selectedLayer) {
      setFilename(buildDefaultFilename(selectedLayer.name, nextFormat));
    }
    setErrorMsg(null);
  };

  const submitExport = (mode: 'picker' | 'downloads') => {
    if (!selectedLayer) {
      setErrorMsg(
        isEs
          ? 'No hay ninguna capa disponible para exportar.'
          : 'No layer available to export.'
      );
      return;
    }
    if (!filename.trim()) {
      setErrorMsg(
        isEs
          ? 'Ingresa un nombre de archivo válido.'
          : 'Enter a valid file name.'
      );
      return;
    }
    setErrorMsg(null);
    executeLayerExport({
      layer: selectedLayer,
      format,
      filename,
      mode,
    });
  };

  const borderCol = isDark ? 'border-[#384252]' : 'border-[#B0B0B0]';
  const inputCls = `w-full px-3 py-2.5 sm:py-1.5 min-h-[44px] sm:min-h-0 rounded border text-sm sm:text-xs focus:outline-none focus:border-[#0079C1] ${
    isDark
      ? 'bg-[#282E3A] border-[#384252] text-white placeholder:text-gray-400'
      : 'bg-white border-gray-300 text-black placeholder:text-gray-400'
  }`;

  return (
    <div className="fixed inset-0 bg-black/55 z-50 flex items-center justify-center p-3">
      <div
        className={`w-full max-w-md rounded-md border shadow-2xl flex flex-col max-h-[90dvh] ${
          isDark
            ? 'bg-[#1E222B] border-[#384252] text-[#E6EDF3]'
            : 'bg-[#F7F6F2] border-[#B0B0B0] text-black'
        }`}
      >
        {/* Header */}
        <div className={`px-4 py-2.5 border-b ${borderCol} flex items-center justify-between gap-2`}>
          <div className="flex items-center gap-2 min-w-0">
            <div className="w-7 h-7 sm:w-6 sm:h-6 rounded-[3px] bg-[#0079C1] flex items-center justify-center shrink-0">
              <Download className="w-4 h-4 sm:w-3.5 sm:h-3.5 text-white" />
            </div>
            <div className="min-w-0">
              <h3 className="font-bold text-sm truncate">
                {isEs ? 'Guardar / Exportar Capa' : 'Save / Export Layer'}
              </h3>
              <p className="text-[11px] opacity-65 truncate">
                {isEs
                  ? 'Configura el formato y destino de almacenamiento'
                  : 'Configure file format and save destination'}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={closeExportDialog}
            aria-label={isEs ? 'Cerrar' : 'Close'}
            className="min-h-[44px] min-w-[44px] sm:min-h-0 sm:min-w-0 sm:p-1.5 flex items-center justify-center rounded hover:bg-black/10 cursor-pointer shrink-0"
          >
            <X className="w-5 h-5 sm:w-4 sm:h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-3.5 text-xs">
          {/* Layer selector */}
          <div>
            <label className="font-bold block mb-1.5">
              {isEs ? 'Capa a exportar' : 'Layer to export'}
            </label>
            {layers.length === 0 ? (
              <div
                className={`p-3 rounded border text-xs ${
                  isDark
                    ? 'bg-[#282E3A] border-[#384252] text-amber-300'
                    : 'bg-amber-50 border-amber-300 text-amber-900'
                }`}
              >
                {isEs
                  ? 'No hay capas vectoriales cargadas. Crea una capa con "Nuevo SHP" o añade datos antes de exportar.'
                  : 'No vector layers loaded. Create a layer with "New SHP" or add data before exporting.'}
              </div>
            ) : (
              <select
                value={selectedLayer?.id ?? ''}
                onChange={(e) => handleLayerChange(e.target.value)}
                className={inputCls}
              >
                {layers.map((layer) => (
                  <option key={layer.id} value={layer.id}>
                    {layer.name} ({layer.features.length})
                  </option>
                ))}
              </select>
            )}
          </div>

          {/* Segmented format selector */}
          <div>
            <label className="font-bold block mb-1.5">
              {isEs ? 'Formato de salida' : 'Output format'}
            </label>
            <div
              className={`grid grid-cols-2 p-1 rounded border gap-1 ${
                isDark ? 'bg-[#161B22] border-[#384252]' : 'bg-[#E5E3DD] border-[#C5C3BC]'
              }`}
            >
              <button
                type="button"
                onClick={() => handleFormatChange(ExportFileFormat.SHP_ZIP)}
                className={`min-h-[44px] sm:min-h-[32px] px-2.5 py-1.5 rounded text-xs font-bold flex items-center justify-center gap-1.5 cursor-pointer transition-colors ${
                  format === ExportFileFormat.SHP_ZIP
                    ? 'bg-[#0079C1] text-white shadow-xs'
                    : isDark
                      ? 'text-gray-300 hover:bg-[#242A34]'
                      : 'text-gray-700 hover:bg-white/60'
                }`}
              >
                <Download className="w-3.5 h-3.5 shrink-0" />
                <span>Shapefile (.zip)</span>
              </button>

              <button
                type="button"
                onClick={() => handleFormatChange(ExportFileFormat.GEOJSON)}
                className={`min-h-[44px] sm:min-h-[32px] px-2.5 py-1.5 rounded text-xs font-bold flex items-center justify-center gap-1.5 cursor-pointer transition-colors ${
                  format === ExportFileFormat.GEOJSON
                    ? 'bg-[#0079C1] text-white shadow-xs'
                    : isDark
                      ? 'text-gray-300 hover:bg-[#242A34]'
                      : 'text-gray-700 hover:bg-white/60'
                }`}
              >
                <Share2 className="w-3.5 h-3.5 shrink-0" />
                <span>GeoJSON (.geojson)</span>
              </button>
            </div>
            <p className="mt-1.5 text-[11px] opacity-70">
              {format === ExportFileFormat.SHP_ZIP
                ? isEs
                  ? 'Incluye archivos .shp, .shx, .dbf y .prj (WGS 84) listos para ArcGIS o QGIS.'
                  : 'Includes .shp, .shx, .dbf, and .prj (WGS 84) files for ArcGIS or QGIS.'
                : isEs
                  ? 'Archivo vectorial único RFC 7946 en codificación UTF-8.'
                  : 'Single-file RFC 7946 vector layer in UTF-8 encoding.'}
            </p>
          </div>

          {/* Filename */}
          <div>
            <label className="font-bold block mb-1.5">
              {isEs ? 'Nombre del archivo' : 'File name'}
            </label>
            <input
              type="text"
              value={filename}
              onChange={(e) => {
                setFilename(e.target.value);
                setErrorMsg(null);
              }}
              placeholder={
                format === ExportFileFormat.SHP_ZIP
                  ? 'mi_capa_shapefile.zip'
                  : 'mi_capa.geojson'
              }
              className={inputCls}
            />
          </div>

          {/* Clean unboxed layer summary */}
          {selectedLayer && (
            <div className={`pt-2.5 border-t ${borderCol} flex flex-col gap-1`}>
              <div className="text-[11px] font-medium tabular-nums opacity-85">
                {selectedLayer.features.length}{' '}
                {isEs ? 'entidades' : 'features'} · {selectedLayer.fields.length}{' '}
                {isEs ? 'campos' : 'fields'} · {selectedLayer.geometryType}
              </div>
              <p className="text-[11px] opacity-70 leading-snug">
                {isAndroidNative
                  ? isEs
                    ? 'Al elegir carpeta se abrirá el explorador nativo de Android para que decidas la ubicación exacta.'
                    : 'Choosing a folder opens the native Android file picker so you can select the exact location.'
                  : isEs
                    ? 'En navegador web se descargará el archivo directamente en tu carpeta de descargas.'
                    : 'In web browser mode, the file will download directly to your Downloads folder.'}
              </p>
            </div>
          )}

          {errorMsg && (
            <p className="text-xs font-semibold text-[#D32F2F] dark:text-[#FF8A80]">
              {errorMsg}
            </p>
          )}
        </div>

        {/* Footer actions */}
        <div
          className={`px-4 py-3 border-t ${borderCol} flex flex-col sm:flex-row-reverse sm:items-center gap-2`}
        >
          {isAndroidNative ? (
            <>
              <button
                type="button"
                disabled={!selectedLayer}
                onClick={() => submitExport('picker')}
                className="min-h-[46px] sm:min-h-[34px] px-4 py-2 sm:py-1.5 rounded bg-[#0079C1] hover:bg-[#00639E] text-white font-bold text-[13px] sm:text-xs flex items-center justify-center gap-2 disabled:opacity-40 cursor-pointer"
              >
                <FolderOpen className="w-4 h-4 shrink-0" />
                <span>
                  {isEs ? 'Elegir carpeta y guardar...' : 'Choose folder & save...'}
                </span>
              </button>

              <button
                type="button"
                disabled={!selectedLayer}
                onClick={() => submitExport('downloads')}
                className={`min-h-[44px] sm:min-h-[34px] px-3.5 py-2 sm:py-1.5 rounded border text-[13px] sm:text-xs font-semibold flex items-center justify-center gap-1.5 disabled:opacity-40 cursor-pointer ${
                  isDark
                    ? 'border-[#384252] bg-[#282E3A] text-gray-100 hover:bg-[#323A49]'
                    : 'border-[#B0B0B0] bg-white text-gray-800 hover:bg-gray-100'
                }`}
              >
                <Download className="w-4 h-4 shrink-0" />
                <span>{isEs ? 'Directo a Descargas' : 'Save to Downloads'}</span>
              </button>
            </>
          ) : (
            <button
              type="button"
              disabled={!selectedLayer}
              onClick={() => submitExport('picker')}
              className="min-h-[46px] sm:min-h-[34px] px-4 py-2 sm:py-1.5 rounded bg-[#0079C1] hover:bg-[#00639E] text-white font-bold text-[13px] sm:text-xs flex items-center justify-center gap-2 disabled:opacity-40 cursor-pointer"
            >
              <Download className="w-4 h-4 shrink-0" />
              <span>{isEs ? 'Guardar archivo' : 'Save file'}</span>
            </button>
          )}

          <button
            type="button"
            onClick={closeExportDialog}
            className={`min-h-[44px] sm:min-h-[34px] px-4 py-2 sm:py-1.5 rounded border text-[13px] sm:text-xs cursor-pointer sm:mr-auto ${
              isDark
                ? 'border-[#384252] text-gray-300 hover:bg-[#282E3A]'
                : 'border-gray-300 text-gray-700 hover:bg-gray-100'
            }`}
          >
            {isEs ? 'Cancelar' : 'Cancel'}
          </button>
        </div>
      </div>
    </div>
  );
};
