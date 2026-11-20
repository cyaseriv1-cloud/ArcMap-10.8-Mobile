import React from 'react';
import {
  CheckCircle2,
  Layers,
  Layers2,
  Map as MapIcon,
  Moon,
  Mountain,
  Satellite,
  Sun,
  X,
} from 'lucide-react';
import { BASEMAPS, BasemapId } from '../map/basemap';
import { AppLanguage, ArcMapTheme, useArcMap } from '../state/ArcMapContext';

function getBasemapBadgeStyle(id: BasemapId): { bg: string; icon: React.ReactNode } {
  switch (id) {
    case BasemapId.ESRI_IMAGERY:
      return { bg: '#1B5E20', icon: <Satellite className="w-5 h-5 text-white" /> };
    case BasemapId.ESRI_STREETS:
      return { bg: '#0079C1', icon: <MapIcon className="w-5 h-5 text-white" /> };
    case BasemapId.OPEN_STREET_MAP:
      return { bg: '#0277BD', icon: <MapIcon className="w-5 h-5 text-white" /> };
    case BasemapId.CARTO_DARK:
      return { bg: '#263238', icon: <Moon className="w-5 h-5 text-white" /> };
    case BasemapId.CARTO_POSITRON:
      return { bg: '#78909C', icon: <Sun className="w-5 h-5 text-white" /> };
    case BasemapId.OPEN_TOPO:
      return { bg: '#6D4C41', icon: <Mountain className="w-5 h-5 text-white" /> };
    case BasemapId.NONE:
    default:
      return { bg: '#9E9E9E', icon: <Layers2 className="w-5 h-5 text-white" /> };
  }
}

export const ArcMapBasemapDialog: React.FC = () => {
  const {
    isBasemapDialogVisible,
    closeBasemapDialog,
    currentBasemap,
    setBasemap,
    theme,
    language,
  } = useArcMap();

  if (!isBasemapDialogVisible) return null;

  const isDark = theme === ArcMapTheme.DARK;
  const isEs = language === AppLanguage.ES;

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-3">
      <div
        className={`w-full max-w-md rounded-lg border shadow-2xl flex flex-col max-h-[85vh] ${
          isDark
            ? 'bg-[#1E222B] border-[#384252] text-[#E6EDF3]'
            : 'bg-[#F9F9F9] border-[#B0B0B0] text-slate-800'
        }`}
      >
        {/* Title */}
        <div
          className={`px-4 py-3 border-b flex items-center justify-between gap-2 ${
            isDark ? 'border-[#384252]' : 'border-slate-200'
          }`}
        >
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-md bg-[#0079C1]/15 flex items-center justify-center shrink-0">
              <Layers className="w-5 h-5 text-[#0079C1]" />
            </div>
            <div>
              <h3 className="font-bold text-sm">
                {isEs ? 'Añadir mapa base...' : 'Add Basemap...'}
              </h3>
              <p
                className={`text-[11px] ${
                  isDark ? 'text-[#8B949E]' : 'text-slate-500'
                }`}
              >
                ArcGIS Online & Open Map Services
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={closeBasemapDialog}
            className="p-2 sm:p-1 rounded hover:bg-black/10 cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* List */}
        <div className="flex-1 overflow-y-auto p-3 flex flex-col gap-2">
          {BASEMAPS.map((basemap) => {
            const isSelected = currentBasemap === basemap.id;
            const badge = getBasemapBadgeStyle(basemap.id);

            return (
              <button
                key={basemap.id}
                type="button"
                onClick={() => {
                  setBasemap(basemap.id);
                  closeBasemapDialog();
                }}
                className={`w-full text-left p-2.5 rounded-lg flex items-center gap-3 cursor-pointer transition-colors ${
                  isSelected
                    ? 'border-2 border-[#0079C1]'
                    : isDark
                      ? 'border border-[#384252] hover:border-[#0079C1]/60'
                      : 'border border-slate-200 hover:border-[#0079C1]/60'
                } ${isDark ? 'bg-[#282E3A]' : 'bg-white'}`}
              >
                <div
                  className="w-10 h-10 rounded-md flex items-center justify-center shrink-0"
                  style={{ backgroundColor: badge.bg }}
                >
                  {badge.icon}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="font-bold text-[13px] truncate">
                    {isEs ? basemap.titleEs : basemap.titleEn}
                  </div>
                  <div
                    className={`text-[11px] leading-snug mt-0.5 ${
                      isDark ? 'text-[#8B949E]' : 'text-slate-500'
                    }`}
                  >
                    {isEs ? basemap.descriptionEs : basemap.descriptionEn}
                  </div>
                </div>

                {isSelected && (
                  <CheckCircle2 className="w-5 h-5 text-[#0079C1] shrink-0" />
                )}
              </button>
            );
          })}
        </div>

        {/* Footer */}
        <div
          className={`px-4 py-2.5 border-t flex justify-end ${
            isDark ? 'border-[#384252]' : 'border-slate-200'
          }`}
        >
          <button
            type="button"
            onClick={closeBasemapDialog}
            className="px-4 py-2 sm:px-3 sm:py-1 text-[13px] sm:text-xs font-bold text-[#0079C1] hover:bg-[#0079C1]/10 rounded cursor-pointer"
          >
            {isEs ? 'Cerrar' : 'Close'}
          </button>
        </div>
      </div>
    </div>
  );
};
