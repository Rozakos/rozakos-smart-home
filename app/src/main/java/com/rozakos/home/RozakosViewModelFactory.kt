package com.rozakos.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rozakos.home.ui.adddevice.AddDeviceViewModel
import com.rozakos.home.ui.device.DeviceDetailViewModel
import com.rozakos.home.ui.home.HomeViewModel

class RozakosViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(container.deviceRepository, container.statusChecker) as T

            modelClass.isAssignableFrom(AddDeviceViewModel::class.java) ->
                AddDeviceViewModel(container.addDeviceUseCase) as T

            modelClass.isAssignableFrom(DeviceDetailViewModel::class.java) ->
                DeviceDetailViewModel(container.deviceRepository, container.statusChecker) as T

            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
