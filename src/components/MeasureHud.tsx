import React from 'react';
import { Check, Trash2, Undo2 } from 'lucide-react';
import { MapToolMode } from '../map/interaction';
import { AppLanguage, ArcMapTheme, useArcMap } from '../state/ArcMapContext';

export const MeasureHud: React.FC = () => {
  const {
    activeTool,
    measureSession,
    digitizeSession,
    theme,
    language,
    setTool,
    undoMeasurePoint,
    clearMeasureSession,
    undoDigitizePoint,
    clearDigitizeSession,
    completeDigitizing,
  } = useArcMap();

  const isMeasuring =
    activeTool === MapToolMode.MEASURE_DISTANCE || activeTool === MapToolMode.MEASURE_AREA;
  const isDigitizing =
    activeTool === MapToolMode.DIGITIZE_LINE || activeTool === MapToolMode.DIGITIZE_POLYGON;

  if (!isMeasuring && !isDigitizing) return null;

  const isDark = theme === ArcMapTheme.DARK;
  const isEs = language === AppLanguage.ES;

  const cardBg = isDark
    ? 'bg-[#1E222B] border-[#384252] text-[#E6EDF3]'
    : 'bg-white border-[#CBD5E1] text-[#1E293B]';

  return (
    <div className="absolute bottom-14 left-1/2 -translate-x-1/2 z-10 w-[min(360px,calc(100%-130px))]">
      <div className={`rounded-lg border shadow-xl p-3 flex flex-col gap-2 ${cardBg}`}>
        {isMeasuring && (
          <>
            <div className="flex items-center justify-between gap-2">
              <span className="text-xs font-bold text-[#0079C1]">
                {activeTool === MapToolMode.MEASURE_DISTANCE
                  ? isEs
                    ? 'Medición de Distancia'
                    : 'Distance Measurement'
                  : isEs
                    ? 'Medición de Área'
                    : 'Area Measurement'}
              </span>
              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => setTool(MapToolMode.MEASURE_DISTANCE)}
                  className={`px-2 py-0.5 text-[10px] font-semibold rounded cursor-pointer ${
                    activeTool === MapToolMode.MEASURE_DISTANCE
                      ? 'bg-[#0079C1] text-white'
                      : isDark
                        ? 'bg-[#282E3A] text-gray-300'
                        : 'bg-gray-100 text-gray-700'
                  }`}
                >
                  {isEs ? 'Distancia' : 'Distance'}
                </button>
                <button
                  type="button"
                  onClick={() => setTool(MapToolMode.MEASURE_AREA)}
                  className={`px-2 py-0.5 text-[10px] font-semibold rounded cursor-pointer ${
                    activeTool === MapToolMode.MEASURE_AREA
                      ? 'bg-[#0079C1] text-white'
                      : isDark
                        ? 'bg-[#282E3A] text-gray-300'
                        : 'bg-gray-100 text-gray-700'
                  }`}
                >
                  {isEs ? 'Área' : 'Area'}
                </button>
              </div>
            </div>

            {activeTool === MapToolMode.MEASURE_DISTANCE ? (
              <div className="text-base font-bold font-mono tabular-nums">
                {isEs ? 'Distancia:' : 'Distance:'} {measureSession.formatDistance()}
              </div>
            ) : (
              <div>
                <div className="text-base font-bold font-mono tabular-nums">
                  {isEs ? 'Área:' : 'Area:'} {measureSession.formatArea()}
                </div>
                <div className="text-xs font-mono tabular-nums opacity-80">
                  {isEs ? 'Perímetro:' : 'Perimeter:'} {measureSession.formatDistance()}
                </div>
              </div>
            )}

            <div className="text-[11px] opacity-70">
              {measureSession.points.length}{' '}
              {isEs
                ? 'vértices marcados (haz clic en el mapa para agregar)'
                : 'vertices marked (click map to add)'}
            </div>

            <div className="flex items-center gap-2 pt-1">
              <button
                type="button"
                disabled={measureSession.points.length === 0}
                onClick={undoMeasurePoint}
                className="flex items-center gap-1 px-2.5 py-1 text-xs rounded border border-current/25 disabled:opacity-40 cursor-pointer"
              >
                <Undo2 className="w-3.5 h-3.5" />
                <span>{isEs ? 'Deshacer' : 'Undo'}</span>
              </button>

              <button
                type="button"
                onClick={clearMeasureSession}
                className="flex items-center gap-1 px-2.5 py-1 text-xs rounded bg-[#0079C1] text-white cursor-pointer"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>{isEs ? 'Limpiar' : 'Clear'}</span>
              </button>
            </div>
          </>
        )}

        {isDigitizing && (
          <>
            <span className="text-xs font-bold text-[#0079C1]">
              {activeTool === MapToolMode.DIGITIZE_POLYGON
                ? isEs
                  ? 'Digitalización de Polígono'
                  : 'Polygon Digitizing'
                : isEs
                  ? 'Digitalización de Línea'
                  : 'Line Digitizing'}
            </span>
            <div className="text-sm font-medium">
              {digitizeSession.points.length}{' '}
              {isEs ? 'vértices agregados' : 'vertices added'}
            </div>
            <div className="flex items-center flex-wrap gap-2 pt-1">
              <button
                type="button"
                disabled={digitizeSession.points.length === 0}
                onClick={undoDigitizePoint}
                className="flex items-center gap-1 px-2.5 py-1 text-xs rounded border border-current/25 disabled:opacity-40 cursor-pointer"
              >
                <Undo2 className="w-3.5 h-3.5" />
                <span>{isEs ? 'Deshacer' : 'Undo'}</span>
              </button>

              <button
                type="button"
                disabled={digitizeSession.points.length === 0}
                onClick={clearDigitizeSession}
                className="flex items-center gap-1 px-2.5 py-1 text-xs rounded border border-current/25 disabled:opacity-40 cursor-pointer"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>{isEs ? 'Limpiar' : 'Clear'}</span>
              </button>

              <button
                type="button"
                disabled={
                  !(
                    (activeTool === MapToolMode.DIGITIZE_LINE &&
                      digitizeSession.points.length >= 2) ||
                    (activeTool === MapToolMode.DIGITIZE_POLYGON &&
                      digitizeSession.points.length >= 3)
                  )
                }
                onClick={completeDigitizing}
                className="flex items-center gap-1 px-2.5 py-1 text-xs rounded bg-[#0079C1] text-white disabled:opacity-40 cursor-pointer"
              >
                <Check className="w-3.5 h-3.5" />
                <span>{isEs ? 'Guardar Entidad' : 'Save Feature'}</span>
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};
