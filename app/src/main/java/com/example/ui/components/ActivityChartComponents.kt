package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.PineGreenPrimary
import com.example.ui.theme.TrailOrange
import kotlin.math.max

data class ChartDataPoint(
    val label: String,
    val distanceKm: Float,
    val elevationGainM: Float,
    val dateLabel: String = label
)

/**
 * Recharts-inspired Interactive Bar Chart for Jetpack Compose.
 * Supports tap selection to inspect exact values with an interactive tooltip.
 */
@Composable
fun RechartsStyleBarChart(
    data: List<ChartDataPoint>,
    showDistance: Boolean = true,
    showElevation: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    var selectedIndex by remember { mutableIntStateOf(-1) }

    val maxDistance = max(1f, data.maxOfOrNull { it.distanceKm } ?: 1f)
    val maxElevation = max(10f, data.maxOfOrNull { it.elevationGainM } ?: 10f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_bar_chart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Selected point tooltip display
            if (selectedIndex in data.indices) {
                val point = data[selectedIndex]
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${point.dateLabel}: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (showDistance) {
                            Text(
                                text = "%.1f km".format(point.distanceKm),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (showDistance && showElevation) {
                            Text(" • ", fontSize = 12.sp, color = Color.Gray)
                        }
                        if (showElevation) {
                            Text(
                                text = "+%.0f m".format(point.elevationGainM),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = TrailOrange
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Toca cualquier barra para inspeccionar valores exactos",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Canvas drawing the grid lines and rounded bars
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .pointerInput(data) {
                        detectTapGestures { tapOffset ->
                            val slotWidth = size.width / data.size
                            val tapped = (tapOffset.x / slotWidth).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = if (selectedIndex == tapped) -1 else tapped
                        }
                    }
                    .testTag("bar_chart_canvas")
            ) {
                val w = size.width
                val h = size.height
                val numItems = data.size
                val slotWidth = w / numItems
                val barWidth = (slotWidth * 0.55f).coerceIn(12f, 36f)

                // Draw subtle horizontal grid lines (Y-axis 25%, 50%, 75%, 100%)
                for (step in 1..3) {
                    val y = h * (step / 4f)
                    drawLine(
                        color = Color(0x15000000),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                // Draw bars
                for (i in data.indices) {
                    val pt = data[i]
                    val isSelected = (i == selectedIndex)
                    val centerX = slotWidth * i + (slotWidth / 2f)

                    if (showDistance && !showElevation) {
                        val barHeight = (pt.distanceKm / maxDistance) * (h - 10f)
                        val y = h - barHeight
                        val x = centerX - (barWidth / 2f)

                        // Light background pillar
                        drawRoundRect(
                            color = Color(0x0C000000),
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, h),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        // Active data bar with vibrant gradient
                        if (barHeight > 2f) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = if (isSelected) {
                                        listOf(HighContrastGreen, PineGreenPrimary)
                                    } else {
                                        listOf(PineGreenPrimary, PineGreenPrimary.copy(alpha = 0.75f))
                                    }
                                ),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    } else if (showElevation && !showDistance) {
                        val barHeight = (pt.elevationGainM / maxElevation) * (h - 10f)
                        val y = h - barHeight
                        val x = centerX - (barWidth / 2f)

                        drawRoundRect(
                            color = Color(0x0C000000),
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, h),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        if (barHeight > 2f) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = if (isSelected) {
                                        listOf(Color(0xFFFFB74D), TrailOrange)
                                    } else {
                                        listOf(TrailOrange, TrailOrange.copy(alpha = 0.75f))
                                    }
                                ),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    } else {
                        // Dual Mode: Twin bars side-by-side
                        val twinWidth = (barWidth * 0.48f).coerceAtLeast(8f)
                        val distHeight = (pt.distanceKm / maxDistance) * (h - 10f)
                        val eleHeight = (pt.elevationGainM / maxElevation) * (h - 10f)

                        val xDist = centerX - twinWidth - 2f
                        val xEle = centerX + 2f

                        if (distHeight > 2f) {
                            drawRoundRect(
                                color = PineGreenPrimary,
                                topLeft = Offset(xDist, h - distHeight),
                                size = Size(twinWidth, distHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                        if (eleHeight > 2f) {
                            drawRoundRect(
                                color = TrailOrange,
                                topLeft = Offset(xEle, h - eleHeight),
                                size = Size(twinWidth, eleHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                }
            }

            // X-Axis Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                data.forEachIndexed { idx, pt ->
                    Text(
                        text = pt.label,
                        fontSize = 11.sp,
                        fontWeight = if (idx == selectedIndex) FontWeight.Black else FontWeight.Medium,
                        color = if (idx == selectedIndex) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * Recharts-inspired Interactive Smooth Area Chart with gradient fill and scrubber line.
 */
@Composable
fun RechartsStyleAreaChart(
    data: List<ChartDataPoint>,
    isElevation: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return

    val values = if (isElevation) data.map { it.elevationGainM } else data.map { it.distanceKm }
    val maxVal = max(1f, values.maxOrNull() ?: 1f)
    val minVal = values.minOrNull() ?: 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_area_chart"),
        shape = RoundedCornerShape(16.dp),
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
                    text = if (isElevation) "Evolución Acumulada de Desnivel" else "Evolución Acumulada de Distancia",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isElevation) "Máx: +%.0f m".format(maxVal) else "Máx: %.1f km".format(maxVal),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("area_chart_canvas")
            ) {
                val w = size.width
                val h = size.height
                val stepX = w / (data.size - 1)

                val linePath = Path()
                val areaPath = Path()

                val p0X = 0f
                val p0Y = h - ((values[0] / maxVal) * (h - 16f))
                linePath.moveTo(p0X, p0Y)
                areaPath.moveTo(p0X, h)
                areaPath.lineTo(p0X, p0Y)

                for (i in 1 until data.size) {
                    val prevX = stepX * (i - 1)
                    val prevY = h - ((values[i - 1] / maxVal) * (h - 16f))
                    val currX = stepX * i
                    val currY = h - ((values[i] / maxVal) * (h - 16f))

                    // Smooth Bezier curve control points
                    val cX1 = prevX + (currX - prevX) / 2f
                    val cY1 = prevY
                    val cX2 = prevX + (currX - prevX) / 2f
                    val cY2 = currY

                    linePath.cubicTo(cX1, cY1, cX2, cY2, currX, currY)
                    areaPath.cubicTo(cX1, cY1, cX2, cY2, currX, currY)
                }

                areaPath.lineTo(w, h)
                areaPath.close()

                val primaryColor = if (isElevation) TrailOrange else PineGreenPrimary
                val secondaryColor = if (isElevation) Color(0xFFFFB74D) else HighContrastGreen

                // Draw Gradient Fill
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.45f),
                            primaryColor.copy(alpha = 0.05f)
                        )
                    )
                )

                // Draw Stroke
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw points on each node
                for (i in data.indices) {
                    val cx = stepX * i
                    val cy = h - ((values[i] / maxVal) * (h - 16f))
                    drawCircle(color = Color.White, radius = 5f, center = Offset(cx, cy))
                    drawCircle(color = primaryColor, radius = 3.5f, center = Offset(cx, cy))
                }
            }

            // X-Axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(data.first().label, fontSize = 11.sp, color = Color.Gray)
                if (data.size > 2) {
                    Text(data[data.size / 2].label, fontSize = 11.sp, color = Color.Gray)
                }
                Text(data.last().label, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}
