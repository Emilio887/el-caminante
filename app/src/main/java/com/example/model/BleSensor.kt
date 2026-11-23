package com.example.model

enum class BleConnectionState(val labelEs: String) {
    DISCONNECTED("Desconectado"),
    CONNECTING("Conectando..."),
    CONNECTED("Conectado")
}

enum class BleSensorType(val labelEs: String, val unit: String, val shortDesc: String) {
    HEART_RATE("Frecuencia Cardíaca (HR)", "ppm", "Banda pectoral de pulso"),
    SPEED_CADENCE("Sensor de Cadencia", "rpm", "Sensor en biela de pedaleo"),
    POWER_METER("Potenciómetro", "W", "Medidor de potencia en pedal/biela")
}

data class BleSensor(
    val address: String,
    val name: String,
    val type: BleSensorType,
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val isPaired: Boolean = false,
    val autoConnect: Boolean = true,
    val batteryPercent: Int? = 90,
    val lastValue: String = "--",
    val liveReading: Int? = null,
    val rssi: Int = -60
) {
    val isConnected: Boolean
        get() = connectionState == BleConnectionState.CONNECTED

    val signalQuality: String
        get() = when {
            rssi >= -60 -> "Excelente"
            rssi >= -75 -> "Buena"
            else -> "Débil"
        }
}
