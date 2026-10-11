package com.rozakos.home.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rozakos.home.domain.model.Device
import com.rozakos.home.domain.model.DeviceCategory
import com.rozakos.home.domain.model.DeviceTypeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.deviceStore: DataStore<Preferences> by preferencesDataStore(name = "devices")

class DeviceDataStore(
    context: Context,
) {
    private val store = context.deviceStore

    val devices: Flow<List<Device>> = store.data.map { preferences ->
        decodeDevices(preferences[DevicesKey].orEmpty())
    }

    suspend fun saveDevices(devices: List<Device>) {
        store.edit { preferences ->
            preferences[DevicesKey] = encodeDevices(devices)
        }
    }

    private fun encodeDevices(devices: List<Device>): String {
        val array = JSONArray()
        devices.forEach { device ->
            array.put(
                JSONObject()
                    .put("id", device.id)
                    .put("customName", device.customName)
                    .put("category", device.category.id)
                    .put("type", device.type.id)
                    .put("ipAddress", device.ipAddress)
                    .put("room", device.room)
                    .put("lastSeenEpochMillis", device.lastSeenEpochMillis ?: JSONObject.NULL)
                    .put("uid", device.uid ?: JSONObject.NULL)
                    .put("firmware", device.firmware ?: JSONObject.NULL),
            )
        }
        return array.toString()
    }

    private fun decodeDevices(raw: String): List<Device> {
        if (raw.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        Device(
                            id = json.getString("id"),
                            customName = json.getString("customName"),
                            category = DeviceCategory.fromId(json.getString("category")),
                            type = DeviceTypeId.fromId(json.getString("type")),
                            ipAddress = json.getString("ipAddress"),
                            room = json.optString("room").takeIf { it.isNotBlank() && it != "null" },
                            lastSeenEpochMillis = json.optLong("lastSeenEpochMillis")
                                .takeIf { !json.isNull("lastSeenEpochMillis") },
                            uid = json.optString("uid").takeIf { it.isNotBlank() && it != "null" },
                            firmware = json.optString("firmware").takeIf { it.isNotBlank() && it != "null" },
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        val DevicesKey = stringPreferencesKey("saved_devices_json")
    }
}
