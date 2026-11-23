package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import com.example.ui.theme.PineGreenPrimary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.core.content.FileProvider
import com.example.format.GpxHandler
import com.example.model.ActivityType
import com.example.model.Route
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.OffTrackRed
import com.example.ui.theme.TrailOrange
import com.example.ui.viewmodel.AppViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    viewModel: AppViewModel,
    onRouteSelectedForDetail: (Route) -> Unit = {},
    onStartNavigation: (Route) -> Unit = {},
    onOpenRoutePlanner: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Mis Rutas, 1: Comunidad

    val myRoutes by viewModel.myRoutes.collectAsState()
    val communityRoutes by viewModel.communityRoutes.collectAsState()
    val filterActivity by viewModel.filterActivity.collectAsState()
    val filterProvince by viewModel.filterProvince.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showPublishDialog by remember { mutableStateOf<Route?>(null) }
    var publisherNickname by remember { mutableStateOf("BikerPro") }

    // File picker launcher for GPX / KML / GeoJSON
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importTrackFile(it) }
    }

    val activeList = if (selectedTab == 0) myRoutes else communityRoutes
    val filteredRoutes = activeList.filter { route ->
        val matchesSearch = searchQuery.isBlank() ||
                route.name.contains(searchQuery, ignoreCase = true) ||
                route.province.contains(searchQuery, ignoreCase = true)
        val matchesActivity = filterActivity == null || route.activityType == filterActivity
        val matchesProvince = filterProvince == "Todas" || route.province.contains(filterProvince, ignoreCase = true)
        matchesSearch && matchesActivity && matchesProvince
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Tabs
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Mis Rutas (${myRoutes.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_my_routes")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Comunidad (${communityRoutes.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_community_routes")
                )
            }

            // Search Bar & Filter Strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = onOpenRoutePlanner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("btn_open_route_planner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PineGreenPrimary)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Planificar Nueva Ruta con Waypoints", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Buscar por nombre o provincia...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_routes_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                // Activity type filter chips
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterActivity == null,
                        onClick = { viewModel.filterActivity.value = null },
                        label = { Text("Todas") }
                    )
                    ActivityType.entries.forEach { type ->
                        FilterChip(
                            selected = filterActivity == type,
                            onClick = { viewModel.filterActivity.value = if (filterActivity == type) null else type },
                            label = { Text(type.labelEs) },
                            leadingIcon = { Icon(type.icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                // Province filter chips (Madrid, Asturias, Granada, Barcelona)
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Todas", "Madrid", "Asturias", "Granada", "Barcelona").forEach { prov ->
                        FilterChip(
                            selected = filterProvince == prov,
                            onClick = { viewModel.filterProvince.value = prov },
                            label = { Text(prov, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Routes List
            if (filteredRoutes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No se encontraron rutas con estos filtros", color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            viewModel.filterActivity.value = null
                            viewModel.filterProvince.value = "Todas"
                            viewModel.searchQuery.value = ""
                        }) {
                            Text("Restablecer filtros")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRoutes, key = { it.id }) { route ->
                        RouteCard(
                            route = route,
                            onCardClick = { onRouteSelectedForDetail(route) },
                            onFollowRoute = {
                                viewModel.loadReferenceRoute(route)
                                onStartNavigation(route)
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(route) },
                            onPublishClick = { showPublishDialog = route },
                            onExportGpx = { exportGpxFile(context, route) },
                            onDelete = { viewModel.deleteRoute(route) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // Import FAB Button (GPX, KML, GeoJSON)
        FloatingActionButton(
            onClick = {
                filePickerLauncher.launch(
                    arrayOf(
                        "*/*",
                        "application/gpx+xml",
                        "application/vnd.google-earth.kml+xml",
                        "application/geo+json",
                        "application/json"
                    )
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("btn_import_route_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FileUpload, contentDescription = "Importar")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Importar GPX / KML", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Publish to Community with Privacy Trim Dialog
    showPublishDialog?.let { routeToPublish ->
        AlertDialog(
            onDismissRequest = { showPublishDialog = null },
            icon = { Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Publicar Ruta en la Comunidad") },
            text = {
                Column {
                    Text(
                        text = "Para proteger tu privacidad, Sendero MTB recortará automáticamente 150 metros del inicio y fin de la ruta. Esto oculta la ubicación exacta de tu casa o punto de salida.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = publisherNickname,
                        onValueChange = { publisherNickname = it },
                        label = { Text("Tu apodo de creador") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.publishToCommunity(routeToPublish, publisherNickname)
                        showPublishDialog = null
                    },
                    modifier = Modifier.testTag("btn_confirm_publish_community")
                ) {
                    Text("Recortar 150m y Publicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun RouteCard(
    route: Route,
    onCardClick: () -> Unit,
    onFollowRoute: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPublishClick: () -> Unit,
    onExportGpx: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("route_card_${route.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Activity icon, Name, Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(route.activityType.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(route.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                    Text(
                        text = "${route.activityType.labelEs} • ${route.province} • por ${route.authorNickname}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (route.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (route.isFavorite) OffTrackRed else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Strip: Distance, Desnivel, Difficulty, Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricChip(label = "DISTANCIA", value = "%.1f km".format(route.distanceKm))
                MetricChip(label = "DESNIVEL +", value = "+%.0f m".format(route.elevationGain))
                MetricChip(label = "TIEMPO EST.", value = "${route.estimatedDurationMinutes} min")

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (route.difficulty) {
                        "Fácil" -> HighContrastGreen.copy(alpha = 0.2f)
                        "Moderada" -> TrailOrange.copy(alpha = 0.2f)
                        else -> OffTrackRed.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = route.difficulty,
                        color = when (route.difficulty) {
                            "Fácil" -> HighContrastGreen
                            "Moderada" -> TrailOrange
                            else -> OffTrackRed
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Privacy Trimmed Tag indicator
            if (route.privacyTrimmed) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = HighContrastGreen, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Protección de privacidad activa (-150m en inicio/fin)", fontSize = 11.sp, color = HighContrastGreen)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Card Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onFollowRoute,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Seguir Ruta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onExportGpx,
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir GPX", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GPX", fontSize = 12.sp)
                }

                if (!route.isCommunity) {
                    OutlinedButton(
                        onClick = onPublishClick,
                        modifier = Modifier.height(38.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Publicar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricChip(label: String, value: String) {
    Column {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
    }
}

fun exportGpxFile(context: android.content.Context, route: Route) {
    val xml = GpxHandler.exportToGpx(route.name, route.description, route.points)
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/xml"
            putExtra(Intent.EXTRA_SUBJECT, "Ruta GPX - ${route.name}")
            putExtra(Intent.EXTRA_TEXT, xml)
        }
        val chooser = Intent.createChooser(sendIntent, "Exportar GPX")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (_: Exception) {}
}
