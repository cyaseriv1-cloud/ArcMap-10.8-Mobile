package com.terraaxis.app.ui

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
import com.terraaxis.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun ArcMapScreen(
    viewModel: MainViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val isTableVisible by viewModel.isAttributeTableVisible.collectAsState()
    val isToolboxVisible by viewModel.isToolboxVisible.collectAsState()
    val tableLayer by viewModel.selectedLayerForTable.collectAsState()
    val identified by viewModel.identifiedFeature.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            TableOfContentsDrawer(
                viewModel = viewModel,
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                ArcGisTopBar(
                    viewModel = viewModel,
                    onOpenToc = { scope.launch { drawerState.open() } },
                    onOpenToolbox = { viewModel.toggleToolbox(true) }
                )
            },
            bottomBar = {
                ArcGisStatusBar(viewModel = viewModel)
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Interactive Map Canvas
                MapCanvasView(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )

                // Measure & Digitize HUD
                MeasureHud(
                    viewModel = viewModel,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // Floating GPS / Center FAB
                FloatingActionButton(
                    onClick = {
                        // Reset to center
                        viewModel.viewport.centerLon = -71.9675
                        viewModel.viewport.centerLat = -13.5320
                        viewModel.viewport.zoom = 13.0
                        viewModel.updateCoordinates(viewModel.viewport.centerLon, viewModel.viewport.centerLat)
                        viewModel.updateRender()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Mi Ubicación")
                }
            }
        }
    }

    // Modal Sheets & Dialogs
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
