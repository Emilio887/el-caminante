package com.example.map

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GpsPoint
import com.example.model.GroupMember
import com.example.model.MapProvider
import com.example.model.Route
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.OffTrackRed
import com.example.ui.theme.TrailOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

@Composable
fun ComposeMapView(
    modifier: Modifier = Modifier,
    currentLocation: GpsPoint?,
    referenceRoute: Route? = null,
    recordedPoints: List<GpsPoint> = emptyList(),
    groupMembers: List<GroupMember> = emptyList(),
    mapProvider: MapProvider = MapProvider.OPEN_TOPO,
    headingUpMode: Boolean = false,
    currentHeading: Float = 0f,
    isOffTrack: Boolean = false,
    offTrackDistanceMeters: Double = 0.0,
    onToggleHeadingUp: () -> Unit = {},
    isFullScreen: Boolean = false,
    onToggleFullScreen: (() -> Unit)? = null,
    isDarkMode: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Map Center coordinates (Lat, Lon)
    var centerLat by remember { mutableStateOf(currentLocation?.latitude ?: 40.7512) }
    var centerLon by remember { mutableStateOf(currentLocation?.longitude ?: -4.0125) }
    var zoomLevel by remember { mutableFloatStateOf(14f) }
    var userInteracting by remember { mutableStateOf(false) }

    // Follow current location if user hasn't panned away
    LaunchedEffect(currentLocation) {
        if (!userInteracting && currentLocation != null) {
            centerLat = currentLocation.latitude
            centerLon = currentLocation.longitude
        }
    }

    // Auto-fit reference route if no current GPS fix yet
    LaunchedEffect(referenceRoute) {
        if (currentLocation == null && referenceRoute != null && referenceRoute.points.isNotEmpty()) {
            val mid = referenceRoute.points[referenceRoute.points.size / 2]
            centerLat = mid.latitude
            centerLon = mid.longitude
        }
    }

    // In-memory tile cache for UI
    val tileBitmaps = remember { mutableStateMapOf<String, Bitmap>() }

    // Fetch tiles around center (5x5 grid = -2..2 to fully cover expanded large map canvas)
    val intZoom = zoomLevel.toInt().coerceIn(4, 18)
    LaunchedEffect(centerLat, centerLon, intZoom, mapProvider) {
        val centerTileX = MapTileEngine.latLonToTileX(centerLon, intZoom)
        val centerTileY = MapTileEngine.latLonToTileY(centerLat, intZoom)

        // Preload 5x5 surrounding tiles to guarantee seamless full-screen coverage
        for (dx in -2..2) {
            for (dy in -2..2) {
                val tx = centerTileX + dx
                val ty = centerTileY + dy
                val key = MapTileEngine.getTileKey(mapProvider.id, intZoom, tx, ty)
                if (!tileBitmaps.containsKey(key)) {
                    scope.launch(Dispatchers.IO) {
                        val bmp = MapTileEngine.getTileBitmap(context, mapProvider, intZoom, tx, ty)
                        if (bmp != null) {
                            tileBitmaps[key] = bmp
                        }
                    }
                }
            }
        }
    }

    val mapRotation = if (headingUpMode) -currentHeading else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                when {
                    isDarkMode -> Color(0xFF0A0E12)
                    mapProvider.isSatellite -> Color(0xFF1B231D)
                    else -> Color(0xFFE5EDE6)
                }
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    userInteracting = true
                    zoomLevel = (zoomLevel * zoom).coerceIn(8f, 18f)

                    // Pan delta to lat/lon conversion based on zoom
                    val degPerPixel = 360.0 / (256.0 * (1 shl zoomLevel.toInt()))
                    centerLon -= pan.x * degPerPixel
                    centerLat += pan.y * degPerPixel
                }
            }
            .testTag("map_view_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val cX = canvasW / 2f
            val cY = canvasH / 2f

            // Projection helper: lat/lon to screen pixels
            val scale = (256.0 * (1 shl intZoom)).toFloat()
            fun project(lat: Double, lon: Double): Offset {
                val x = (lon + 180.0) / 360.0 * scale
                val latRad = Math.toRadians(lat)
                val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * scale

                val centerX = (centerLon + 180.0) / 360.0 * scale
                val centerLatRad = Math.toRadians(centerLat)
                val centerY = (1.0 - ln(tan(centerLatRad) + 1.0 / cos(centerLatRad)) / PI) / 2.0 * scale

                var dx = (x - centerX).toFloat()
                var dy = (y - centerY).toFloat()

                if (mapRotation != 0f) {
                    val rad = Math.toRadians(mapRotation.toDouble())
                    val rx = dx * cos(rad) - dy * sin(rad)
                    val ry = dx * sin(rad) + dy * cos(rad)
                    dx = rx.toFloat()
                    dy = ry.toFloat()
                }

                return Offset(cX + dx, cY + dy)
            }

            // Draw Map Tiles
            val centerTileX = MapTileEngine.latLonToTileX(centerLon, intZoom)
            val centerTileY = MapTileEngine.latLonToTileY(centerLat, intZoom)

            for (dx in -2..2) {
                for (dy in -2..2) {
                    val tx = centerTileX + dx
                    val ty = centerTileY + dy
                    val key = MapTileEngine.getTileKey(mapProvider.id, intZoom, tx, ty)
                    val bmp = tileBitmaps[key]

                    // Calculate tile corner lat/lon
                    val n = 1 shl intZoom
                    val tileLon = tx.toDouble() / n * 360.0 - 180.0
                    val tileLatRad = kotlin.math.atan(kotlin.math.sinh(PI * (1.0 - 2.0 * ty / n)))
                    val tileLat = Math.toDegrees(tileLatRad)

                    val screenPos = project(tileLat, tileLon)
                    val tileSizePx = (256f * (zoomLevel / intZoom))

                    if (bmp != null) {
                        drawImage(
                            image = bmp.asImageBitmap(),
                            dstOffset = IntOffset(screenPos.x.toInt(), screenPos.y.toInt()),
                            dstSize = IntSize(tileSizePx.toInt(), tileSizePx.toInt())
                        )
                    } else {
                        // Draw light grid box placeholder
                        drawRect(
                            color = Color(0x15000000),
                            topLeft = screenPos,
                            size = androidx.compose.ui.geometry.Size(tileSizePx, tileSizePx),
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }

            // Draw Reference Route Polyline (Amber/Orange)
            referenceRoute?.points?.let { pts ->
                if (pts.size >= 2) {
                    val routePath = Path()
                    val p0 = project(pts[0].latitude, pts[0].longitude)
                    routePath.moveTo(p0.x, p0.y)
                    for (i in 1 until pts.size) {
                        val pi = project(pts[i].latitude, pts[i].longitude)
                        routePath.lineTo(pi.x, pi.y)
                    }

                    // Outer border
                    drawPath(
                        path = routePath,
                        color = Color.Black.copy(alpha = 0.5f),
                        style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Inner trail line
                    drawPath(
                        path = routePath,
                        color = TrailOrange,
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw Start & Finish Pins
                    val startPt = project(pts.first().latitude, pts.first().longitude)
                    drawCircle(color = HighContrastGreen, radius = 10f, center = startPt)
                    drawCircle(color = Color.White, radius = 5f, center = startPt)

                    val endPt = project(pts.last().latitude, pts.last().longitude)
                    drawCircle(color = Color.Red, radius = 10f, center = endPt)
                    drawCircle(color = Color.White, radius = 5f, center = endPt)
                }
            }

            // Draw Recorded Track Polyline (High Contrast Green glow)
            if (recordedPoints.size >= 2) {
                val trackPath = Path()
                val p0 = project(recordedPoints[0].latitude, recordedPoints[0].longitude)
                trackPath.moveTo(p0.x, p0.y)
                for (i in 1 until recordedPoints.size) {
                    val pi = project(recordedPoints[i].latitude, recordedPoints[i].longitude)
                    trackPath.lineTo(pi.x, pi.y)
                }

                drawPath(
                    path = trackPath,
                    color = Color.Black.copy(alpha = 0.6f),
                    style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = trackPath,
                    color = HighContrastGreen,
                    style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Draw Live Group Members
            for (member in groupMembers) {
                val mPos = project(member.latitude, member.longitude)
                val memberColor = if (member.isSos) OffTrackRed else Color(0xFF2979FF)
                // Halo
                drawCircle(color = memberColor.copy(alpha = 0.3f), radius = 18f, center = mPos)
                drawCircle(color = memberColor, radius = 9f, center = mPos)
                drawCircle(color = Color.White, radius = 4f, center = mPos)
            }

            // Draw Current Location Marker
            currentLocation?.let { loc ->
                val userPos = project(loc.latitude, loc.longitude)

                // Accuracy circle
                drawCircle(
                    color = Color(0x3300E676),
                    radius = (loc.accuracy * 2f).coerceIn(16f, 80f),
                    center = userPos
                )

                // Core beacon
                drawCircle(color = Color.White, radius = 12f, center = userPos)
                drawCircle(color = if (isOffTrack) OffTrackRed else HighContrastGreen, radius = 9f, center = userPos)

                // Heading arrow/cone
                val angleRad = Math.toRadians((loc.bearing + mapRotation - 90.0))
                val arrowTip = Offset(
                    userPos.x + (22f * cos(angleRad)).toFloat(),
                    userPos.y + (22f * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = Color.Black,
                    start = userPos,
                    end = arrowTip,
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }

            if (isDarkMode) {
                drawRect(color = Color(0xFF0F1722), blendMode = androidx.compose.ui.graphics.BlendMode.Multiply)
                drawRect(color = Color(0x77000000))
            }
        }

        // Off-Track Warning Banner
        if (isOffTrack) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                    .testTag("off_track_warning_banner"),
                shape = RoundedCornerShape(24.dp),
                color = OffTrackRed,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alerta desvío",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "¡FUERA DE RUTA!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Te has alejado %.0f m del trazado".format(offTrackDistanceMeters),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Map Control Floating Buttons (Right Side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom In
            SmallFloatingActionButton(
                onClick = { zoomLevel = (zoomLevel + 1f).coerceAtMost(18f) },
                modifier = Modifier.padding(bottom = 8.dp).testTag("btn_zoom_in"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.Add, contentDescription = "Acercar mapa")
            }

            // Zoom Out
            SmallFloatingActionButton(
                onClick = { zoomLevel = (zoomLevel - 1f).coerceAtLeast(8f) },
                modifier = Modifier.padding(bottom = 8.dp).testTag("btn_zoom_out"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Alejar mapa")
            }

            // Fullscreen Map Area Toggle (Maximizar área de mapa)
            if (onToggleFullScreen != null) {
                SmallFloatingActionButton(
                    onClick = onToggleFullScreen,
                    modifier = Modifier.padding(bottom = 8.dp).testTag("btn_toggle_map_fullscreen"),
                    containerColor = if (isFullScreen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    contentColor = if (isFullScreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(
                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullScreen) "Salir de pantalla completa" else "Pantalla completa (área máxima de mapa)"
                    )
                }
            }

            // Heading Follow Toggle (North up vs Heading up)
            SmallFloatingActionButton(
                onClick = onToggleHeadingUp,
                modifier = Modifier.padding(bottom = 8.dp).testTag("btn_heading_toggle"),
                containerColor = if (headingUpMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (headingUpMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Orientación del mapa",
                    modifier = Modifier.rotate(if (headingUpMode) currentHeading else 0f)
                )
            }

            // Center on GPS Position
            FloatingActionButton(
                onClick = {
                    userInteracting = false
                    currentLocation?.let {
                        centerLat = it.latitude
                        centerLon = it.longitude
                    }
                },
                modifier = Modifier.testTag("btn_recenter_gps"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Centrar en mi posición")
            }
        }

        // Active Map Layer Label (Top Left, below top bar, never covered by bottom cards)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 56.dp, start = 12.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Text(
                text = "${mapProvider.displayName} • z${intZoom}",
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
