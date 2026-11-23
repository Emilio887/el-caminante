package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.map.ComposeMapView
import com.example.model.Route
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.OffTrackRed
import com.example.ui.theme.PineGreenPrimary
import com.example.ui.theme.TrailOrange
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    route: Route,
    onBack: () -> Unit,
    onStartNavigation: (Route) -> Unit
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    val minAlt = route.points.minOfOrNull { it.altitude } ?: 0.0
    val maxAlt = route.points.maxOfOrNull { it.altitude } ?: 1000.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route.name, maxLines = 1, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_route_detail")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { exportGpxFile(context, route) }) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar GPX")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Map Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                ComposeMapView(
                    modifier = Modifier.fillMaxSize(),
                    currentLocation = null,
                    referenceRoute = route
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Author info
            Text(route.name, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(
                text = "${route.activityType.labelEs} • ${route.province} • Autor: ${route.authorNickname}",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (route.description.isNotBlank()) {
                Text(
                    text = route.description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Key Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("DISTANCIA", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("%.1f km".format(route.distanceKm), fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("DESNIVEL +", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("+%.0f m".format(route.elevationGain), fontSize = 18.sp, fontWeight = FontWeight.Black, color = TrailOrange)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("TIEMPO EST.", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("${route.estimatedDurationMinutes} min", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Elevation Profile Chart Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Perfil Altimétrico", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Altitud: %.0f m - %.0f m".format(minAlt, maxAlt), fontSize = 12.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Canvas Elevation Profile
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("elevation_profile_canvas")
                    ) {
                        val w = size.width
                        val h = size.height
                        val pts = route.points

                        if (pts.size >= 2) {
                            val altRange = max(10.0, maxAlt - minAlt)
                            val path = Path()
                            val fillPath = Path()

                            val p0X = 0f
                            val p0Y = (h - ((pts[0].altitude - minAlt) / altRange * (h - 20f))).toFloat()

                            path.moveTo(p0X, p0Y)
                            fillPath.moveTo(p0X, h)
                            fillPath.lineTo(p0X, p0Y)

                            for (i in 1 until pts.size) {
                                val x = (i.toFloat() / (pts.size - 1)) * w
                                val y = (h - ((pts[i].altitude - minAlt) / altRange * (h - 20f))).toFloat()
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }

                            fillPath.lineTo(w, h)
                            fillPath.close()

                            // Draw gradient under elevation
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        PineGreenPrimary.copy(alpha = 0.5f),
                                        PineGreenPrimary.copy(alpha = 0.05f)
                                    )
                                )
                            )

                            // Draw profile line
                            drawPath(
                                path = path,
                                color = PineGreenPrimary,
                                style = Stroke(width = 4f, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Axis labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0 km", fontSize = 11.sp, color = Color.Gray)
                        Text("%.1f km".format(route.distanceKm / 2), fontSize = 11.sp, color = Color.Gray)
                        Text("%.1f km".format(route.distanceKm), fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action: Export GPX
            OutlinedButton(
                onClick = { exportRouteGpx(context, route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_export_route_gpx"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar Ruta a GPX (Compartir)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Iniciar Navegación
            Button(
                onClick = { onStartNavigation(route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_start_route_navigation"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("INICIAR NAVEGACIÓN GPS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

fun exportRouteGpx(context: android.content.Context, route: Route) {
    val xml = com.example.format.GpxHandler.exportToGpx(route.name, route.description, route.points)
    try {
        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/xml"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Ruta GPX - ${route.name}")
            putExtra(android.content.Intent.EXTRA_TEXT, xml)
        }
        val chooser = android.content.Intent.createChooser(sendIntent, "Exportar Ruta GPX")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (_: Exception) {}
}
