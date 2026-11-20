import React, { useEffect, useState } from 'react';
import { Layers, Plus, Trash2, X } from 'lucide-react';
import { GeometryType } from '../core/geometry';
import { type Field, FieldType, makeField } from '../core/model';
import { AppLanguage, ArcMapTheme, useArcMap } from '../state/ArcMapContext';

interface DraftField {
  id: string;
  name: string;
  type: FieldType;
}

export const NewLayerDialog: React.FC = () => {
  const {
    isNewLayerDialogVisible,
    newLayerDefaultGeomType,
    closeNewLayerDialog,
    createNewLayer,
    layers,
    theme,
    language,
  } = useArcMap();

  const [layerName, setLayerName] = useState('');
  const [geomType, setGeomType] = useState<GeometryType>(GeometryType.POLYGON);
  const [fields, setFields] = useState<DraftField[]>([]);

  useEffect(() => {
    if (isNewLayerDialogVisible) {
      setLayerName(`Capa Nueva ${layers.length + 1}`);
      setGeomType(newLayerDefaultGeomType);
      setFields([
        { id: 'f-id', name: 'ID', type: FieldType.INTEGER },
        { id: 'f-name', name: 'NOMBRE', type: FieldType.STRING },
        { id: 'f-type', name: 'TIPO', type: FieldType.STRING },
      ]);
    }
  }, [isNewLayerDialogVisible, newLayerDefaultGeomType, layers.length]);

  if (!isNewLayerDialogVisible) return null;

  const isDark = theme === ArcMapTheme.DARK;
  const isEs = language === AppLanguage.ES;

  const handleAddFieldRow = () => {
    setFields((prev) => [
      ...prev,
      {
        id: `f-${Date.now()}-${prev.length}`,
        name: `CAMPO_${prev.length + 1}`,
        type: FieldType.STRING,
      },
    ]);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const validFields: Field[] = fields
      .map((f) => ({ ...f, name: f.name.trim() }))
      .filter((f) => f.name.length > 0)
      .filter(
        (f, idx, arr) =>
          arr.findIndex((other) => other.name.toLowerCase() === f.name.toLowerCase()) === idx
      )
      .map((f) => makeField(f.name, f.type));

    createNewLayer(layerName, geomType, validFields);
  };

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-3">
      <form
        onSubmit={handleSubmit}
        className={`w-full max-w-md max-h-[88vh] rounded-md border shadow-2xl flex flex-col ${
          isDark
            ? 'bg-[#1E222B] border-[#384252] text-[#E6EDF3]'
            : 'bg-[#F7F6F2] border-[#B0B0B0] text-slate-900'
        }`}
      >
        {/* Header */}
        <div className="bg-[#335E8A] text-white px-3 py-2 rounded-t-[4px] flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Layers className="w-4 h-4 shrink-0" />
            <span className="font-bold text-xs">
              {isEs ? 'Nueva Capa Vectorial (Shapefile)' : 'New Vector Layer'}
            </span>
          </div>
          <button
            type="button"
            onClick={closeNewLayerDialog}
            className="p-0.5 hover:bg-white/20 rounded cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-3 text-xs">
          <div>
            <label className="font-bold block mb-1">
              {isEs ? 'Nombre de la Capa' : 'Layer Name'}
            </label>
            <input
              type="text"
              value={layerName}
              onChange={(e) => setLayerName(e.target.value)}
              required
              autoFocus
              className={`w-full px-2.5 py-1.5 rounded border text-xs focus:outline-none focus:border-[#0079C1] ${
                isDark
                  ? 'bg-[#282E3A] border-[#384252] text-white'
                  : 'bg-white border-gray-300 text-black'
              }`}
            />
          </div>

          <div>
            <label className="font-bold block mb-1">
              {isEs ? 'Tipo de Geometría' : 'Geometry Type'}
            </label>
            <div className="grid grid-cols-3 gap-1.5">
              {[
                {
                  type: GeometryType.POINT,
                  labelEs: 'Punto',
                  labelEn: 'Point',
                },
                {
                  type: GeometryType.POLYLINE,
                  labelEs: 'Línea',
                  labelEn: 'Polyline',
                },
                {
                  type: GeometryType.POLYGON,
                  labelEs: 'Polígono',
                  labelEn: 'Polygon',
                },
              ].map((opt) => (
                <button
                  key={opt.type}
                  type="button"
                  onClick={() => setGeomType(opt.type)}
                  className={`py-1.5 px-2 rounded border text-xs font-semibold cursor-pointer ${
                    geomType === opt.type
                      ? 'bg-[#0079C1] text-white border-[#0079C1]'
                      : isDark
                        ? 'bg-[#282E3A] border-[#384252] text-gray-300'
                        : 'bg-white border-gray-300 text-gray-700'
                  }`}
                >
                  {isEs ? opt.labelEs : opt.labelEn}
                </button>
              ))}
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between mb-1.5">
              <span className="font-bold">
                {isEs ? 'Campos de Atributos Iniciales' : 'Initial Attribute Fields'}
              </span>
              <button
                type="button"
                onClick={handleAddFieldRow}
                className="flex items-center gap-1 px-2 py-0.5 rounded bg-[#0079C1]/15 text-[#0079C1] hover:bg-[#0079C1]/25 font-semibold cursor-pointer"
              >
                <Plus className="w-3 h-3" />
                <span>{isEs ? 'Añadir Campo' : 'Add Field'}</span>
              </button>
            </div>

            <div className="flex flex-col gap-1.5 max-h-48 overflow-y-auto pr-0.5">
              {fields.map((field) => (
                <div key={field.id} className="flex items-center gap-1.5">
                  <input
                    type="text"
                    value={field.name}
                    onChange={(e) =>
                      setFields((prev) =>
                        prev.map((f) =>
                          f.id === field.id ? { ...f, name: e.target.value } : f
                        )
                      )
                    }
                    placeholder={isEs ? 'Nombre del campo' : 'Field name'}
                    className={`flex-1 px-2 py-1 rounded border text-xs ${
                      isDark
                        ? 'bg-[#282E3A] border-[#384252] text-white'
                        : 'bg-white border-gray-300 text-black'
                    }`}
                  />
                  <select
                    value={field.type}
                    onChange={(e) =>
                      setFields((prev) =>
                        prev.map((f) =>
                          f.id === field.id
                            ? { ...f, type: e.target.value as FieldType }
                            : f
                        )
                      )
                    }
                    className={`w-28 px-1.5 py-1 rounded border text-xs ${
                      isDark
                        ? 'bg-[#282E3A] border-[#384252] text-white'
                        : 'bg-white border-gray-300 text-black'
                    }`}
                  >
                    <option value={FieldType.STRING}>STRING</option>
                    <option value={FieldType.INTEGER}>INTEGER</option>
                    <option value={FieldType.DOUBLE}>DOUBLE</option>
                    <option value={FieldType.DATE}>DATE</option>
                    <option value={FieldType.BOOLEAN}>BOOLEAN</option>
                  </select>
                  <button
                    type="button"
                    disabled={fields.length <= 1}
                    onClick={() =>
                      setFields((prev) => prev.filter((f) => f.id !== field.id))
                    }
                    className="p-1 text-[#D32F2F] hover:bg-black/10 rounded disabled:opacity-30 cursor-pointer"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div
          className={`px-4 py-2.5 border-t flex justify-end gap-2 ${
            isDark ? 'border-[#384252]' : 'border-gray-300'
          }`}
        >
          <button
            type="button"
            onClick={closeNewLayerDialog}
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
            disabled={!layerName.trim()}
            className="px-4 py-1.5 text-xs rounded bg-[#0079C1] hover:bg-[#00639E] text-white font-bold disabled:opacity-40 cursor-pointer"
          >
            {isEs ? 'Crear Capa' : 'Create Layer'}
          </button>
        </div>
      </form>
    </div>
  );
};
