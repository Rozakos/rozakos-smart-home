package com.rozakos.home

import android.app.Application
import com.rozakos.home.data.ble.AndroidBleProvisioningService
import com.rozakos.home.data.ble.BleProvisioningService
import com.rozakos.home.data.device.DeviceDiscoveryService
import com.rozakos.home.data.device.HttpDeviceStatusChecker
import com.rozakos.home.data.device.LanDiscoveryService
import com.rozakos.home.data.device.LocalDeviceRepository
import com.rozakos.home.data.protocol.DeviceApi
import com.rozakos.home.data.local.DeviceDataStore
import com.rozakos.home.domain.repository.DeviceRepository
import com.rozakos.home.domain.repository.DeviceStatusChecker
import com.rozakos.home.domain.usecase.AddDeviceUseCase
import com.rozakos.home.data.market.SymbolCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class RozakosHomeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val symbolCatalog = SymbolCatalog(File(application.filesDir, "symbol-catalog"), bundledJson = {
        application.assets.open("symbol_catalog.json").bufferedReader().use { it.readText() }
    })
    private val catalogScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        catalogScope.launch { runCatching { symbolCatalog.refreshIfNeeded() } }
    }

    val deviceRepository: DeviceRepository =
        LocalDeviceRepository(DeviceDataStore(application.applicationContext))

    val deviceApi = DeviceApi()

    val statusChecker: DeviceStatusChecker = HttpDeviceStatusChecker(deviceApi)

    val discoveryService: DeviceDiscoveryService =
        LanDiscoveryService(application.applicationContext, deviceApi)

    val bleProvisioningService: BleProvisioningService =
        AndroidBleProvisioningService(application.applicationContext)

    val addDeviceUseCase = AddDeviceUseCase(deviceRepository)
}
