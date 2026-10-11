package com.rozakos.home.domain.model

data class Device(
    val id: String,
    val customName: String,
    val category: DeviceCategory,
    val type: DeviceTypeId,
    val ipAddress: String,
    val room: String? = null,
    val lastSeenEpochMillis: Long? = null,
    /** Stable id the device reports in /api/device-info (e.g. `cyd-ticker-ab12`); lets the
     *  app find the device again over mDNS when DHCP hands it a new IP. Null for devices
     *  added manually before they were ever reachable. */
    val uid: String? = null,
    val firmware: String? = null,
)
