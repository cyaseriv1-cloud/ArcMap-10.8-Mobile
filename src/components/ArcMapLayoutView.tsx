import React, { useEffect, useRef, useState } from 'react';
import { Edit3, Printer, Share2 } from 'lucide-react';
import { colorToCss } from '../core/symbology';
import { getBasemapById } from '../map/basemap';
import { MapCanvasRenderer } from '../map/renderer';
import { AppLanguage, useArcMap } from '../state/ArcMapContext';

export const ArcMapLayoutView: React.FC = () => {
  const {
    layers,
    layoutTitle,
    setLayoutTitle,
    mapScale,
    viewport,
    currentBasemap,
    tileManager,
    renderVersion,
    updateRender,
    cursorUtm,
    language,
  } = useArcMap();

  const [showTitleDialog, setShowTitleDialog] = useState(false);
  const [titleDraft, setTitleDraft] = useState(layoutTitle);
  const frameContainerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const rendererRef = useRef<MapCanvasRenderer>(new MapCanvasRenderer());

  const isEs = language === AppLanguage.ES;

  useEffect(() => {
    const canvas = canvasRef.current;
    const container = frameContainerRef.current;
    if (!canvas || !container) return;

    const rect = container.getBoundingClientRect();
    if (rect.width <= 0 || rect.height <= 0) return;

    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(rect.width * dpr);
    canvas.height = Math.round(rect.height * dpr);

    viewport.screenWidth = rect.width;
    viewport.screenHeight = rect.height;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.save();
    ctx.scale(dpr, dpr);
    rendererRef.current.render({
      ctx,
      viewport,
      layers,
      isDarkMode: false,
      basemap: getBasemapById(currentBasemap),
      tileManager,
    });
    ctx.restore();
  }, [layers, viewport, currentBasemap, tileManager, renderVersion]);

  useEffect(() => {
    const container = frameContainerRef.current;
    if (!container) return;
    const observer = new ResizeObserver(() => updateRender());
    observer.observe(container);
    return () => observer.disconnect();
  }, [updateRender]);

  const handleExportPng = () => {
    updateRender();
    const canvas = canvasRef.current;
    if (!canvas) return;
    try {
      const dataUrl = canvas.toDataURL('image/png');
      const filename = `${layoutTitle.replace(/[^a-zA-Z0-9_-]/g, '_') || 'mapa_layout'}.png`;
      if (window.AndroidBridge?.saveBase64File) {
        const base64 = dataUrl.includes(',') ? dataUrl.split(',')[1] : dataUrl;
        window.AndroidBridge.saveBase64File(filename, 'image/png', base64);
        return;
      }
      const a = document.createElement('a');
      a.href = dataUrl;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    } catch {
      // Canvas may be tainted if non-CORS basemap tiles are active
      window.print();
    }
  };

  return (
    <div className="w-full h-full bg-[#808080] p-3 flex flex-col items-center gap-2 overflow-auto">
      {/* Layout Control Bar */}
      <div className="w-full max-w-[900px] bg-[#EBE9E4] border border-[#B0B0B0] rounded-[4px] px-2.5 py-1 flex items-center justify-between gap-2 shrink-0">
        <div className="flex items-center gap-1.5">
          <Printer className="w-4 h-4 text-[#0079C1]" />
          <span className="text-xs font-bold text-black">Layout View (Lámina A4)</span>
        </div>

        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={() => {
              setTitleDraft(layoutTitle);
              setShowTitleDialog(true);
            }}
            className="flex items-center gap-1 px-2.5 py-1 text-xs border border-[#909090] bg-white hover:bg-gray-100 text-black rounded-[3px] cursor-pointer"
          >
            <Edit3 className="w-3.5 h-3.5" />
            <span>{isEs ? 'Título' : 'Title'}</span>
          </button>

          <button
            type="button"
            onClick={handleExportPng}
            className="flex items-center gap-1 px-2.5 py-1 text-xs bg-[#0079C1] hover:bg-[#00639E] text-white font-semibold rounded-[3px] cursor-pointer"
          >
            <Share2 className="w-3.5 h-3.5" />
            <span>{isEs ? 'Exportar' : 'Export'}</span>
          </button>
        </div>
      </div>

      {/* Paper Sheet */}
      <div className="w-full max-w-[900px] flex-1 min-h-[460px] bg-white border border-gray-700 shadow-2xl p-4 flex flex-col gap-2">
        <div className="text-center">
          <h2 className="text-base font-bold text-black tracking-wide uppercase">
            {layoutTitle}
          </h2>
          <p className="text-[10px] text-gray-500">
            GOBIERNO REGIONAL / SISTEMA DE INFORMACIÓN GEOGRÁFICA
          </p>
        </div>

        {/* Data Frame */}
        <div
          ref={frameContainerRef}
          className="flex-1 w-full border-[1.5px] border-black relative overflow-hidden"
        >
          <canvas ref={canvasRef} className="w-full h-full block" />
        </div>

        {/* Legend & Cartographic Metadata Box */}
        <div className="w-full border border-black bg-[#FAFAFA] p-2 flex items-center justify-between gap-4 text-black">
          <div className="flex-1 min-w-0">
            <div className="text-[10px] font-bold mb-1">
              {isEs ? 'LEYENDA' : 'LEGEND'}
            </div>
            <div className="flex flex-wrap gap-x-4 gap-y-1">
              {layers
                .filter((l) => l.isVisible)
                .slice(0, 6)
                .map((layer) => {
                  const sym = layer.symbology;
                  const swatchColor =
                    sym.type === 'Simple'
                      ? colorToCss(sym.fillColor)
                      : colorToCss(
                          Object.values(sym.valueColors)[0] ?? sym.defaultFillColor
                        );
                  return (
                    <div key={layer.id} className="flex items-center gap-1.5 text-[10px]">
                      <span
                        className="w-2.5 h-2.5 inline-block border border-black/40"
                        style={{ backgroundColor: swatchColor }}
                      />
                      <span className="truncate max-w-[180px]">
                        {layer.name} ({layer.features.length})
                      </span>
                    </div>
                  );
                })}
              {layers.filter((l) => l.isVisible).length === 0 && (
                <span className="text-[10px] text-gray-500">
                  {isEs ? 'Sin capas vectoriales visibles' : 'No visible vector layers'}
                </span>
              )}
            </div>
          </div>

          <div className="text-right text-[10px] shrink-0 leading-tight">
            <div className="font-bold">
              PROYECCIÓN: WGS 84 / UTM {cursorUtm.zone}
              {cursorUtm.hemisphere === 'NORTH' ? 'N' : 'S'}
            </div>
            <div className="text-gray-600">FUENTE: ArcMap 10.8 Esri Mobile</div>
            <div className="text-gray-700 font-mono tabular-nums">
              ESCALA: 1:{mapScale.toLocaleString('en-US')}
            </div>
          </div>
        </div>
      </div>

      {/* Edit Title Dialog */}
      {showTitleDialog && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl border border-gray-300 w-full max-w-sm p-4 flex flex-col gap-3 text-black">
            <h3 className="font-bold text-sm">
              {isEs ? 'Editar Título del Mapa' : 'Edit Map Title'}
            </h3>
            <input
              type="text"
              value={titleDraft}
              onChange={(e) => setTitleDraft(e.target.value)}
              className="w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm focus:outline-none focus:border-[#0079C1]"
            />
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setShowTitleDialog(false)}
                className="px-3 py-1.5 text-xs text-gray-700 hover:bg-gray-100 rounded cursor-pointer"
              >
                {isEs ? 'Cancelar' : 'Cancel'}
              </button>
              <button
                type="button"
                onClick={() => {
                  setLayoutTitle(titleDraft);
                  setShowTitleDialog(false);
                }}
                className="px-3 py-1.5 text-xs bg-[#0079C1] text-white font-semibold rounded cursor-pointer"
              >
                {isEs ? 'Aceptar' : 'Accept'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
