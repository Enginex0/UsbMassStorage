package com.enginex0.usbmassstorage.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.enginex0.usbmassstorage.R
import com.enginex0.usbmassstorage.data.BackgroundPreference
import com.enginex0.usbmassstorage.ui.components.DeviceCard
import com.enginex0.usbmassstorage.ui.components.UsbStatusBar
import com.enginex0.usbmassstorage.viewmodel.UiState

private const val TAG = "UsbMsUI"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    state: UiState,
    bgIndex: Int = 0,
    onRefresh: () -> Unit,
    onAddDevice: () -> Unit,
    onSettings: () -> Unit,
    onGuide: () -> Unit = {},
    onImages: () -> Unit = {},
    onEjectDevice: (Int) -> Unit,
    onDeviceClick: (Int) -> Unit = {},
    onAcknowledgeAlert: () -> Unit = {}
) {
    Log.d(TAG, "DeviceListScreen: composed, connected=${state.connected}, devices=${state.activeDevices.size}")
    val bgOpacity = BackgroundPreference.load(LocalContext.current)
    val bgRes = if (bgIndex % 2 == 0) R.drawable.bg_1 else R.drawable.bg_2
    var menuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showAlertDetail by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    val currentAlert = state.alerts.firstOrNull()
    val detailsLabel = stringResource(R.string.action_details)
    LaunchedEffect(currentAlert) {
        if (currentAlert != null) {
            val result = snackbarHostState.showSnackbar(
                message = currentAlert.message.take(80),
                actionLabel = detailsLabel,
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.ActionPerformed -> showAlertDetail = currentAlert.message
                SnackbarResult.Dismissed -> onAcknowledgeAlert()
            }
        }
    }

    if (showAlertDetail != null) {
        AlertDialog(
            onDismissRequest = {
                showAlertDetail = null
                onAcknowledgeAlert()
            },
            title = { Text(stringResource(R.string.error_details_title)) },
            text = { Text(showAlertDetail ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(showAlertDetail ?: ""))
                }) { Text(stringResource(R.string.action_copy)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAlertDetail = null
                    onAcknowledgeAlert()
                }) { Text(stringResource(R.string.action_close)) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(bgRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(bgOpacity),
            contentScale = ContentScale.Crop
        )

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = {
                        Log.d(TAG, "DeviceListScreen: refresh clicked")
                        onRefresh()
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_menu))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.images_title)) },
                                onClick = {
                                    menuExpanded = false
                                    onImages()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.title_settings)) },
                                onClick = {
                                    menuExpanded = false
                                    onSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.title_how_to_use)) },
                                onClick = {
                                    menuExpanded = false
                                    onGuide()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_refresh)) },
                                onClick = {
                                    menuExpanded = false
                                    onRefresh()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.connected) {
                val haptic = LocalHapticFeedback.current
                val bounce = rememberInfiniteTransition(label = "fab")
                val scale by bounce.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "fab_scale"
                )
                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddDevice()
                    },
                    modifier = Modifier.scale(scale),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_device_fab))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            UsbStatusBar(
                connected = state.connected,
                rootGranted = state.rootGranted,
                functions = state.functions,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                state.connecting -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                !state.connected -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.alerts.firstOrNull()?.message ?: stringResource(R.string.status_not_connected),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Button(
                                onClick = onRefresh,
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text(stringResource(R.string.action_retry))
                            }
                        }
                    }
                }

                state.activeDevices.isEmpty() -> {
                    PullToRefreshBox(
                        isRefreshing = state.connecting,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_devices_mounted),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.connecting,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(state.activeDevices) { index, device ->
                                DeviceCard(
                                    device = device,
                                    index = index,
                                    onEject = { onEjectDevice(index) },
                                    onClick = { onDeviceClick(index) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
