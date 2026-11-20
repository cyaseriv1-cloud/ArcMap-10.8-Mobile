import React, { useState } from 'react';
import { Palette, X } from 'lucide-react';
import { GeometryType } from '../core/geometry';
import type { VectorLayer } from '../core/model';
import {
  MarkerShape,
  colorToCss,
  colorToHex,
  createSimpleSymbology,
  createUniqueValuesFromFeatures,
  hexToColorRgba,
  type LayerSymbology,
} from '../core/symbology';
import { AppLanguage, ArcMapTheme, useArcMap } from '../state/ArcMapContext';

const PRESET_COLORS: Array<{ hex: string; nameEs: string; nameEn: string }> = [
  { hex: '#E91E63', nameEs: 'Rosa', nameEn: 'Pink' },
  { hex: '#2196F3', nameEs: 'Azul', nameEn: 'Blue' },
  { hex: '#4CAF50', nameEs: 'Verde', nameEn: 'Green' },
  { hex: '#FF9800', nameEs: 'Naranja', nameEn: 'Orange' },
  { hex: '#9C27B0', nameEs: 'Púrpura', nameEn: 'Purple' },
  { hex: '#F44336', nameEs: 'Rojo', nameEn: 'Red' },
  { hex: '#00BCD4', nameEs: 'Cian', nameEn: 'Cyan' },
  { hex: '#FFEB3B', nameEs: 'Amarillo', nameEn: 'Yellow' },
  { hex: '#795548', nameEs: 'Marrón', nameEn: 'Brown' },
  { hex: '#607D8B', nameEs: 'Gris Azul', nameEn: 'Blue Gray' },
  { hex: '#000000', nameEs: 'Negro', nameEn: 'Black' },
  { hex: '#FFFFFF', nameEs: 'Blanco', nameEn: 'White' },
];

export const LayerPropertiesDialog: React.FC<{
  layer: VectorLayer;
  onClose: () => void;
}> = ({ layer, onClose }) => {
  const { setLayerSymbology, setLayerLabelField, theme, language } = useArcMap();
  const isDark = theme === ArcMapTheme.DARK;
  const isEs = language === AppLanguage.ES;

  const initialSimple =
    layer.symbology.type === 'Simple' ? layer.symbology : createSimpleSymbology();

  const [symMode, setSymMode] = useState<'Simple' | 'UniqueValues'>(
    layer.symbology.type === 'UniqueValues' ? 'UniqueValues' : 'Simple'
  );
  const [uniqueField, setUniqueField] = useState<string>(
    layer.symbology.type === 'UniqueValues'
      ? layer.symbology.fieldName
      : layer.fields[0]?.name ?? ''
  );
  const [fillHex, setFillHex] = useState<string>(colorToHex(initialSimple.fillColor));
  const [fillAlpha, setFillAlpha] = useState<number>(initialSimple.fillColor.alpha);
  const [strokeHex, setStrokeHex] = useState<string>(colorToHex(layer.symbology.strokeColor));
  const [strokeWidth, setStrokeWidth] = useState<string>(String(layer.symbology.strokeWidthDp));
  const [pointSize, setPointSize] = useState<string>(String(initialSimple.pointSizeDp));
  const [markerShape, setMarkerShape] = useState<MarkerShape>(initialSimple.markerShape);
  const [labelField, setLabelField] = useState<string>(layer.labelFieldName ?? '');

  const isPointLayer =
    layer.geometryType === GeometryType.POINT ||
    layer.geometryType === GeometryType.MULTI_POINT;

  const uniquePreview =
    symMode === 'UniqueValues' && uniqueField
      ? createUniqueValuesFromFeatures(uniqueField, layer.features)
      : null;

  const handleApply = () => {
    const parsedStrokeW = parseFloat(strokeWidth);
    const finalStrokeW = Number.isFinite(parsedStrokeW) ? Math.max(0.5, parsedStrokeW) : 1.5;
    const parsedPointSize = parseFloat(pointSize);
    const finalPointSize = Number.isFinite(parsedPointSize)
      ? Math.max(2, parsedPointSize)
      : 8;

    let newSymbology: LayerSymbology;
    if (symMode === 'UniqueValues' && uniqueField) {
      const uv = createUniqueValuesFromFeatures(uniqueField, layer.features);
      newSymbology = {
        ...uv,
        strokeColor: hexToColorRgba(strokeHex, 1.0),
        strokeWidthDp: finalStrokeW,
      };
    } else {
      newSymbology = createSimpleSymbology({
        fillColor: hexToColorRgba(fillHex, fillAlpha),
        strokeColor: hexToColorRgba(strokeHex, 1.0),
        strokeWidthDp: finalStrokeW,
        pointSizeDp: finalPointSize,
        markerShape,
      });
    }

    setLayerSymbology(layer.id, newSymbology);
    setLayerLabelField(layer.id, labelField.trim() || null);
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-3">
      <div
        className={`w-full max-w-md max-h-[90vh] flex flex-col rounded-md border shadow-2xl ${
          isDark
            ? 'bg-[#1E222B] border-[#384252] text-[#E6EDF3]'
            : 'bg-[#F7F6F2] border-[#B0B0B0] text-slate-900'
        }`}
      >
        {/* Header */}
        <div className="bg-[#335E8A] text-white px-3 py-2 rounded-t-[4px] flex items-center justify-between gap-2">
          <div className="flex items-center gap-2 min-w-0">
            <Palette className="w-4 h-4 shrink-0" />
            <span className="font-bold text-xs truncate">
              {isEs ? 'Propiedades de Capa:' : 'Layer Properties:'} {layer.name}
            </span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-0.5 hover:bg-white/20 rounded cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-4 text-xs">
          {/* Symbology Mode Switcher */}
          <div>
            <div className="font-bold mb-1.5">
              {isEs ? 'Modo de Simbología' : 'Symbology Mode'}
            </div>
            <div className="grid grid-cols-2 gap-1.5">
              <button
                type="button"
                onClick={() => setSymMode('Simple')}
                className={`py-1.5 px-2 rounded border text-xs font-semibold cursor-pointer ${
                  symMode === 'Simple'
                    ? 'bg-[#0079C1] text-white border-[#0079C1]'
                    : isDark
                      ? 'bg-[#282E3A] border-[#384252] text-gray-300'
                      : 'bg-white border-gray-300 text-gray-700'
                }`}
              >
                {isEs ? 'Símbolo Único' : 'Single Symbol'}
              </button>
              <button
                type="button"
                disabled={layer.fields.length === 0}
                onClick={() => setSymMode('UniqueValues')}
                className={`py-1.5 px-2 rounded border text-xs font-semibold cursor-pointer disabled:opacity-40 ${
                  symMode === 'UniqueValues'
                    ? 'bg-[#0079C1] text-white border-[#0079C1]'
                    : isDark
                      ? 'bg-[#282E3A] border-[#384252] text-gray-300'
                      : 'bg-white border-gray-300 text-gray-700'
                }`}
              >
                {isEs ? 'Valores Únicos' : 'Unique Values'}
              </button>
            </div>
          </div>

          {symMode === 'Simple' ? (
            <>
              {/* Fill Color */}
              <div>
                <div className="font-bold mb-1.5">
                  {isEs ? 'Color de Relleno' : 'Fill Color'}
                </div>
                <div className="grid grid-cols-6 gap-2 mb-2.5">
                  {PRESET_COLORS.map((c) => {
                    const selected = fillHex.toUpperCase() === c.hex.toUpperCase();
                    return (
                      <button
                        key={`fill-${c.hex}`}
                        type="button"
                        title={isEs ? c.nameEs : c.nameEn}
                        onClick={() => setFillHex(c.hex)}
                        className={`w-8 h-8 rounded-full mx-auto cursor-pointer transition-transform ${
                          selected
                            ? 'ring-2 ring-[#0079C1] ring-offset-2 scale-105'
                            : 'border border-gray-400/50'
                        }`}
                        style={{ backgroundColor: c.hex }}
                      />
                    );
                  })}
                </div>

                <div className="flex items-center gap-2 mb-2.5">
                  <span
                    className="w-6 h-6 rounded-full border border-gray-400 shrink-0"
                    style={{ backgroundColor: fillHex }}
                  />
                  <input
                    type="text"
                    value={fillHex}
                    onChange={(e) => setFillHex(e.target.value)}
                    placeholder="#2196F3"
                    className={`flex-1 px-2.5 py-1.5 rounded border font-mono text-xs ${
                      isDark
                        ? 'bg-[#282E3A] border-[#384252] text-white'
                        : 'bg-white border-gray-300 text-black'
                    }`}
                  />
                  <input
                    type="color"
                    value={/^#[0-9A-Fa-f]{6}$/.test(fillHex) ? fillHex : '#2196F3'}
                    onChange={(e) => setFillHex(e.target.value.toUpperCase())}
                    className="w-8 h-7 p-0 border-0 bg-transparent cursor-pointer"
                  />
                </div>

                <div className="flex items-center gap-3">
                  <span className="w-24 shrink-0">
                    {isEs ? 'Opacidad:' : 'Opacity:'} {Math.round(fillAlpha * 100)}%
                  </span>
                  <input
                    type="range"
                    min={5}
                    max={100}
                    value={Math.round(fillAlpha * 100)}
                    onChange={(e) => setFillAlpha(Number(e.target.value) / 100)}
                    className="flex-1 accent-[#0079C1] cursor-pointer"
                  />
                </div>
              </div>
            </>
          ) : (
            <div>
              <label className="font-bold block mb-1">
                {isEs ? 'Campo de Categoría' : 'Value Field'}
              </label>
              <select
                value={uniqueField}
                onChange={(e) => setUniqueField(e.target.value)}
                className={`w-full px-2.5 py-1.5 rounded border text-xs mb-2 ${
                  isDark
                    ? 'bg-[#282E3A] border-[#384252] text-white'
                    : 'bg-white border-gray-300 text-black'
                }`}
              >
                {layer.fields.map((f) => (
                  <option key={f.name} value={f.name}>
                    {f.name}
                  </option>
                ))}
              </select>

              {uniquePreview && (
                <div
                  className={`max-h-32 overflow-y-auto rounded border p-2 flex flex-col gap-1 ${
                    isDark ? 'bg-[#161B22] border-[#30363D]' : 'bg-white border-gray-300'
                  }`}
                >
                  {Object.entries(uniquePreview.valueColors).map(([val, col]) => (
                    <div key={val} className="flex items-center gap-2 text-[11px]">
                      <span
                        className="w-3.5 h-3.5 rounded-sm border border-black/30 shrink-0"
                        style={{ backgroundColor: colorToCss(col) }}
                      />
                      <span className="truncate">{val}</span>
                    </div>
                  ))}
                  {Object.keys(uniquePreview.valueColors).length === 0 && (
                    <span className="text-[11px] opacity-60">
                      {isEs ? 'Sin valores distintos' : 'No distinct values'}
                    </span>
                  )}
                </div>
              )}
            </div>
          )}

          <div className={`border-t ${isDark ? 'border-[#30363D]' : 'border-gray-300'}`} />

          {/* Stroke */}
          <div>
            <div className="font-bold mb-1.5">
              {isEs ? 'Contorno / Borde' : 'Outline / Stroke'}
            </div>
            <div className="flex items-center gap-2 mb-2">
              {PRESET_COLORS.slice(0, 6).map((c) => {
                const selected = strokeHex.toUpperCase() === c.hex.toUpperCase();
                return (
                  <button
                    key={`stroke-${c.hex}`}
                    type="button"
                    onClick={() => setStrokeHex(c.hex)}
                    className={`w-7 h-7 rounded-full cursor-pointer ${
                      selected
                        ? 'ring-2 ring-[#0079C1] ring-offset-1'
                        : 'border border-gray-400/50'
                    }`}
                    style={{ backgroundColor: c.hex }}
                  />
                );
              })}
            </div>
            <div className="flex items-center gap-2">
              <div className="flex-1">
                <label className="text-[10px] opacity-75 block mb-0.5">
                  {isEs ? 'HEX Borde' : 'Stroke HEX'}
                </label>
                <input
                  type="text"
                  value={strokeHex}
                  onChange={(e) => setStrokeHex(e.target.value)}
                  className={`w-full px-2 py-1.5 rounded border font-mono text-xs ${
                    isDark
                      ? 'bg-[#282E3A] border-[#384252] text-white'
                      : 'bg-white border-gray-300 text-black'
                  }`}
                />
              </div>
              <div className="w-24">
                <label className="text-[10px] opacity-75 block mb-0.5">
                  {isEs ? 'Grosor (px)' : 'Width (px)'}
                </label>
                <input
                  type="number"
                  step="0.5"
                  min="0.5"
                  max="12"
                  value={strokeWidth}
                  onChange={(e) => setStrokeWidth(e.target.value)}
                  className={`w-full px-2 py-1.5 rounded border text-xs ${
                    isDark
                      ? 'bg-[#282E3A] border-[#384252] text-white'
                      : 'bg-white border-gray-300 text-black'
                  }`}
                />
              </div>
            </div>
          </div>

          {/* Point options */}
          {isPointLayer && symMode === 'Simple' && (
            <>
              <div className={`border-t ${isDark ? 'border-[#30363D]' : 'border-gray-300'}`} />
              <div>
                <div className="font-bold mb-1.5">
                  {isEs ? 'Símbolo de Punto' : 'Point Symbol'}
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="text-[10px] opacity-75 block mb-0.5">
                      {isEs ? 'Tamaño (px)' : 'Size (px)'}
                    </label>
                    <input
                      type="number"
                      step="1"
                      min="2"
                      max="30"
                      value={pointSize}
                      onChange={(e) => setPointSize(e.target.value)}
                      className={`w-full px-2 py-1.5 rounded border text-xs ${
                        isDark
                          ? 'bg-[#282E3A] border-[#384252] text-white'
                          : 'bg-white border-gray-300 text-black'
                      }`}
                    />
                  </div>
                  <div>
                    <label className="text-[10px] opacity-75 block mb-0.5">
                      {isEs ? 'Forma' : 'Shape'}
                    </label>
                    <select
                      value={markerShape}
                      onChange={(e) => setMarkerShape(e.target.value as MarkerShape)}
                      className={`w-full px-2 py-1.5 rounded border text-xs ${
                        isDark
                          ? 'bg-[#282E3A] border-[#384252] text-white'
                          : 'bg-white border-gray-300 text-black'
                      }`}
                    >
                      <option value={MarkerShape.CIRCLE}>Circle</option>
                      <option value={MarkerShape.SQUARE}>Square</option>
                      <option value={MarkerShape.TRIANGLE}>Triangle</option>
                      <option value={MarkerShape.CROSS}>Cross</option>
                    </select>
                  </div>
                </div>
              </div>
            </>
          )}

          <div className={`border-t ${isDark ? 'border-[#30363D]' : 'border-gray-300'}`} />

          {/* Labels */}
          <div>
            <label className="font-bold block mb-1">
              {isEs ? 'Campo de Etiqueta' : 'Label Field'}
            </label>
            <select
              value={labelField}
              onChange={(e) => setLabelField(e.target.value)}
              className={`w-full px-2.5 py-1.5 rounded border text-xs ${
                isDark
                  ? 'bg-[#282E3A] border-[#384252] text-white'
                  : 'bg-white border-gray-300 text-black'
              }`}
            >
              <option value="">{isEs ? 'Sin etiquetas' : 'No labels'}</option>
              {layer.fields.map((f) => (
                <option key={f.name} value={f.name}>
                  {f.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Footer */}
        <div
          className={`px-4 py-2.5 border-t flex justify-end gap-2 ${
            isDark ? 'border-[#30363D]' : 'border-gray-300'
          }`}
        >
          <button
            type="button"
            onClick={onClose}
            className={`px-3 py-1.5 text-xs rounded border cursor-pointer ${
              isDark
                ? 'border-[#384252] text-gray-300 hover:bg-[#282E3A]'
                : 'border-gray-300 text-gray-700 hover:bg-gray-100'
            }`}
          >
            {isEs ? 'Cancelar' : 'Cancel'}
          </button>
          <button
            type="button"
            onClick={handleApply}
            className="px-4 py-1.5 text-xs rounded bg-[#0079C1] hover:bg-[#00639E] text-white font-bold cursor-pointer"
          >
            {isEs ? 'Aplicar' : 'Apply'}
          </button>
        </div>
      </div>
    </div>
  );
};
