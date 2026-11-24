package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SampleRoutes
import com.example.format.GeoJsonHandler
import com.example.format.GpxHandler
import com.example.format.KmlHandler
import com.example.format.PrivacyTrimmer
import com.example.model.ActivityRecord
import com.example.model.ActivityType
import com.example.model.BleSensor
import com.example.model.GpsPoint
import com.example.model.GroupMember
import com.example.model.GroupSession
import com.example.model.MapProvider
import com.example.model.OfflineMapArea
import com.example.model.OfflineStatus
import com.example.model.Route
import com.example.model.UserProfile
import com.example.sensor.BleSensorManager
import com.example.service.LocationTrackingService
import com.example.service.RecordingStatus
import com.example.service.TrackingManager
import com.example.service.TrackingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val routeDao = db.routeDao()
    private val activityDao = db.activityDao()
    private val offlineDao = db.offlineAreaDao()

    val trackingState: StateFlow<TrackingState> = TrackingManager.state

    val myRoutes = routeDao.getMyRoutes().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val communityRoutes = routeDao.getCommunityRoutes().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val activities = activityDao.getAllActivities().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _activitySearchQuery = MutableStateFlow("")
    val activitySearchQuery: StateFlow<String> = _activitySearchQuery.asStateFlow()

    fun setActivitySearchQuery(query: String) {
        _activitySearchQuery.value = query
    }

    val filteredActivities: StateFlow<List<ActivityRecord>> = kotlinx.coroutines.flow.combine(
        activityDao.getAllActivities(),
        _activitySearchQuery
    ) { acts, query ->
        if (query.isBlank()) acts
        else acts.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.notes.contains(query, ignoreCase = true) ||
            it.activityType.labelEs.contains(query, ignoreCase = true)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val offlineAreas = offlineDao.getAllAreas().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _currentMapProvider = MutableStateFlow(MapProvider.OPEN_TOPO)
    val currentMapProvider: StateFlow<MapProvider> = _currentMapProvider.asStateFlow()

    private val _selectedRoute = MutableStateFlow<Route?>(null)
    val selectedRoute: StateFlow<Route?> = _selectedRoute.asStateFlow()

    private val _selectedActivity = MutableStateFlow<ActivityRecord?>(null)
    val selectedActivity: StateFlow<ActivityRecord?> = _selectedActivity.asStateFlow()

    // Community filter state
    val filterActivity = MutableStateFlow<ActivityType?>(null)
    val filterProvince = MutableStateFlow("Todas")
    val searchQuery = MutableStateFlow("")

    // Group ride session state
    private val _groupSession = MutableStateFlow<GroupSession?>(null)
    val groupSession: StateFlow<GroupSession?> = _groupSession.asStateFlow()

    private var groupSimulationJob: Job? = null

    // User feedback messages (e.g. "Ruta importada con éxito")
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        // Seed initial routes and activities if empty
        viewModelScope.launch(Dispatchers.IO) {
            val initial = SampleRoutes.getInitialRoutes()
            routeDao.insertRoutes(initial)
            val initialActivities = SampleRoutes.getInitialActivities()
            initialActivities.forEach { activityDao.insertActivity(it) }
        }
    }

    private val _userProfile = MutableStateFlow(
        UserProfile(
            name = "Carlos El Caminante",
            email = "carlos@elcaminante.app",
            age = 34,
            weightKg = 74.5f,
            heightCm = 180,
            preferredActivity = "Ciclismo de Montaña (MTB)",
            emergencyContact = "+34 611 223 344",
            autoPauseEnabled = true,
            voicePromptsEnabled = true
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    fun updateUserProfile(newProfile: UserProfile) {
        _userProfile.value = newProfile
        TrackingManager.setAutoPauseEnabled(newProfile.autoPauseEnabled)
        TrackingManager.setBatterySaverBelow20Enabled(newProfile.batterySaverBelow20Enabled)
    }

    private val _isMapFullScreen = MutableStateFlow(false)
    val isMapFullScreen: StateFlow<Boolean> = _isMapFullScreen.asStateFlow()

    fun toggleMapFullScreen() {
        _isMapFullScreen.value = !_isMapFullScreen.value
    }

    fun setMapFullScreen(fullScreen: Boolean) {
        _isMapFullScreen.value = fullScreen
    }

    fun setMapProvider(provider: MapProvider) {
        _currentMapProvider.value = provider
    }

    fun toggleCommunityHeatmap() {
        val current = _userProfile.value
        updateUserProfile(current.copy(isCommunityHeatmapEnabled = !current.isCommunityHeatmapEnabled))
    }


    fun selectRoute(route: Route?) {
        _selectedRoute.value = route
    }

    fun selectActivity(activity: ActivityRecord?) {
        _selectedActivity.value = activity
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun startTracking() {
        TrackingManager.setAutoPauseEnabled(_userProfile.value.autoPauseEnabled)
        TrackingManager.setBatterySaverBelow20Enabled(_userProfile.value.batterySaverBelow20Enabled)
        TrackingManager.startRecording(getApplication())
        LocationTrackingService.startService(getApplication())
    }

    fun pauseTracking() {
        TrackingManager.pauseRecording()
    }

    fun resumeTracking() {
        TrackingManager.resumeRecording()
    }

    fun stopAndSaveTracking(notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val record = TrackingManager.finishRecording(notes)
            if (record != null) {
                activityDao.insertActivity(record)
                _statusMessage.value = "Actividad guardada: %.2f km".format(record.distanceKm)
            }
            LocationTrackingService.stopService(getApplication())
        }
    }

    fun discardTracking() {
        TrackingManager.discardRecording()
        LocationTrackingService.stopService(getApplication())
        _statusMessage.value = "Actividad descartada"
    }

    fun setActivityType(type: ActivityType) {
        TrackingManager.setActivityType(type)
    }

    fun loadReferenceRoute(route: Route) {
        TrackingManager.setReferenceRoute(route)
        _statusMessage.value = "Ruta cargada para navegación: ${route.name}"
    }

    fun clearReferenceRoute() {
        TrackingManager.setReferenceRoute(null)
        _statusMessage.value = "Navegación de ruta desactivada"
    }

    fun toggleHeadingUp() {
        val curr = trackingState.value.headingUpMode
        TrackingManager.setHeadingUpMode(!curr)
    }

    fun toggleKeepScreenOn() {
        val curr = trackingState.value.keepScreenOn
        TrackingManager.setKeepScreenOn(!curr)
    }

    fun toggleFavorite(route: Route) {
        viewModelScope.launch(Dispatchers.IO) {
            routeDao.updateRoute(route.copy(isFavorite = !route.isFavorite))
        }
    }

    fun deleteRoute(route: Route) {
        viewModelScope.launch(Dispatchers.IO) {
            routeDao.deleteRoute(route)
            _statusMessage.value = "Ruta eliminada"
        }
    }

    fun deleteActivity(activity: ActivityRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            activityDao.deleteActivity(activity)
            _statusMessage.value = "Actividad eliminada del historial"
        }
    }

    /**
     * Publishes a route to the community catalog with 150m privacy trimming applied.
     */
    fun publishToCommunity(route: Route, authorNickname: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedPoints = PrivacyTrimmer.trimTrack(route.points, 150.0)
            val communityRoute = route.copy(
                id = "comm_" + UUID.randomUUID().toString().take(8),
                points = trimmedPoints,
                authorNickname = authorNickname,
                isCommunity = true,
                privacyTrimmed = true,
                createdTimestamp = System.currentTimeMillis()
            )
            routeDao.insertRoute(communityRoute)
            _statusMessage.value = "¡Ruta publicada en la comunidad! (Recorte de privacidad 150m aplicado)"
        }
    }

    fun importTrackFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cr = getApplication<Application>().contentResolver
                val inputStream = cr.openInputStream(uri)
                if (inputStream == null) {
                    _statusMessage.value = "Error al abrir el archivo"
                    return@launch
                }

                val uriStr = uri.toString().lowercase()
                val (name, points) = when {
                    uriStr.endsWith(".kml") -> KmlHandler.parseKml(inputStream)
                    uriStr.endsWith(".geojson") || uriStr.endsWith(".json") -> GeoJsonHandler.parseGeoJson(inputStream)
                    else -> GpxHandler.parseGpx(inputStream)
                }

                inputStream.close()

                if (points.isEmpty()) {
                    _statusMessage.value = "El archivo no contiene puntos de track válidos"
                    return@launch
                }

                var totalDist = 0.0
                var gain = 0.0
                var loss = 0.0
                for (i in 0 until points.size - 1) {
                    totalDist += points[i].distanceTo(points[i + 1])
                    val dEle = points[i + 1].altitude - points[i].altitude
                    if (dEle > 0) gain += dEle else loss += -dEle
                }

                val newRoute = Route(
                    id = UUID.randomUUID().toString(),
                    name = name.ifBlank { "Ruta Importada" },
                    description = "Importada desde archivo (${points.size} puntos)",
                    activityType = ActivityType.MTB,
                    points = points,
                    distanceMeters = totalDist,
                    elevationGain = gain,
                    elevationLoss = loss,
                    difficulty = if (gain > 800) "Difícil" else if (gain > 400) "Moderada" else "Fácil",
                    authorNickname = "Mi Dispositivo",
                    isCommunity = false
                )

                routeDao.insertRoute(newRoute)
                _statusMessage.value = "Ruta '${newRoute.name}' importada (%.1f km)".format(newRoute.distanceKm)
            } catch (e: Exception) {
                _statusMessage.value = "Error importando ruta: ${e.localizedMessage}"
            }
        }
    }

    fun savePlannedRoute(route: Route) {
        viewModelScope.launch(Dispatchers.IO) {
            routeDao.insertRoute(route)
            _statusMessage.value = "Ruta planificada guardada: ${route.name} (%.1f km)".format(route.distanceKm)
        }
    }

    fun createGroupSession(groupName: String, nickname: String) {
        val code = "MTB-" + (1000..9999).random()
        val leader = GroupMember(
            id = "user_me",
            nickname = nickname,
            latitude = trackingState.value.currentPoint?.latitude ?: 40.7512,
            longitude = trackingState.value.currentPoint?.longitude ?: -4.0125,
            isLeader = true
        )
        // Add sample nearby riders for group cycling experience
        val member2 = GroupMember(
            id = "user_2",
            nickname = "Marcos_Orbea",
            latitude = leader.latitude + 0.0035,
            longitude = leader.longitude + 0.0028,
            speedKmh = 19.5f,
            heartRate = 145
        )
        val member3 = GroupMember(
            id = "user_3",
            nickname = "Laura_Canyon",
            latitude = leader.latitude - 0.0022,
            longitude = leader.longitude - 0.0019,
            speedKmh = 17.8f,
            heartRate = 152
        )

        _groupSession.value = GroupSession(
            groupCode = code,
            groupName = groupName,
            adminNickname = nickname,
            members = listOf(leader, member2, member3)
        )
        startGroupSimulation()
        _statusMessage.value = "Grupo '$groupName' creado con código: $code"
    }

    fun joinGroupSession(groupCode: String, nickname: String) {
        val myPos = trackingState.value.currentPoint
        val me = GroupMember(
            id = "user_me",
            nickname = nickname,
            latitude = myPos?.latitude ?: 40.7512,
            longitude = myPos?.longitude ?: -4.0125
        )
        val buddy1 = GroupMember(
            id = "user_lead",
            nickname = "Guía_Sierra",
            latitude = me.latitude + 0.0018,
            longitude = me.longitude + 0.0015,
            isLeader = true,
            speedKmh = 21.0f
        )

        _groupSession.value = GroupSession(
            groupCode = groupCode.uppercase(),
            groupName = "Pelotón Sendero $groupCode",
            adminNickname = "Guía_Sierra",
            members = listOf(me, buddy1)
        )
        startGroupSimulation()
        _statusMessage.value = "Conectado al grupo $groupCode"
    }

    fun sendSosSignal() {
        _groupSession.update { session ->
            session?.copy(
                members = session.members.map {
                    if (it.id == "user_me") it.copy(isSos = true) else it
                }
            )
        }
        _statusMessage.value = "🚨 ¡Alerta SOS emitida al grupo!"
    }

    fun leaveGroup() {
        groupSimulationJob?.cancel()
        _groupSession.value = null
        _statusMessage.value = "Has salido del grupo"
    }

    private fun startGroupSimulation() {
        groupSimulationJob?.cancel()
        groupSimulationJob = viewModelScope.launch(Dispatchers.Default) {
            var step = 0
            while (isActive && _groupSession.value != null) {
                delay(3000L)
                step++
                _groupSession.update { session ->
                    session?.copy(
                        members = session.members.map { m ->
                            if (m.id == "user_me") {
                                val cur = trackingState.value.currentPoint
                                if (cur != null) m.copy(latitude = cur.latitude, longitude = cur.longitude, speedKmh = cur.speedKmh)
                                else m
                            } else {
                                // Realistic group movement along trails
                                val deltaLat = Math.sin((step + m.id.hashCode()) * 0.2) * 0.0003
                                val deltaLon = Math.cos((step + m.id.hashCode()) * 0.2) * 0.0003
                                m.copy(
                                    latitude = m.latitude + deltaLat,
                                    longitude = m.longitude + deltaLon,
                                    speedKmh = (16f + (Math.sin(step * 0.3) * 6f).toFloat()).coerceAtLeast(0f),
                                    heartRate = 138 + (Math.sin(step * 0.2) * 20).toInt()
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    fun downloadArea(name: String, providerId: String, minLat: Double, maxLat: Double, minLon: Double, maxLon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val area = OfflineMapArea(
                id = UUID.randomUUID().toString(),
                name = name,
                providerId = providerId,
                minLat = minLat,
                maxLat = maxLat,
                minLon = minLon,
                maxLon = maxLon,
                minZoom = 12,
                maxZoom = 14,
                status = OfflineStatus.DOWNLOADING
            )
            offlineDao.insertArea(area)
            _statusMessage.value = "Descargando zona sin conexión: $name"

            com.example.map.MapTileEngine.downloadAreaOffline(
                context = getApplication(),
                area = area
            ) { downloaded, total, bytes ->
                viewModelScope.launch(Dispatchers.IO) {
                    offlineDao.updateArea(
                        area.copy(
                            downloadedTiles = downloaded,
                            totalTiles = total,
                            sizeBytes = bytes
                        )
                    )
                }
            }
        }
    }

    fun deleteOfflineArea(area: OfflineMapArea) {
        viewModelScope.launch(Dispatchers.IO) {
            com.example.map.MapTileEngine.getRepository(getApplication()).deleteArea(area)
            _statusMessage.value = "Zona sin conexión eliminada"
        }
    }
}
