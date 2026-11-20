import React, { useMemo, useRef, useState } from 'react';
import { FolderOpen, Map as MapIcon, Plus, Upload, X } from 'lucide-react';
import { AppLanguage, ArcMapTheme, useArcMap } from '../state/ArcMapContext';

interface CatalogFileEntry {
  key: string;
  name: string;
  stem: string;
  ext: string;
  size: number;
  file: File;
}

export const ArcCatalogDialog: React.FC = () => {
  const {
    isCatalogVisible,
    toggleCatalog,
    loadSampleData,
    loadGeoJsonText,
    loadShapefileBuffers,
    theme,
    language,
  } = useArcMap();

  const fileInputRef = useRef<HTMLInputElement>(null);
  const [entries, setEntries] = useState<CatalogFileEntry[]>([]);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [zipError, setZipError] = useState<string | null>(null);

  const isDark = theme === ArcMapTheme.DARK;
  const isEs = language === AppLanguage.ES;

  const selectedEntry = useMemo(
    () => entries.find((e) => e.key === selectedKey) ?? null,
    [entries, selectedKey]
  );

  if (!isCatalogVisible) return null;

  const addFiles = (fileList: FileList | null) => {
    if (!fileList) return;
    setZipError(null);
    const incoming: CatalogFileEntry[] = [];
    Array.from(fileList).forEach((file) => {
      const dotIdx = file.name.lastIndexOf('.');
      const ext = dotIdx >= 0 ? file.name.slice(dotIdx + 1).toLowerCase() : '';
      const stem = dotIdx >= 0 ? file.name.slice(0, dotIdx).toLowerCase() : file.name.toLowerCase();
      if (['shp', 'dbf', 'geojson', 'json', 'zip'].includes(ext)) {
        incoming.push({
          key: `${file.name}-${file.size}-${file.lastModified}`,
          name: file.name,
          stem,
          ext,
          size: file.size,
          file,
        });
      }
    });

    if (incoming.length > 0) {
      setEntries((prev) => {
        const map = new Map(prev.map((e) => [e.key, e]));
        incoming.forEach((item) => map.set(item.key, item));
        const next = Array.from(map.values());
        return next;
      });
      const primary = incoming.find((e) => e.ext !== 'dbf') ?? incoming[0];
      if (primary) setSelectedKey(primary.key);
    }
  };

  const handleConfirmAdd = async () => {
    if (!selectedEntry) return;
    setZipError(null);
    if (selectedEntry.ext === 'geojson' || selectedEntry.ext === 'json') {
      const text = await selectedEntry.file.text();
      loadGeoJsonText(selectedEntry.file.name.replace(/\.(geo)?json$/i, ''), text);
      toggleCatalog(false);
    } else if (selectedEntry.ext === 'shp') {
      const shpBuf = await selectedEntry.file.arrayBuffer();
      const matchingDbf = entries.find(
        (e) => e.ext === 'dbf' && e.stem === selectedEntry.stem
      );
      const dbfBuf = matchingDbf ? await matchingDbf.file.arrayBuffer() : null;
      loadShapefileBuffers(
        selectedEntry.file.name.replace(/\.shp$/i, ''),
        shpBuf,
        dbfBuf
      );
      toggleCatalog(false);
    } else if (selectedEntry.ext === 'zip') {
      try {
        const zipBuf = await selectedEntry.file.arrayBuffer();
        const extracted = await extractZipShapefile(zipBuf);
        if (!extracted) {
          setZipError(
            isEs
              ? 'El archivo .zip no contiene ningún archivo .shp válido.'
              : 'The .zip archive does not contain a valid .shp file.'
          );
          return;
        }
        loadShapefileBuffers(extracted.layerName, extracted.shpBuffer, extracted.dbfBuffer);
        toggleCatalog(false);
      } catch (err) {
        setZipError(
          isEs
            ? `No se pudo leer el .zip: ${err instanceof Error ? err.message : 'Error'}`
            : `Failed to read .zip: ${err instanceof Error ? err.message : 'Error'}`
        );
      }
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-3">
      <div
        className={`w-full max-w-lg rounded-md border shadow-2xl flex flex-col max-h-[88vh] ${
          isDark
            ? 'bg-[#1E222B] border-[#384252] text-[#E6EDF3]'
            : 'bg-[#F7F6F2] border-[#B0B0B0] text-black'
        }`}
      >
        {/* Header */}
        <div
          className={`px-3 py-2.5 border-b flex items-center justify-between ${
            isDark ? 'border-[#384252]' : 'border-[#D0D0D0]'
          }`}
        >
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 bg-[#FFD54F] rounded-[3px] flex items-center justify-center border border-[#D4A72C]">
              <Plus className="w-4 h-4 text-black" strokeWidth={2.75} />
            </div>
            <div>
              <h3 className="font-bold text-sm">Add Data (ArcCatalog)</h3>
              <p className="text-[11px] opacity-65">
                {isEs
                  ? 'Importar .zip (Shapefile), .shp (+ .dbf) o .geojson'
                  : 'Import .zip (Shapefile), .shp (+ .dbf) or .geojson'}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => toggleCatalog(false)}
            className="p-2 sm:p-1 rounded hover:bg-black/10 cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="p-3 flex flex-col gap-2.5 flex-1 min-h-0 text-xs">
          {/* Look in bar */}
          <div
            className={`px-2.5 py-1.5 rounded flex items-center gap-2 border ${
              isDark
                ? 'bg-[#282E3A] border-[#384252]'
                : 'bg-[#EBE9E4] border-[#CCCCCC]'
            }`}
          >
            <FolderOpen className="w-4 h-4 text-[#F57F17] shrink-0" />
            <span className="truncate flex-1 font-medium">
              {isEs ? 'Explorador Local / Navegador' : 'Local Browser Storage'}
            </span>
          </div>

          {/* Quick action buttons */}
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className={`px-3 py-2 sm:px-2.5 sm:py-1 rounded text-xs sm:text-[11px] font-semibold border cursor-pointer flex items-center gap-1.5 ${
                isDark
                  ? 'bg-[#282E3A] border-[#384252] hover:bg-[#30363D]'
                  : 'bg-white border-gray-300 hover:bg-gray-100'
              }`}
            >
              <Upload className="w-4 h-4 sm:w-3.5 sm:h-3.5 text-[#0079C1]" />
              <span>
                {isEs ? 'Seleccionar archivos...' : 'Choose files...'}
              </span>
            </button>

            <button
              type="button"
              onClick={() => {
                loadSampleData();
                toggleCatalog(false);
              }}
              className="px-3 py-2 sm:px-2.5 sm:py-1 rounded text-xs sm:text-[11px] font-semibold bg-[#E3F2FD] hover:bg-[#BBDEFB] text-[#0D47A1] border border-[#90CAF9] cursor-pointer"
            >
              {isEs ? 'Muestra Esri (Cusco)' : 'Esri Sample (Cusco)'}
            </button>
          </div>

          <input
            ref={fileInputRef}
            type="file"
            multiple
            accept=".shp,.dbf,.geojson,.json,.zip"
            onChange={(e) => {
              addFiles(e.target.files);
              e.target.value = '';
            }}
            className="hidden"
          />

          {zipError && (
            <div className="px-2.5 py-2 rounded border border-[#D32F2F] bg-[#D32F2F]/10 text-[#D32F2F] text-xs font-medium">
              {zipError}
            </div>
          )}

          {/* Drop zone + File list */}
          <div
            onDragOver={(e) => {
              e.preventDefault();
              setIsDragging(true);
            }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={(e) => {
              e.preventDefault();
              setIsDragging(false);
              addFiles(e.dataTransfer.files);
            }}
            className={`flex-1 min-h-[190px] max-h-[250px] overflow-y-auto rounded border ${
              isDragging
                ? 'border-2 border-dashed border-[#0079C1] bg-[#0079C1]/10'
                : isDark
                  ? 'border-[#384252] bg-[#161B22]'
                  : 'border-gray-300 bg-white'
            }`}
          >
            {entries.length === 0 ? (
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="w-full h-full min-h-[180px] flex flex-col items-center justify-center gap-2 p-4 text-center opacity-70 hover:opacity-100 cursor-pointer"
              >
                <Upload className="w-7 h-7 text-[#0079C1]" />
                <div className="font-medium">
                  {isEs
                    ? 'Toca para abrir .zip (Shapefile), .shp (+ .dbf) o .geojson'
                    : 'Tap to open .zip (Shapefile), .shp (+ .dbf) or .geojson'}
                </div>
                <div className="text-[11px] opacity-75">
                  {isEs
                    ? 'O haz clic para explorar en tu equipo'
                    : 'Or click to browse local files'}
                </div>
              </button>
            ) : (
              <div className="divide-y divide-current/10">
                {entries.map((entry) => {
                  const isSelected = selectedKey === entry.key;
                  const canSelect = entry.ext !== 'dbf';
                  return (
                    <button
                      key={entry.key}
                      type="button"
                      disabled={!canSelect}
                      onClick={() => canSelect && setSelectedKey(entry.key)}
                      className={`w-full px-2.5 py-2 flex items-center gap-2 text-left ${
                        isSelected
                          ? isDark
                            ? 'bg-[#1E3A5F]'
                            : 'bg-[#CCE8FF]'
                          : canSelect
                            ? isDark
                              ? 'hover:bg-[#21262D]'
                              : 'hover:bg-slate-50'
                            : 'opacity-60'
                      } ${canSelect ? 'cursor-pointer' : 'cursor-default'}`}
                    >
                      {entry.ext === 'shp' ? (
                        <span className="w-4 h-4 rounded-[2px] bg-[#4CAF50] text-white font-bold text-[10px] flex items-center justify-center shrink-0">
                          S
                        </span>
                      ) : entry.ext === 'dbf' ? (
                        <span className="w-4 h-4 rounded-[2px] bg-[#FF9800] text-white font-bold text-[10px] flex items-center justify-center shrink-0">
                          D
                        </span>
                      ) : entry.ext === 'zip' ? (
                        <span className="w-4 h-4 rounded-[2px] bg-[#7B1FA2] text-white font-bold text-[10px] flex items-center justify-center shrink-0">
                          Z
                        </span>
                      ) : (
                        <MapIcon className="w-4 h-4 text-[#1976D2] shrink-0" />
                      )}

                      <span className="flex-1 truncate font-medium">{entry.name}</span>
                      <span className="text-[10px] opacity-65 shrink-0">
                        {Math.max(1, Math.round(entry.size / 1024))} KB
                      </span>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {/* Name & Type rows */}
          <div className="flex items-center gap-2">
            <span className="w-12 font-semibold opacity-75">Name:</span>
            <input
              type="text"
              readOnly
              value={selectedEntry?.name ?? ''}
              placeholder={
                isEs
                  ? 'Selecciona un archivo .zip, .shp o .geojson'
                  : 'Select a .zip, .shp or .geojson file'
              }
              className={`flex-1 px-2 py-1.5 sm:py-1 rounded border text-xs ${
                isDark
                  ? 'bg-[#282E3A] border-[#384252] text-white'
                  : 'bg-white border-gray-300 text-black'
              }`}
            />
          </div>

          <div className="flex items-center gap-2">
            <span className="w-12 font-semibold opacity-75">Type:</span>
            <span className="text-[11px] opacity-80">
              {isEs
                ? 'Todos los formatos (*.zip, *.shp, *.dbf, *.geojson)'
                : 'All supported formats (*.zip, *.shp, *.dbf, *.geojson)'}
            </span>
          </div>
        </div>

        {/* Footer */}
        <div
          className={`px-3 py-2.5 border-t flex justify-end gap-2 ${
            isDark ? 'border-[#384252]' : 'border-[#D0D0D0]'
          }`}
        >
          <button
            type="button"
            onClick={() => toggleCatalog(false)}
            className={`px-4 py-2.5 sm:px-3 sm:py-1.5 text-[13px] sm:text-xs rounded border cursor-pointer ${
              isDark
                ? 'border-[#384252] text-gray-300 hover:bg-[#282E3A]'
                : 'border-gray-300 text-gray-700 hover:bg-gray-100'
            }`}
          >
            {isEs ? 'Cancelar' : 'Cancel'}
          </button>
          <button
            type="button"
            disabled={!selectedEntry || selectedEntry.ext === 'dbf'}
            onClick={handleConfirmAdd}
            className="px-5 py-2.5 sm:px-4 sm:py-1.5 text-[13px] sm:text-xs rounded bg-[#0079C1] hover:bg-[#00639E] text-white font-bold disabled:opacity-40 cursor-pointer"
          >
            {isEs ? 'Añadir' : 'Add'}
          </button>
        </div>
      </div>
    </div>
  );
};

async function decompressDeflateRaw(compressed: Uint8Array): Promise<Uint8Array> {
  if (typeof DecompressionStream === 'undefined') {
    throw new Error('ZIP comprimido no soportado en este WebView');
  }
  const ds = new DecompressionStream('deflate-raw');
  const writer = ds.writable.getWriter();
  const copy = new Uint8Array(compressed.byteLength);
  copy.set(compressed);
  await writer.write(copy);
  await writer.close();
  const outBuf = await new Response(ds.readable).arrayBuffer();
  return new Uint8Array(outBuf);
}

async function extractZipShapefile(
  zipBuffer: ArrayBuffer
): Promise<{ layerName: string; shpBuffer: ArrayBuffer; dbfBuffer: ArrayBuffer | null } | null> {
  const bytes = new Uint8Array(zipBuffer);
  const view = new DataView(zipBuffer);

  // Locate End of Central Directory (EOCD: 0x06054b50)
  let eocdOffset = -1;
  const minScan = Math.max(0, bytes.length - 65557);
  for (let i = bytes.length - 22; i >= minScan; i--) {
    if (view.getUint32(i, true) === 0x06054b50) {
      eocdOffset = i;
      break;
    }
  }
  if (eocdOffset < 0) {
    throw new Error('Archivo ZIP inválido');
  }

  const centralEntries = view.getUint16(eocdOffset + 10, true);
  const centralOffset = view.getUint32(eocdOffset + 16, true);
  const decoder = new TextDecoder('utf-8');

  const fileEntries = new Map<string, Uint8Array>();
  let pos = centralOffset;
  for (let i = 0; i < centralEntries && pos + 46 <= bytes.length; i++) {
    if (view.getUint32(pos, true) !== 0x02014b50) break;
    const compressionMethod = view.getUint16(pos + 10, true);
    const compressedSize = view.getUint32(pos + 20, true);
    const nameLen = view.getUint16(pos + 28, true);
    const extraLen = view.getUint16(pos + 30, true);
    const commentLen = view.getUint16(pos + 32, true);
    const localHeaderOffset = view.getUint32(pos + 42, true);

    const rawName = decoder.decode(bytes.subarray(pos + 46, pos + 46 + nameLen));
    pos += 46 + nameLen + extraLen + commentLen;

    if (rawName.endsWith('/') || localHeaderOffset + 30 > bytes.length) continue;
    if (view.getUint32(localHeaderOffset, true) !== 0x04034b50) continue;

    const lNameLen = view.getUint16(localHeaderOffset + 26, true);
    const lExtraLen = view.getUint16(localHeaderOffset + 28, true);
    const dataStart = localHeaderOffset + 30 + lNameLen + lExtraLen;
    const dataEnd = dataStart + compressedSize;
    if (dataEnd > bytes.length) continue;

    const slice = bytes.subarray(dataStart, dataEnd);
    let data: Uint8Array | null = null;
    if (compressionMethod === 0) {
      data = bytes.slice(dataStart, dataEnd);
    } else if (compressionMethod === 8) {
      data = await decompressDeflateRaw(slice);
    }

    if (data) {
      const cleanBase = rawName.split('/').pop() ?? rawName;
      fileEntries.set(cleanBase.toLowerCase(), data);
    }
  }

  let shpKey: string | null = null;
  for (const key of fileEntries.keys()) {
    if (key.endsWith('.shp')) {
      shpKey = key;
      break;
    }
  }
  if (!shpKey) return null;

  const shpBytes = fileEntries.get(shpKey)!;
  const stem = shpKey.slice(0, -4);
  const dbfBytes = fileEntries.get(`${stem}.dbf`) ?? null;

  const toArrayBuffer = (u8: Uint8Array): ArrayBuffer => {
    const out = new ArrayBuffer(u8.byteLength);
    new Uint8Array(out).set(u8);
    return out;
  };

  return {
    layerName: stem || 'shapefile',
    shpBuffer: toArrayBuffer(shpBytes),
    dbfBuffer: dbfBytes ? toArrayBuffer(dbfBytes) : null,
  };
}
