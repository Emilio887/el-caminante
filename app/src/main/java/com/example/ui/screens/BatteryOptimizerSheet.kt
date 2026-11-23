package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.BatteryUsageManager
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.OffTrackRed
import com.example.ui.theme.PineGreenPrimary
import com.example.ui.theme.TrailOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryOptimizerSheet(
    onDismiss: () -> Unit,
    onToggleEcoMode: () -> Unit = { BatteryUsageManager.toggleEcoMode() }
) {
    val batterySummary by BatteryUsageManager.summary.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
                .testTag("battery_optimizer_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = if (batterySummary.currentLevelPercent <= 20) OffTrackRed else HighContrastGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Monitor y Optimizador de Batería", fontWeight = FontWeight.Black, fontSize = 17.sp)
                        Text("Servicio de análisis de consumo GPS activo", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (batterySummary.currentLevelPercent <= 20) OffTrackRed.copy(alpha = 0.2f) else HighContrastGreen.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${batterySummary.currentLevelPercent}%",
                        color = if (batterySummary.currentLevelPercent <= 20) OffTrackRed else HighContrastGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Telemetry Metrics: Remaining Time & Drain Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("AUTONOMÍA ESTIMADA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = batterySummary.formattedRemainingTime,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("de grabación GPS continua", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("RITMO DE CONSUMO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = batterySummary.formattedDrainRate,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (batterySummary.drainRatePerHour > 12f) OffTrackRed else TrailOrange
                        )
                        Text("gasto medio por hora", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Stats: Temperature & Voltage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Thermostat, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Temperatura: ${String.format("%.1f", batterySummary.averageTemperatureCelsius)}°C",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Caída en salida: -${batterySummary.totalDropPercent}% (${batterySummary.elapsedDurationMinutes} min)",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Eco Mode Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_eco_mode_toggle"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (batterySummary.isEcoModeEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = if (batterySummary.isEcoModeEnabled) HighContrastGreen else Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Eco, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Modo Eco GPS Sendero", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                if (batterySummary.isEcoModeEnabled) "Muestreo a 3s activo (Ahorro ~35%)"
                                else "Muestreo a 1s de alta precisión",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Switch(
                        checked = batterySummary.isEcoModeEnabled,
                        onCheckedChange = { onToggleEcoMode() },
                        modifier = Modifier.testTag("switch_eco_mode")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Discharge Curve Canvas Chart (if snapshots available)
            val snaps = batterySummary.snapshots
            if (snaps.size >= 2) {
                Text("Curva de Descarga de la Sesión", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color(0x0A000000), shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val minLevel = (snaps.minOfOrNull { it.batteryLevelPercent } ?: 50) - 5
                    val maxLevel = (snaps.maxOfOrNull { it.batteryLevelPercent } ?: 100) + 2
                    val range = maxOf(5, maxLevel - minLevel)

                    val path = Path()
                    val p0Y = h - ((snaps[0].batteryLevelPercent - minLevel).toFloat() / range * h)
                    path.moveTo(0f, p0Y)

                    for (i in 1 until snaps.size) {
                        val x = (i.toFloat() / (snaps.size - 1)) * w
                        val y = h - ((snaps[i].batteryLevelPercent - minLevel).toFloat() / range * h)
                        path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = if (batterySummary.isEcoModeEnabled) HighContrastGreen else PineGreenPrimary,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Recommendations / Optimization Tips
            Text("Recomendaciones de Optimización", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            batterySummary.optimizationTips.forEach { tip ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = TrailOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tip.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(tip.description, fontSize = 11.sp, color = Color.Gray, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
