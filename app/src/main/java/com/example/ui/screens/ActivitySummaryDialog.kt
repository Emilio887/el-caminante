package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.TrackingState
import com.example.ui.components.ChartDataPoint
import com.example.ui.components.RechartsStyleAreaChart
import com.example.ui.theme.PineGreenPrimary

@Composable
fun ActivitySummaryDialog(
    state: TrackingState,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    val points = state.recordedPoints
    val chartPoints = if (points.size >= 2) {
        val step = maxOf(1, points.size / 6)
        points.mapIndexedNotNull { index, pt ->
            if (index % step == 0 || index == points.size - 1) {
                val distFraction = (index.toFloat() / points.size) * state.distanceKm
                ChartDataPoint(
                    label = "%.1f km".format(distFraction),
                    distanceKm = distFraction.toFloat(),
                    elevationGainM = pt.altitude.toFloat()
                )
            } else null
        }
    } else {
        listOf(
            ChartDataPoint("Inicio", 0f, state.currentAltitude.toFloat()),
            ChartDataPoint("Fin", state.distanceKm.toFloat(), state.currentAltitude.toFloat())
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PineGreenPrimary, modifier = Modifier.size(28.dp))
                Text("¡Resumen de Actividad!", fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tu actividad ha sido guardada con éxito en el historial.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                // Key Stats Grid Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SummaryStatItem(label = "Distancia Total", value = "%.2f km".format(state.distanceKm))
                            SummaryStatItem(label = "Tiempo Transcurrido", value = state.formattedDuration)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SummaryStatItem(label = "Desnivel Positivo", value = "+%.0f m".format(state.elevationGain))
                            SummaryStatItem(label = "Desnivel Negativo", value = "-%.0f m".format(state.elevationLoss))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SummaryStatItem(label = "Velocidad Media", value = "%.1f km/h".format(state.avgSpeedKmh))
                            SummaryStatItem(label = "Velocidad Máxima", value = "%.1f km/h".format(state.maxSpeedKmh))
                        }
                    }
                }

                // Elevation Profile Chart Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Perfil de Elevación", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        RechartsStyleAreaChart(
                            data = chartPoints,
                            isElevation = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PineGreenPrimary),
                modifier = Modifier.testTag("btn_close_summary")
            ) {
                Text("Aceptar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onShare,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_share_summary")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Compartir")
            }
        }
    )
}

@Composable
fun SummaryStatItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
    }
}
