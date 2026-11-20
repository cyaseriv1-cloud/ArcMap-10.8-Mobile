import React, { useMemo, useState } from 'react';
import {
  ArrowRight,
  Globe,
  Loader2,
  MapPin,
  Navigation,
  PlaneTakeoff,
  Search,
  X,
} from 'lucide-react';
import { featureGetString } from '../core/model';
import { AppLanguage, ArcMapTheme, useArcMap } from '../state/ArcMapContext';

export const ArcMapSearchDialog: React.FC = () => {
  const {
    isSearchVisible,
    toggleSearch,
    searchPlaces,
    searchWorldPlaces,
    clearWorldSearchResults,
    worldSearchResults,
    isSearchingWorld,
    selectFeatureInLayer,
    zoomToFeature,
    zoomToLocation,
    theme,
    language,
  } = useArcMap();

  const [selectedTab, setSelectedTab] = useState<0 | 1>(1);
  const [searchQuery, setSearchQuery] = useState('');

  const localResults = useMemo(
    () => (selectedTab === 0 ? searchPlaces(searchQuery) : []),
    [searchPlaces, searchQuery, selectedTab]
  );

  if (!isSearchVisible) return null;

  const isDark = theme === ArcMapTheme.DARK;
  const isEs = language === AppLanguage.ES;

  const handleQueryChange = (val: string) => {
    setSearchQuery(val);
    if (selectedTab === 1) {
      searchWorldPlaces(val);
    }
  };

  const handleQuickWorldChip = (q: string) => {
    setSearchQuery(q);
    searchWorldPlaces(q);
  };

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-3">
      <div
        className={`w-full max-w-lg rounded-lg border shadow-2xl flex flex-col max-h-[85vh] ${
          isDark
            ? 'bg-[#1E222B] border-[#384252] text-[#E6EDF3]'
            : 'bg-white border-[#B0B0B0] text-slate-800'
        }`}
      >
        {/* Header */}
        <div
          className={`px-4 py-3 border-b flex items-center justify-between gap-2 ${
            isDark ? 'border-[#384252]' : 'border-slate-200'
          }`}
        >
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-md bg-[#0079C1]/15 flex items-center justify-center shrink-0">
              <Search className="w-5 h-5 text-[#0079C1]" />
            </div>
            <div>
              <h3 className="font-bold text-sm">
                {isEs
                  ? 'Find (Buscador y Geocodificador)'
                  : 'Find (Geocoder & Search)'}
              </h3>
              <p
                className={`text-[11px] ${
                  isDark ? 'text-[#8B949E]' : 'text-slate-500'
                }`}
              >
                ArcMap World & Local Feature Search
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => toggleSearch(false)}
            className="p-1 rounded hover:bg-black/10 cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="p-4 flex flex-col gap-3 flex-1 min-h-0">
          {/* Tabs */}
          <div
            className={`grid grid-cols-2 rounded p-0.5 ${
              isDark ? 'bg-[#282E3A]' : 'bg-slate-100'
            }`}
          >
            <button
              type="button"
              onClick={() => setSelectedTab(0)}
              className={`py-1.5 text-xs rounded cursor-pointer transition-colors ${
                selectedTab === 0
                  ? isDark
                    ? 'bg-[#1E222B] text-[#64B5F6] font-bold shadow-xs'
                    : 'bg-white text-[#0079C1] font-bold shadow-xs'
                  : 'opacity-70'
              }`}
            >
              {isEs ? '📍 Entidades Locales' : '📍 Local Layers'}
            </button>
            <button
              type="button"
              onClick={() => setSelectedTab(1)}
              className={`py-1.5 text-xs rounded cursor-pointer transition-colors ${
                selectedTab === 1
                  ? isDark
                    ? 'bg-[#1E222B] text-[#64B5F6] font-bold shadow-xs'
                    : 'bg-white text-[#0079C1] font-bold shadow-xs'
                  : 'opacity-70'
              }`}
            >
              {isEs ? '🌍 Ciudades del Mundo' : '🌍 World Cities'}
            </button>
          </div>

          {/* Search input */}
          <div className="relative flex items-center">
            <span className="absolute left-2.5 text-[#0079C1] pointer-events-none">
              {selectedTab === 0 ? (
                <MapPin className="w-4 h-4" />
              ) : (
                <Globe className="w-4 h-4" />
              )}
            </span>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => handleQueryChange(e.target.value)}
              placeholder={
                selectedTab === 0
                  ? isEs
                    ? 'Ej. Evitamiento, San Antonio, Uvima...'
                    : 'E.g. Street, POI, Zone...'
                  : isEs
                    ? 'Ej. Lima, París, Tokio, Madrid, New York...'
                    : 'E.g. Paris, Tokyo, London, Rome...'
              }
              autoFocus
              className={`w-full pl-9 pr-9 py-2 rounded border text-xs focus:outline-none focus:border-[#0079C1] ${
                isDark
                  ? 'bg-[#282E3A] border-[#384252] text-white placeholder-gray-400'
                  : 'bg-white border-slate-300 text-black placeholder-slate-400'
              }`}
            />
            {isSearchingWorld ? (
              <Loader2 className="w-4 h-4 text-[#0079C1] animate-spin absolute right-2.5" />
            ) : (
              searchQuery && (
                <button
                  type="button"
                  onClick={() => {
                    setSearchQuery('');
                    clearWorldSearchResults();
                  }}
                  className="absolute right-2 p-0.5 rounded hover:bg-black/10 cursor-pointer"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )
            )}
          </div>

          {/* Quick Chips */}
          <div className="flex flex-wrap gap-1.5">
            {selectedTab === 0 ? (
              <>
                {['Evitamiento', 'San Antonio', 'Uvima 7', 'Aeropuerto'].map((chip) => (
                  <button
                    key={chip}
                    type="button"
                    onClick={() => setSearchQuery(chip === 'Uvima 7' ? 'Uvima' : chip)}
                    className={`px-2 py-0.5 text-[11px] rounded border cursor-pointer ${
                      isDark
                        ? 'bg-[#282E3A] border-[#384252] hover:bg-[#30363D]'
                        : 'bg-slate-100 border-slate-300 hover:bg-slate-200'
                    }`}
                  >
                    {chip}
                  </button>
                ))}
              </>
            ) : (
              <>
                {[
                  { label: 'Lima', query: 'Lima, Perú' },
                  { label: 'París', query: 'París, Francia' },
                  { label: 'Tokio', query: 'Tokio, Japón' },
                  { label: 'Madrid', query: 'Madrid, España' },
                ].map((chip) => (
                  <button
                    key={chip.label}
                    type="button"
                    onClick={() => handleQuickWorldChip(chip.query)}
                    className={`px-2 py-0.5 text-[11px] rounded border cursor-pointer ${
                      isDark
                        ? 'bg-[#282E3A] border-[#384252] hover:bg-[#30363D]'
                        : 'bg-slate-100 border-slate-300 hover:bg-slate-200'
                    }`}
                  >
                    {chip.label}
                  </button>
                ))}
              </>
            )}
          </div>

          <div className={`border-t ${isDark ? 'border-[#384252]' : 'border-slate-200'}`} />

          {/* Results */}
          <div className="flex-1 min-h-[160px] max-h-[260px] overflow-y-auto divide-y divide-current/10">
            {selectedTab === 0 ? (
              searchQuery.trim() && localResults.length === 0 ? (
                <div className="py-8 text-center text-xs opacity-65">
                  {isEs
                    ? `No se encontraron entidades para "${searchQuery}"`
                    : `No features found for "${searchQuery}"`}
                </div>
              ) : (
                localResults.map(({ layer, feature }) => {
                  const featName =
                    featureGetString(feature, 'NOMBRE') ?? `Entidad #${feature.id}`;
                  const featSub =
                    featureGetString(feature, 'CATEGORIA') ??
                    featureGetString(feature, 'TIPO') ??
                    layer.name;
                  return (
                    <button
                      key={`${layer.id}-${feature.id}`}
                      type="button"
                      onClick={() => {
                        selectFeatureInLayer(layer.id, feature.id, false);
                        zoomToFeature(feature);
                        toggleSearch(false);
                      }}
                      className={`w-full text-left py-2 px-1.5 flex items-center gap-2.5 cursor-pointer ${
                        isDark ? 'hover:bg-[#282E3A]' : 'hover:bg-slate-50'
                      }`}
                    >
                      <div className="w-7 h-7 rounded bg-[#0079C1]/15 flex items-center justify-center shrink-0">
                        <MapPin className="w-4 h-4 text-[#0079C1]" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="font-bold text-xs truncate">{featName}</div>
                        <div className="text-[11px] opacity-65 truncate">
                          {layer.name} • {featSub}
                        </div>
                      </div>
                      <Navigation className="w-4 h-4 text-[#2E7D32] shrink-0" />
                    </button>
                  );
                })
              )
            ) : searchQuery.trim() && worldSearchResults.length === 0 && !isSearchingWorld ? (
              <div className="py-8 text-center text-xs opacity-65">
                {isEs
                  ? 'Escribe el nombre de una ciudad o dirección del mundo...'
                  : 'Type a world city or address...'}
              </div>
            ) : (
              worldSearchResults.map((place, idx) => (
                <button
                  key={`${place.lat}-${place.lon}-${idx}`}
                  type="button"
                  onClick={() => {
                    zoomToLocation(place.lat, place.lon, 14.5);
                    toggleSearch(false);
                  }}
                  className={`w-full text-left py-2 px-1.5 flex items-center gap-2.5 cursor-pointer ${
                    isDark ? 'hover:bg-[#282E3A]' : 'hover:bg-slate-50'
                  }`}
                >
                  <div className="w-7 h-7 rounded bg-[#2E7D32]/15 flex items-center justify-center shrink-0">
                    <PlaneTakeoff className="w-4 h-4 text-[#2E7D32]" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="font-bold text-xs truncate">
                      {place.displayName.split(',')[0] || place.displayName}
                    </div>
                    <div className="text-[11px] opacity-70 line-clamp-2">
                      {place.displayName}
                    </div>
                    <div className="text-[10px] text-[#0079C1] font-mono">
                      Coords: {place.lat.toFixed(4)}, {place.lon.toFixed(4)} • {place.type}
                    </div>
                  </div>
                  <ArrowRight className="w-4 h-4 text-[#0079C1] shrink-0" />
                </button>
              ))
            )}
          </div>
        </div>

        {/* Footer */}
        <div
          className={`px-4 py-2.5 border-t flex justify-end ${
            isDark ? 'border-[#384252]' : 'border-slate-200'
          }`}
        >
          <button
            type="button"
            onClick={() => toggleSearch(false)}
            className="px-3 py-1 text-xs font-bold text-[#0079C1] hover:bg-[#0079C1]/10 rounded cursor-pointer"
          >
            {isEs ? 'Cerrar' : 'Close'}
          </button>
        </div>
      </div>
    </div>
  );
};
