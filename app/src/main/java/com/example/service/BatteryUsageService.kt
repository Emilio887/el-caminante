package com.example.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Service that tracks and logs battery consumption levels, temperature, and voltage
 * during long-duration GPS activities to help users optimize battery life.
 */
class BatteryUsageService : Service() {

    private var monitoringJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                processBatteryIntent(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_LOGGING -> {
                stopMonitoring()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_LOGGING -> {
                startMonitoring()
            }
            else -> {
                startMonitoring()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val initialLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 80
        BatteryUsageManager.startSession(initialLevel)

        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive) {
                // Poll every 30 seconds for long duration outdoor rides
                delay(30000L)
                val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                if (intent != null) {
                    processBatteryIntent(intent)
                }
            }
        }
    }

    private fun processBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 80
        val tempCelsius = tempRaw / 10.0f
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isScreenOn = pm?.isInteractive ?: true

        BatteryUsageManager.logSnapshot(
            level = batteryPct,
            temperatureCelsius = tempCelsius,
            voltageMilliVolts = voltage,
            isCharging = isCharging,
            screenOn = isScreenOn
        )
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        BatteryUsageManager.endSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_LOGGING = "com.example.service.ACTION_START_BATTERY_LOGGING"
        const val ACTION_STOP_LOGGING = "com.example.service.ACTION_STOP_BATTERY_LOGGING"

        fun start(context: Context) {
            val intent = Intent(context, BatteryUsageService::class.java).apply {
                action = ACTION_START_LOGGING
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, BatteryUsageService::class.java).apply {
                action = ACTION_STOP_LOGGING
            }
            context.startService(intent)
        }
    }
}
