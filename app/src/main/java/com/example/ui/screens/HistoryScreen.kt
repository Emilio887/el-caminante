package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.format.GpxHandler
import com.example.map.ComposeMapView
import com.example.model.ActivityRecord
import com.example.ui.theme.HeartRateRed
import com.example.ui.theme.OffTrackRed
import com.example.ui.theme.TrailOrange
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    val activities by viewModel.filteredActivities.collectAsState()
    val searchQuery by viewModel.activitySearchQuery.collectAsState()
    var selectedActivityDetail by remember { mutableStateOf<ActivityRecord?>(null) }
    var activityToDelete by remember { mutableStateOf<ActivityRecord?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Actividades", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_history")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (activities.isEmpty() && searchQuery.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Aún no has registrado ninguna actividad", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Inicia una grabación GPS desde la pantalla principal", fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setActivitySearchQuery(it) },
                        label = { Text("Buscar por nombre, lugar o nota...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp)
                            .testTag("input_search_activities"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                items(activities, key = { it.id }) { act ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedActivityDetail = act }
                            .testTag("activity_card_${act.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(act.activityType.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(act.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(dateFormat.format(Date(act.startTime)), fontSize = 12.sp, color = Color.Gray)
                                    }
                                }

                                IconButton(onClick = { activityToDelete = act }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Stats Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("%.2f km".format(act.distanceKm), fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    Text("DISTANCIA", fontSize = 10.sp, color = Color.Gray)
                                }

                                Column {
                                    val durationMin = act.totalDurationMillis / 60000
                                    Text("${durationMin} min", fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    Text("TIEMPO", fontSize = 10.sp, color = Color.Gray)
                                }

                                Column {
                                    Text("+%.0f m".format(act.elevationGain), fontWeight = FontWeight.Black, fontSize = 16.sp, color = TrailOrange)
                                    Text("DESNIVEL +", fontSize = 10.sp, color = Color.Gray)
                                }

                                Column {
                                    Text("%.1f km/h".format(act.avgSpeedKmh), fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    Text("MED. VEL", fontSize = 10.sp, color = Color.Gray)
                                }

                                act.avgHeartRate?.let { hr ->
                                    Column {
                                        Text("$hr bpm", fontWeight = FontWeight.Black, fontSize = 16.sp, color = HeartRateRed)
                                        Text("PULSO MED.", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Export GPX button
                            OutlinedButton(
                                onClick = { exportActivityGpx(context, act) },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exportar a GPX (Strava / Garmin)", fontSize = 12.sp)
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Detail dialog with track map
    selectedActivityDetail?.let { act ->
        AlertDialog(
            onDismissRequest = { selectedActivityDetail = null },
            title = { Text(act.title) },
            text = {
                Column {
                    Text(
                        text = "Realizada el ${dateFormat.format(Date(act.startTime))}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        ComposeMapView(
                            modifier = Modifier.fillMaxSize(),
                            currentLocation = null,
                            recordedPoints = act.points
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Elevation Profile Chart (Recharts-inspired Style)
                    val chartPoints = remember(act.points) {
                        if (act.points.isEmpty()) {
                            listOf(
                                com.example.ui.components.ChartDataPoint("Inicio", 0f, 0f),
                                com.example.ui.components.ChartDataPoint("Fin", act.distanceKm.toFloat(), act.elevationGain.toFloat())
                            )
                        } else {
                            val step = maxOf(1, act.points.size / 6)
                            act.points.mapIndexedNotNull { index, pt ->
                                if (index % step == 0 || index == act.points.size - 1) {
                                    val distFraction = (index.toFloat() / act.points.size) * act.distanceKm
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
                        data = chartPoints,
                        isElevation = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Distancia: %.2f km".format(act.distanceKm), fontWeight = FontWeight.Bold)
                    Text("Desnivel acumulado: +%.0f m / -%.0f m".format(act.elevationGain, act.elevationLoss))
                    Text("Velocidad: Media %.1f km/h • Máxima %.1f km/h".format(act.avgSpeedKmh, act.maxSpeedKmh))
                    Text("Calorías estimadas: ${act.estimatedCalories} kcal")

                    if (act.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Notas: ${act.notes}", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { exportActivityGpx(context, act) }) {
                    Text("Exportar GPX")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedActivityDetail = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Delete confirmation
    activityToDelete?.let { act ->
        AlertDialog(
            onDismissRequest = { activityToDelete = null },
            title = { Text("¿Eliminar actividad?") },
            text = { Text("Esta acción eliminará el track GPS registrado del historial de forma permanente.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteActivity(act)
                        activityToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OffTrackRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { activityToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

fun exportActivityGpx(context: Context, activity: ActivityRecord) {
    val xml = GpxHandler.exportToGpx(activity.title, activity.notes, activity.points)
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/xml"
            putExtra(Intent.EXTRA_SUBJECT, "Actividad GPX - ${activity.title}")
            putExtra(Intent.EXTRA_TEXT, xml)
        }
        val chooser = Intent.createChooser(sendIntent, "Exportar Actividad GPX")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (_: Exception) {}
}
