import React, { useEffect, useState } from 'react';
import {
  Check,
  Crosshair,
  Edit3,
  MousePointerClick,
  Plus,
  Trash2,
  X,
} from 'lucide-react';
import { type AttributeValue, FieldType } from '../core/model';
import { AppLanguage, ArcMapTheme, useArcMap } from '../state/ArcMapContext';

export const IdentifyBottomSheet: React.FC = () => {
  const {
    identifiedFeature,
    dismissIdentify,
    selectFeatureInLayer,
    zoomToFeature,
    updateFeatureAttributes,
    deleteFeature,
    addFieldToLayer,
    theme,
    language,
  } = useArcMap();

  const [isEditing, setIsEditing] = useState(false);
  const [draftAttrs, setDraftAttrs] = useState<Record<string, string>>({});
  const [showAddField, setShowAddField] = useState(false);
  const [newFieldName, setNewFieldName] = useState('');
  const [newFieldType, setNewFieldType] = useState<FieldType>(FieldType.STRING);

  useEffect(() => {
    if (!identifiedFeature) {
      setIsEditing(false);
      setShowAddField(false);
      return;
    }
    const { layer, feature } = identifiedFeature;
    const init: Record<string, string> = {};
    const fieldKeys = new Set<string>([
      ...layer.fields.map((f) => f.name),
      ...Object.keys(feature.attributes),
    ]);
    fieldKeys.forEach((key) => {
      const val = feature.attributes[key];
      init[key] = val === null || val === undefined ? '' : String(val);
    });
    setDraftAttrs(init);
    setIsEditing(false);
  }, [identifiedFeature]);

  if (!identifiedFeature) return null;

  const { layer, feature } = identifiedFeature;
  const isDark = theme === ArcMapTheme.DARK;
  const isEs = language === AppLanguage.ES;

  const allKeys = Array.from(
    new Set([
      ...layer.fields.map((f) => f.name),
      ...Object.keys(feature.attributes),
    ])
  );

  const handleSaveEdits = () => {
    const parsed: Record<string, AttributeValue> = {};
    for (const key of allKeys) {
      const fieldDef = layer.fields.find((f) => f.name === key);
      const raw = (draftAttrs[key] ?? '').trim();
      if (raw === '') {
        parsed[key] = null;
      } else if (fieldDef?.type === FieldType.INTEGER) {
        const n = parseInt(raw, 10);
        parsed[key] = Number.isNaN(n) ? raw : n;
      } else if (fieldDef?.type === FieldType.DOUBLE) {
        const n = parseFloat(raw);
        parsed[key] = Number.isNaN(n) ? raw : n;
      } else if (fieldDef?.type === FieldType.BOOLEAN) {
        parsed[key] = raw.toLowerCase() === 'true';
      } else {
        parsed[key] = raw;
      }
    }
    updateFeatureAttributes(layer.id, feature.id, parsed);
    setIsEditing(false);
  };

  const handleAddField = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFieldName.trim()) return;
    const added = addFieldToLayer(layer.id, newFieldName, newFieldType);
    if (added) {
      setDraftAttrs((prev) => ({ ...prev, [newFieldName.trim()]: '' }));
      setNewFieldName('');
      setShowAddField(false);
    }
  };

  return (
    <div className="fixed inset-x-0 bottom-14 sm:bottom-9 z-40 flex justify-center px-3 pointer-events-none">
      <div
        className={`w-full max-w-md rounded-lg border shadow-2xl pointer-events-auto flex flex-col max-h-[65vh] ${
          isDark
            ? 'bg-[#1E222B] border-[#384252] text-[#E6EDF3]'
            : 'bg-white border-[#B0B0B0] text-slate-900'
        }`}
      >
        {/* Header */}
        <div
          className={`px-4 py-2.5 border-b flex items-center justify-between gap-2 ${
            isDark ? 'border-[#384252]' : 'border-slate-200'
          }`}
        >
          <div className="min-w-0">
            <div className="text-[11px] font-bold text-[#0079C1]">
              {isEs ? 'Identificar (Identify)' : 'Identify'}
            </div>
            <div className="text-sm font-bold truncate">
              {layer.name} — ID: {feature.id}
            </div>
          </div>

          <button
            type="button"
            onClick={dismissIdentify}
            className="p-2 sm:p-1 rounded hover:bg-black/10 cursor-pointer"
          >
            <X className="w-5 h-5 sm:w-4 sm:h-4" />
          </button>
        </div>

        {/* Actions */}
        <div className="px-4 py-2 grid grid-cols-3 gap-2 sm:gap-1.5">
          <button
            type="button"
            onClick={() => selectFeatureInLayer(layer.id, feature.id, false)}
            className="py-2.5 sm:py-1.5 px-2 rounded bg-[#0079C1] hover:bg-[#00639E] text-white text-[13px] sm:text-xs font-bold flex items-center justify-center gap-1 cursor-pointer"
          >
            <MousePointerClick className="w-4 h-4 sm:w-3.5 sm:h-3.5 shrink-0" />
            <span className="truncate">{isEs ? 'Seleccionar' : 'Select'}</span>
          </button>

          <button
            type="button"
            onClick={() => zoomToFeature(feature)}
            className={`py-2.5 sm:py-1.5 px-2 rounded border text-[13px] sm:text-xs font-bold flex items-center justify-center gap-1 cursor-pointer ${
              isDark
                ? 'border-[#384252] hover:bg-[#282E3A]'
                : 'border-slate-300 hover:bg-slate-100'
            }`}
          >
            <Crosshair className="w-4 h-4 sm:w-3.5 sm:h-3.5 shrink-0" />
            <span className="truncate">{isEs ? 'Centrar' : 'Center'}</span>
          </button>

          <button
            type="button"
            onClick={() => deleteFeature(layer.id, feature.id)}
            className="py-2.5 sm:py-1.5 px-2 rounded border border-[#D32F2F]/50 text-[#D32F2F] hover:bg-[#D32F2F]/10 text-[13px] sm:text-xs font-bold flex items-center justify-center gap-1 cursor-pointer"
          >
            <Trash2 className="w-4 h-4 sm:w-3.5 sm:h-3.5 shrink-0" />
            <span className="truncate">{isEs ? 'Eliminar' : 'Delete'}</span>
          </button>
        </div>

        {/* Attributes Header */}
        <div className="px-4 py-1.5 sm:py-1 flex items-center justify-between gap-2">
          <span className="text-xs font-bold opacity-75">
            {isEs ? 'Atributos:' : 'Attributes:'}
          </span>

          <div className="flex items-center gap-2 sm:gap-1.5">
            <button
              type="button"
              onClick={() => setShowAddField((v) => !v)}
              className="flex items-center gap-1 px-2.5 py-1.5 sm:px-2 sm:py-0.5 rounded text-xs sm:text-[11px] font-semibold bg-[#0079C1]/15 text-[#0079C1] hover:bg-[#0079C1]/25 cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5 sm:w-3 sm:h-3" />
              <span>{isEs ? 'Campo' : 'Field'}</span>
            </button>

            {isEditing ? (
              <button
                type="button"
                onClick={handleSaveEdits}
                className="flex items-center gap-1 px-2.5 py-1.5 sm:px-2 sm:py-0.5 rounded text-xs sm:text-[11px] font-semibold bg-[#2E7D32] text-white cursor-pointer"
              >
                <Check className="w-3.5 h-3.5 sm:w-3 sm:h-3" />
                <span>{isEs ? 'Guardar' : 'Save'}</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={() => setIsEditing(true)}
                className={`flex items-center gap-1 px-2.5 py-1.5 sm:px-2 sm:py-0.5 rounded text-xs sm:text-[11px] font-semibold border cursor-pointer ${
                  isDark
                    ? 'border-[#384252] hover:bg-[#282E3A]'
                    : 'border-slate-300 hover:bg-slate-100'
                }`}
              >
                <Edit3 className="w-3.5 h-3.5 sm:w-3 sm:h-3" />
                <span>{isEs ? 'Editar' : 'Edit'}</span>
              </button>
            )}
          </div>
        </div>

        {/* Inline Add Field form */}
        {showAddField && (
          <form
            onSubmit={handleAddField}
            className="mx-4 mb-2 p-2 rounded border border-[#0079C1]/40 bg-[#0079C1]/5 flex items-center gap-1.5 text-xs"
          >
            <input
              type="text"
              value={newFieldName}
              onChange={(e) => setNewFieldName(e.target.value)}
              placeholder={isEs ? 'Nuevo campo...' : 'New field...'}
              className={`flex-1 min-w-0 px-2 py-1 rounded border text-xs ${
                isDark
                  ? 'bg-[#282E3A] border-[#384252] text-white'
                  : 'bg-white border-slate-300 text-black'
              }`}
            />
            <select
              value={newFieldType}
              onChange={(e) => setNewFieldType(e.target.value as FieldType)}
              className={`px-1.5 py-1 rounded border text-xs ${
                isDark
                  ? 'bg-[#282E3A] border-[#384252] text-white'
                  : 'bg-white border-slate-300 text-black'
              }`}
            >
              <option value={FieldType.STRING}>STRING</option>
              <option value={FieldType.INTEGER}>INT</option>
              <option value={FieldType.DOUBLE}>DOUBLE</option>
              <option value={FieldType.BOOLEAN}>BOOL</option>
            </select>
            <button
              type="submit"
              className="px-2 py-1 rounded bg-[#0079C1] text-white font-bold text-xs cursor-pointer"
            >
              +
            </button>
          </form>
        )}

        {/* Attributes List */}
        <div className="px-4 pb-3 flex-1 overflow-y-auto">
          <div className="divide-y divide-current/10 border-t border-current/15">
            {allKeys.map((key) => {
              const val = feature.attributes[key];
              return (
                <div
                  key={key}
                  className="py-1.5 flex items-center justify-between gap-3 text-xs"
                >
                  <span className="font-bold opacity-75 shrink-0">{key}</span>
                  {isEditing ? (
                    <input
                      type="text"
                      value={draftAttrs[key] ?? ''}
                      onChange={(e) =>
                        setDraftAttrs((prev) => ({ ...prev, [key]: e.target.value }))
                      }
                      className={`flex-1 min-w-0 px-2 py-1 rounded border text-xs text-right ${
                        isDark
                          ? 'bg-[#282E3A] border-[#384252] text-white'
                          : 'bg-white border-slate-300 text-black'
                      }`}
                    />
                  ) : (
                    <span className="text-right break-words">
                      {val === null || val === undefined
                        ? isEs
                          ? '<nulo>'
                          : '<null>'
                        : String(val)}
                    </span>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};
