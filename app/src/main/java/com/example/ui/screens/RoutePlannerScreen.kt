package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActivityType
import com.example.model.GpsPoint
import com.example.model.Route
import com.example.ui.theme.PineGreenPrimary
import com.example.ui.viewmodel.AppViewModel
import java.util.UUID

data class WaypointPreset(val name: String, val lat: Double, val lon: Double, val alt: Double)

@Composable
fun RoutePlannerScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onRouteSaved: () -> Unit
) {
    var routeName by remember { mutableStateOf("Nueva Ruta Planificada") }
    var routeDescription by remember { mutableStateOf("Ruta trazada mediante puntos de paso personalizados.") }
    var selectedActivityType by remember { mutableStateOf(ActivityType.HIKING) }

    var waypoints by remember {
        mutableStateOf(
            listOf(
                WaypointPreset("Punto de Inicio (Parking)", 40.7512, -4.0125, 1250.0),
                WaypointPreset("Refugio Forestal", 40.7625, -4.0250, 1420.0),
                WaypointPreset("Collado Alto", 40.7780, -4.0390, 1680.0),
                WaypointPreset("Cima / Mirador", 40.7910, -4.0520, 1890.0)
            )
        )
    }

    val presets = listOf(
        WaypointPreset("Cascada del Río", 40.7650, -4.0180, 1380.0),
        WaypointPreset("Pradera Alta", 40.7820, -4.0450, 1720.0),
        WaypointPreset("Collado Ventoso", 40.7710, -4.0310, 1550.0),
        WaypointPreset("Punto de Vista Sur", 40.7580, -4.0090, 1310.0)
    )

    var totalDistanceMeters = 0.0
    var elevationGain = 0.0
    var elevationLoss = 0.0

    val gpsPoints = waypoints.mapIndexed { index, wp ->
        GpsPoint(
            latitude = wp.lat,
            longitude = wp.lon,
            altitude = wp.alt,
            timestamp = System.currentTimeMillis() + index * 1000L,
            speed = 0f
        )
    }

    for (i in 0 until gpsPoints.size - 1) {
        val p1 = gpsPoints[i]
        val p2 = gpsPoints[i + 1]
        totalDistanceMeters += p1.distanceTo(p2)
        val dAlt = p2.altitude - p1.altitude
        if (dAlt > 0) elevationGain += dAlt
        else elevationLoss += -dAlt
    }

    val distanceKm = totalDistanceMeters / 1000.0
    val estimatedMinutes = ((distanceKm / when(selectedActivityType) {
        ActivityType.HIKING -> 4.0
        ActivityType.MTB -> 14.0
        ActivityType.RUNNING -> 10.0
        ActivityType.WALKING -> 4.5
        ActivityType.ROAD_CYCLING -> 24.0
    } + (elevationGain / 500.0)) * 60).toInt().coerceAtLeast(1)

    val difficulty = when {
        elevationGain > 800 || distanceKm > 15.0 -> "Difícil"
        elevationGain > 400 || distanceKm > 8.0 -> "Moderada"
        else -> "Fácil"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = PineGreenPrimary, modifier = Modifier.size(28.dp))
                    Text(text = "Planificador de Rutas y Waypoints", fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Text(
                    text = "Traza puntos de paso para calcular automáticamente distancia, desnivel y dificultad antes de iniciar tu ruta.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Detalles de la Ruta", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = routeName,
                        onValueChange = { routeName = it },
                        label = { Text("Nombre de la ruta") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = routeDescription,
                        onValueChange = { routeDescription = it },
                        label = { Text("Descripción / Notas") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Tipo de Actividad", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActivityType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedActivityType == type,
                                onClick = { selectedActivityType = type },
                                label = { Text(type.labelEs) },
                                leadingIcon = { Icon(type.icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estadísticas Calculadas (Pre-actividad)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatCell(label = "Distancia", value = "%.2f km".format(distanceKm))
                        StatCell(label = "Desnivel +", value = "+%.0f m".format(elevationGain))
                        StatCell(label = "Desnivel -", value = "-%.0f m".format(elevationLoss))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatCell(label = "Duración Est.", value = "${estimatedMinutes / 60}h ${estimatedMinutes % 60}min")
                        StatCell(label = "Dificultad", value = difficulty)
                        StatCell(label = "Waypoints", value = "${waypoints.size} puntos")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Puntos de Paso (Waypoints)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${waypoints.size} puntos", fontSize = 12.sp, color = Color.Gray)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Añadir waypoint recomendado:", fontSize = 12.sp, color = Color.Gray)
                presets.forEach { preset ->
                    OutlinedButton(
                        onClick = {
                            waypoints = waypoints + preset
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ ${preset.name} (${preset.alt}m)", fontSize = 12.sp)
                    }
                }
            }
        }

        itemsIndexed(waypoints) { index, wp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = PineGreenPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${index + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PineGreenPrimary)
                            }
                        }
                        Column {
                            Text(wp.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Lat: %.4f • Lon: %.4f • Alt: %.0fm".format(wp.lat, wp.lon, wp.alt), fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    if (waypoints.size > 2) {
                        IconButton(
                            onClick = {
                                waypoints = waypoints.filterIndexed { idx, _ -> idx != index }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val newRoute = Route(
                        id = UUID.randomUUID().toString(),
                        name = routeName.ifBlank { "Ruta Planificada" },
                        description = routeDescription,
                        activityType = selectedActivityType,
                        points = gpsPoints,
                        distanceMeters = totalDistanceMeters,
                        elevationGain = elevationGain,
                        elevationLoss = elevationLoss,
                        difficulty = difficulty,
                        authorNickname = "Planificador Local",
                        isCommunity = false
                    )
                    viewModel.savePlannedRoute(newRoute)
                    onRouteSaved()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_planned_route"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PineGreenPrimary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("GUARDAR Y PLANIFICAR RUTA", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun StatCell(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
    }
}
