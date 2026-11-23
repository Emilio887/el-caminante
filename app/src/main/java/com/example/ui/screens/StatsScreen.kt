package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.ActivityRecord
import com.example.model.ActivityType
import com.example.ui.components.ChartDataPoint
import com.example.ui.components.RechartsStyleAreaChart
import com.example.ui.components.RechartsStyleBarChart
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.PineGreenPrimary
import com.example.ui.theme.TrailOrange
import com.example.ui.viewmodel.AppViewModel
import java.util.Calendar

enum class MetricViewMode(val label: String) {
    DISTANCE("Distancia (km)"),
    ELEVATION("Desnivel (+m)"),
    DUAL("Doble Métrica")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: AppViewModel,
    onNavigateToHistory: () -> Unit
) {
    val activities by viewModel.activities.collectAsState()

    var selectedPeriodTab by remember { mutableIntStateOf(0) } // 0: Semana, 1: Mes, 2: Año, 3: Total
    var selectedMetricMode by remember { mutableStateOf(MetricViewMode.DISTANCE) }
    var selectedActivityFilter by remember { mutableStateOf<ActivityType?>(null) }

    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()

    // 1. Filter by activity type
    val typeFilteredActivities = activities.filter { act ->
        selectedActivityFilter == null || act.activityType == selectedActivityFilter
    }

    // 2. Filter by time period
    val periodActivities = typeFilteredActivities.filter { act ->
        val diffDays = (now - act.startTime) / (1000 * 60 * 60 * 24)
        when (selectedPeriodTab) {
            0 -> diffDays <= 7 // Esta semana
            1 -> diffDays <= 30 // Este mes
            2 -> diffDays <= 365 // Este año
            else -> true // Histórico total
        }
    }

    // Aggregates
    val totalDistanceKm = periodActivities.sumOf { it.distanceKm }
    val totalElevationGain = periodActivities.sumOf { it.elevationGain }
    val totalDurationMillis = periodActivities.sumOf { it.totalDurationMillis }
    val totalCalories = periodActivities.sumOf { it.estimatedCalories }
    val totalHours = totalDurationMillis / 3600000.0
    val avgSpeed = if (periodActivities.isNotEmpty()) {
        periodActivities.map { it.avgSpeedKmh }.average().toFloat()
    } else 0f
    val hardnessRatio = if (totalDistanceKm > 0) (totalElevationGain / totalDistanceKm).toFloat() else 0f // m/km

    // All-time bests
    val longestRideKm = activities.maxOfOrNull { it.distanceKm } ?: 0.0
    val maxClimbM = activities.maxOfOrNull { it.elevationGain } ?: 0.0
    val maxSpeed = activities.maxOfOrNull { it.maxSpeedKmh } ?: 0f

    // 3. Generate ChartDataPoints according to period
    val chartDataPoints = remember(periodActivities, selectedPeriodTab) {
        when (selectedPeriodTab) {
            0 -> {
                // Days of week (7 days: Lun..Dom)
                val dayNames = listOf("L", "M", "X", "J", "V", "S", "D")
                val dayFull = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
                val points = mutableListOf<ChartDataPoint>()
                for (i in 0..6) {
                    val targetDayCal = Calendar.getInstance().apply {
                        timeInMillis = now - ((6 - i) * 86400000L)
                    }
                    val targetDayOfYear = targetDayCal.get(Calendar.DAY_OF_YEAR)
                    val matching = periodActivities.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.startTime }
                        c.get(Calendar.DAY_OF_YEAR) == targetDayOfYear
                    }
                    val dist = matching.sumOf { it.distanceKm }.toFloat()
                    val ele = matching.sumOf { it.elevationGain }.toFloat()
                    points.add(
                        ChartDataPoint(
                            label = dayNames[i],
                            distanceKm = dist,
                            elevationGainM = ele,
                            dateLabel = dayFull[i]
                        )
                    )
                }
                points
            }
            1 -> {
                // Weeks of month (4 weeks: Sem 1..Sem 4)
                val points = mutableListOf<ChartDataPoint>()
                for (w in 0..3) {
                    val startDiff = (3 - w) * 7
                    val endDiff = startDiff - 7
                    val matching = periodActivities.filter {
                        val diffDays = ((now - it.startTime) / 86400000L).toInt()
                        diffDays in (endDiff + 1)..startDiff
                    }
                    val dist = matching.sumOf { it.distanceKm }.toFloat()
                    val ele = matching.sumOf { it.elevationGain }.toFloat()
                    points.add(
                        ChartDataPoint(
                            label = "Sem ${w + 1}",
                            distanceKm = dist,
                            elevationGainM = ele,
                            dateLabel = "Semana ${w + 1} del mes"
                        )
                    )
                }
                points
            }
            2 -> {
                // Months of year (12 months: Ene..Dic)
                val monthLabels = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
                val points = mutableListOf<ChartDataPoint>()
                val currYear = cal.get(Calendar.YEAR)
                for (m in 0..11) {
                    val matching = periodActivities.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.startTime }
                        c.get(Calendar.YEAR) == currYear && c.get(Calendar.MONTH) == m
                    }
                    val dist = matching.sumOf { it.distanceKm }.toFloat()
                    val ele = matching.sumOf { it.elevationGain }.toFloat()
                    points.add(
                        ChartDataPoint(
                            label = monthLabels[m],
                            distanceKm = dist,
                            elevationGainM = ele,
                            dateLabel = monthLabels[m]
                        )
                    )
                }
                points
            }
            else -> {
                // Total by year or recent chunks
                val points = mutableListOf<ChartDataPoint>()
                val recent = periodActivities.take(6).reversed()
                if (recent.isEmpty()) {
                    points.add(ChartDataPoint("Total", totalDistanceKm.toFloat(), totalElevationGain.toFloat()))
                } else {
                    recent.forEachIndexed { idx, act ->
                        points.add(
                            ChartDataPoint(
                                label = "#${idx + 1}",
                                distanceKm = act.distanceKm.toFloat(),
                                elevationGainM = act.elevationGain.toFloat(),
                                dateLabel = act.title
                            )
                        )
                    }
                }
                points
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("dashboard_visual_analytics")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Dashboard de Rendimiento", fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Gráficos de actividad, desnivel y evolución", fontSize = 12.sp, color = Color.Gray)
            }

            Surface(
                modifier = Modifier
                    .clickable { onNavigateToHistory() }
                    .testTag("btn_view_history_from_dashboard"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Historial (${activities.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Time Period Tabs: Semana, Mes, Año, Total
        SecondaryTabRow(
            selectedTabIndex = selectedPeriodTab,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("tabs_dashboard_period")
        ) {
            listOf("Semana", "Mes", "Año", "Histórico").forEachIndexed { index, title ->
                Tab(
                    selected = selectedPeriodTab == index,
                    onClick = { selectedPeriodTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab_period_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Activity Type Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedActivityFilter == null,
                onClick = { selectedActivityFilter = null },
                label = { Text("Todas") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("chip_filter_all_types")
            )

            ActivityType.entries.forEach { type ->
                FilterChip(
                    selected = selectedActivityFilter == type,
                    onClick = {
                        selectedActivityFilter = if (selectedActivityFilter == type) null else type
                    },
                    label = { Text(type.labelEs) },
                    leadingIcon = {
                        Icon(type.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("chip_filter_${type.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Key Summary Metrics Strip (Distance, Climbing, Hardness Ratio, Speed)
        Card(
            modifier = Modifier.fillMaxWidth().testTag("card_summary_dashboard"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedPeriodTab) {
                            0 -> "RESUMEN DE ESTA SEMANA"
                            1 -> "RESUMEN DE ESTE MES"
                            2 -> "RESUMEN DE ESTE AÑO"
                            else -> "TOTAL ACUMULADO HISTÓRICO"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HighContrastGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${periodActivities.size} actividades",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = String.format("%.1f", totalDistanceKm),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("KILÓMETROS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }

                    Column {
                        Text(
                            text = "+%.0f m".format(totalElevationGain),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = TrailOrange
                        )
                        Text("DESNIVEL POSITIVO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Landscape, contentDescription = null, tint = TrailOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%.0f m/km dureza".format(hardnessRatio),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%.1f km/h media".format(avgSpeed),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%.1f h rodadas".format(totalHours),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metric Mode Selector (Distancia vs Desnivel vs Doble Métrica)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricViewMode.entries.forEach { mode ->
                FilterChip(
                    selected = selectedMetricMode == mode,
                    onClick = { selectedMetricMode = mode },
                    label = { Text(mode.label, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f).testTag("chip_metric_mode_${mode.name.lowercase()}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (mode == MetricViewMode.ELEVATION) TrailOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = if (mode == MetricViewMode.ELEVATION) TrailOrange else MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Recharts Style Bar Chart
        Text(
            text = "Distribución Temporal (${selectedMetricMode.label})",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        RechartsStyleBarChart(
            data = chartDataPoints,
            showDistance = selectedMetricMode == MetricViewMode.DISTANCE || selectedMetricMode == MetricViewMode.DUAL,
            showElevation = selectedMetricMode == MetricViewMode.ELEVATION || selectedMetricMode == MetricViewMode.DUAL
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Recharts Style Smooth Area Curve
        Text(
            text = "Tendencia de Volumen Acumulado",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        RechartsStyleAreaChart(
            data = chartDataPoints,
            isElevation = selectedMetricMode == MetricViewMode.ELEVATION
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Personal Bests and Records
        Card(
            modifier = Modifier.fillMaxWidth().testTag("card_personal_records"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TrailOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Récords y Mejores Salidas", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("%.1f km".format(longestRideKm), fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text("Ruta Más Larga", fontSize = 11.sp, color = Color.Gray)
                    }

                    Column {
                        Text("+%.0f m".format(maxClimbM), fontSize = 18.sp, fontWeight = FontWeight.Black, color = TrailOrange)
                        Text("Mayor Desnivel", fontSize = 11.sp, color = Color.Gray)
                    }

                    Column {
                        Text("%.1f km/h".format(maxSpeed), fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Velocidad Punta", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Link to Full History
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToHistory() }
                .testTag("card_history_link_dashboard"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Ver Registro Completo de Salidas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Consulta tracks GPS, perfiles y exporta a GPX", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
