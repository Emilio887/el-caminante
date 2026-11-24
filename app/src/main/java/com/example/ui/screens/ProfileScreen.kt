package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AchievementCategory
import com.example.model.AchievementManager
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.PineGreenPrimary
import com.example.ui.theme.TrailOrange
import com.example.ui.viewmodel.AppViewModel

@Composable
fun ProfileScreen(
    viewModel: AppViewModel
) {
    val activities by viewModel.activities.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(profile.name) }
    var editEmail by remember { mutableStateOf(profile.email) }
    var editAge by remember { mutableStateOf(profile.age.toString()) }
    var editWeight by remember { mutableStateOf(profile.weightKg.toString()) }
    var editHeight by remember { mutableStateOf(profile.heightCm.toString()) }
    var editEmergency by remember { mutableStateOf(profile.emergencyContact) }

    // Cumulative stats
    val totalDistance = activities.sumOf { it.distanceKm }
    val totalElevation = activities.sumOf { it.elevationGain }
    val totalHours = activities.sumOf { it.totalDurationMillis } / 3600000.0
    val totalActivities = activities.size

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(activities, profile) {
        com.example.service.NotificationHelper.checkAndNotifyProgress(context, activities, profile)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("profile_screen_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = profile.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = profile.email,
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cumulative Stats Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%.1f km".format(totalDistance),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("DISTANCIA TOTAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "+%.0f m".format(totalElevation),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TrailOrange
                        )
                        Text("DESNIVEL ACUM.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%.1fh".format(totalHours),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text("TIEMPO RODAJE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }
            }
        }

        // Section: Weekly Goals (Metas Semanales)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = PineGreenPrimary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Metas Semanales",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    var isEditingGoals by remember { mutableStateOf(false) }
                    var goalDist by remember { mutableStateOf(profile.weeklyDistanceGoalKm.toString()) }
                    var goalEle by remember { mutableStateOf(profile.weeklyElevationGoalM.toString()) }

                    Button(
                        onClick = {
                            if (isEditingGoals) {
                                val newDist = goalDist.toFloatOrNull() ?: profile.weeklyDistanceGoalKm
                                val newEle = goalEle.toFloatOrNull() ?: profile.weeklyElevationGoalM
                                viewModel.updateUserProfile(profile.copy(weeklyDistanceGoalKm = newDist, weeklyElevationGoalM = newEle))
                            }
                            isEditingGoals = !isEditingGoals
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isEditingGoals) "Guardar Metas" else "Configurar", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Calculate current week activities (last 7 days)
                val now = System.currentTimeMillis()
                val weekActivities = activities.filter { (now - it.startTime) <= (7 * 86400000L) }
                val weekDist = weekActivities.sumOf { it.distanceKm }.toFloat()
                val weekEle = weekActivities.sumOf { it.elevationGain }.toFloat()

                val distProgress = (weekDist / maxOf(1f, profile.weeklyDistanceGoalKm)).coerceIn(0f, 1f)
                val eleProgress = (weekEle / maxOf(1f, profile.weeklyElevationGoalM)).coerceIn(0f, 1f)

                if (false) { // Handled inline above or via dialog if editing
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Distance Goal Progress
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Distancia Semanal", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("%.1f / %.0f km".format(weekDist, profile.weeklyDistanceGoalKm), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PineGreenPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { distProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = PineGreenPrimary,
                            trackColor = Color.Gray.copy(alpha = 0.2f)
                        )
                    }

                    // Elevation Goal Progress
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Desnivel Semanal", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("+%.0f / +%.0f m".format(weekEle, profile.weeklyElevationGoalM), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TrailOrange)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { eleProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = TrailOrange,
                            trackColor = Color.Gray.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // Section: Achievements & Virtual Badges
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TrailOrange, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Medallas y Logros",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    val unlockedCount = AchievementManager.allAchievements.count { ach ->
                        val currentVal = when (ach.category) {
                            AchievementCategory.DISTANCE -> totalDistance
                            AchievementCategory.ELEVATION -> totalElevation
                            AchievementCategory.ACTIVITIES -> totalActivities.toDouble()
                        }
                        currentVal >= ach.requiredValue
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "$unlockedCount / ${AchievementManager.allAchievements.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AchievementManager.allAchievements.forEach { ach ->
                        val currentVal = when (ach.category) {
                            AchievementCategory.DISTANCE -> totalDistance
                            AchievementCategory.ELEVATION -> totalElevation
                            AchievementCategory.ACTIVITIES -> totalActivities.toDouble()
                        }
                        val isUnlocked = currentVal >= ach.requiredValue
                        val progress = (currentVal / ach.requiredValue).toFloat().coerceIn(0f, 1f)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isUnlocked) HighContrastGreen.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.2f),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = ach.iconName, fontSize = 20.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = ach.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            if (isUnlocked) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Desbloqueado", tint = PineGreenPrimary, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        Text(
                                            text = ach.description,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = if (isUnlocked) PineGreenPrimary else TrailOrange,
                                            trackColor = Color.Gray.copy(alpha = 0.2f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isUnlocked) "¡Completado!" else "%.0f / %.0f %s".format(currentVal, ach.requiredValue, ach.unit),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) PineGreenPrimary else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Personal Preferences & Biometrics
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preferencias y Biometría",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Button(
                        onClick = {
                            if (isEditing) {
                                val updated = profile.copy(
                                    name = editName,
                                    email = editEmail,
                                    age = editAge.toIntOrNull() ?: profile.age,
                                    weightKg = editWeight.toFloatOrNull() ?: profile.weightKg,
                                    heightCm = editHeight.toIntOrNull() ?: profile.heightCm,
                                    emergencyContact = editEmergency
                                )
                                viewModel.updateUserProfile(updated)
                            }
                            isEditing = !isEditing
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_edit_profile")
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEditing) "Guardar" else "Editar", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isEditing) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Nombre Completo") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("Correo Electrónico") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editAge,
                                onValueChange = { editAge = it },
                                label = { Text("Edad") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editWeight,
                                onValueChange = { editWeight = it },
                                label = { Text("Peso (kg)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editHeight,
                                onValueChange = { editHeight = it },
                                label = { Text("Altura (cm)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = editEmergency,
                            onValueChange = { editEmergency = it },
                            label = { Text("Contacto de Emergencia") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileInfoRow(icon = Icons.Default.Badge, label = "Nombre", value = profile.name)
                        ProfileInfoRow(icon = Icons.Default.Email, label = "Correo", value = profile.email)
                        ProfileInfoRow(icon = Icons.Default.FitnessCenter, label = "Edad", value = "${profile.age} años")
                        ProfileInfoRow(icon = Icons.Default.MonitorWeight, label = "Peso", value = "${profile.weightKg} kg")
                        ProfileInfoRow(icon = Icons.Default.Height, label = "Altura", value = "${profile.heightCm} cm")
                        ProfileInfoRow(icon = Icons.Default.Call, label = "Emergencia SOS", value = profile.emergencyContact)
                        ProfileInfoRow(icon = Icons.Default.DirectionsBike, label = "Actividad Principal", value = profile.preferredActivity)
                    }
                }
            }
        }

        // Section: App Preferences (Auto-pause & Voice prompts)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Ajustes de Ruta",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pausa automática en semáforos", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Pausa el cronómetro cuando te detienes", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = profile.autoPauseEnabled,
                        onCheckedChange = {
                            viewModel.updateUserProfile(profile.copy(autoPauseEnabled = it))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Avisos de voz de distancia", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Anuncia cada kilómetro recorrido", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = profile.voicePromptsEnabled,
                        onCheckedChange = {
                            viewModel.updateUserProfile(profile.copy(voicePromptsEnabled = it))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ahorro automático de batería (<20%)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Reduce frecuencia GPS y desactiva escaneo BLE si batería baja del 20%", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = profile.batterySaverBelow20Enabled,
                        onCheckedChange = {
                            viewModel.updateUserProfile(profile.copy(batterySaverBelow20Enabled = it))
                        },
                        modifier = Modifier.testTag("switch_battery_saver")
                    )
                }
            }
        }

        // Section: Barometer Calibration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Calibración del Sensor Barométrico",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Introduce una altitud de referencia conocida (p. ej. cartel de puerto de montaña o cima) para corregir el error barométrico.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                var referenceAltitudeInput by remember { mutableStateOf("") }
                var calibrationStatus by remember { mutableStateOf<String?>(null) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = referenceAltitudeInput,
                        onValueChange = { referenceAltitudeInput = it },
                        label = { Text("Altitud real (metros)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val targetAlt = referenceAltitudeInput.toFloatOrNull()
                            if (targetAlt != null) {
                                viewModel.updateUserProfile(profile.copy(barometerOffsetMeters = targetAlt))
                                calibrationStatus = "¡Calibrado a ${targetAlt.toInt()} m! Desnivel ajustado."
                            } else {
                                calibrationStatus = "Introduce un valor numérico válido."
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Calibrar")
                    }
                }

                if (profile.barometerOffsetMeters != 0f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Offset actual aplicado: %.0f m".format(profile.barometerOffsetMeters),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PineGreenPrimary
                    )
                }

                calibrationStatus?.let { msg ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = msg, fontSize = 11.sp, color = TrailOrange)
                }
            }
        }

        // Section: External Platform Sync (Strava & Garmin Connect)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sincronización con Strava y Garmin Connect",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Sube tus rutas y entrenamientos automáticamente o bajo demanda a tus perfiles externos.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strava API", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(if (profile.stravaConnected) "Conectado y listo para sincronizar" else "Desconectado", fontSize = 11.sp, color = if (profile.stravaConnected) PineGreenPrimary else Color.Gray)
                    }
                    Button(
                        onClick = {
                            viewModel.updateUserProfile(profile.copy(stravaConnected = !profile.stravaConnected))
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (profile.stravaConnected) Color.Gray else Color(0xFFFC4C02)
                        )
                    ) {
                        Text(if (profile.stravaConnected) "Desconectar" else "Conectar Strava", fontSize = 12.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Garmin Connect API", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(if (profile.garminConnected) "Conectado y listo para sincronizar" else "Desconectado", fontSize = 11.sp, color = if (profile.garminConnected) PineGreenPrimary else Color.Gray)
                    }
                    Button(
                        onClick = {
                            viewModel.updateUserProfile(profile.copy(garminConnected = !profile.garminConnected))
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (profile.garminConnected) Color.Gray else Color(0xFF007ACC)
                        )
                    ) {
                        Text(if (profile.garminConnected) "Desconectar" else "Conectar Garmin", fontSize = 12.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sincronización automática post-ruta", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Sube la ruta a Strava/Garmin al finalizar el recorrido", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = profile.autoSyncExternalEnabled,
                        onCheckedChange = {
                            viewModel.updateUserProfile(profile.copy(autoSyncExternalEnabled = it))
                        }
                    )
                }
            }
        }

        // Section: Visual Theme & Appearance (Dark Mode & Dynamic Colors)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Apariencia y Tema (Modo Oscuro / Material You)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modo Oscuro (Noche)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Ideal para rutas nocturnas o bajo brillo", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = profile.isDarkModeEnabled,
                        onCheckedChange = {
                            viewModel.updateUserProfile(profile.copy(isDarkModeEnabled = it))
                        },
                        modifier = Modifier.testTag("switch_dark_mode")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Colores Dinámicos (Material You)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Utiliza la paleta del sistema Android 12+", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = profile.useDynamicColor,
                        onCheckedChange = {
                            viewModel.updateUserProfile(profile.copy(useDynamicColor = it))
                        },
                        modifier = Modifier.testTag("switch_dynamic_colors")
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = PineGreenPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontSize = 13.sp, color = Color.Gray)
        }
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
