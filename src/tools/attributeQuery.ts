import type { Feature, VectorLayer } from '../core/model';

export enum SelectionMode {
  NEW_SELECTION = 'NEW_SELECTION',
  ADD_TO_SELECTION = 'ADD_TO_SELECTION',
  REMOVE_FROM_SELECTION = 'REMOVE_FROM_SELECTION',
  SELECT_SUBSET = 'SELECT_SUBSET',
}

export const AttributeQuery = {
  computeSelection(
    layer: VectorLayer,
    whereClause: string,
    selectionMode: SelectionMode = SelectionMode.NEW_SELECTION
  ): Set<number> {
    const trimmed = whereClause.trim();
    if (!trimmed) {
      return selectionMode === SelectionMode.NEW_SELECTION
        ? new Set<number>()
        : new Set(layer.selectedFeatureIds);
    }

    const matchingIds = new Set(
      layer.features.filter((f) => evaluateExpression(f, trimmed)).map((f) => f.id)
    );

    const next =
      selectionMode === SelectionMode.NEW_SELECTION
        ? new Set<number>()
        : new Set(layer.selectedFeatureIds);

    switch (selectionMode) {
      case SelectionMode.NEW_SELECTION:
      case SelectionMode.ADD_TO_SELECTION:
        matchingIds.forEach((id) => next.add(id));
        break;
      case SelectionMode.REMOVE_FROM_SELECTION:
        matchingIds.forEach((id) => next.delete(id));
        break;
      case SelectionMode.SELECT_SUBSET:
        for (const id of Array.from(next)) {
          if (!matchingIds.has(id)) next.delete(id);
        }
        break;
    }

    return next;
  },

  selectByAttributes(
    layer: VectorLayer,
    whereClause: string,
    selectionMode: SelectionMode = SelectionMode.NEW_SELECTION
  ): number {
    const next = this.computeSelection(layer, whereClause, selectionMode);
    layer.selectedFeatureIds.clear();
    next.forEach((id) => layer.selectedFeatureIds.add(id));
    return layer.selectedFeatureIds.size;
  },
};

function evaluateExpression(feature: Feature, expr: string): boolean {
  if (/\s+OR\s+/i.test(expr)) {
    const parts = expr.split(/\s+OR\s+/i);
    return parts.some((p) => evaluateExpression(feature, p));
  }
  if (/\s+AND\s+/i.test(expr)) {
    const parts = expr.split(/\s+AND\s+/i);
    return parts.every((p) => evaluateExpression(feature, p));
  }
  return evaluateSimpleCondition(feature, expr.trim());
}

function stripSurrounding(str: string, open: string, close: string): string {
  if (str.startsWith(open) && str.endsWith(close) && str.length >= open.length + close.length) {
    return str.slice(open.length, str.length - close.length);
  }
  return str;
}

function evaluateSimpleCondition(feature: Feature, condition: string): boolean {
  const match = /(<=|>=|!=|=|<|>|\s+LIKE\s+)/i.exec(condition);
  if (!match || match.index === undefined) return false;

  const op = match[0].trim().toUpperCase();
  let left = condition.slice(0, match.index).trim();
  left = stripSurrounding(stripSurrounding(left, '[', ']'), '"', '"');

  let right = condition.slice(match.index + match[0].length).trim();
  right = stripSurrounding(stripSurrounding(right, "'", "'"), '"', '"');

  const rawEntry = Object.entries(feature.attributes).find(
    ([k]) => k.toLowerCase() === left.toLowerCase()
  );
  const rawAttr = rawEntry ? rawEntry[1] : undefined;

  const numAttr =
    typeof rawAttr === 'number'
      ? rawAttr
      : rawAttr !== null && rawAttr !== undefined && String(rawAttr).trim() !== ''
        ? Number(rawAttr)
        : NaN;
  const numRight = right.trim() !== '' ? Number(right) : NaN;

  if (Number.isFinite(numAttr) && Number.isFinite(numRight)) {
    switch (op) {
      case '=':
        return Math.abs(numAttr - numRight) < 1e-9;
      case '!=':
        return Math.abs(numAttr - numRight) >= 1e-9;
      case '>':
        return numAttr > numRight;
      case '>=':
        return numAttr >= numRight;
      case '<':
        return numAttr < numRight;
      case '<=':
        return numAttr <= numRight;
      default:
        return false;
    }
  }

  const strAttr = rawAttr !== null && rawAttr !== undefined ? String(rawAttr) : '';
  switch (op) {
    case '=':
      return strAttr.toLowerCase() === right.toLowerCase();
    case '!=':
      return strAttr.toLowerCase() !== right.toLowerCase();
    case 'LIKE': {
      const escaped = right.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const pattern = escaped.replace(/%/g, '.*').replace(/_/g, '.');
      return new RegExp(`^${pattern}$`, 'i').test(strAttr);
    }
    default:
      return false;
  }
}
