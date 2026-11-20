import React from 'react';
import { Globe, Printer, RefreshCw } from 'lucide-react';
import { formatUtm } from '../core/crs';
import { ArcMapTheme, ArcMapViewMode, useArcMap } from '../state/ArcMapContext';

export const ArcMapBottomBar: React.FC = () => {
  const {
    statusMessage,
    cursorUtm,
    mapScale,
    viewMode,
    theme,
    viewport,
    setViewMode,
    updateRender,
  } = useArcMap();

  const isDark = theme === ArcMapTheme.DARK;
  const barBg = isDark ? 'bg-[#161B22]' : 'bg-[#EBE9E4]';
  const borderCol = isDark ? 'border-[#30363D]' : 'border-[#B0B0B0]';
  const textColor = isDark ? 'text-[#E6EDF3]' : 'text-black';
  const switchBg = isDark ? 'bg-[#21262D]' : 'bg-[#D6D4CF]';

  const lon = viewport.centerLon;
  const lat = viewport.centerLat;
  const lonDir = lon >= 0 ? 'E' : 'W';
  const latDir = lat >= 0 ? 'N' : 'S';
  const coordText = `${Math.abs(lon).toFixed(4)}°${lonDir} ${Math.abs(lat).toFixed(4)}°${latDir}`;

  return (
    <footer
      className={`w-full ${barBg} border-t ${borderCol} px-1.5 py-0.5 flex items-center gap-2 text-[10px] sm:text-[11px] select-none z-20`}
    >
      {/* Classic Data View / Layout View / Refresh Switcher */}
      <div
        className={`flex items-center ${switchBg} border ${borderCol} rounded-[2px] p-0.5 gap-0.5 shrink-0`}
      >
        <button
          type="button"
          title="Data View"
          onClick={() => setViewMode(ArcMapViewMode.DATA_VIEW)}
          className={`w-6 h-6 flex items-center justify-center rounded-[2px] border cursor-pointer ${
            viewMode === ArcMapViewMode.DATA_VIEW
              ? isDark
                ? 'bg-[#1E3A5F] border-[#0079C1] text-[#64B5F6]'
                : 'bg-white border-[#0079C1] text-[#0079C1]'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <Globe className="w-3.5 h-3.5" />
        </button>

        <button
          type="button"
          title="Layout View"
          onClick={() => setViewMode(ArcMapViewMode.LAYOUT_VIEW)}
          className={`w-6 h-6 flex items-center justify-center rounded-[2px] border cursor-pointer ${
            viewMode === ArcMapViewMode.LAYOUT_VIEW
              ? isDark
                ? 'bg-[#1E3A5F] border-[#0079C1] text-[#64B5F6]'
                : 'bg-white border-[#0079C1] text-[#0079C1]'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <Printer className="w-3.5 h-3.5" />
        </button>

        <button
          type="button"
          title="Refresh"
          onClick={updateRender}
          className={`w-6 h-6 flex items-center justify-center rounded-[2px] cursor-pointer ${
            isDark ? 'text-gray-300 hover:bg-[#30363D]' : 'text-gray-700 hover:bg-white/60'
          }`}
        >
          <RefreshCw className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Responsive Status + WGS84 + UTM + Scale (prevents UTM/scale from clipping off mobile screens) */}
      <div className="flex-1 min-w-0 flex flex-col sm:flex-row sm:items-center gap-0.5 sm:gap-2 leading-tight">
        <div className="flex items-center justify-between gap-2 min-w-0 sm:flex-1">
          <span className={`font-medium truncate ${textColor}`}>
            {statusMessage || 'Listo'}
          </span>
          <span className={`font-bold font-mono tabular-nums shrink-0 sm:hidden ${textColor}`}>
            1:{mapScale.toLocaleString('en-US')}
          </span>
        </div>

        <div className="flex items-center gap-1.5 sm:gap-2 overflow-x-auto whitespace-nowrap shrink-0">
          <span className="hidden sm:inline text-gray-400">|</span>

          <span
            className={`font-mono tabular-nums ${
              isDark ? 'text-[#4DD0E1]' : 'text-[#004D40]'
            }`}
          >
            {coordText}
          </span>

          <span className="text-gray-400">|</span>

          <span
            className={`font-mono tabular-nums ${
              isDark ? 'text-[#90CAF9]' : 'text-[#1A237E]'
            }`}
          >
            UTM: {formatUtm(cursorUtm)}
          </span>

          <span className="hidden sm:inline text-gray-400">|</span>

          <span className={`hidden sm:inline font-bold font-mono tabular-nums ${textColor}`}>
            1: {mapScale.toLocaleString('en-US')}
          </span>
        </div>
      </div>
    </footer>
  );
};
