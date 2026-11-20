package com.terraaxis.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terraaxis.app.ui.components.*
import com.terraaxis.app.viewmodel.ArcMapViewMode
import com.terraaxis.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla principal auténtica de ArcMap 10.8 (ArcGIS Desktop Mobile).
 */
@Composable
fun ArcMapScreen(
    viewModel: MainViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val isTableVisible by viewModel.isAttributeTableVisible.collectAsState()
    val isToolboxVisible by viewModel.isToolboxVisible.collectAsState()
    val isCatalogVisible by viewModel.isCatalogVisible.collectAsState()
    val tableLayer by viewModel.selectedLayerForTable.collectAsState()
    val identified by viewModel.identifiedFeature.collectAsState()
    val activeTool by viewModel.activeTool.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    BackHandler(enabled = drawerState.isOpen || identified != null || isTableVisible || isToolboxVisible || isCatalogVisible || activeTool != com.terraaxis.map.interaction.MapToolMode.PAN || viewMode != ArcMapViewMode.DATA_VIEW) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            identified != null -> viewModel.dismissIdentify()
            isCatalogVisible -> viewModel.toggleCatalog(false)
            isTableVisible -> viewModel.closeAttributeTable()
            isToolboxVisible -> viewModel.toggleToolbox(false)
            viewMode != ArcMapViewMode.DATA_VIEW -> viewModel.setViewMode(ArcMapViewMode.DATA_VIEW)
            activeTool != com.terraaxis.map.interaction.MapToolMode.PAN -> viewModel.setTool(com.terraaxis.map.interaction.MapToolMode.PAN)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ArcMapTableOfContentsDrawer(
                viewModel = viewModel,
                onClose = { scope.launch { drawerState.close() } },
                onOpenCatalog = { viewModel.toggleCatalog(true) }
            )
        }
    ) {
        Scaffold(
            topBar = {
                ArcMapClassicTopBar(
                    viewModel = viewModel,
                    onOpenToc = { scope.launch { drawerState.open() } },
                    onOpenToolbox = { viewModel.toggleToolbox(true) },
                    onOpenCatalog = { viewModel.toggleCatalog(true) }
                )
            },
            bottomBar = {
                ArcMapBottomBar(viewModel = viewModel)
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Alternar entre DATA VIEW (Mapa) y LAYOUT VIEW (Lámina)
                if (viewMode == ArcMapViewMode.DATA_VIEW) {
                    // Vista geográfica interactiva
                    MapCanvasView(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Measure & Digitize HUD
                    MeasureHud(
                        viewModel = viewModel,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // Botón flotante para centrar mapa en el dataset
                    FloatingActionButton(
                        onClick = {
                            viewModel.zoomFullExtent()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Encuadrar Capas")
                    }
                } else {
                    // Vista de Lámina de Impresión (Layout View)
                    ArcMapLayoutView(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Ventanas modales y diálogos de ArcGIS Desktop 10.8
    if (isCatalogVisible) {
        ArcCatalogDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.toggleCatalog(false) }
        )
    }

    if (isTableVisible && tableLayer != null) {
        AttributeTableSheet(
            layer = tableLayer!!,
            viewModel = viewModel,
            onClose = { viewModel.closeAttributeTable() }
        )
    }

    if (isToolboxVisible) {
        ArcToolboxDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.toggleToolbox(false) }
        )
    }

    identified?.let { (layer, feature) ->
        IdentifyBottomSheet(
            layer = layer,
            feature = feature,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissIdentify() }
        )
    }
}
