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
) {
    /** Whether a device seen at [ip] reporting [otherUid] is this one. Stable ids decide when
     *  both sides have one: DHCP can hand a stale card's old IP to a different device. The IP
     *  only decides when either id is unknown. */
    fun matches(ip: String, otherUid: String?): Boolean =
        if (uid != null && otherUid != null) uid == otherUid else ipAddress == ip.trim()
}
