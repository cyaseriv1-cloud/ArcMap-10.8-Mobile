package com.terraaxis.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terraaxis.app.ui.components.*
import com.terraaxis.app.viewmodel.ArcMapTheme
import com.terraaxis.app.viewmodel.ArcMapViewMode
import com.terraaxis.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla principal de ArcMap 10.8 (ArcGIS Desktop Mobile):
 * - Soporte de GPS en tiempo real con punto azul pulsante
 * - Tema Claro / Tema Oscuro
 * - Soporte Bilingüe Español / Inglés
 * - Buscador / Geocodificador Find
 * - Data View y Layout View
 */
@Composable
fun ArcMapScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val isTableVisible by viewModel.isAttributeTableVisible.collectAsState()
    val isToolboxVisible by viewModel.isToolboxVisible.collectAsState()
    val isCatalogVisible by viewModel.isCatalogVisible.collectAsState()
    val isSearchVisible by viewModel.isSearchVisible.collectAsState()
    val isBasemapDialogVisible by viewModel.isBasemapDialogVisible.collectAsState()
    val tableLayer by viewModel.selectedLayerForTable.collectAsState()
    val identified by viewModel.identifiedFeature.collectAsState()
    val activeTool by viewModel.activeTool.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val theme by viewModel.theme.collectAsState()

    val locationManager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    // Launcher para permisos de ubicación en tiempo de ejecución
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            startLocationUpdates(context, locationManager, viewModel)
            viewModel.centerOnGps()
        }
    }

    // Al iniciar, intentar obtener la última ubicación conocida
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            startLocationUpdates(context, locationManager, viewModel)
        }
    }

    BackHandler(enabled = drawerState.isOpen || identified != null || isTableVisible || isToolboxVisible || isCatalogVisible || isSearchVisible || activeTool != com.terraaxis.map.interaction.MapToolMode.PAN || viewMode != ArcMapViewMode.DATA_VIEW) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            identified != null -> viewModel.dismissIdentify()
            isSearchVisible -> viewModel.toggleSearch(false)
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
                    MapCanvasView(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )

                    val gpsLocation by viewModel.gpsLocation.collectAsState()

                    // Measure & Digitize HUD
                    MeasureHud(
                        viewModel = viewModel,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // ── Botones flotantes GPS ──────────────────────────────
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Botón capturar punto GPS (📍 principal de campo)
                        SmallFloatingActionButton(
                            onClick = {
                                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasFine || hasCoarse) {
                                    startLocationUpdates(context, locationManager, viewModel)
                                    viewModel.captureGpsPoint()
                                } else {
                                    locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                }
                            },
                            containerColor = if (theme == ArcMapTheme.DARK) Color(0xFF1B3A1A) else Color(0xFFE8F5E9),
                            contentColor = Color(0xFF2E7D32)
                        ) {
                            Icon(Icons.Default.AddLocation, contentDescription = "Capturar punto GPS")
                        }

                        // Botón centrar en mi ubicación
                        FloatingActionButton(
                            onClick = {
                                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasFine || hasCoarse) {
                                    startLocationUpdates(context, locationManager, viewModel)
                                    viewModel.centerOnGps()
                                } else {
                                    locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                }
                            },
                            containerColor = if (gpsLocation != null) {
                                if (theme == ArcMapTheme.DARK) Color(0xFF0D2B4A) else Color(0xFFE3F2FD)
                            } else {
                                if (theme == ArcMapTheme.DARK) Color(0xFF2A2A2A) else Color(0xFFF5F5F5)
                            },
                            contentColor = if (gpsLocation != null) Color(0xFF0079C1) else Color(0xFF9E9E9E)
                        ) {
                            Icon(
                                if (gpsLocation != null) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                                contentDescription = "Mi Ubicación GPS"
                            )
                        }
                    }
                } else {
                    ArcMapLayoutView(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // ── Ventanas modales y diálogos ───────────────────────────────────────────
    if (isBasemapDialogVisible) {
        ArcMapBasemapDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeBasemapDialog() }
        )
    }

    if (isSearchVisible) {
        ArcMapSearchDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.toggleSearch(false) }
        )
    }

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

private fun startLocationUpdates(
    context: Context,
    lm: LocationManager?,
    viewModel: MainViewModel
) {
    if (lm == null) return
    try {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        var bestLoc: Location? = null
        for (p in providers) {
            val loc = lm.getLastKnownLocation(p)
            if (loc != null && (bestLoc == null || loc.accuracy < bestLoc.accuracy)) {
                bestLoc = loc
            }
        }
        if (bestLoc != null) {
            viewModel.updateGpsLocation(bestLoc.longitude, bestLoc.latitude, bestLoc.accuracy)
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                viewModel.updateGpsLocation(location.longitude, location.latitude, location.accuracy)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 3f, listener)
        } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 3f, listener)
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
