import React, { useEffect, useState } from 'react';
import { Check, FileText, X } from 'lucide-react';
import { type AttributeValue, type Field, FieldType, makeField } from '../core/model';
import { AppLanguage, ArcMapTheme, useArcMap } from '../state/ArcMapContext';

export const FeatureAttributesDialog: React.FC = () => {
  const {
    pendingFeatureDraft,
    layers,
    commitPendingFeature,
    cancelPendingFeature,
    theme,
    language,
  } = useArcMap();

  const [values, setValues] = useState<Record<string, string>>({});

  const targetLayer = pendingFeatureDraft
    ? layers.find((l) => l.id === pendingFeatureDraft.layerId) ?? null
    : null;

  const editableFields: Field[] = targetLayer
    ? targetLayer.fields.length > 0
      ? targetLayer.fields
      : [makeField('ID', FieldType.INTEGER), makeField('NOMBRE', FieldType.STRING)]
    : [];

  useEffect(() => {
    if (!pendingFeatureDraft || !targetLayer) return;
    const init: Record<string, string> = {};
    const fieldsToUse =
      targetLayer.fields.length > 0
        ? targetLayer.fields
        : [makeField('ID', FieldType.INTEGER), makeField('NOMBRE', FieldType.STRING)];

    for (const f of fieldsToUse) {
      if (f.name.toUpperCase() === 'ID') {
        init[f.name] = String(pendingFeatureDraft.nextId);
      } else if (f.name.toUpperCase() === 'NOMBRE') {
        init[f.name] = `Entidad ${pendingFeatureDraft.nextId}`;
      } else if (f.type === FieldType.BOOLEAN) {
        init[f.name] = 'false';
      } else {
        init[f.name] = '';
      }
    }
    setValues(init);
  }, [pendingFeatureDraft, targetLayer]);

  if (!pendingFeatureDraft || !targetLayer) return null;

  const isDark = theme === ArcMapTheme.DARK;
  const isEs = language === AppLanguage.ES;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const parsed: Record<string, AttributeValue> = {};
    for (const field of editableFields) {
      const raw = (values[field.name] ?? '').trim();
      if (raw === '') {
        parsed[field.name] = null;
      } else if (field.type === FieldType.INTEGER) {
        const n = parseInt(raw, 10);
        parsed[field.name] = Number.isNaN(n) ? null : n;
      } else if (field.type === FieldType.DOUBLE) {
        const n = parseFloat(raw);
        parsed[field.name] = Number.isNaN(n) ? null : n;
      } else if (field.type === FieldType.BOOLEAN) {
        parsed[field.name] = raw === 'true';
      } else {
        parsed[field.name] = raw;
      }
    }
    commitPendingFeature(parsed);
  };

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-3">
      <form
        onSubmit={handleSubmit}
        className={`w-full max-w-md max-h-[85vh] rounded-md border shadow-2xl flex flex-col ${
          isDark
            ? 'bg-[#1E222B] border-[#384252] text-[#E6EDF3]'
            : 'bg-[#F7F6F2] border-[#B0B0B0] text-slate-900'
        }`}
      >
        <div className="bg-[#335E8A] text-white px-3 py-2 rounded-t-[4px] flex items-center justify-between gap-2">
          <div className="flex items-center gap-2 min-w-0">
            <FileText className="w-4 h-4 shrink-0" />
            <span className="font-bold text-xs truncate">
              {isEs ? 'Atributos de Nueva Entidad:' : 'New Feature Attributes:'}{' '}
              {targetLayer.name}
            </span>
          </div>
          <button
            type="button"
            onClick={cancelPendingFeature}
            className="p-0.5 hover:bg-white/20 rounded cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-3 text-xs">
          {editableFields.map((field) => (
            <div key={field.name}>
              <label className="font-bold block mb-1">
                {field.name}{' '}
                <span className="text-[10px] font-normal opacity-60">
                  ({field.type})
                </span>
              </label>

              {field.type === FieldType.BOOLEAN ? (
                <select
                  value={values[field.name] ?? 'false'}
                  onChange={(e) =>
                    setValues((prev) => ({ ...prev, [field.name]: e.target.value }))
                  }
                  className={`w-full px-2.5 py-1.5 rounded border text-xs ${
                    isDark
                      ? 'bg-[#282E3A] border-[#384252] text-white'
                      : 'bg-white border-gray-300 text-black'
                  }`}
                >
                  <option value="true">true</option>
                  <option value="false">false</option>
                </select>
              ) : (
                <input
                  type={
                    field.type === FieldType.INTEGER || field.type === FieldType.DOUBLE
                      ? 'number'
                      : field.type === FieldType.DATE
                        ? 'date'
                        : 'text'
                  }
                  step={field.type === FieldType.DOUBLE ? 'any' : undefined}
                  value={values[field.name] ?? ''}
                  onChange={(e) =>
                    setValues((prev) => ({ ...prev, [field.name]: e.target.value }))
                  }
                  className={`w-full px-2.5 py-1.5 rounded border text-xs focus:outline-none focus:border-[#0079C1] ${
                    isDark
                      ? 'bg-[#282E3A] border-[#384252] text-white'
                      : 'bg-white border-gray-300 text-black'
                  }`}
                />
              )}
            </div>
          ))}
        </div>

        <div
          className={`px-4 py-2.5 border-t flex justify-end gap-2 ${
            isDark ? 'border-[#384252]' : 'border-gray-300'
          }`}
        >
          <button
            type="button"
            onClick={cancelPendingFeature}
            className={`px-3 py-1.5 text-xs rounded border cursor-pointer ${
              isDark
                ? 'border-[#384252] text-gray-300 hover:bg-[#282E3A]'
                : 'border-gray-300 text-gray-700 hover:bg-gray-100'
            }`}
          >
            {isEs ? 'Cancelar' : 'Cancel'}
          </button>
          <button
            type="submit"
            className="px-4 py-1.5 text-xs rounded bg-[#0079C1] hover:bg-[#00639E] text-white font-bold flex items-center gap-1 cursor-pointer"
          >
            <Check className="w-3.5 h-3.5" />
            <span>{isEs ? 'Guardar Entidad' : 'Save Feature'}</span>
          </button>
        </div>
      </form>
    </div>
  );
};
