package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MapTileRepository
import com.example.model.MapProvider
import com.example.model.OfflineMapArea
import com.example.model.OfflineStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MapViewModel(
    application: Application,
    private val repository: MapTileRepository = run {
        val db = AppDatabase.getDatabase(application)
        MapTileRepository(db.mapTileDao(), db.offlineAreaDao())
    }
) : AndroidViewModel(application) {

    val offlineAreas: StateFlow<List<OfflineMapArea>> = repository.offlineAreas.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val totalCachedBytes: StateFlow<Long> = repository.totalCachedBytes
        .map { it ?: 0L }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0L
        )

    val totalCachedTilesCount: StateFlow<Int> = repository.totalCachedTilesCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    private val _currentProvider = MutableStateFlow(MapProvider.OPEN_TOPO)
    val currentProvider: StateFlow<MapProvider> = _currentProvider.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setMapProvider(provider: MapProvider) {
        _currentProvider.value = provider
    }

    fun clearMessage() {
        _message.value = null
    }

    suspend fun getTileBitmap(
        provider: MapProvider,
        zoom: Int,
        x: Int,
        y: Int
    ): Bitmap? {
        return repository.getTile(provider, zoom, x, y)
    }

    fun downloadArea(
        name: String,
        provider: MapProvider = _currentProvider.value,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        minZoom: Int = 12,
        maxZoom: Int = 14
    ) {
        val areaId = UUID.randomUUID().toString()
        val area = OfflineMapArea(
            id = areaId,
            name = name,
            providerId = provider.id,
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon,
            minZoom = minZoom,
            maxZoom = maxZoom,
            status = OfflineStatus.DOWNLOADING
        )

        _message.value = "Iniciando descarga en Room: $name"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.cacheArea(area) { downloaded, total, bytes ->
                    val progress = if (total > 0) (downloaded.toFloat() / total.toFloat()) * 100f else 0f
                    _downloadProgress.update { curr ->
                        curr + (areaId to progress)
                    }
                }
                _downloadProgress.update { it - areaId }
                _message.value = "Descarga completada en base de datos: $name"
            } catch (e: Exception) {
                _downloadProgress.update { it - areaId }
                _message.value = "Error al descargar $name: ${e.localizedMessage}"
            }
        }
    }

    fun deleteArea(area: OfflineMapArea) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteArea(area)
            _message.value = "Zona eliminada de Room: ${area.name}"
        }
    }

    fun clearAllTilesCache() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllTiles()
            _message.value = "Caché de teselas borrado de Room"
        }
    }
}
