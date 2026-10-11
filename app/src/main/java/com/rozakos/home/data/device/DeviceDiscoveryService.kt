package com.rozakos.home.data.device

import com.rozakos.home.domain.model.DeviceTypeId
import kotlinx.coroutines.flow.Flow

/** A Rozakos device found on the LAN (mDNS `_rozakos._tcp` or subnet probe). */
data class DiscoveredDevice(
    val name: String,
    val ipAddress: String,
    val type: DeviceTypeId,
    val uid: String?,
    val firmware: String?,
    val source: Source,
) {
    enum class Source { Mdns, SubnetScan }
}

interface DeviceDiscoveryService {
    /** Emits the growing list of devices found so far; completes when the scan is done. */
    fun discover(timeoutMs: Long = 8_000): Flow<List<DiscoveredDevice>>

    /** Fast re-lookup of one known device (by its uid) whose IP may have changed. */
    suspend fun locate(uid: String, timeoutMs: Long = 4_000): String?
}

/** Merge results from several sources: one entry per device, mDNS details win. */
fun mergeDiscovered(current: List<DiscoveredDevice>, found: DiscoveredDevice): List<DiscoveredDevice> {
    val index = current.indexOfFirst {
        (found.uid != null && it.uid == found.uid) || it.ipAddress == found.ipAddress
    }
    if (index < 0) return (current + found).sortedBy { it.name.lowercase() + it.ipAddress }
    val existing = current[index]
    val merged = existing.copy(
        name = existing.name.ifBlank { found.name },
        ipAddress = found.ipAddress,
        uid = existing.uid ?: found.uid,
        firmware = existing.firmware ?: found.firmware,
    )
    return current.toMutableList().also { it[index] = merged }
}
