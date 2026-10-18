package com.rozakos.home.data

import com.rozakos.home.data.ble.*
import com.rozakos.home.data.device.*
import com.rozakos.home.domain.model.*
import com.rozakos.home.domain.repository.DeviceRepository
import com.rozakos.home.domain.usecase.AddDeviceUseCase
import com.rozakos.home.ui.adddevice.AddDeviceViewModel
import com.rozakos.home.ui.onboarding.BleOnboardingViewModel
import com.rozakos.home.ui.onboarding.OnboardingStep
import com.rozakos.home.ui.util.formatLastSeen
import com.rozakos.home.ui.util.isLikelyIpv4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UiFlowRecoveryTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setMain() = Dispatchers.setMain(dispatcher)
    @After fun resetMain() = Dispatchers.resetMain()

    @Test fun invalidManualFieldsHaveSpecificErrorsAndCannotSave() = runTest {
        val repo = Repository()
        val vm = AddDeviceViewModel(AddDeviceUseCase(repo), Discovery(), repo)
        vm.updateName(""); vm.updateIpAddress("192.168.1.+2"); vm.save(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.nameError); assertNotNull(vm.uiState.value.ipError)
        assertEquals(0, repo.writes)
        assertFalse(isLikelyIpv4("192.168.1.+2"))
        assertTrue(isLikelyIpv4("192.168.1.58"))
    }

    @Test fun rapidAddTapsPersistOneDeviceAndExposeSavingState() = runTest {
        val repo = Repository()
        val vm = AddDeviceViewModel(AddDeviceUseCase(repo), Discovery(), repo)
        vm.updateIpAddress("192.168.1.60"); vm.save(); vm.save()
        assertTrue(vm.uiState.value.saving)
        advanceUntilIdle()
        assertEquals(1, repo.writes); assertEquals(1, repo.devices.value.size)
        assertTrue(vm.uiState.value.saved); assertFalse(vm.uiState.value.saving)
    }

    @Test fun knownDiscoveryResultsAreCountedAndManualDuplicatesAreRejected() = runTest {
        val repo = Repository(listOf(DEVICE))
        val found = DiscoveredDevice("OLED", DEVICE.ipAddress, DEVICE.type, "uid", "1.1.0", DiscoveredDevice.Source.Mdns)
        val vm = AddDeviceViewModel(AddDeviceUseCase(repo), Discovery(flowOf(listOf(found))), repo)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.alreadyAddedCount); assertTrue(vm.uiState.value.nearby.isEmpty())
        vm.updateIpAddress(DEVICE.ipAddress); vm.save(); advanceUntilIdle()
        assertEquals(0, repo.writes); assertTrue(vm.uiState.value.error!!.contains("already in your home"))
    }

    @Test fun aDifferentDeviceOnAStaleCardsOldIpCanStillBeFoundAndAdded() = runTest {
        val repo = Repository(listOf(DEVICE))
        // DHCP handed the old card's IP to another board, which reports its own stable id.
        val other = DiscoveredDevice("CYD", DEVICE.ipAddress, DEVICE.type, "other-uid", "1.1.0", DiscoveredDevice.Source.Mdns)
        val vm = AddDeviceViewModel(AddDeviceUseCase(repo), Discovery(flowOf(listOf(other))), repo)
        advanceUntilIdle()
        assertEquals(listOf(other), vm.uiState.value.nearby); assertEquals(0, vm.uiState.value.alreadyAddedCount)
        vm.addDiscovered(other, "Kitchen CYD", ""); advanceUntilIdle()
        assertEquals(1, repo.writes); assertTrue(vm.uiState.value.saved)
    }

    @Test fun storageFailureShowsAnErrorAndAllowsRetry() = runTest {
        val repo = Repository().apply { fail = true }
        val vm = AddDeviceViewModel(AddDeviceUseCase(repo), Discovery(), repo)
        vm.updateIpAddress("192.168.1.60"); vm.save(); advanceUntilIdle()
        assertFalse(vm.uiState.value.saving); assertFalse(vm.uiState.value.saved)
        assertNotNull(vm.uiState.value.error)
        repo.fail = false; vm.save(); advanceUntilIdle()
        assertTrue(vm.uiState.value.saved)
    }

    @Test fun networkScanFailureStopsTheSpinnerAndExplainsTheManualPath() = runTest {
        val repo = Repository()
        val vm = AddDeviceViewModel(AddDeviceUseCase(repo), Discovery(flow { error("Network unavailable") }), repo)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.scanning); assertTrue(vm.uiState.value.scannedOnce)
        assertTrue(vm.uiState.value.scanError!!.contains("Manual"))
    }

    @Test fun bluetoothScanEndsAndPermissionRecoveryClearsItsError() = runTest {
        val vm = BleOnboardingViewModel(Ble(), AddDeviceUseCase(Repository()))
        vm.startScan(); advanceTimeBy(12_001); runCurrent()
        assertFalse(vm.uiState.value.scanning); assertTrue(vm.uiState.value.scannedOnce)
        vm.permissionDenied(); assertTrue(vm.uiState.value.permissionDenied)
        vm.permissionsGranted(); assertFalse(vm.uiState.value.permissionDenied); assertNull(vm.uiState.value.error)
    }

    @Test fun failedWifiSetupKeepsCredentialsAndCanBeRetriedWithoutDuplicateSaves() = runTest {
        val service = Ble()
        val repo = Repository()
        val vm = BleOnboardingViewModel(service, AddDeviceUseCase(repo))
        vm.select(ProvisioningCandidate("CYD", "AA:BB", -50))
        vm.updateSsid("Test network"); vm.updatePassword("test-only-password")
        vm.provision(); advanceUntilIdle()
        assertEquals(OnboardingStep.Credentials, vm.uiState.value.step)
        assertEquals("Test network", vm.uiState.value.ssid)
        service.success = true; vm.provision(); advanceUntilIdle()
        assertEquals(OnboardingStep.Done, vm.uiState.value.step)
        vm.saveDevice(); vm.saveDevice(); advanceUntilIdle()
        assertEquals(1, repo.writes); assertTrue(vm.uiState.value.saved)
    }

    @Test fun lastSeenIsReadableAndHandlesClockSkew() {
        val now = 10_000_000L
        assertEquals("Never", formatLastSeen(null, now))
        assertEquals("Just now", formatLastSeen(now + 1000, now))
        assertEquals("2 min ago", formatLastSeen(now - 120_000, now))
        assertEquals("2 h ago", formatLastSeen(now - 7_200_000, now))
    }

    @Test fun bluetoothDisconnectErrorIsNotReplacedByAMisleadingTimeout() = runTest {
        val service = Ble().apply { disconnect = true }
        val vm = BleOnboardingViewModel(service, AddDeviceUseCase(Repository()))
        vm.select(ProvisioningCandidate("CYD", "AA:BB", -50)); vm.updateSsid("Test network")
        vm.provision(); advanceUntilIdle()
        assertEquals(OnboardingStep.Credentials, vm.uiState.value.step)
        assertEquals("Bluetooth disconnected", vm.uiState.value.error)
    }

    private class Repository(initial: List<Device> = emptyList()) : DeviceRepository {
        override val devices = MutableStateFlow(initial)
        var writes = 0
        var fail = false
        override suspend fun addDevice(device: Device) {
            delay(100); if (fail) error("Could not store device")
            writes++; devices.value += device
        }
        override suspend fun removeDevice(deviceId: String) { devices.value = devices.value.filterNot { it.id == deviceId } }
        override suspend fun renameDevice(deviceId: String, name: String) = Unit
        override suspend fun updateLastSeen(deviceId: String, epochMillis: Long) = Unit
        override suspend fun updateIdentity(deviceId: String, ipAddress: String, uid: String?, firmware: String?) = Unit
        override suspend fun updateRoom(deviceId: String, room: String?) = Unit
    }

    private class Discovery(private val results: Flow<List<DiscoveredDevice>> = flowOf(emptyList())) : DeviceDiscoveryService {
        override fun discover(timeoutMs: Long) = results
        override suspend fun locate(uid: String, timeoutMs: Long): String? = null
    }

    private class Ble : BleProvisioningService {
        var success = false
        var disconnect = false
        override fun isBluetoothReady() = true
        override fun scan(): Flow<List<ProvisioningCandidate>> = flow { emit(emptyList()); awaitCancellation() }
        override fun provision(address: String, ssid: String, password: String): Flow<ProvisioningProgress> {
            if (disconnect) return flow { error("Bluetooth disconnected") }
            return flowOf(if (success) ProvisioningProgress.Success("192.168.1.60", null)
                else ProvisioningProgress.Failure("Wrong Wi-Fi password"))
        }
    }

    companion object {
        private val DEVICE = Device("oled", "OLED", DeviceCategory.StockTickers,
            DeviceTypeId.OledStockTicker, "192.168.1.58", uid = "uid")
    }
}
