package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.OfflineMapArea
import com.example.model.OfflineStatus
import com.example.ui.theme.OffTrackRed
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.MapViewModel

@Composable
fun OfflineMapsScreen(
    appViewModel: AppViewModel? = null,
    viewModel: AppViewModel? = appViewModel,
    mapViewModel: MapViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val offlineAreas by mapViewModel.offlineAreas.collectAsState()
    val totalCachedBytes by mapViewModel.totalCachedBytes.collectAsState()
    val totalCachedTilesCount by mapViewModel.totalCachedTilesCount.collectAsState()
    val currentProvider by mapViewModel.currentProvider.collectAsState()
    val downloadProgress by mapViewModel.downloadProgress.collectAsState()

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val formattedTotalStorage = if (totalCachedBytes >= 1024 * 1024) {
        String.format("%.1f MB", totalCachedBytes / (1024.0 * 1024.0))
    } else {
        String.format("%.0f KB", totalCachedBytes / 1024.0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Text("Mapas Sin Conexión (Room Storage)", fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(
            "Almacenamiento y recuperación de teselas sin conexión gestionadas directamente en base de datos local SQLite/Room.",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        // Storage Usage Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("card_offline_storage_info"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Base de Datos Room de Teselas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "$formattedTotalStorage ocupados • $totalCachedTilesCount teselas • ${offlineAreas.size} zonas",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (totalCachedTilesCount > 0) {
                        OutlinedButton(
                            onClick = { showClearConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OffTrackRed),
                            modifier = Modifier.testTag("btn_clear_all_tiles_cache")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Limpiar", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pre-configured MTB Zones Ready to Download into Room
        Text("Zonas Topográficas Rápidas", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickZoneCard(
                name = "Guadarrama MTB",
                sub = "Madrid / Segovia",
                modifier = Modifier.weight(1f).testTag("btn_download_guadarrama"),
                onDownload = {
                    mapViewModel.downloadArea("Sierra de Guadarrama", currentProvider, 40.70, 40.85, -4.15, -3.95)
                }
            )

            QuickZoneCard(
                name = "Picos de Europa",
                sub = "Asturias / Cantabria",
                modifier = Modifier.weight(1f).testTag("btn_download_picos"),
                onDownload = {
                    mapViewModel.downloadArea("Picos de Europa", currentProvider, 43.10, 43.25, -4.90, -4.70)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickZoneCard(
                name = "Sierra Nevada",
                sub = "Granada / Alpujarra",
                modifier = Modifier.weight(1f).testTag("btn_download_sierra_nevada"),
                onDownload = {
                    mapViewModel.downloadArea("Sierra Nevada Veleta", currentProvider, 37.00, 37.15, -3.45, -3.25)
                }
            )

            QuickZoneCard(
                name = "Collserola BCN",
                sub = "Barcelona",
                modifier = Modifier.weight(1f).testTag("btn_download_collserola"),
                onDownload = {
                    mapViewModel.downloadArea("Parc Collserola", currentProvider, 41.40, 41.48, 2.05, 2.18)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Downloaded Areas List in Room
        Text("Zonas en Memoria Local (${offlineAreas.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (offlineAreas.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No hay mapas descargados en Room", color = Color.Gray, fontSize = 13.sp)
                    Text("Pulsa en 'Descargar' para almacenar las teselas en SQLite", color = Color.Gray, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(offlineAreas, key = { it.id }) { area ->
                    val activeProgress = downloadProgress[area.id] ?: area.progressPercent

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("area_card_${area.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(area.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "${area.providerId.uppercase()} • ${area.downloadedTiles}/${area.totalTiles} teselas • ${area.formattedSize}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                IconButton(onClick = { mapViewModel.deleteArea(area) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar de Room", tint = Color.Gray)
                                }
                            }

                            if (area.status == OfflineStatus.DOWNLOADING || downloadProgress.containsKey(area.id)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (activeProgress / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Guardando teselas en Room... %.0f%%".format(activeProgress),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for clearing all tiles
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("¿Vaciar caché de teselas?") },
            text = { Text("Se eliminarán todas las teselas guardadas en la base de datos Room. Deberás volver a descargarlas para navegar sin conexión.") },
            confirmButton = {
                Button(
                    onClick = {
                        mapViewModel.clearAllTilesCache()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OffTrackRed)
                ) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun QuickZoneCard(
    name: String,
    sub: String,
    modifier: Modifier = Modifier,
    onDownload: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(sub, fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth().height(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Descargar", fontSize = 11.sp)
            }
        }
    }
}
