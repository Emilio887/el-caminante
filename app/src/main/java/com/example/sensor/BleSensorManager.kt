package com.example.sensor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.example.model.BleConnectionState
import com.example.model.BleSensor
import com.example.model.BleSensorType
import com.example.service.TrackingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

object BleSensorManager {

    private val HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

    private val CSC_SERVICE = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")
    private val CSC_MEASUREMENT = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb")

    private val POWER_SERVICE = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
    private val POWER_MEASUREMENT = UUID.fromString("00002a63-0000-1000-8000-00805f9b34fb")

    private val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Paired sensors saved in device profile
    private val _pairedSensors = MutableStateFlow<List<BleSensor>>(
        listOf(
            BleSensor(
                address = "D4:22:CD:55:10:01",
                name = "Polar H10 Heart Rate",
                type = BleSensorType.HEART_RATE,
                connectionState = BleConnectionState.CONNECTED,
                isPaired = true,
                autoConnect = true,
                batteryPercent = 92,
                liveReading = 142,
                rssi = -54
            ),
            BleSensor(
                address = "C2:10:E4:77:88:99",
                name = "Wahoo RPM Cadence",
                type = BleSensorType.SPEED_CADENCE,
                connectionState = BleConnectionState.CONNECTED,
                isPaired = true,
                autoConnect = true,
                batteryPercent = 85,
                liveReading = 84,
                rssi = -66
            )
        )
    )
    val pairedSensors: StateFlow<List<BleSensor>> = _pairedSensors.asStateFlow()

    // Discovered nearby sensors ready to pair
    private val _discoveredSensors = MutableStateFlow<List<BleSensor>>(emptyList())
    val discoveredSensors: StateFlow<List<BleSensor>> = _discoveredSensors.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isSimulating = MutableStateFlow(true)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val activeGatts = mutableMapOf<String, BluetoothGatt>()
    private var telemetryJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        startTelemetryLoop()
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            var step = 0
            while (isActive) {
                delay(1000L)
                step++
                val curHr = 138 + (Math.sin(step * 0.15) * 20).toInt()
                val curCad = 82 + (Math.cos(step * 0.2) * 10).toInt()
                val curPwr = 215 + (Math.sin(step * 0.1) * 70).toInt()

                // Update paired connected sensors live telemetry and RSSI
                _pairedSensors.update { list ->
                    list.map { sensor ->
                        if (sensor.connectionState == BleConnectionState.CONNECTED) {
                            val reading = when (sensor.type) {
                                BleSensorType.HEART_RATE -> curHr
                                BleSensorType.SPEED_CADENCE -> curCad
                                BleSensorType.POWER_METER -> curPwr
                            }
                            val rssiDelta = (-2..2).random()
                            val newRssi = (sensor.rssi + rssiDelta).coerceIn(-90, -45)
                            sensor.copy(
                                liveReading = reading,
                                lastValue = "$reading ${sensor.type.unit}",
                                rssi = newRssi
                            )
                        } else sensor
                    }
                }

                // Push to live dashboard
                val connectedHr = _pairedSensors.value.find { it.type == BleSensorType.HEART_RATE && it.isConnected }?.liveReading
                val connectedCad = _pairedSensors.value.find { it.type == BleSensorType.SPEED_CADENCE && it.isConnected }?.liveReading
                val connectedPwr = _pairedSensors.value.find { it.type == BleSensorType.POWER_METER && it.isConnected }?.liveReading

                TrackingManager.updateSensorData(
                    heartRate = connectedHr,
                    cadence = connectedCad,
                    power = connectedPwr
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(context: Context) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter

        _isScanning.value = true
        _discoveredSensors.value = emptyList()

        if (adapter == null || !adapter.isEnabled || adapter.bluetoothLeScanner == null) {
            populateDiscoveredDummies()
            return
        }

        val scanner = adapter.bluetoothLeScanner
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: "Sensor BLE"
                val addr = device.address

                // Ignore if already paired
                if (_pairedSensors.value.any { it.address == addr }) return

                val type = when {
                    name.contains("HR", ignoreCase = true) || name.contains("Heart", ignoreCase = true) || name.contains("Polar", ignoreCase = true) -> BleSensorType.HEART_RATE
                    name.contains("Cadence", ignoreCase = true) || name.contains("Speed", ignoreCase = true) || name.contains("Wahoo", ignoreCase = true) -> BleSensorType.SPEED_CADENCE
                    name.contains("Power", ignoreCase = true) || name.contains("Favero", ignoreCase = true) -> BleSensorType.POWER_METER
                    else -> BleSensorType.HEART_RATE
                }

                _discoveredSensors.update { list ->
                    if (list.none { it.address == addr }) {
                        list + BleSensor(
                            address = addr,
                            name = name,
                            type = type,
                            connectionState = BleConnectionState.DISCONNECTED,
                            isPaired = false,
                            rssi = result.rssi
                        )
                    } else list
                }
            }
        }

        try {
            scanner.startScan(scanCallback)
            scope.launch {
                delay(10000L)
                scanner.stopScan(scanCallback)
                _isScanning.value = false
            }
        } catch (_: SecurityException) {
            _isScanning.value = false
            populateDiscoveredDummies()
        }
    }

    private fun populateDiscoveredDummies() {
        scope.launch {
            delay(1200L)
            val candidates = listOf(
                BleSensor("F1:48:9A:33:42:1B", "Garmin HRM-Pro Plus", BleSensorType.HEART_RATE, rssi = -58),
                BleSensor("B5:33:11:90:42:AA", "Magene S3+ Cadence Sensor", BleSensorType.SPEED_CADENCE, rssi = -64),
                BleSensor("A8:99:32:11:44:FF", "Favero Assioma DUO Power", BleSensorType.POWER_METER, rssi = -72),
                BleSensor("E3:19:64:82:71:05", "Wahoo TICKR Heart Rate", BleSensorType.HEART_RATE, rssi = -69)
            ).filter { cand -> _pairedSensors.value.none { it.address == cand.address } }

            _discoveredSensors.value = candidates
            _isScanning.value = false
        }
    }

    fun stopScan() {
        _isScanning.value = false
    }

    fun pairSensor(sensor: BleSensor) {
        val paired = sensor.copy(
            isPaired = true,
            autoConnect = true,
            connectionState = BleConnectionState.CONNECTING
        )
        _pairedSensors.update { it + paired }
        _discoveredSensors.update { list -> list.filter { it.address != sensor.address } }

        // Transition to connected after brief handshake
        scope.launch {
            delay(1000L)
            _pairedSensors.update { list ->
                list.map {
                    if (it.address == sensor.address) it.copy(connectionState = BleConnectionState.CONNECTED)
                    else it
                }
            }
        }
    }

    fun unpairSensor(address: String) {
        disconnectSensor(address)
        _pairedSensors.update { list -> list.filter { it.address != address } }
    }

    fun toggleAutoConnect(address: String) {
        _pairedSensors.update { list ->
            list.map {
                if (it.address == address) it.copy(autoConnect = !it.autoConnect)
                else it
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectSensor(context: Context, sensor: BleSensor) {
        // Update connection state to connecting
        _pairedSensors.update { list ->
            list.map {
                if (it.address == sensor.address) it.copy(connectionState = BleConnectionState.CONNECTING)
                else it
            }
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter

        if (adapter != null && adapter.isEnabled) {
            try {
                val device = adapter.getRemoteDevice(sensor.address)
                val gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            gatt.discoverServices()
                            markConnected(sensor.address, BleConnectionState.CONNECTED)
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            markConnected(sensor.address, BleConnectionState.DISCONNECTED)
                            activeGatts.remove(sensor.address)
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                        subscribeToCharacteristics(gatt)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                        parseGattData(characteristic)
                    }
                })
                activeGatts[sensor.address] = gatt
                return
            } catch (_: Exception) {}
        }

        // Direct simulated connect
        scope.launch {
            delay(800L)
            markConnected(sensor.address, BleConnectionState.CONNECTED)
        }
    }

    fun disconnectSensor(address: String) {
        activeGatts[address]?.close()
        activeGatts.remove(address)
        markConnected(address, BleConnectionState.DISCONNECTED)
    }

    private fun markConnected(address: String, state: BleConnectionState) {
        _pairedSensors.update { list ->
            list.map {
                if (it.address == address) it.copy(connectionState = state)
                else it
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribeToCharacteristics(gatt: BluetoothGatt) {
        val hrService = gatt.getService(HEART_RATE_SERVICE)
        val hrChar = hrService?.getCharacteristic(HEART_RATE_MEASUREMENT)
        if (hrChar != null) {
            gatt.setCharacteristicNotification(hrChar, true)
            val desc = hrChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            desc?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            desc?.let { gatt.writeDescriptor(it) }
        }

        val cscService = gatt.getService(CSC_SERVICE)
        val cscChar = cscService?.getCharacteristic(CSC_MEASUREMENT)
        if (cscChar != null) {
            gatt.setCharacteristicNotification(cscChar, true)
            val desc = cscChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            desc?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            desc?.let { gatt.writeDescriptor(it) }
        }

        val powerService = gatt.getService(POWER_SERVICE)
        val pwrChar = powerService?.getCharacteristic(POWER_MEASUREMENT)
        if (pwrChar != null) {
            gatt.setCharacteristicNotification(pwrChar, true)
            val desc = pwrChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            desc?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            desc?.let { gatt.writeDescriptor(it) }
        }
    }

    private fun parseGattData(characteristic: BluetoothGattCharacteristic) {
        val data = characteristic.value ?: return
        when (characteristic.uuid) {
            HEART_RATE_MEASUREMENT -> {
                val flag = data[0].toInt()
                val hr = if ((flag and 0x01) != 0) {
                    ((data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8))
                } else {
                    data[1].toInt() and 0xFF
                }
                TrackingManager.updateSensorData(heartRate = hr, cadence = null, power = null)
            }
            CSC_MEASUREMENT -> {
                if (data.size >= 5) {
                    val crankRevs = (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
                    TrackingManager.updateSensorData(heartRate = null, cadence = 80 + (crankRevs % 25), power = null)
                }
            }
            POWER_MEASUREMENT -> {
                if (data.size >= 4) {
                    val powerWatts = (data[2].toInt() and 0xFF) or ((data[3].toInt() and 0xFF) shl 8)
                    TrackingManager.updateSensorData(heartRate = null, cadence = null, power = powerWatts)
                }
            }
        }
    }

    fun toggleSimulation() {
        val next = !_isSimulating.value
        _isSimulating.value = next
        if (next) {
            startTelemetryLoop()
        } else {
            telemetryJob?.cancel()
            _pairedSensors.update { list ->
                list.map { it.copy(liveReading = null) }
            }
            TrackingManager.updateSensorData(null, null, null)
        }
    }
}
