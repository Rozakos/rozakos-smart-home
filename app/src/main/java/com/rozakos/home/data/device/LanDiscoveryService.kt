package com.rozakos.home.data.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.rozakos.home.data.protocol.DeviceApi
import com.rozakos.home.domain.model.DeviceTypeId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address

/**
 * Finds Rozakos devices on the current WiFi:
 *  1. mDNS: every `_rozakos._tcp` service (ESP32 devices advertise one), resolved to an IP.
 *  2. Subnet probe: `GET /api/device-info` on every address of the phone's /24, for
 *     devices without mDNS (the STM32 ticker) or networks that drop multicast.
 * Each candidate is confirmed through /api/device-info before it is shown.
 */
class LanDiscoveryService(
    context: Context,
    private val api: DeviceApi,
) : DeviceDiscoveryService {
    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    override fun discover(timeoutMs: Long): Flow<List<DiscoveredDevice>> = channelFlow {
        var found = emptyList<DiscoveredDevice>()
        val lock = Mutex()
        suspend fun add(device: DiscoveredDevice) = lock.withLock {
            found = mergeDiscovered(found, device)
            send(found)
        }
        send(found)
        coroutineScope {
            launch {
                withTimeoutOrNull(timeoutMs) {
                    mdnsServices().collect { (host, txt) ->
                        launch { confirm(host, DiscoveredDevice.Source.Mdns, txt)?.let { add(it) } }
                    }
                }
            }
            launch {
                val hosts = subnetHosts()
                val gate = Semaphore(48)
                hosts.forEach { ip ->
                    launch {
                        gate.withPermit {
                            confirm(ip, DiscoveredDevice.Source.SubnetScan, emptyMap(), connectTimeoutMs = 600)
                                ?.let { add(it) }
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun locate(uid: String, timeoutMs: Long): String? =
        withTimeoutOrNull(timeoutMs) {
            mdnsServices().firstOrNull { (host, txt) ->
                txt["id"] == uid || runCatching { api.info(host).id }.getOrNull() == uid
            }?.first
        }

    private suspend fun confirm(
        ip: String,
        source: DiscoveredDevice.Source,
        txt: Map<String, String>,
        connectTimeoutMs: Int = 1_500,
    ): DiscoveredDevice? {
        val info = runCatching { api.info(ip, connectTimeoutMs) }.getOrNull() ?: return null
        val type = DeviceTypeId.fromIdOrNull(info.type) ?: return null
        return DiscoveredDevice(
            name = info.name.ifBlank { txt["name"].orEmpty() },
            ipAddress = ip,
            type = type,
            uid = info.id,
            firmware = info.firmware ?: txt["fw"],
            source = source,
        )
    }

    /** (ip, TXT record) for each resolved `_rozakos._tcp` service. */
    private fun mdnsServices(): Flow<Pair<String, Map<String, String>>> = callbackFlow {
        val resolveLock = Mutex()   // pre-API-34 NsdManager resolves one service at a time
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { close() }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                launch {
                    resolveLock.withLock {
                        resolve(serviceInfo)?.let { trySend(it) }
                    }
                }
            }
        }
        runCatching { nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { close(it) }
        awaitClose { runCatching { nsd.stopServiceDiscovery(listener) } }
    }

    @Suppress("DEPRECATION")
    private suspend fun resolve(service: NsdServiceInfo): Pair<String, Map<String, String>>? {
        val result = CompletableDeferred<NsdServiceInfo?>()
        nsd.resolveService(service, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                result.complete(null)
            }
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                result.complete(serviceInfo)
            }
        })
        val resolved = withTimeoutOrNull(3_000) { result.await() } ?: return null
        val host = (resolved.host as? Inet4Address)?.hostAddress ?: return null
        val txt = resolved.attributes.mapValues { (_, v) -> v?.decodeToString().orEmpty() }
        return host to txt
    }

    /** Every other host address of the phone's IPv4 network (capped at a /24). */
    private fun subnetHosts(): List<String> {
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return emptyList()
        val props: LinkProperties = cm.getLinkProperties(cm.activeNetwork) ?: return emptyList()
        val link = props.linkAddresses.firstOrNull { it.address is Inet4Address } ?: return emptyList()
        return subnetHostsFor(link.address.hostAddress ?: return emptyList(), link.prefixLength)
    }

    companion object {
        const val SERVICE_TYPE = "_rozakos._tcp."
    }
}

/** Hosts of [ip]'s network for prefix >= 24; wider networks are scanned as the /24 around [ip]. */
fun subnetHostsFor(ip: String, prefixLength: Int): List<String> {
    val parts = ip.split(".").mapNotNull { it.toIntOrNull() }
    if (parts.size != 4) return emptyList()
    val prefix = prefixLength.coerceIn(24, 30)
    val self = (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
    val mask = -1 shl (32 - prefix)
    val network = self and mask
    val broadcast = network or mask.inv()
    return ((network + 1) until broadcast)
        .filter { it != self }
        .map { "${(it ushr 24) and 255}.${(it ushr 16) and 255}.${(it ushr 8) and 255}.${it and 255}" }
}
