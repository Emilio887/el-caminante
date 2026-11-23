package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Route
import com.example.ui.viewmodel.AppViewModel

enum class MainTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    RECORDING("Navegar", Icons.Default.Hiking),
    ROUTES("Rutas", Icons.Default.Explore),
    GROUP("Grupo", Icons.Default.Group),
    SENSORS("Sensores", Icons.Default.Bluetooth),
    STATS("Estadísticas", Icons.Default.TrendingUp),
    PROFILE("Perfil", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavHost(viewModel: AppViewModel) {
    var selectedTab by remember { mutableStateOf(MainTab.RECORDING) }
    var showOfflineMaps by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showRoutePlanner by remember { mutableStateOf(false) }

    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Display snackbars
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // Permission launcher for Location, Notifications, Bluetooth
    val permissionsToRequest = remember {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    // Back handling for secondary views
    if (selectedRoute != null) {
        RouteDetailScreen(
            route = selectedRoute!!,
            onBack = { viewModel.selectRoute(null) },
            onStartNavigation = { route ->
                viewModel.loadReferenceRoute(route)
                viewModel.selectRoute(null)
                selectedTab = MainTab.RECORDING
            }
        )
        return
    }

    if (showHistory) {
        HistoryScreen(
            viewModel = viewModel,
            onBack = { showHistory = false }
        )
        return
    }

    if (showOfflineMaps) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mapas Sin Conexión", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { showOfflineMaps = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                OfflineMapsScreen(viewModel = viewModel)
            }
        }
        return
    }

    if (showRoutePlanner) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Planificador de Rutas", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { showRoutePlanner = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                RoutePlannerScreen(
                    viewModel = viewModel,
                    onBack = { showRoutePlanner = false },
                    onRouteSaved = { showRoutePlanner = false }
                )
            }
        }
        return
    }

    val isMapFullScreen by viewModel.isMapFullScreen.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!(selectedTab == MainTab.RECORDING && isMapFullScreen)) {
                TopAppBar(
                    title = {
                        Text(
                            text = "El Caminante",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    actions = {
                        // Offline Maps Quick Button
                        IconButton(
                            onClick = { showOfflineMaps = true },
                            modifier = Modifier.testTag("btn_top_offline_maps")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Descarga de mapas sin conexión",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (!(selectedTab == MainTab.RECORDING && isMapFullScreen)) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    MainTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(tab.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.RECORDING -> {
                    RecordingScreen(
                        viewModel = viewModel,
                        onNavigateToRoutes = { selectedTab = MainTab.ROUTES },
                        onNavigateToSensors = { selectedTab = MainTab.SENSORS }
                    )
                }
                MainTab.ROUTES -> {
                    RoutesScreen(
                        viewModel = viewModel,
                        onRouteSelectedForDetail = { route -> viewModel.selectRoute(route) },
                        onStartNavigation = { route ->
                            viewModel.loadReferenceRoute(route)
                            selectedTab = MainTab.RECORDING
                        },
                        onOpenRoutePlanner = { showRoutePlanner = true }
                    )
                }
                MainTab.GROUP -> {
                    GroupRideScreen(
                        viewModel = viewModel,
                        onNavigateToMap = { selectedTab = MainTab.RECORDING }
                    )
                }
                MainTab.SENSORS -> {
                    SensorsScreen(viewModel = viewModel)
                }
                MainTab.STATS -> {
                    StatsScreen(
                        viewModel = viewModel,
                        onNavigateToHistory = { showHistory = true }
                    )
                }
                MainTab.PROFILE -> {
                    ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }
}
