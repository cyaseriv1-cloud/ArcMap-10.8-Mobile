import React, { useEffect, useRef } from 'react';
import { Locate, LocateFixed, MapPinPlus, Maximize2, Minus, Plus } from 'lucide-react';
import { getBasemapById } from '../map/basemap';
import { MapToolMode } from '../map/interaction';
import { MapCanvasRenderer } from '../map/renderer';
import { ArcMapTheme, useArcMap } from '../state/ArcMapContext';
import { MeasureHud } from './MeasureHud';

export const MapCanvasView: React.FC = () => {
  const {
    layers,
    activeTool,
    measureSession,
    digitizeSession,
    renderVersion,
    theme,
    gpsLocation,
    gpsAccuracy,
    currentBasemap,
    viewport,
    tileManager,
    handleMapTap,
    zoomIn,
    zoomOut,
    zoomFullExtent,
    zoomAround,
    updateCoordinates,
    updateRender,
    requestGpsAndCenter,
    requestGpsAndCapture,
  } = useArcMap();

  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const rendererRef = useRef<MapCanvasRenderer>(new MapCanvasRenderer());

  const dragStateRef = useRef<{
    isDown: boolean;
    startX: number;
    startY: number;
    lastX: number;
    lastY: number;
    movedDistance: number;
  }>({
    isDown: false,
    startX: 0,
    startY: 0,
    lastX: 0,
    lastY: 0,
    movedDistance: 0,
  });

  const pinchRef = useRef<{
    active: boolean;
    lastDist: number;
    lastCenterX: number;
    lastCenterY: number;
  }>({
    active: false,
    lastDist: 0,
    lastCenterX: 0,
    lastCenterY: 0,
  });

  const lastTouchTimeRef = useRef<number>(0);

  // Render canvas whenever state or dimensions change
  useEffect(() => {
    const canvas = canvasRef.current;
    const container = containerRef.current;
    if (!canvas || !container) return;

    const rect = container.getBoundingClientRect();
    if (rect.width <= 0 || rect.height <= 0) return;

    const dpr = window.devicePixelRatio || 1;
    const targetW = Math.round(rect.width * dpr);
    const targetH = Math.round(rect.height * dpr);

    if (canvas.width !== targetW || canvas.height !== targetH) {
      canvas.width = targetW;
      canvas.height = targetH;
    }

    viewport.screenWidth = rect.width;
    viewport.screenHeight = rect.height;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.save();
    ctx.scale(dpr, dpr);

    const isMeasuring =
      activeTool === MapToolMode.MEASURE_DISTANCE || activeTool === MapToolMode.MEASURE_AREA;
    const isDigitizing =
      activeTool === MapToolMode.DIGITIZE_LINE || activeTool === MapToolMode.DIGITIZE_POLYGON;

    rendererRef.current.render({
      ctx,
      viewport,
      layers,
      measureSession: isMeasuring ? measureSession : null,
      digitizeSession: isDigitizing ? digitizeSession : null,
      isDarkMode: theme === ArcMapTheme.DARK,
      gpsLocation,
      gpsAccuracy,
      basemap: getBasemapById(currentBasemap),
      tileManager,
    });

    ctx.restore();
  }, [
    layers,
    activeTool,
    measureSession,
    digitizeSession,
    renderVersion,
    theme,
    gpsLocation,
    gpsAccuracy,
    currentBasemap,
    viewport,
    tileManager,
  ]);

  // ResizeObserver so canvas adapts smoothly when drawer opens/closes or window resizes
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const observer = new ResizeObserver(() => {
      updateRender();
    });
    observer.observe(container);
    return () => observer.disconnect();
  }, [updateRender]);

  const getLocalCoords = (clientX: number, clientY: number): [number, number] => {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return [0, 0];
    return [clientX - rect.left, clientY - rect.top];
  };

  const handleMouseDown = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (Date.now() - lastTouchTimeRef.current < 650) return;
    const [x, y] = getLocalCoords(e.clientX, e.clientY);
    dragStateRef.current = {
      isDown: true,
      startX: x,
      startY: y,
      lastX: x,
      lastY: y,
      movedDistance: 0,
    };
  };

  const handleMouseMove = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (Date.now() - lastTouchTimeRef.current < 650) return;
    const [x, y] = getLocalCoords(e.clientX, e.clientY);
    if (dragStateRef.current.isDown) {
      const dx = x - dragStateRef.current.lastX;
      const dy = y - dragStateRef.current.lastY;
      dragStateRef.current.lastX = x;
      dragStateRef.current.lastY = y;
      dragStateRef.current.movedDistance += Math.hypot(dx, dy);

      if (dx !== 0 || dy !== 0) {
        viewport.panByPixels(dx, dy);
        updateCoordinates(viewport.centerLon, viewport.centerLat);
        updateRender();
      }
    } else {
      const geo = viewport.screenToGeo(x, y);
      updateCoordinates(geo.x, geo.y);
    }
  };

  const handleMouseUp = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (Date.now() - lastTouchTimeRef.current < 650) return;
    if (!dragStateRef.current.isDown) return;
    const moved = dragStateRef.current.movedDistance;
    dragStateRef.current.isDown = false;
    if (moved < 5) {
      const [x, y] = getLocalCoords(e.clientX, e.clientY);
      handleMapTap(x, y);
    }
  };

  const handleWheel = (e: React.WheelEvent<HTMLCanvasElement>) => {
    e.preventDefault();
    const [x, y] = getLocalCoords(e.clientX, e.clientY);
    const delta = e.deltaY < 0 ? 0.5 : -0.5;
    zoomAround(x, y, delta);
  };

  const handleTouchStart = (e: React.TouchEvent<HTMLCanvasElement>) => {
    lastTouchTimeRef.current = Date.now();
    if (e.touches.length === 1) {
      const [x, y] = getLocalCoords(e.touches[0].clientX, e.touches[0].clientY);
      dragStateRef.current = {
        isDown: true,
        startX: x,
        startY: y,
        lastX: x,
        lastY: y,
        movedDistance: 0,
      };
      pinchRef.current.active = false;
    } else if (e.touches.length === 2) {
      const [x1, y1] = getLocalCoords(e.touches[0].clientX, e.touches[0].clientY);
      const [x2, y2] = getLocalCoords(e.touches[1].clientX, e.touches[1].clientY);
      dragStateRef.current.isDown = false;
      pinchRef.current = {
        active: true,
        lastDist: Math.hypot(x2 - x1, y2 - y1),
        lastCenterX: (x1 + x2) / 2,
        lastCenterY: (y1 + y2) / 2,
      };
    }
  };

  const handleTouchMove = (e: React.TouchEvent<HTMLCanvasElement>) => {
    lastTouchTimeRef.current = Date.now();
    if (e.touches.length === 1 && dragStateRef.current.isDown) {
      const [x, y] = getLocalCoords(e.touches[0].clientX, e.touches[0].clientY);
      const dx = x - dragStateRef.current.lastX;
      const dy = y - dragStateRef.current.lastY;
      dragStateRef.current.lastX = x;
      dragStateRef.current.lastY = y;
      dragStateRef.current.movedDistance += Math.hypot(dx, dy);
      if (dx !== 0 || dy !== 0) {
        viewport.panByPixels(dx, dy);
        updateCoordinates(viewport.centerLon, viewport.centerLat);
        updateRender();
      }
    } else if (e.touches.length === 2 && pinchRef.current.active) {
      const [x1, y1] = getLocalCoords(e.touches[0].clientX, e.touches[0].clientY);
      const [x2, y2] = getLocalCoords(e.touches[1].clientX, e.touches[1].clientY);
      const dist = Math.hypot(x2 - x1, y2 - y1);
      const cx = (x1 + x2) / 2;
      const cy = (y1 + y2) / 2;

      const panDx = cx - pinchRef.current.lastCenterX;
      const panDy = cy - pinchRef.current.lastCenterY;
      if (panDx !== 0 || panDy !== 0) {
        viewport.panByPixels(panDx, panDy);
      }

      if (pinchRef.current.lastDist > 0 && dist > 0) {
        const scale = dist / pinchRef.current.lastDist;
        const delta = Math.log2(scale);
        zoomAround(cx, cy, delta);
      }

      pinchRef.current.lastDist = dist;
      pinchRef.current.lastCenterX = cx;
      pinchRef.current.lastCenterY = cy;
    }
  };

  const handleTouchEnd = (e: React.TouchEvent<HTMLCanvasElement>) => {
    lastTouchTimeRef.current = Date.now();
    if (e.touches.length === 0) {
      if (dragStateRef.current.isDown && dragStateRef.current.movedDistance < 10) {
        handleMapTap(dragStateRef.current.startX, dragStateRef.current.startY);
      }
      dragStateRef.current.isDown = false;
      pinchRef.current.active = false;
    }
  };

  const cursorClass =
    activeTool === MapToolMode.PAN
      ? 'cursor-grab active:cursor-grabbing'
      : 'cursor-crosshair';

  const isDark = theme === ArcMapTheme.DARK;

  return (
    <div ref={containerRef} className="relative w-full h-full overflow-hidden">
      <canvas
        ref={canvasRef}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={() => {
          dragStateRef.current.isDown = false;
        }}
        onWheel={handleWheel}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
        className={`w-full h-full block touch-none ${cursorClass}`}
      />

      {/* Measure & Digitize HUD */}
      <MeasureHud />

      {/* Floating Right Controls: Thumb Zoom + GPS Actions */}
      <div className="absolute right-3 bottom-3 flex flex-col items-end gap-2.5 z-10">
        {/* Compact Zoom & Extent Control Stack */}
        <div
          className={`flex flex-col rounded-xl shadow-md border overflow-hidden ${
            isDark
              ? 'bg-[#1E222B]/95 border-[#384252] text-[#E6EDF3]'
              : 'bg-white/95 border-[#CBD5E1] text-slate-800'
          }`}
        >
          <button
            type="button"
            onClick={zoomIn}
            title="Zoom In"
            className={`w-12 h-11 sm:w-10 sm:h-9 flex items-center justify-center cursor-pointer active:scale-95 ${
              isDark ? 'hover:bg-white/10 border-b border-[#384252]' : 'hover:bg-slate-100 border-b border-slate-200'
            }`}
          >
            <Plus className="w-5 h-5 sm:w-4 sm:h-4" />
          </button>
          <button
            type="button"
            onClick={zoomOut}
            title="Zoom Out"
            className={`w-12 h-11 sm:w-10 sm:h-9 flex items-center justify-center cursor-pointer active:scale-95 ${
              isDark ? 'hover:bg-white/10 border-b border-[#384252]' : 'hover:bg-slate-100 border-b border-slate-200'
            }`}
          >
            <Minus className="w-5 h-5 sm:w-4 sm:h-4" />
          </button>
          <button
            type="button"
            onClick={zoomFullExtent}
            title="Full Extent"
            className={`w-12 h-11 sm:w-10 sm:h-9 flex items-center justify-center cursor-pointer active:scale-95 ${
              isDark ? 'hover:bg-white/10' : 'hover:bg-slate-100'
            }`}
          >
            <Maximize2 className="w-4 h-4 sm:w-3.5 sm:h-3.5 text-[#0079C1]" />
          </button>
        </div>

        <button
          type="button"
          onClick={requestGpsAndCapture}
          title="Capturar punto GPS"
          className={`w-12 h-12 sm:w-10 sm:h-10 rounded-xl shadow-md flex items-center justify-center cursor-pointer transition-transform active:scale-95 ${
            isDark
              ? 'bg-[#1B3A1A] text-[#66BB6A] border border-[#2E7D32]/50'
              : 'bg-[#E8F5E9] text-[#2E7D32] border border-[#A5D6A7]'
          }`}
        >
          <MapPinPlus className="w-5 h-5" />
        </button>

        <button
          type="button"
          onClick={requestGpsAndCenter}
          title="Mi Ubicación GPS"
          className={`w-14 h-14 sm:w-12 sm:h-12 rounded-2xl shadow-lg flex items-center justify-center cursor-pointer transition-transform active:scale-95 ${
            gpsLocation
              ? isDark
                ? 'bg-[#0D2B4A] text-[#0079C1] border border-[#0079C1]/50'
                : 'bg-[#E3F2FD] text-[#0079C1] border border-[#90CAF9]'
              : isDark
                ? 'bg-[#2A2A2A] text-[#9E9E9E] border border-[#404040]'
                : 'bg-[#F5F5F5] text-[#757575] border border-[#DCDCDC]'
          }`}
        >
          {gpsLocation ? (
            <LocateFixed className="w-6 h-6" />
          ) : (
            <Locate className="w-6 h-6" />
          )}
        </button>
      </div>
    </div>
  );
};
