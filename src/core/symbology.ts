import type { Feature } from './model';

export interface ColorRgba {
  red: number;
  green: number;
  blue: number;
  alpha: number;
}

export function makeColor(red: number, green: number, blue: number, alpha = 1.0): ColorRgba {
  return { red, green, blue, alpha };
}

export function colorToHex(c: ColorRgba): string {
  const r = Math.max(0, Math.min(255, Math.round(c.red))).toString(16).padStart(2, '0');
  const g = Math.max(0, Math.min(255, Math.round(c.green))).toString(16).padStart(2, '0');
  const b = Math.max(0, Math.min(255, Math.round(c.blue))).toString(16).padStart(2, '0');
  return `#${r}${g}${b}`.toUpperCase();
}

export function colorToCss(c: ColorRgba, layerOpacity = 1.0): string {
  const a = Math.max(0, Math.min(1, c.alpha * layerOpacity));
  return `rgba(${c.red}, ${c.green}, ${c.blue}, ${a})`;
}

export function hexToColorRgba(hex: string, alpha = 1.0): ColorRgba {
  const clean = hex.replace('#', '').padEnd(6, '0').slice(0, 6);
  const num = parseInt(clean, 16);
  if (Number.isNaN(num)) {
    return makeColor(128, 128, 128, alpha);
  }
  return {
    red: (num >> 16) & 0xff,
    green: (num >> 8) & 0xff,
    blue: num & 0xff,
    alpha: Math.max(0, Math.min(1, alpha)),
  };
}

export const Colors = {
  RED: makeColor(230, 0, 0),
  GREEN: makeColor(76, 175, 80),
  BLUE: makeColor(33, 150, 243),
  ORANGE: makeColor(255, 152, 0),
  PURPLE: makeColor(156, 39, 176),
  CYAN: makeColor(0, 255, 255),
  BLACK: makeColor(0, 0, 0),
  WHITE: makeColor(255, 255, 255),
  GRAY: makeColor(158, 158, 158),
  TRANSPARENT: makeColor(0, 0, 0, 0),
  DEFAULT_PALETTE: [
    makeColor(66, 133, 244),
    makeColor(234, 67, 53),
    makeColor(251, 188, 5),
    makeColor(52, 168, 83),
    makeColor(171, 71, 188),
    makeColor(0, 172, 193),
    makeColor(255, 112, 67),
    makeColor(141, 110, 99),
  ],
};

export enum MarkerShape {
  CIRCLE = 'CIRCLE',
  SQUARE = 'SQUARE',
  TRIANGLE = 'TRIANGLE',
  CROSS = 'CROSS',
}

export type LayerSymbology =
  | {
      type: 'Simple';
      fillColor: ColorRgba;
      strokeColor: ColorRgba;
      strokeWidthDp: number;
      pointSizeDp: number;
      markerShape: MarkerShape;
    }
  | {
      type: 'UniqueValues';
      fieldName: string;
      valueColors: Record<string, ColorRgba>;
      defaultFillColor: ColorRgba;
      strokeColor: ColorRgba;
      strokeWidthDp: number;
    };

export function createSimpleSymbology(
  partial: Partial<Omit<Extract<LayerSymbology, { type: 'Simple' }>, 'type'>> = {}
): Extract<LayerSymbology, { type: 'Simple' }> {
  return {
    type: 'Simple',
    fillColor: partial.fillColor ?? makeColor(33, 150, 243, 0.5),
    strokeColor: partial.strokeColor ?? Colors.BLUE,
    strokeWidthDp: partial.strokeWidthDp ?? 2.0,
    pointSizeDp: partial.pointSizeDp ?? 8.0,
    markerShape: partial.markerShape ?? MarkerShape.CIRCLE,
  };
}

export function createUniqueValuesFromFeatures(
  fieldName: string,
  features: Feature[],
  palette: ColorRgba[] = Colors.DEFAULT_PALETTE
): Extract<LayerSymbology, { type: 'UniqueValues' }> {
  const seen = new Set<string>();
  const distinctValues: string[] = [];
  for (const f of features) {
    const val = f.attributes[fieldName];
    if (val !== null && val !== undefined) {
      const str = String(val);
      if (!seen.has(str)) {
        seen.add(str);
        distinctValues.push(str);
        if (distinctValues.length >= palette.length) break;
      }
    }
  }
  const valueColors: Record<string, ColorRgba> = {};
  distinctValues.forEach((val, idx) => {
    const base = palette[idx % palette.length];
    valueColors[val] = { ...base, alpha: 0.7 };
  });
  return {
    type: 'UniqueValues',
    fieldName,
    valueColors,
    defaultFillColor: makeColor(158, 158, 158, 0.5),
    strokeColor: Colors.BLACK,
    strokeWidthDp: 1.5,
  };
}

export function getSymbologyFillColor(symbology: LayerSymbology, feature: Feature): ColorRgba {
  if (symbology.type === 'Simple') {
    return symbology.fillColor;
  }
  const raw = feature.attributes[symbology.fieldName];
  if (raw === null || raw === undefined) return symbology.defaultFillColor;
  return symbology.valueColors[String(raw)] ?? symbology.defaultFillColor;
}

export function getSymbologyStrokeColor(symbology: LayerSymbology): ColorRgba {
  return symbology.strokeColor;
}

export function getSymbologyStrokeWidth(symbology: LayerSymbology): number {
  return symbology.strokeWidthDp;
}
