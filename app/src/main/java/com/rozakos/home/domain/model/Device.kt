package com.rozakos.home.domain.model

data class Device(
    val id: String,
    val customName: String,
    val category: DeviceCategory,
    val type: DeviceTypeId,
    val ipAddress: String,
    val room: String? = null,
    val lastSeenEpochMillis: Long? = null,
)
