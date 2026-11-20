import React from 'react';
import { Check, Trash2, Undo2, X } from 'lucide-react';
import { MapToolMode } from '../map/interaction';
import { AppLanguage, ArcMapTheme, useArcMap } from '../state/ArcMapContext';

export const MeasureHud: React.FC = () => {
  const {
    activeTool,
    measureSession,
    digitizeSession,
    pendingDeleteCandidate,
    theme,
    language,
    setTool,
    undoMeasurePoint,
    clearMeasureSession,
    undoDigitizePoint,
    clearDigitizeSession,
    completeDigitizing,
    confirmPendingDelete,
    cancelPendingDelete,
  } = useArcMap();

  const isMeasuring =
    activeTool === MapToolMode.MEASURE_DISTANCE || activeTool === MapToolMode.MEASURE_AREA;
  const isDigitizing =
    activeTool === MapToolMode.DIGITIZE_LINE || activeTool === MapToolMode.DIGITIZE_POLYGON;
  const isDeleting = activeTool === MapToolMode.DELETE_FEATURE;

  if (!isMeasuring && !isDigitizing && !isDeleting) return null;

  const isDark = theme === ArcMapTheme.DARK;
  const isEs = language === AppLanguage.ES;

  const cardBg = isDark
    ? 'bg-[#1E222B] border-[#384252] text-[#E6EDF3]'
    : 'bg-white border-[#CBD5E1] text-[#1E293B]';

  return (
    <div className="absolute bottom-14 left-3 right-[68px] sm:left-1/2 sm:right-auto sm:-translate-x-1/2 z-10 sm:w-[340px]">
      <div className={`rounded-lg border shadow-xl p-3 flex flex-col gap-2 ${cardBg}`}>
        {isMeasuring && (
          <>
            <div className="flex items-center justify-between gap-2">
              <span className="text-xs font-bold text-[#0079C1] truncate">
                {activeTool === MapToolMode.MEASURE_DISTANCE
                  ? isEs
                    ? 'Medición de Distancia'
                    : 'Distance Measurement'
                  : isEs
                    ? 'Medición de Área'
                    : 'Area Measurement'}
              </span>
              <div className="flex items-center gap-1.5 shrink-0">
                <button
                  type="button"
                  onClick={() => setTool(MapToolMode.MEASURE_DISTANCE)}
                  className={`px-2.5 py-1.5 sm:px-2 sm:py-1 text-xs sm:text-[10px] font-semibold rounded cursor-pointer ${
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
                  className={`px-2.5 py-1.5 sm:px-2 sm:py-1 text-xs sm:text-[10px] font-semibold rounded cursor-pointer ${
                    activeTool === MapToolMode.MEASURE_AREA
                      ? 'bg-[#0079C1] text-white'
                      : isDark
                        ? 'bg-[#282E3A] text-gray-300'
                        : 'bg-gray-100 text-gray-700'
                  }`}
                >
                  {isEs ? 'Área' : 'Area'}
                </button>
                <button
                  type="button"
                  title={isEs ? 'Cerrar medición' : 'Close measure'}
                  onClick={() => setTool(MapToolMode.PAN)}
                  className="p-2 sm:p-1 rounded hover:bg-black/10 cursor-pointer opacity-70 hover:opacity-100"
                >
                  <X className="w-4 h-4 sm:w-3.5 sm:h-3.5" />
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
                ? 'vértices marcados (toca el mapa para agregar)'
                : 'vertices marked (tap map to add)'}
            </div>

            <div className="flex items-center gap-2 pt-0.5">
              <button
                type="button"
                disabled={measureSession.points.length === 0}
                onClick={undoMeasurePoint}
                className="flex items-center gap-1.5 px-3.5 py-2 sm:px-3 sm:py-1.5 text-[13px] sm:text-xs rounded border border-current/25 disabled:opacity-40 cursor-pointer"
              >
                <Undo2 className="w-4 h-4 sm:w-3.5 sm:h-3.5" />
                <span>{isEs ? 'Deshacer' : 'Undo'}</span>
              </button>

              <button
                type="button"
                onClick={clearMeasureSession}
                className="flex items-center gap-1.5 px-3.5 py-2 sm:px-3 sm:py-1.5 text-[13px] sm:text-xs rounded bg-[#0079C1] text-white cursor-pointer"
              >
                <Trash2 className="w-4 h-4 sm:w-3.5 sm:h-3.5" />
                <span>{isEs ? 'Limpiar' : 'Clear'}</span>
              </button>
            </div>
          </>
        )}

        {isDigitizing && (
          <>
            <div className="flex items-center justify-between gap-2">
              <span className="text-xs font-bold text-[#0079C1]">
                {activeTool === MapToolMode.DIGITIZE_POLYGON
                  ? isEs
                    ? 'Digitalización de Polígono'
                    : 'Polygon Digitizing'
                  : isEs
                    ? 'Digitalización de Línea'
                    : 'Line Digitizing'}
              </span>
              <button
                type="button"
                title={isEs ? 'Salir de digitalización' : 'Exit digitizing'}
                onClick={() => setTool(MapToolMode.PAN)}
                className="p-2 sm:p-1 rounded hover:bg-black/10 cursor-pointer opacity-70 hover:opacity-100"
              >
                <X className="w-4 h-4 sm:w-3.5 sm:h-3.5" />
              </button>
            </div>
            <div className="text-sm font-medium">
              {digitizeSession.points.length}{' '}
              {isEs ? 'vértices agregados' : 'vertices added'}
            </div>
            <div className="flex items-center flex-wrap gap-2 pt-0.5">
              <button
                type="button"
                disabled={digitizeSession.points.length === 0}
                onClick={undoDigitizePoint}
                className="flex items-center gap-1.5 px-3 py-2 sm:px-2.5 sm:py-1.5 text-[13px] sm:text-xs rounded border border-current/25 disabled:opacity-40 cursor-pointer"
              >
                <Undo2 className="w-4 h-4 sm:w-3.5 sm:h-3.5" />
                <span>{isEs ? 'Deshacer' : 'Undo'}</span>
              </button>

              <button
                type="button"
                disabled={digitizeSession.points.length === 0}
                onClick={clearDigitizeSession}
                className="flex items-center gap-1.5 px-3 py-2 sm:px-2.5 sm:py-1.5 text-[13px] sm:text-xs rounded border border-current/25 disabled:opacity-40 cursor-pointer"
              >
                <Trash2 className="w-4 h-4 sm:w-3.5 sm:h-3.5" />
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
                className="flex items-center gap-1.5 px-3.5 py-2 sm:px-3 sm:py-1.5 text-[13px] sm:text-xs rounded bg-[#0079C1] text-white disabled:opacity-40 cursor-pointer font-semibold"
              >
                <Check className="w-4 h-4 sm:w-3.5 sm:h-3.5" />
                <span>{isEs ? 'Guardar' : 'Save'}</span>
              </button>
            </div>
          </>
        )}

        {isDeleting && (
          <>
            <div className="flex items-center justify-between gap-2">
              <span className="text-xs font-bold text-[#D32F2F]">
                {isEs ? 'Borrar Entidad (Editor)' : 'Delete Feature (Editor)'}
              </span>
              <button
                type="button"
                title={isEs ? 'Salir' : 'Exit'}
                onClick={() => setTool(MapToolMode.PAN)}
                className="p-2 sm:p-1 rounded hover:bg-black/10 cursor-pointer opacity-70 hover:opacity-100"
              >
                <X className="w-4 h-4 sm:w-3.5 sm:h-3.5" />
              </button>
            </div>

            {pendingDeleteCandidate ? (
              <>
                <div className="text-xs leading-snug">
                  {isEs ? '¿Eliminar' : 'Delete'}{' '}
                  <span className="font-bold">"{pendingDeleteCandidate.label}"</span>{' '}
                  {isEs ? 'de la capa' : 'from layer'}{' '}
                  <span className="font-semibold">{pendingDeleteCandidate.layerName}</span>?
                </div>
                <div className="flex items-center gap-2 pt-0.5">
                  <button
                    type="button"
                    onClick={cancelPendingDelete}
                    className="flex-1 px-3 py-2 sm:py-1.5 text-[13px] sm:text-xs rounded border border-current/25 cursor-pointer"
                  >
                    {isEs ? 'Cancelar' : 'Cancel'}
                  </button>
                  <button
                    type="button"
                    onClick={confirmPendingDelete}
                    className="flex-1 flex items-center justify-center gap-1.5 px-3 py-2 sm:py-1.5 text-[13px] sm:text-xs rounded bg-[#D32F2F] text-white font-bold cursor-pointer"
                  >
                    <Trash2 className="w-4 h-4 sm:w-3.5 sm:h-3.5" />
                    <span>{isEs ? 'Eliminar' : 'Delete'}</span>
                  </button>
                </div>
              </>
            ) : (
              <div className="text-xs sm:text-[11px] opacity-75">
                {isEs
                  ? 'Toca cualquier entidad visible en el mapa para eliminarla.'
                  : 'Tap any visible feature on the map to delete it.'}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};
