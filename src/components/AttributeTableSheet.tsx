import React, { useMemo, useState } from 'react';
import {
  ArrowDown,
  ArrowUp,
  Calculator,
  Check,
  Crosshair,
  Edit3,
  Plus,
  Search,
  Trash2,
  X,
} from 'lucide-react';
import {
  EMPTY_BBOX,
  bboxExpandBox,
  getGeometryBoundingBox,
  isBBoxEmpty,
} from '../core/geometry';
import { type AttributeValue, FieldType, GeometryProperty } from '../core/model';
import { AppLanguage, ArcMapTheme, useArcMap } from '../state/ArcMapContext';

export const AttributeTableSheet: React.FC = () => {
  const {
    isAttributeTableVisible,
    selectedLayerForTable: layer,
    closeAttributeTable,
    selectFeatureInLayer,
    deselectFeatureInLayer,
    selectAllInLayer,
    clearLayerSelection,
    runCalculateGeometry,
    updateFeatureAttributes,
    deleteSelectedFeatures,
    addFieldToLayer,
    viewport,
    updateCoordinates,
    updateRender,
    renderVersion,
    theme,
    language,
  } = useArcMap();

  const [searchQuery, setSearchQuery] = useState('');
  const [sortedField, setSortedField] = useState<string | null>(null);
  const [sortAscending, setSortAscending] = useState(true);
  const [showCalcDialog, setShowCalcDialog] = useState(false);
  const [showAddFieldDialog, setShowAddFieldDialog] = useState(false);
  const [editingCell, setEditingCell] = useState<{
    featureId: number;
    fieldName: string;
  } | null>(null);
  const [editingValue, setEditingValue] = useState('');

  const isDark = theme === ArcMapTheme.DARK;
  const isEs = language === AppLanguage.ES;

  const filteredFeatures = useMemo(() => {
    if (!layer) return [];
    const q = searchQuery.trim().toLowerCase();
    const list = !q
      ? [...layer.features]
      : layer.features.filter((feat) =>
          Object.values(feat.attributes).some(
            (v) => v !== null && v !== undefined && String(v).toLowerCase().includes(q)
          )
        );

    if (sortedField) {
      list.sort((a, b) => {
        const vA = a.attributes[sortedField];
        const vB = b.attributes[sortedField];
        let comp = 0;
        if (vA == null && vB == null) comp = 0;
        else if (vA == null) comp = -1;
        else if (vB == null) comp = 1;
        else if (typeof vA === 'number' && typeof vB === 'number') comp = vA - vB;
        else comp = String(vA).localeCompare(String(vB));
        return sortAscending ? comp : -comp;
      });
    }
    return list;
  }, [layer, searchQuery, sortedField, sortAscending, renderVersion]);

  if (!isAttributeTableVisible || !layer) return null;

  const selectedCount = layer.selectedFeatureIds.size;
  const totalCount = layer.features.length;

  const handleSortClick = (fieldName: string) => {
    if (sortedField === fieldName) {
      setSortAscending((v) => !v);
    } else {
      setSortedField(fieldName);
      setSortAscending(true);
    }
  };

  const handleZoomToSelection = () => {
    let box = EMPTY_BBOX;
    for (const feat of layer.features) {
      if (!layer.selectedFeatureIds.has(feat.id) || !feat.geometry) continue;
      box = bboxExpandBox(box, getGeometryBoundingBox(feat.geometry));
    }
    if (!isBBoxEmpty(box)) {
      viewport.zoomToExtent(box);
      updateCoordinates(viewport.centerLon, viewport.centerLat);
      updateRender();
      closeAttributeTable();
    }
  };

  const commitCellEdit = (featureId: number, fieldName: string) => {
    const feat = layer.features.find((f) => f.id === featureId);
    if (!feat) {
      setEditingCell(null);
      return;
    }
    const fieldDef = layer.fields.find((f) => f.name === fieldName);
    const trimmed = editingValue.trim();
    let parsed: AttributeValue;
    if (trimmed === '') {
      parsed = null;
    } else if (fieldDef?.type === FieldType.INTEGER) {
      const n = parseInt(trimmed, 10);
      parsed = Number.isNaN(n) ? trimmed : n;
    } else if (fieldDef?.type === FieldType.DOUBLE) {
      const n = parseFloat(trimmed);
      parsed = Number.isNaN(n) ? trimmed : n;
    } else if (fieldDef?.type === FieldType.BOOLEAN) {
      parsed = trimmed.toLowerCase() === 'true';
    } else {
      parsed = editingValue;
    }

    updateFeatureAttributes(layer.id, featureId, {
      ...feat.attributes,
      [fieldName]: parsed,
    });
    setEditingCell(null);
  };

  return (
    <div className="fixed inset-0 bg-black/45 z-50 flex flex-col justify-end">
      <div
        className={`w-full h-[78vh] rounded-t-xl border-t shadow-2xl flex flex-col ${
          isDark
            ? 'bg-[#1A1F26] border-[#30363D] text-[#E6EDF3]'
            : 'bg-white border-[#B0B0B0] text-slate-900'
        }`}
      >
        {/* Header */}
        <div
          className={`px-4 py-3 border-b flex items-center justify-between gap-2 ${
            isDark ? 'border-[#30363D]' : 'border-slate-200'
          }`}
        >
          <div className="min-w-0">
            <h3 className="font-bold text-sm truncate">
              {isEs ? 'Tabla de Atributos:' : 'Attribute Table:'} {layer.name}
            </h3>
            <p className="text-xs text-[#0079C1] font-medium">
              {totalCount} {isEs ? 'registros' : 'records'} ({selectedCount}{' '}
              {isEs ? 'seleccionados' : 'selected'}) ·{' '}
              <span className="opacity-75 sm:hidden">
                {isEs ? 'Toca ✎ para editar' : 'Tap ✎ to edit'}
              </span>
              <span className="opacity-75 hidden sm:inline">
                {isEs
                  ? 'Doble clic en celda para editar'
                  : 'Double-click cell to edit'}
              </span>
            </p>
          </div>

          <div className="flex items-center gap-1 shrink-0">
            <button
              type="button"
              title={isEs ? 'Añadir Campo' : 'Add Field'}
              onClick={() => setShowAddFieldDialog(true)}
              className={`px-2.5 py-2 sm:px-2 sm:py-1.5 rounded hover:bg-black/10 cursor-pointer flex items-center gap-1 text-xs font-medium ${
                isDark ? 'text-gray-200 hover:bg-white/10' : 'text-slate-700'
              }`}
            >
              <Plus className="w-4 h-4 text-[#2E7D32]" />
              <span className="hidden sm:inline">
                {isEs ? 'Nuevo Campo' : 'Add Field'}
              </span>
            </button>

            <button
              type="button"
              title={isEs ? 'Calcular Geometría' : 'Calculate Geometry'}
              onClick={() => setShowCalcDialog(true)}
              className={`px-2.5 py-2 sm:px-2 sm:py-1.5 rounded hover:bg-black/10 cursor-pointer flex items-center gap-1 text-xs font-medium ${
                isDark ? 'text-gray-200 hover:bg-white/10' : 'text-slate-700'
              }`}
            >
              <Calculator className="w-4 h-4 text-[#0079C1]" />
              <span className="hidden sm:inline">
                {isEs ? 'Calc. Geometría' : 'Calc. Geometry'}
              </span>
            </button>

            <button
              type="button"
              onClick={closeAttributeTable}
              className={`p-2 sm:p-1.5 rounded cursor-pointer ${
                isDark ? 'hover:bg-white/10' : 'hover:bg-black/10'
              }`}
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Action Toolbar & Search */}
        <div
          className={`px-4 py-2 border-b flex flex-wrap items-center gap-2 ${
            isDark ? 'border-[#30363D] bg-[#21262D]' : 'border-slate-200 bg-slate-50'
          }`}
        >
          <div className="relative flex-1 min-w-[180px]">
            <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 opacity-55" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={isEs ? 'Buscar...' : 'Search...'}
              className={`w-full pl-8 pr-3 py-1.5 rounded border text-xs focus:outline-none focus:border-[#0079C1] ${
                isDark
                  ? 'bg-[#1A1F26] border-[#384252] text-white'
                  : 'bg-white border-slate-300 text-black'
              }`}
            />
          </div>

          <button
            type="button"
            onClick={() => selectAllInLayer(layer.id)}
            className="px-3.5 py-2 sm:px-3 sm:py-1.5 text-xs font-semibold rounded bg-[#E3F2FD] hover:bg-[#BBDEFB] text-[#0D47A1] border border-[#90CAF9] cursor-pointer"
          >
            {isEs ? 'Todos' : 'Select All'}
          </button>

          <button
            type="button"
            onClick={() => clearLayerSelection(layer.id)}
            className={`px-3.5 py-2 sm:px-3 sm:py-1.5 text-xs font-semibold rounded border cursor-pointer ${
              isDark
                ? 'border-[#384252] hover:bg-[#30363D]'
                : 'border-slate-300 bg-white hover:bg-slate-100'
            }`}
          >
            {isEs ? 'Limpiar' : 'Clear'}
          </button>

          {selectedCount > 0 && (
            <>
              <button
                type="button"
                title={isEs ? 'Zoom a Selección' : 'Zoom to Selection'}
                onClick={handleZoomToSelection}
                className="p-2 sm:p-1.5 rounded bg-[#0079C1] hover:bg-[#00639E] text-white cursor-pointer flex items-center gap-1 text-xs font-semibold"
              >
                <Crosshair className="w-4 h-4" />
                <span className="hidden sm:inline">
                  {isEs ? 'Zoom Selección' : 'Zoom Selection'}
                </span>
              </button>

              <button
                type="button"
                title={isEs ? 'Eliminar Seleccionados' : 'Delete Selected'}
                onClick={() => deleteSelectedFeatures(layer.id)}
                className="px-3 py-2 sm:px-2.5 sm:py-1.5 rounded bg-[#D32F2F] hover:bg-[#B71C1C] text-white cursor-pointer flex items-center gap-1 text-xs font-semibold"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>
                  {isEs ? `Eliminar (${selectedCount})` : `Delete (${selectedCount})`}
                </span>
              </button>
            </>
          )}
        </div>

        {/* Data Grid */}
        <div className="flex-1 overflow-auto">
          <table className="w-max min-w-full border-collapse text-xs">
            <thead
              className={`sticky top-0 z-10 ${
                isDark
                  ? 'bg-[#282E3A] text-[#E6EDF3]'
                  : 'bg-[#E3F2FD] text-[#0D47A1]'
              }`}
            >
              <tr>
                <th className="w-12 px-2.5 py-2 text-left font-bold border-b border-current/20">
                  #
                </th>
                {layer.fields.map((field) => (
                  <th
                    key={field.name}
                    onClick={() => handleSortClick(field.name)}
                    className="min-w-[140px] max-w-[220px] px-2.5 py-2 text-left font-bold border-b border-current/20 cursor-pointer hover:bg-black/5 select-none"
                  >
                    <div className="flex items-center justify-between gap-1">
                      <span className="truncate">{field.name}</span>
                      {sortedField === field.name &&
                        (sortAscending ? (
                          <ArrowUp className="w-3.5 h-3.5 shrink-0" />
                        ) : (
                          <ArrowDown className="w-3.5 h-3.5 shrink-0" />
                        ))}
                    </div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-current/10">
              {filteredFeatures.map((feat) => {
                const isSelected = layer.selectedFeatureIds.has(feat.id);
                return (
                  <tr
                    key={feat.id}
                    onClick={() => {
                      if (isSelected) {
                        deselectFeatureInLayer(layer.id, feat.id);
                      } else {
                        selectFeatureInLayer(layer.id, feat.id, true);
                      }
                    }}
                    className={`cursor-pointer transition-colors ${
                      isSelected
                        ? isDark
                          ? 'bg-[#006064]/70'
                          : 'bg-[#80DEEA]/60'
                        : isDark
                          ? 'hover:bg-[#21262D]'
                          : 'hover:bg-slate-50'
                    }`}
                  >
                    <td className="px-2.5 py-2 opacity-65 font-mono">{feat.id}</td>
                    {layer.fields.map((field) => {
                      const val = feat.attributes[field.name];
                      const isCellEditing =
                        editingCell?.featureId === feat.id &&
                        editingCell.fieldName === field.name;

                      return (
                        <td
                          key={field.name}
                          onDoubleClick={(e) => {
                            e.stopPropagation();
                            setEditingCell({ featureId: feat.id, fieldName: field.name });
                            setEditingValue(
                              val === null || val === undefined ? '' : String(val)
                            );
                          }}
                          className="px-2.5 py-1.5 max-w-[220px]"
                        >
                          {isCellEditing ? (
                            <div
                              className="flex items-center gap-1"
                              onClick={(e) => e.stopPropagation()}
                            >
                              <input
                                type="text"
                                autoFocus
                                value={editingValue}
                                onChange={(e) => setEditingValue(e.target.value)}
                                onKeyDown={(e) => {
                                  if (e.key === 'Enter') {
                                    commitCellEdit(feat.id, field.name);
                                  } else if (e.key === 'Escape') {
                                    setEditingCell(null);
                                  }
                                }}
                                onBlur={() => commitCellEdit(feat.id, field.name)}
                                className={`w-full px-1.5 py-0.5 rounded border text-xs ${
                                  isDark
                                    ? 'bg-[#1A1F26] border-[#0079C1] text-white'
                                    : 'bg-white border-[#0079C1] text-black'
                                }`}
                              />
                              <button
                                type="button"
                                onPointerDown={(e) => {
                                  e.preventDefault();
                                  commitCellEdit(feat.id, field.name);
                                }}
                                onMouseDown={(e) => {
                                  e.preventDefault();
                                  commitCellEdit(feat.id, field.name);
                                }}
                                className="p-1.5 sm:p-0.5 text-[#2E7D32] cursor-pointer"
                              >
                                <Check className="w-4 h-4 sm:w-3.5 sm:h-3.5" />
                              </button>
                            </div>
                          ) : (
                            <div className="flex items-center justify-between gap-1 group">
                              <span className="truncate">
                                {val === null || val === undefined
                                  ? '<null>'
                                  : String(val)}
                              </span>
                              <button
                                type="button"
                                title={isEs ? 'Editar celda' : 'Edit cell'}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setEditingCell({
                                    featureId: feat.id,
                                    fieldName: field.name,
                                  });
                                  setEditingValue(
                                    val === null || val === undefined ? '' : String(val)
                                  );
                                }}
                                className="opacity-75 sm:opacity-0 sm:group-hover:opacity-75 hover:!opacity-100 p-2 sm:p-1 -mr-1 cursor-pointer shrink-0"
                              >
                                <Edit3 className="w-3.5 h-3.5 sm:w-3 sm:h-3" />
                              </button>
                            </div>
                          )}
                        </td>
                      );
                    })}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {showCalcDialog && (
        <CalculateGeometryDialog
          isDark={isDark}
          isEs={isEs}
          onClose={() => setShowCalcDialog(false)}
          onCalculate={(fieldName, property) => {
            runCalculateGeometry(layer, fieldName, property);
            setShowCalcDialog(false);
          }}
        />
      )}

      {showAddFieldDialog && (
        <AddFieldDialog
          isDark={isDark}
          isEs={isEs}
          onClose={() => setShowAddFieldDialog(false)}
          onAdd={(fieldName, fieldType) => {
            if (addFieldToLayer(layer.id, fieldName, fieldType)) {
              setShowAddFieldDialog(false);
            }
          }}
        />
      )}
    </div>
  );
};

const AddFieldDialog: React.FC<{
  isDark: boolean;
  isEs: boolean;
  onClose: () => void;
  onAdd: (fieldName: string, fieldType: FieldType) => void;
}> = ({ isDark, isEs, onClose, onAdd }) => {
  const [name, setName] = useState('');
  const [type, setType] = useState<FieldType>(FieldType.STRING);

  return (
    <div className="fixed inset-0 bg-black/55 z-[60] flex items-center justify-center p-3">
      <div
        className={`w-full max-w-sm rounded-lg border shadow-2xl p-4 flex flex-col gap-3 text-xs ${
          isDark
            ? 'bg-[#1E222B] border-[#384252] text-[#E6EDF3]'
            : 'bg-white border-slate-300 text-slate-900'
        }`}
      >
        <h4 className="font-bold text-sm">
          {isEs ? 'Añadir Nuevo Campo' : 'Add New Field'}
        </h4>

        <div>
          <label className="font-semibold block mb-1">
            {isEs ? 'Nombre del Campo' : 'Field Name'}
          </label>
          <input
            type="text"
            autoFocus
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder={isEs ? 'Ej. PROPIETARIO' : 'E.g. OWNER'}
            className={`w-full px-2.5 py-1.5 rounded border text-xs ${
              isDark
                ? 'bg-[#282E3A] border-[#384252] text-white'
                : 'bg-white border-slate-300 text-black'
            }`}
          />
        </div>

        <div>
          <label className="font-semibold block mb-1">
            {isEs ? 'Tipo de Dato' : 'Data Type'}
          </label>
          <select
            value={type}
            onChange={(e) => setType(e.target.value as FieldType)}
            className={`w-full px-2.5 py-1.5 rounded border text-xs ${
              isDark
                ? 'bg-[#282E3A] border-[#384252] text-white'
                : 'bg-white border-slate-300 text-black'
            }`}
          >
            <option value={FieldType.STRING}>STRING (Texto)</option>
            <option value={FieldType.INTEGER}>INTEGER (Entero)</option>
            <option value={FieldType.DOUBLE}>DOUBLE (Decimal)</option>
            <option value={FieldType.DATE}>DATE (Fecha)</option>
            <option value={FieldType.BOOLEAN}>BOOLEAN (Sí/No)</option>
          </select>
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2.5 sm:px-3 sm:py-1.5 text-[13px] sm:text-xs rounded border border-current/25 cursor-pointer"
          >
            {isEs ? 'Cancelar' : 'Cancel'}
          </button>
          <button
            type="button"
            disabled={!name.trim()}
            onClick={() => onAdd(name.trim(), type)}
            className="px-5 py-2.5 sm:px-4 sm:py-1.5 text-[13px] sm:text-xs rounded bg-[#0079C1] hover:bg-[#00639E] text-white font-bold disabled:opacity-40 cursor-pointer"
          >
            {isEs ? 'Crear Campo' : 'Add Field'}
          </button>
        </div>
      </div>
    </div>
  );
};

const CalculateGeometryDialog: React.FC<{
  isDark: boolean;
  isEs: boolean;
  onClose: () => void;
  onCalculate: (fieldName: string, property: GeometryProperty) => void;
}> = ({ isDark, isEs, onClose, onCalculate }) => {
  const [fieldName, setFieldName] = useState('AREA_CALC');
  const [property, setProperty] = useState<GeometryProperty>(GeometryProperty.AREA);

  const options: Array<{ prop: GeometryProperty; labelEs: string; labelEn: string }> = [
    { prop: GeometryProperty.AREA, labelEs: 'Área Geodésica (m²)', labelEn: 'Geodesic Area (m²)' },
    {
      prop: GeometryProperty.LENGTH_OR_PERIMETER,
      labelEs: 'Longitud / Perímetro (m)',
      labelEn: 'Length / Perimeter (m)',
    },
    {
      prop: GeometryProperty.CENTROID_X,
      labelEs: 'Coordenada Centroide X',
      labelEn: 'Centroid X Coordinate',
    },
    {
      prop: GeometryProperty.CENTROID_Y,
      labelEs: 'Coordenada Centroide Y',
      labelEn: 'Centroid Y Coordinate',
    },
  ];

  return (
    <div className="fixed inset-0 bg-black/55 z-[60] flex items-center justify-center p-3">
      <div
        className={`w-full max-w-sm rounded-lg border shadow-2xl p-4 flex flex-col gap-3 text-xs ${
          isDark
            ? 'bg-[#1E222B] border-[#384252] text-[#E6EDF3]'
            : 'bg-white border-slate-300 text-slate-900'
        }`}
      >
        <h4 className="font-bold text-sm">
          {isEs ? 'Calcular Geometría' : 'Calculate Geometry'}
        </h4>
        <p className="text-[11px] opacity-75">
          {isEs
            ? 'Calcula propiedades geométricas directamente sobre los registros (ArcMap Calculate Geometry):'
            : 'Calculate geometric properties directly onto layer records:'}
        </p>

        <div>
          <label className="font-semibold block mb-1">
            {isEs ? 'Nombre del Campo' : 'Field Name'}
          </label>
          <input
            type="text"
            value={fieldName}
            onChange={(e) => setFieldName(e.target.value)}
            className={`w-full px-2.5 py-1.5 rounded border text-xs ${
              isDark
                ? 'bg-[#282E3A] border-[#384252] text-white'
                : 'bg-white border-slate-300 text-black'
            }`}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          {options.map((opt) => (
            <label key={opt.prop} className="flex items-center gap-2 cursor-pointer">
              <input
                type="radio"
                name="geomProp"
                checked={property === opt.prop}
                onChange={() => setProperty(opt.prop)}
                className="accent-[#0079C1]"
              />
              <span>{isEs ? opt.labelEs : opt.labelEn}</span>
            </label>
          ))}
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2.5 sm:px-3 sm:py-1.5 text-[13px] sm:text-xs rounded border border-current/25 cursor-pointer"
          >
            {isEs ? 'Cancelar' : 'Cancel'}
          </button>
          <button
            type="button"
            disabled={!fieldName.trim()}
            onClick={() => onCalculate(fieldName.trim(), property)}
            className="px-5 py-2.5 sm:px-4 sm:py-1.5 text-[13px] sm:text-xs rounded bg-[#0079C1] hover:bg-[#00639E] text-white font-bold disabled:opacity-40 cursor-pointer"
          >
            {isEs ? 'Calcular' : 'Calculate'}
          </button>
        </div>
      </div>
    </div>
  );
};
