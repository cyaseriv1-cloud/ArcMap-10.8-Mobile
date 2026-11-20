import React, { useEffect } from 'react';
import { ArcCatalogDialog } from './components/ArcCatalogDialog';
import { ArcMapBasemapDialog } from './components/ArcMapBasemapDialog';
import { ArcMapBottomBar } from './components/ArcMapBottomBar';
import { ArcMapClassicTopBar } from './components/ArcMapClassicTopBar';
import { ArcMapLayoutView } from './components/ArcMapLayoutView';
import { ArcMapSearchDialog } from './components/ArcMapSearchDialog';
import { ArcMapTableOfContentsDrawer } from './components/ArcMapTableOfContentsDrawer';
import { ArcToolboxDialog } from './components/ArcToolboxDialog';
import { AttributeTableSheet } from './components/AttributeTableSheet';
import { FeatureAttributesDialog } from './components/FeatureAttributesDialog';
import { IdentifyBottomSheet } from './components/IdentifyBottomSheet';
import { MapCanvasView } from './components/MapCanvasView';
import { NewLayerDialog } from './components/NewLayerDialog';
import { MapToolMode } from './map/interaction';
import {
  ArcMapProvider,
  ArcMapTheme,
  ArcMapViewMode,
  useArcMap,
} from './state/ArcMapContext';

const ArcMapWorkspace: React.FC = () => {
  const {
    theme,
    viewMode,
    isTocVisible,
    identifiedFeature,
    isSearchVisible,
    isCatalogVisible,
    isAttributeTableVisible,
    isToolboxVisible,
    isBasemapDialogVisible,
    isNewLayerDialogVisible,
    pendingFeatureDraft,
    activeTool,
    toggleToc,
    dismissIdentify,
    toggleSearch,
    toggleCatalog,
    closeAttributeTable,
    toggleToolbox,
    closeBasemapDialog,
    closeNewLayerDialog,
    cancelPendingFeature,
    setViewMode,
    setTool,
  } = useArcMap();

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return;
      if (pendingFeatureDraft) cancelPendingFeature();
      else if (isNewLayerDialogVisible) closeNewLayerDialog();
      else if (isBasemapDialogVisible) closeBasemapDialog();
      else if (identifiedFeature) dismissIdentify();
      else if (isSearchVisible) toggleSearch(false);
      else if (isCatalogVisible) toggleCatalog(false);
      else if (isAttributeTableVisible) closeAttributeTable();
      else if (isToolboxVisible) toggleToolbox(false);
      else if (isTocVisible) toggleToc(false);
      else if (viewMode !== ArcMapViewMode.DATA_VIEW) setViewMode(ArcMapViewMode.DATA_VIEW);
      else if (activeTool !== MapToolMode.PAN) setTool(MapToolMode.PAN);
    };

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [
    activeTool,
    cancelPendingFeature,
    closeAttributeTable,
    closeBasemapDialog,
    closeNewLayerDialog,
    dismissIdentify,
    identifiedFeature,
    isAttributeTableVisible,
    isBasemapDialogVisible,
    isCatalogVisible,
    isNewLayerDialogVisible,
    isSearchVisible,
    isTocVisible,
    isToolboxVisible,
    pendingFeatureDraft,
    setTool,
    setViewMode,
    toggleCatalog,
    toggleSearch,
    toggleToc,
    toggleToolbox,
    viewMode,
  ]);

  const isDark = theme === ArcMapTheme.DARK;

  return (
    <div
      className={`h-screen w-screen flex flex-col overflow-hidden ${
        isDark ? 'dark bg-[#0D1117] text-[#E6EDF3]' : 'bg-[#ECEBE6] text-black'
      }`}
    >
      <ArcMapClassicTopBar />

      <main className="flex-1 min-h-0 flex relative overflow-hidden">
        <ArcMapTableOfContentsDrawer />

        <div className="flex-1 min-w-0 h-full relative">
          {viewMode === ArcMapViewMode.DATA_VIEW ? (
            <MapCanvasView />
          ) : (
            <ArcMapLayoutView />
          )}
        </div>
      </main>

      <ArcMapBottomBar />

      {/* Modals & Sheets */}
      <ArcMapBasemapDialog />
      <ArcMapSearchDialog />
      <ArcCatalogDialog />
      <NewLayerDialog />
      <FeatureAttributesDialog />
      <AttributeTableSheet />
      <ArcToolboxDialog />
      <IdentifyBottomSheet />
    </div>
  );
};

export const App: React.FC = () => (
  <ArcMapProvider>
    <ArcMapWorkspace />
  </ArcMapProvider>
);

export default App;
