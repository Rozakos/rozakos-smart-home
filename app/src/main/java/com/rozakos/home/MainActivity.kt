package com.rozakos.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rozakos.home.ui.adddevice.AddDeviceScreen
import com.rozakos.home.ui.adddevice.AddDeviceViewModel
import com.rozakos.home.ui.device.DeviceDetailScreen
import com.rozakos.home.ui.device.DeviceDetailViewModel
import com.rozakos.home.ui.home.HomeScreen
import com.rozakos.home.ui.home.HomeViewModel
import com.rozakos.home.ui.onboarding.BleOnboardingScreen
import com.rozakos.home.ui.onboarding.BleOnboardingViewModel
import com.rozakos.home.ui.theme.RozakosHomeTheme
import com.rozakos.home.ui.webview.WebUiScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as RozakosHomeApp).container

        setContent {
            RozakosHomeTheme {
                RozakosHomeNav(container)
            }
        }
    }
}

@Composable
private fun RozakosHomeNav(container: AppContainer) {
    val navController = rememberNavController()
    val factory = remember(container) { RozakosViewModelFactory(container) }

    NavHost(navController = navController, startDestination = Routes.Home) {
        composable(Routes.Home) {
            val viewModel: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = viewModel,
                onAddDevice = { navController.navigate(Routes.AddDevice) },
                onOpenDevice = { deviceId -> navController.navigate(Routes.device(deviceId)) },
            )
        }

        composable(Routes.AddDevice) {
            val viewModel: AddDeviceViewModel = viewModel(factory = factory)
            AddDeviceScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onDeviceAdded = {
                    navController.popBackStack(Routes.Home, inclusive = false)
                },
                onStartBleOnboarding = { navController.navigate(Routes.BleOnboarding) },
                onOpenSetupPortal = {
                    navController.navigate(Routes.web("Setup Portal", "http://192.168.4.1"))
                },
            )
        }

        composable(Routes.BleOnboarding) {
            val viewModel: BleOnboardingViewModel = viewModel(factory = factory)
            BleOnboardingScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onDeviceAdded = {
                    navController.popBackStack(Routes.Home, inclusive = false)
                },
            )
        }

        composable(
            route = Routes.Device,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType }),
        ) {
            val viewModel: DeviceDetailViewModel = viewModel(factory = factory)
            DeviceDetailScreen(
                viewModel = viewModel,
                deviceId = it.arguments?.getString("deviceId").orEmpty(),
                onBack = { navController.popBackStack() },
                onOpenWebUi = { title, url -> navController.navigate(Routes.web(title, url)) },
            )
        }

        composable(
            route = Routes.Web,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType },
            ),
        ) {
            WebUiScreen(
                title = it.arguments?.getString("title").orEmpty().decodeUrl(),
                url = it.arguments?.getString("url").orEmpty().decodeUrl(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private object Routes {
    const val Home = "home"
    const val AddDevice = "add-device"
    const val BleOnboarding = "ble-onboarding"
    const val Device = "device/{deviceId}"
    const val Web = "web?title={title}&url={url}"

    fun device(deviceId: String) = "device/$deviceId"

    fun web(title: String, url: String) = "web?title=${title.encodeUrl()}&url=${url.encodeUrl()}"
}

private fun String.encodeUrl(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

private fun String.decodeUrl(): String =
    URLDecoder.decode(this, StandardCharsets.UTF_8.toString())
