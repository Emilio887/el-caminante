package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BleConnectionState
import com.example.model.BleSensor
import com.example.model.BleSensorType
import com.example.sensor.BleSensorManager
import com.example.ui.theme.CadenceBlue
import com.example.ui.theme.PineGreenPrimary
import com.example.ui.theme.HeartRateRed
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.OffTrackRed
import com.example.ui.theme.PowerPurple
import com.example.ui.viewmodel.AppViewModel

@Composable
fun SensorsScreen(
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val trackingState by viewModel.trackingState.collectAsState()
    val pairedSensors by BleSensorManager.pairedSensors.collectAsState()
    val discoveredSensors by BleSensorManager.discoveredSensors.collectAsState()
    val isScanning by BleSensorManager.isScanning.collectAsState()
    val isSimulating by BleSensorManager.isSimulating.collectAsState()

    var selectedFilter by remember { mutableStateOf<BleSensorType?>(null) }
    var sensorToUnpair by remember { mutableStateOf<BleSensor?>(null) }
    var sensorForDiagnostics by remember { mutableStateOf<BleSensor?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartbeat_scale"
    )

    // Sensor Diagnostics Dialog
    sensorForDiagnostics?.let { sensor ->
        SensorDiagnosticsDialog(
            sensor = sensor,
            onDismiss = { sensorForDiagnostics = null }
        )
    }

    // Unpair Confirmation Dialog
    sensorToUnpair?.let { sensor ->
        AlertDialog(
            onDismissRequest = { sensorToUnpair = null },
            title = { Text("Desemparejar Dispositivo", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Deseas desvincular el sensor \"${sensor.name}\"? Ya no se conectará automáticamente al iniciar una actividad."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        BleSensorManager.unpairSensor(sensor.address)
                        sensorToUnpair = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OffTrackRed),
                    modifier = Modifier.testTag("btn_confirm_unpair")
                ) {
                    Text("Desemparejar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { sensorToUnpair = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BluetoothConnected,
                        contentDescription = null,
                        tint = PineGreenPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Configuración de Sensores BLE",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "Empareja bandas de frecuencia cardíaca y sensores de cadencia. Monitoriza el estado de conexión y telemetría en tiempo real.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Live Telemetry Overview Cards (Heart Rate, Cadence, Power)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Heart Rate Telemetry Card
                TelemetryMetricCard(
                    title = "PULSO (HR)",
                    value = trackingState.currentHeartRate?.let { "$it" } ?: "--",
                    unit = "PPM",
                    icon = Icons.Default.Favorite,
                    iconTint = HeartRateRed,
                    pulse = trackingState.currentHeartRate != null,
                    pulseScale = pulseScale,
                    isConnected = pairedSensors.any { it.type == BleSensorType.HEART_RATE && it.isConnected },
                    modifier = Modifier.weight(1f)
                )

                // Cadence Telemetry Card
                TelemetryMetricCard(
                    title = "CADENCIA",
                    value = trackingState.currentCadence?.let { "$it" } ?: "--",
                    unit = "RPM",
                    icon = Icons.Default.Speed,
                    iconTint = CadenceBlue,
                    pulse = false,
                    pulseScale = 1f,
                    isConnected = pairedSensors.any { it.type == BleSensorType.SPEED_CADENCE && it.isConnected },
                    modifier = Modifier.weight(1f)
                )

                // Power Telemetry Card
                TelemetryMetricCard(
                    title = "POTENCIA",
                    value = trackingState.currentPower?.let { "$it" } ?: "--",
                    unit = "W",
                    icon = Icons.Default.FlashOn,
                    iconTint = PowerPurple,
                    pulse = false,
                    pulseScale = 1f,
                    isConnected = pairedSensors.any { it.type == BleSensorType.POWER_METER && it.isConnected },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // SECTION 1: Sensores Emparejados (Mis Dispositivos)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Sensores Emparejados",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${pairedSensors.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (pairedSensors.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("empty_paired_sensors_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tienes sensores emparejados",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Escanea dispositivos cercanos abajo para vincular tu banda de pulso o sensor de cadencia.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(pairedSensors, key = { it.address }) { sensor ->
                PairedSensorCard(
                    sensor = sensor,
                    onConnect = { BleSensorManager.connectSensor(context, sensor) },
                    onDisconnect = { BleSensorManager.disconnectSensor(sensor.address) },
                    onToggleAutoConnect = { BleSensorManager.toggleAutoConnect(sensor.address) },
                    onUnpair = { sensorToUnpair = sensor },
                    onDiagnose = { sensorForDiagnostics = sensor }
                )
            }
        }

        // SECTION 2: Emparejar Nuevos Dispositivos (Escanear)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Emparejar Nuevos Sensores",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isScanning) "Buscando sensores BLE cercanos..." else "Encuentra bandas de pulso y sensores de biela",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Button(
                            onClick = {
                                if (isScanning) {
                                    BleSensorManager.stopScan()
                                } else {
                                    BleSensorManager.startScan(context)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_scan_ble")
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buscando...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buscar Dispositivos", fontSize = 12.sp)
                            }
                        }
                    }

                    // Category Filter Chips
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedFilter == null,
                                onClick = { selectedFilter = null },
                                label = { Text("Todos") },
                                colors = FilterChipDefaults.filterChipColors()
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == BleSensorType.HEART_RATE,
                                onClick = { selectedFilter = BleSensorType.HEART_RATE },
                                label = { Text("Banda Pulso (HR)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = HeartRateRed, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == BleSensorType.SPEED_CADENCE,
                                onClick = { selectedFilter = BleSensorType.SPEED_CADENCE },
                                label = { Text("Cadencia") },
                                leadingIcon = {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = CadenceBlue, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == BleSensorType.POWER_METER,
                                onClick = { selectedFilter = BleSensorType.POWER_METER },
                                label = { Text("Potencia") },
                                leadingIcon = {
                                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = PowerPurple, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }
        }

        // Discovered Devices List
        val filteredDiscovered = discoveredSensors.filter { selectedFilter == null || it.type == selectedFilter }

        if (filteredDiscovered.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("empty_discovered_sensors"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isScanning) "Buscando señales BLE..." else "Pulsa \"Buscar Dispositivos\" para iniciar el escaneo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(filteredDiscovered, key = { it.address }) { sensor ->
                DiscoveredSensorCard(
                    sensor = sensor,
                    onPair = { BleSensorManager.pairSensor(sensor) }
                )
            }
        }

        // SECTION 3: Simulador de Telemetría BLE y Ayuda
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_sensor_simulation"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSimulating) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSimulating) HighContrastGreen else Color.Gray.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Simulador de Telemetría BLE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Genera flujo de datos en vivo (pulso, cadencia y potencia) para pruebas en emulador",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = isSimulating,
                        onCheckedChange = { BleSensorManager.toggleSimulation() },
                        modifier = Modifier.testTag("toggle_sensor_simulation")
                    )
                }
            }
        }

        // Pairing Tips Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PineGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Consejos para emparejar sensores en ruta", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "• Banda de frecuencia cardíaca: humedece levemente los electrodos de goma antes de colocártela.\n" +
                            "• Sensor de cadencia: haz girar las bielas de la bicicleta un par de vueltas para activar el sensor por acelerómetro.\n" +
                            "• Asegúrate de que el sensor no esté emparejado con otra app simultáneamente.",
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryMetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    iconTint: Color,
    pulse: Boolean,
    pulseScale: Float,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier
                        .size(26.dp)
                        .scale(if (pulse) pulseScale else 1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = iconTint
            )
            Text(
                text = "$unit • ${if (isConnected) "Activo" else "Sin señal"}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isConnected) PineGreenPrimary else Color.Gray
            )
        }
    }
}

@Composable
fun PairedSensorCard(
    sensor: BleSensor,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onToggleAutoConnect: () -> Unit,
    onUnpair: () -> Unit,
    onDiagnose: () -> Unit
) {
    val typeColor = when (sensor.type) {
        BleSensorType.HEART_RATE -> HeartRateRed
        BleSensorType.SPEED_CADENCE -> CadenceBlue
        BleSensorType.POWER_METER -> PowerPurple
    }

    val typeIcon = when (sensor.type) {
        BleSensorType.HEART_RATE -> Icons.Default.Favorite
        BleSensorType.SPEED_CADENCE -> Icons.Default.Speed
        BleSensorType.POWER_METER -> Icons.Default.FlashOn
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("paired_sensor_${sensor.address}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Icon, Name, Address, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = typeColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = null,
                                tint = typeColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = sensor.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${sensor.type.labelEs} • ${sensor.address}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Connection State Badge
                ConnectionStatePill(connectionState = sensor.connectionState)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Middle Row: Live telemetry value + Battery + Signal RSSI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live reading
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Lectura: ",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = if (sensor.isConnected && sensor.liveReading != null) {
                            "${sensor.liveReading} ${sensor.type.unit}"
                        } else {
                            "--"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (sensor.isConnected) typeColor else Color.Gray
                    )
                }

                // Battery percent
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryFull,
                        contentDescription = "Batería",
                        tint = if ((sensor.batteryPercent ?: 0) > 20) PineGreenPrimary else OffTrackRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${sensor.batteryPercent ?: 90}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Signal RSSI
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = "Señal",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${sensor.signalQuality} (${sensor.rssi} dBm)",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Auto-connect toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reconectar automáticamente",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Conecta este sensor automáticamente al iniciar una ruta",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = sensor.autoConnect,
                    onCheckedChange = { onToggleAutoConnect() },
                    modifier = Modifier.testTag("switch_autoconnect_${sensor.address}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Connect/Disconnect + Diagnóstico + Unpair
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sensor.connectionState == BleConnectionState.CONNECTED) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f).testTag("btn_disconnect_${sensor.address}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OffTrackRed)
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Desconectar", fontSize = 11.sp)
                    }
                } else if (sensor.connectionState == BleConnectionState.CONNECTING) {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Conectando...", fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f).testTag("btn_connect_${sensor.address}"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Conectar", fontSize = 11.sp)
                    }
                }

                OutlinedButton(
                    onClick = onDiagnose,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PineGreenPrimary),
                    modifier = Modifier.testTag("btn_diagnose_${sensor.address}")
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = "Diagnóstico", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Diagnóstico", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onUnpair,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                    modifier = Modifier.testTag("btn_unpair_${sensor.address}")
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Desemparejar", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun DiscoveredSensorCard(
    sensor: BleSensor,
    onPair: () -> Unit
) {
    val typeColor = when (sensor.type) {
        BleSensorType.HEART_RATE -> HeartRateRed
        BleSensorType.SPEED_CADENCE -> CadenceBlue
        BleSensorType.POWER_METER -> PowerPurple
    }

    val typeIcon = when (sensor.type) {
        BleSensorType.HEART_RATE -> Icons.Default.Favorite
        BleSensorType.SPEED_CADENCE -> Icons.Default.Speed
        BleSensorType.POWER_METER -> Icons.Default.FlashOn
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discovered_sensor_${sensor.address}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = typeColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = sensor.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${sensor.type.labelEs} • ${sensor.address}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Señal: ${sensor.signalQuality} (${sensor.rssi} dBm)",
                        fontSize = 10.sp,
                        color = PineGreenPrimary
                    )
                }
            }

            Button(
                onClick = onPair,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_pair_${sensor.address}")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Emparejar", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ConnectionStatePill(connectionState: BleConnectionState) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    val (bgColor, textColor, label) = when (connectionState) {
        BleConnectionState.CONNECTED -> Triple(
            HighContrastGreen.copy(alpha = 0.2f),
            PineGreenPrimary,
            "Conectado"
        )
        BleConnectionState.CONNECTING -> Triple(
            Color(0xFFFFF3CD),
            Color(0xFF856404),
            "Conectando..."
        )
        BleConnectionState.DISCONNECTED -> Triple(
            Color.LightGray.copy(alpha = 0.3f),
            Color.Gray,
            "Desconectado"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(
            width = 1.dp,
            color = if (connectionState == BleConnectionState.CONNECTED) HighContrastGreen else Color.Transparent,
            shape = RoundedCornerShape(20.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (connectionState == BleConnectionState.CONNECTED) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PineGreenPrimary.copy(alpha = dotAlpha))
                )
            } else if (connectionState == BleConnectionState.CONNECTING) {
                CircularProgressIndicator(
                    color = Color(0xFF856404),
                    strokeWidth = 1.5.dp,
                    modifier = Modifier.size(8.dp)
                )
            }

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
