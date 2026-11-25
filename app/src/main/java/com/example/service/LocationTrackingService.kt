package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.GpsPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var wakeLock: PowerManager.WakeLock? = null
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    val pct = (level * 100) / scale
                    TrackingManager.checkBattery(this@LocationTrackingService)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) {
                    processLocation(loc)
                }
            }
        }

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SenderoMTB:TrackingWakeLock")
        wakeLock?.acquire(8 * 60 * 60 * 1000L) // up to 8 hours
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundTracking()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                TrackingManager.pauseRecording()
            }
            ACTION_RESUME -> {
                TrackingManager.resumeRecording()
            }
            else -> {
                startForegroundTracking()
            }
        }
        return START_STICKY
    }

    private fun startForegroundTracking() {
        val notification = buildNotification("Grabando actividad...", "0.0 km | 00:00")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        requestLocationUpdates()
        BatteryUsageService.start(this)

        // Periodic notification updater
        serviceJob?.cancel()
        serviceJob = scope.launch {
            while (isActive) {
                delay(2000L)
                val s = TrackingManager.state.value
                if (s.status == RecordingStatus.RECORDING) {
                    val text = String.format(
                        "%.2f km • %.1f km/h • %s",
                        s.distanceKm,
                        s.currentSpeedKmh,
                        s.formattedDuration
                    )
                    val title = if (s.isOffTrack) "⚠️ ¡DESVÍO DE RUTA! (+%.0fm)".format(s.offTrackDistanceMeters)
                    else "${s.activityType.labelEs} en curso"

                    val updatedNotif = buildNotification(title, text)
                    val manager = getSystemService(NotificationManager::class.java)
                    manager?.notify(NOTIFICATION_ID, updatedNotif)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val isSaverActive = TrackingManager.state.value.isBatterySaverActive
        val intervalMs = if (isSaverActive) 10000L else 1000L
        val minDistance = if (isSaverActive) 15.0f else 1.5f
        val priority = if (isSaverActive) Priority.PRIORITY_BALANCED_POWER_ACCURACY else Priority.PRIORITY_HIGH_ACCURACY

        val locationRequest = LocationRequest.Builder(
            priority,
            intervalMs
        ).apply {
            setMinUpdateIntervalMillis(intervalMs / 2)
            setMinUpdateDistanceMeters(minDistance)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
        }
    }

    private fun processLocation(loc: Location) {
        val point = GpsPoint(
            latitude = loc.latitude,
            longitude = loc.longitude,
            altitude = loc.altitude,
            speed = if (loc.hasSpeed()) loc.speed else 0f,
            timestamp = loc.time,
            accuracy = if (loc.hasAccuracy()) loc.accuracy else 5f,
            bearing = if (loc.hasBearing()) loc.bearing else 0f
        )
        TrackingManager.onLocationUpdate(point, this)
    }

    private fun stopForegroundTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}
        serviceJob?.cancel()
        BatteryUsageService.stop(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sendero MTB - Grabación Activa",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra el estado del track GPS en vivo y alertas de desvío"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundTracking()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "sendero_tracking_channel"
        const val NOTIFICATION_ID = 4040
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"

        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
