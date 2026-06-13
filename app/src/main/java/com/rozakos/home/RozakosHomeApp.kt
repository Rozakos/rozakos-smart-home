package com.rozakos.home

import android.app.Application
import com.rozakos.home.data.device.HttpDeviceStatusChecker
import com.rozakos.home.data.device.LocalDeviceRepository
import com.rozakos.home.data.device.PlaceholderDeviceDiscoveryService
import com.rozakos.home.data.local.DeviceDataStore
import com.rozakos.home.domain.repository.DeviceRepository
import com.rozakos.home.domain.repository.DeviceStatusChecker
import com.rozakos.home.domain.usecase.AddDeviceUseCase

class RozakosHomeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val deviceRepository: DeviceRepository =
        LocalDeviceRepository(DeviceDataStore(application.applicationContext))

    val statusChecker: DeviceStatusChecker = HttpDeviceStatusChecker()

    val discoveryService = PlaceholderDeviceDiscoveryService()

    val addDeviceUseCase = AddDeviceUseCase(deviceRepository)
}
