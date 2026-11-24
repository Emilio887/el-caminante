package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.theme.PineGreenPrimary
import com.example.ui.theme.TrailOrange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.map.ComposeMapView
import com.example.model.ActivityType
import com.example.model.MapProvider
import com.example.service.RecordingStatus
import com.example.service.TrackingManager
import com.example.ui.theme.CadenceBlue
import com.example.ui.theme.HeartRateRed
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.OffTrackRed
import com.example.ui.theme.PowerPurple
import com.example.ui.theme.TrailOrange
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: AppViewModel,
    onNavigateToRoutes: () -> Unit = {},
    onNavigateToSensors: () -> Unit = {}
) {
    val context = LocalContext.current
    val trackingState by viewModel.trackingState.collectAsState()
    val groupSession by viewModel.groupSession.collectAsState()
    val currentProvider by viewModel.currentMapProvider.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val activities by viewModel.activities.collectAsState()

    val isMapFullScreen by viewModel.isMapFullScreen.collectAsState()
    var isDashboardExpanded by remember { mutableStateOf(false) }

    val weatherInfo by com.example.service.WeatherServiceManager.weatherInfo.collectAsState()
    androidx.compose.runtime.LaunchedEffect(trackingState.currentPoint) {
        trackingState.currentPoint?.let { pt ->
            com.example.service.WeatherServiceManager.fetchWeather(pt.latitude, pt.longitude)
        }
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var completedActivityState by remember { mutableStateOf<com.example.service.TrackingState?>(null) }
    var showLayerSheet by remember { mutableStateOf(false) }
    var showBatterySheet by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("") }

    // Screen wake lock handling based on user preference
    DisposableEffect(trackingState.keepScreenOn) {
        val activity = context as? Activity
        if (trackingState.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full screen interactive Map (Expands to 100% of available canvas)
        ComposeMapView(
            modifier = Modifier.fillMaxSize(),
            currentLocation = trackingState.currentPoint,
            referenceRoute = trackingState.activeReferenceRoute,
            recordedPoints = trackingState.recordedPoints,
            groupMembers = groupSession?.members ?: emptyList(),
            mapProvider = currentProvider,
            headingUpMode = trackingState.headingUpMode,
            currentHeading = trackingState.currentHeading,
            isOffTrack = trackingState.isOffTrack,
            offTrackDistanceMeters = trackingState.offTrackDistanceMeters,
            onToggleHeadingUp = { viewModel.toggleHeadingUp() },
            isFullScreen = isMapFullScreen,
            onToggleFullScreen = { viewModel.toggleMapFullScreen() },
            isDarkMode = userProfile.isDarkModeEnabled,
            isCommunityHeatmapEnabled = userProfile.isCommunityHeatmapEnabled
        )

        // Top Bar Controls Overlay (Activities, Keep Screen On, Layer Selector)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(top = 8.dp, start = 12.dp, end = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Layer Selector Button
                Surface(
                    modifier = Modifier.clickable { showLayerSheet = true }.testTag("btn_map_layer_selector"),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = "Capas", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(currentProvider.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Weather Badge Chip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("weather_status_chip"),
                        shadowElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Clima",
                                tint = TrailOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "%.0f°C • %d%% lluvia".format(weatherInfo.temperatureC, weatherInfo.precipitationProbabilityPercent),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Battery Monitor Chip with click to open BatteryOptimizerSheet
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (trackingState.isBatteryLowAlert) OffTrackRed else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { showBatterySheet = true }
                            .testTag("btn_battery_status_chip"),
                        shadowElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (trackingState.isBatteryLowAlert) Icons.Default.BatteryAlert else Icons.Default.BatteryChargingFull,
                                contentDescription = "Monitor de batería",
                                tint = if (trackingState.isBatteryLowAlert) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${trackingState.batteryPercent}%",
                                color = if (trackingState.isBatteryLowAlert) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Keep Screen On Toggle Button
                    Surface(
                        modifier = Modifier
                            .clickable { viewModel.toggleKeepScreenOn() }
                            .testTag("btn_toggle_screen_wake"),
                        shape = RoundedCornerShape(20.dp),
                        color = if (trackingState.keepScreenOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (trackingState.keepScreenOn) Icons.Default.FlashOn else Icons.Default.ScreenLockPortrait,
                                contentDescription = "Pantalla activa",
                                tint = if (trackingState.keepScreenOn) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (trackingState.keepScreenOn) "Pantalla ON" else "Auto-apagar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Reference Route Banner (if actively following a route)
            trackingState.activeReferenceRoute?.let { ref ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Navigation, contentDescription = "Siguiendo ruta", tint = TrailOrange, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Siguiendo: ${ref.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                Text("%.1f km • +%.0f m".format(ref.distanceKm, ref.elevationGain), fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        IconButton(onClick = { viewModel.clearReferenceRoute() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar guía de ruta", tint = Color.Gray)
                        }
                    }
                }
            }
        }

        // Bottom Dashboard Card (Collapsible for maximum map area)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("recording_dashboard_card"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle bar
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
                        .clickable { isDashboardExpanded = !isDashboardExpanded }
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (!isDashboardExpanded) {
                    // COMPACT MODE: Maximizes visible map area
                    if (trackingState.status == RecordingStatus.IDLE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { viewModel.startTracking() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_start_recording"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("INICIAR RUTA GPS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Current Activity Type Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .clickable { isDashboardExpanded = true }
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(trackingState.activityType.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(trackingState.activityType.labelEs, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Expand dashboard icon
                            IconButton(
                                onClick = { isDashboardExpanded = true },
                                modifier = Modifier.size(36.dp).testTag("btn_expand_dashboard")
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Expandir panel de métricas", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        // RECORDING or PAUSED: Compact Floating Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Mini stats: Distance, Speed, Time
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isDashboardExpanded = true },
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "%.2f km".format(trackingState.distanceKm),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text("DISTANCIA", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text(
                                        text = "%.1f km/h".format(trackingState.currentSpeedKmh),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("VELOCIDAD", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text(
                                        text = trackingState.formattedDuration,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text("TIEMPO", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Quick Action Buttons
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (trackingState.status == RecordingStatus.RECORDING) {
                                    IconButton(
                                        onClick = { viewModel.pauseTracking() },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(TrailOrange, CircleShape)
                                            .testTag("btn_pause_recording")
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = "Pausar", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                } else if (trackingState.status == RecordingStatus.PAUSED) {
                                    IconButton(
                                        onClick = { viewModel.resumeTracking() },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(HighContrastGreen, CircleShape)
                                            .testTag("btn_resume_recording")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Reanudar", tint = Color.Black, modifier = Modifier.size(20.dp))
                                    }
                                }

                                IconButton(
                                    onClick = { showSaveDialog = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(OffTrackRed, CircleShape)
                                        .testTag("btn_stop_recording")
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Terminar", tint = Color.White, modifier = Modifier.size(20.dp))
                                }

                                IconButton(
                                    onClick = { isDashboardExpanded = true },
                                    modifier = Modifier.size(36.dp).testTag("btn_expand_dashboard")
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Ver métricas completas", tint = Color.Gray)
                                }
                            }
                        }
                    }
                } else {
                    // EXPANDED MODE: Full detailed telemetry dashboard
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Métricas de Navegación",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { isDashboardExpanded = false },
                            modifier = Modifier.size(32.dp).testTag("btn_collapse_dashboard")
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimizar panel (más mapa)", tint = Color.Gray)
                        }
                    }

                    // Activity selector chips if IDLE
                    if (trackingState.status == RecordingStatus.IDLE) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ActivityType.entries.forEach { type ->
                                val isSelected = trackingState.activityType == type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setActivityType(type) },
                                    label = { Text(type.labelEs, fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(type.icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Large telemetry numbers row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.2f", trackingState.distanceKm),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("DISTANCIA (KM)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.1f", trackingState.currentSpeedKmh),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("VELOCIDAD (KM/H)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = trackingState.formattedDuration,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("TIEMPO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary metrics: Elevation Gain, Altitude, Sensors (HR, Cadence, Power)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = "Desnivel", tint = TrailOrange, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("+%.0f m".format(trackingState.elevationGain), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsBike, contentDescription = "Altitud", tint = Color.Gray, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("%.0f m alt".format(trackingState.currentAltitude), fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier.clickable { onNavigateToSensors() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Pulsómetro", tint = HeartRateRed, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = trackingState.currentHeartRate?.let { "$it ppm" } ?: "-- ppm",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        trackingState.currentCadence?.let { cad ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = "Cadencia", tint = CadenceBlue, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("$cad rpm", fontSize = 12.sp)
                            }
                        }

                        trackingState.currentPower?.let { pwr ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FlashOn, contentDescription = "Potencia", tint = PowerPurple, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("$pwr W", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Elevation Profile Chart during recording
                    val liveChartPoints = remember(trackingState.recordedPoints) {
                        val pts = trackingState.recordedPoints
                        if (pts.size < 2) {
                            listOf(
                                com.example.ui.components.ChartDataPoint("Inicio", 0f, trackingState.currentAltitude.toFloat()),
                                com.example.ui.components.ChartDataPoint("Actual", trackingState.distanceKm.toFloat(), trackingState.currentAltitude.toFloat())
                            )
                        } else {
                            val step = maxOf(1, pts.size / 6)
                            pts.mapIndexedNotNull { index, pt ->
                                if (index % step == 0 || index == pts.size - 1) {
                                    val distFraction = (index.toFloat() / pts.size) * trackingState.distanceKm
                                    com.example.ui.components.ChartDataPoint(
                                        label = "%.1f km".format(distFraction),
                                        distanceKm = distFraction.toFloat(),
                                        elevationGainM = pt.altitude.toFloat()
                                    )
                                } else null
                            }
                        }
                    }

                    com.example.ui.components.RechartsStyleAreaChart(
                        data = liveChartPoints,
                        isElevation = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val now = System.currentTimeMillis()
                    val weekActivities = activities.filter { (now - it.startTime) <= (7 * 86400000L) }
                    val weekDist = weekActivities.sumOf { it.distanceKm }.toFloat() + trackingState.distanceKm.toFloat()
                    val weekEle = weekActivities.sumOf { it.elevationGain }.toFloat() + trackingState.elevationGain.toFloat()
                    val distProgress = (weekDist / maxOf(1f, userProfile.weeklyDistanceGoalKm)).coerceIn(0f, 1f)
                    val eleProgress = (weekEle / maxOf(1f, userProfile.weeklyElevationGoalM)).coerceIn(0f, 1f)

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("card_weekly_goals_progress"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Metas Semanales", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Objetivos activos", fontSize = 10.sp, color = Color.Gray)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Distancia semanal", fontSize = 11.sp, color = Color.Gray)
                                    Text("%.1f / %.0f km (%.0f%%)".format(weekDist, userProfile.weeklyDistanceGoalKm, distProgress * 100), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PineGreenPrimary)
                                }
                                LinearProgressIndicator(
                                    progress = { distProgress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = PineGreenPrimary,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Desnivel semanal", fontSize = 11.sp, color = Color.Gray)
                                    Text("+%.0f / +%.0f m (%.0f%%)".format(weekEle, userProfile.weeklyElevationGoalM, eleProgress * 100), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TrailOrange)
                                }
                                LinearProgressIndicator(
                                    progress = { eleProgress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = TrailOrange,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Full action buttons
                    when (trackingState.status) {
                        RecordingStatus.IDLE -> {
                            Button(
                                onClick = { viewModel.startTracking() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_start_recording_full"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("INICIAR GRABACIÓN GPS", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        RecordingStatus.RECORDING -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.pauseTracking() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_pause_recording_full"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TrailOrange)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PAUSAR", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { showSaveDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_stop_recording_full"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = OffTrackRed)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("TERMINAR", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        RecordingStatus.PAUSED -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (trackingState.isAutoPaused) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Pause, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Text(
                                                text = "Auto-pausado por inactividad. Se reanudará al moverte.",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.resumeTracking() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("btn_resume_recording_full"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = HighContrastGreen)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("REANUDAR", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { showSaveDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("btn_finish_recording_full"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = OffTrackRed)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("GUARDAR", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Save Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar Actividad") },
            text = {
                Column {
                    Text(
                        text = "Distancia: %.2f km • Tiempo: %s • Desnivel: +%.0f m".format(
                            trackingState.distanceKm,
                            trackingState.formattedDuration,
                            trackingState.elevationGain
                        ),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notas de la salida (opcional)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_activity_notes")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        completedActivityState = trackingState
                        viewModel.stopAndSaveTracking(notesText)
                        showSaveDialog = false
                        showSummaryDialog = true
                    },
                    modifier = Modifier.testTag("btn_confirm_save_activity")
                ) {
                    Text("Guardar en Historial")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.discardTracking()
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OffTrackRed),
                    modifier = Modifier.testTag("btn_discard_activity")
                ) {
                    Text("Descartar")
                }
            }
        )
    }

    // Activity Summary Dialog after finishing activity
    if (showSummaryDialog && completedActivityState != null) {
        ActivitySummaryDialog(
            state = completedActivityState!!,
            onDismiss = { showSummaryDialog = false; completedActivityState = null },
            onShare = {
                val state = completedActivityState!!
                val shareText = "¡He completado una ruta en El Caminante!\nDistancia: %.2f km\nTiempo: %s\nDesnivel: +%.0f m\n¡Descubre nuevas rutas!".format(
                    state.distanceKm, state.formattedDuration, state.elevationGain
                )
                try {
                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Mi Actividad en El Caminante")
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    }
                    val chooser = android.content.Intent.createChooser(sendIntent, "Compartir Actividad")
                    chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                } catch (_: Exception) {}
            }
        )
    }

    // Layer Selection Bottom Sheet
    if (showLayerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLayerSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Capas y Proveedores Cartográficos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                MapProvider.entries.forEach { provider ->
                    val isCurrent = provider == currentProvider
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                viewModel.setMapProvider(provider)
                                showLayerSheet = false
                            }
                            .testTag("card_layer_${provider.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (provider.isSatellite) Icons.Default.Map else Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = provider.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = provider.description,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            if (isCurrent) {
                                Text("Activo", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleCommunityHeatmap() },
                colors = CardDefaults.cardColors(
                    containerColor = if (userProfile.isCommunityHeatmapEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = TrailOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Mapa de Calor de la Comunidad",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Muestra zonas y senderos más frecuentados",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    androidx.compose.material3.Switch(
                        checked = userProfile.isCommunityHeatmapEnabled,
                        onCheckedChange = { viewModel.toggleCommunityHeatmap() }
                    )
                }
            }
        }
    }

    if (showBatterySheet) {
        BatteryOptimizerSheet(
            onDismiss = { showBatterySheet = false }
        )
    }
}
