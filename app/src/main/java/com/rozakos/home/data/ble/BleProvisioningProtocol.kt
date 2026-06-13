package com.rozakos.home.data.ble

import org.json.JSONObject
import java.util.UUID

/**
 * BLE WiFi-provisioning contract shared with the CYD Stock Ticker firmware.
 *
 * The firmware must expose a GATT server advertising [SERVICE_UUID] with exactly the
 * characteristics below. Keep this file and the firmware's UUID table in lock-step.
 */
object BleProvisioningProtocol {
    /** Primary provisioning service. The device advertises this UUID so we can filter scans. */
    val SERVICE_UUID: UUID = UUID.fromString("5f1d0001-7b3a-4c2e-9d8f-1a2b3c4d5e6f")

    /** Client -> device. UTF-8 JSON `{"ssid":"..","pass":".."}`. Write / Write-No-Response. */
    val CREDENTIALS_UUID: UUID = UUID.fromString("5f1d0002-7b3a-4c2e-9d8f-1a2b3c4d5e6f")

    /** Device -> client. UTF-8 JSON `{"state":"..","ip":"..","reason":".."}`. Read / Notify. */
    val STATUS_UUID: UUID = UUID.fromString("5f1d0003-7b3a-4c2e-9d8f-1a2b3c4d5e6f")

    /** Device -> client. UTF-8 JSON `{"name":"..","type":"..","fw":"..","mac":".."}`. Read. */
    val DEVICE_INFO_UUID: UUID = UUID.fromString("5f1d0004-7b3a-4c2e-9d8f-1a2b3c4d5e6f")

    /** Standard Client Characteristic Configuration descriptor used to enable notifications. */
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    fun credentialsPayload(ssid: String, password: String): ByteArray =
        JSONObject()
            .put("ssid", ssid)
            .put("pass", password)
            .toString()
            .toByteArray(Charsets.UTF_8)

    fun parseStatus(bytes: ByteArray): DeviceProvisioningStatus {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        return DeviceProvisioningStatus(
            state = json.optString("state", "idle"),
            ip = json.optString("ip", "").takeIf { it.isNotBlank() },
            reason = json.optString("reason", "").takeIf { it.isNotBlank() },
        )
    }

    fun parseDeviceInfo(bytes: ByteArray): ProvisionedDeviceInfo {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        return ProvisionedDeviceInfo(
            name = json.optString("name", "").takeIf { it.isNotBlank() },
            type = json.optString("type", "").takeIf { it.isNotBlank() },
            firmware = json.optString("fw", "").takeIf { it.isNotBlank() },
            mac = json.optString("mac", "").takeIf { it.isNotBlank() },
        )
    }
}

data class DeviceProvisioningStatus(
    val state: String,
    val ip: String?,
    val reason: String?,
)

data class ProvisionedDeviceInfo(
    val name: String?,
    val type: String?,
    val firmware: String?,
    val mac: String?,
)
