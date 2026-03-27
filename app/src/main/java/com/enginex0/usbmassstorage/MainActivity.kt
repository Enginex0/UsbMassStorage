package com.enginex0.usbmassstorage

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.enginex0.usbmassstorage.data.AccentColor
import com.enginex0.usbmassstorage.data.AccentPreference
import com.enginex0.usbmassstorage.ui.AddDeviceSheet
import com.enginex0.usbmassstorage.ui.CreateImageSheet
import com.enginex0.usbmassstorage.ui.DeviceEditSheet
import com.enginex0.usbmassstorage.ui.DeviceListScreen
import com.enginex0.usbmassstorage.ui.GuideScreen
import com.enginex0.usbmassstorage.ui.ImageManagerScreen
import com.enginex0.usbmassstorage.ui.SettingsScreen
import com.enginex0.usbmassstorage.ui.theme.UsbMassStorageTheme
import com.enginex0.usbmassstorage.viewmodel.MainViewModel

private const val TAG = "UsbMsMain"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var accent by remember { mutableStateOf(AccentPreference.load(context)) }
            UsbMassStorageTheme(accent = accent) {
                UsbMassStorageApp(onAccentChanged = { accent = it })
            }
        }
    }
}

@Composable
private fun UsbMassStorageApp(
    vm: MainViewModel = viewModel(),
    onAccentChanged: (AccentColor) -> Unit = {}
) {
    val navController = rememberNavController()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddSheet by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var editDeviceIndex by remember { mutableIntStateOf(-1) }
    var bgIndex by remember { mutableIntStateOf(0) }

    val navEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navEntry) {
        if (navEntry?.destination?.route == "devices") bgIndex++
    }

    if (state.mounting) showAddSheet = false

    NavHost(navController = navController, startDestination = "devices") {
        composable("devices") {
            DeviceListScreen(
                bgIndex = bgIndex,
                state = state,
                onRefresh = { vm.refresh() },
                onAddDevice = { showAddSheet = true },
                onSettings = { navController.navigate("settings") },
                onGuide = { navController.navigate("guide") },
                onImages = { navController.navigate("images") },
                onEjectDevice = { index -> vm.ejectDevice(context, index) },
                onDeviceClick = { index -> editDeviceIndex = index },
                onAcknowledgeAlert = { vm.acknowledgeAlert() }
            )
        }
        composable("settings") {
            SettingsScreen(
                state = state,
                onRestartDaemon = { vm.restartDaemon() },
                onStopDaemon = { vm.stopDaemon() },
                onBack = { navController.popBackStack() },
                onAccentChanged = onAccentChanged
            )
        }
        composable("guide") {
            GuideScreen(onBack = { navController.popBackStack() })
        }
        composable("images") {
            ImageManagerScreen(
                mountedPaths = state.activeDevices.map { it.file }.toSet(),
                onBack = { navController.popBackStack() },
                onMount = { deviceInfo ->
                    if (deviceInfo.uri.scheme != "file") {
                        vm.takePersistablePermission(context, deviceInfo.uri)
                    }
                    vm.addDevice(context, deviceInfo)
                }
            )
        }
    }

    if (showAddSheet) {
        AddDeviceSheet(
            mounting = state.mounting,
            onMount = { deviceInfo ->
                if (deviceInfo.uri.scheme != "file") {
                    vm.takePersistablePermission(context, deviceInfo.uri)
                }
                vm.addDevice(context, deviceInfo)
            },
            onDismiss = { showAddSheet = false },
            onCreateImage = {
                showAddSheet = false
                showCreateSheet = true
            }
        )
    }

    if (showCreateSheet) {
        CreateImageSheet(
            onDismiss = { showCreateSheet = false },
            onCreated = { deviceInfo ->
                showCreateSheet = false
                vm.addDevice(context, deviceInfo)
            }
        )
    }

    if (editDeviceIndex >= 0 && editDeviceIndex < state.activeDevices.size) {
        val device = state.activeDevices[editDeviceIndex]
        DeviceEditSheet(
            device = device,
            onChangeType = { newType ->
                vm.updateDeviceType(context, editDeviceIndex, newType)
                editDeviceIndex = -1
            },
            onRemove = {
                vm.removeDevice(context, editDeviceIndex)
                editDeviceIndex = -1
            },
            onDismiss = { editDeviceIndex = -1 }
        )
    }
}
