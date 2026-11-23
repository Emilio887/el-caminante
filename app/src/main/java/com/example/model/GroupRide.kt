package com.example.model

data class GroupMember(
    val id: String,
    val nickname: String,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float = 0f,
    val heartRate: Int? = null,
    val batteryLevel: Int = 85,
    val isSos: Boolean = false,
    val lastUpdate: Long = System.currentTimeMillis(),
    val isLeader: Boolean = false
)

data class GroupSession(
    val groupCode: String,
    val groupName: String,
    val adminNickname: String,
    val members: List<GroupMember> = emptyList(),
    val isActive: Boolean = true,
    val routeId: String? = null
)
