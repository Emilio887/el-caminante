package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GroupMember
import com.example.ui.theme.HeartRateRed
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.OffTrackRed
import com.example.ui.theme.TrailOrange
import com.example.ui.viewmodel.AppViewModel

@Composable
fun GroupRideScreen(
    viewModel: AppViewModel,
    onNavigateToMap: () -> Unit
) {
    val context = LocalContext.current
    val groupSession by viewModel.groupSession.collectAsState()

    var createGroupName by remember { mutableStateOf("Salida Finde Guadarrama") }
    var userNickname by remember { mutableStateOf("BikerPro") }
    var joinCode by remember { mutableStateOf("") }
    var isCreatingMode by remember { mutableStateOf(false) }
    var isJoiningMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Text("Rodar en Grupo y Live Tracking", fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(
            "Comparte tu ubicación en directo con tus compañeros de ruta, mira sus velocidades y pulso en tiempo real, y activa balizas de emergencia.",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        if (groupSession == null) {
            // No active session: Options to Create or Join
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Unirse o Crear Salida en Grupo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = userNickname,
                        onValueChange = { userNickname = it },
                        label = { Text("Tu apodo de ciclista") },
                        modifier = Modifier.fillMaxWidth().testTag("input_rider_nickname"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isCreatingMode && !isJoiningMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { isCreatingMode = true },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("btn_mode_create_group"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Crear Grupo", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { isJoiningMode = true },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("btn_mode_join_group"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Unirme con Código")
                            }
                        }
                    }

                    if (isCreatingMode) {
                        OutlinedTextField(
                            value = createGroupName,
                            onValueChange = { createGroupName = it },
                            label = { Text("Nombre del pelotón / grupo") },
                            modifier = Modifier.fillMaxWidth().testTag("input_group_name"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { isCreatingMode = false }, modifier = Modifier.weight(1f)) {
                                Text("Cancelar")
                            }
                            Button(
                                onClick = {
                                    viewModel.createGroupSession(createGroupName, userNickname)
                                    isCreatingMode = false
                                },
                                modifier = Modifier.weight(1f).testTag("btn_submit_create_group")
                            ) {
                                Text("Iniciar Pelotón", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isJoiningMode) {
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { joinCode = it },
                            label = { Text("Código de invitación (ej: MTB-4821)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_join_code"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { isJoiningMode = false }, modifier = Modifier.weight(1f)) {
                                Text("Cancelar")
                            }
                            Button(
                                onClick = {
                                    if (joinCode.isNotBlank()) {
                                        viewModel.joinGroupSession(joinCode, userNickname)
                                        isJoiningMode = false
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("btn_submit_join_group")
                            ) {
                                Text("Conectar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Active Group Session View
            val session = groupSession!!

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(session.groupName, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text("Código: ${session.groupCode}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        }

                        Row {
                            IconButton(onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "¡Únete a mi pelotón en Sendero MTB con el código: ${session.groupCode}!")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Compartir código de grupo"))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir código")
                            }

                            IconButton(onClick = { viewModel.leaveGroup() }) {
                                Icon(Icons.Default.ExitToApp, contentDescription = "Salir del grupo", tint = OffTrackRed)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons: "Ver en Mapa" & "Baliza SOS"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToMap,
                            modifier = Modifier.weight(1f).height(44.dp).testTag("btn_view_group_map"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver en Mapa", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.sendSosSignal() },
                            modifier = Modifier.weight(1f).height(44.dp).testTag("btn_send_sos"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OffTrackRed)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Baliza SOS", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Ciclistas en el Pelotón (${session.members.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(session.members, key = { it.id }) { member ->
                    MemberCard(member = member)
                }
            }
        }
    }
}

@Composable
fun MemberCard(member: GroupMember) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("member_card_${member.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (member.isSos) OffTrackRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (member.isSos) OffTrackRed else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = member.nickname.take(2).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = if (member.isSos) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(member.nickname, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (member.isLeader) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = TrailOrange.copy(alpha = 0.2f)) {
                                Text("Líder", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TrailOrange, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                        if (member.isSos) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = OffTrackRed) {
                                Text("¡SOS!", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                    Text("%.1f km/h • Batería %d%%".format(member.speedKmh, member.batteryLevel), fontSize = 12.sp, color = Color.Gray)
                }
            }

            member.heartRate?.let { hr ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = HeartRateRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$hr ppm", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
