package com.enginex0.usbmassstorage.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.enginex0.usbmassstorage.BuildConfig
import com.enginex0.usbmassstorage.R
import com.enginex0.usbmassstorage.data.AccentColor
import com.enginex0.usbmassstorage.data.AccentPreference
import com.enginex0.usbmassstorage.viewmodel.UiState
import com.topjohnwu.superuser.Shell

private const val TAG = "UsbMsUI"
private const val DEBUG_PREFS = "debug_prefs"
private const val KEY_DEBUG = "debug_mode"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    state: UiState,
    onRestartDaemon: () -> Unit,
    onBack: () -> Unit,
    onAccentChanged: (AccentColor) -> Unit = {}
) {
    val context = LocalContext.current
    var selinuxContext by remember { mutableStateOf<String?>(null) }
    val debugPrefs = remember { context.getSharedPreferences(DEBUG_PREFS, Context.MODE_PRIVATE) }
    var debugMode by remember { mutableStateOf(debugPrefs.getBoolean(KEY_DEBUG, false)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_daemon_status), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    val statusText = when {
                        state.connecting -> stringResource(R.string.status_connecting)
                        state.connected -> stringResource(R.string.status_running_connected)
                        state.daemonRunning -> stringResource(R.string.status_running_not_connected)
                        else -> stringResource(R.string.status_stopped)
                    }
                    val statusColor = when {
                        state.connected -> MaterialTheme.colorScheme.primary
                        state.daemonRunning -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }

                    Text(text = statusText, color = statusColor, style = MaterialTheme.typography.bodyLarge)

                    val firstAlert = state.alerts.firstOrNull()
                    if (firstAlert != null) {
                        Text(
                            text = firstAlert.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    Log.d(TAG, "Settings: restart daemon clicked")
                    onRestartDaemon()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_restart_daemon))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_app_info), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    val versionText = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME) +
                        if (BuildConfig.DEBUG) stringResource(R.string.settings_version_debug) else ""
                    val debugEnabledMsg = stringResource(R.string.settings_debug_enabled)
                    val debugDisabledMsg = stringResource(R.string.settings_debug_disabled)
                    Text(
                        text = versionText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PROJECT_URL))
                                context.startActivity(intent)
                            },
                            onLongClick = {
                                debugMode = !debugMode
                                debugPrefs.edit().putBoolean(KEY_DEBUG, debugMode).apply()
                                val msg = if (debugMode) debugEnabledMsg else debugDisabledMsg
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    )
                }
            }

            AccentColorCard(context, onAccentChanged)

            Text(stringResource(R.string.settings_debug_title), style = MaterialTheme.typography.titleMedium)

            val logsCopiedMsg = stringResource(R.string.settings_logs_copied)
            OutlinedButton(
                onClick = {
                    Log.d(TAG, "Settings: copy logs clicked")
                    val result = Shell.cmd("logcat -d -s msd-tool MsdClient MsdProto UsbMsVM").exec()
                    val logs = result.out.joinToString("\n")
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("daemon logs", logs))
                    Toast.makeText(context, logsCopiedMsg, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_copy_logs))
            }

            OutlinedButton(
                onClick = {
                    Log.d(TAG, "Settings: SELinux context clicked")
                    val pid = Shell.cmd("pidof daemon").exec()
                    if (pid.isSuccess && pid.out.isNotEmpty()) {
                        val ctxResult = Shell.cmd("cat /proc/${pid.out[0].trim()}/attr/current").exec()
                        selinuxContext = ctxResult.out.joinToString()
                    } else {
                        selinuxContext = "Daemon not running"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_view_selinux))
            }

            if (selinuxContext != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = selinuxContext ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (debugMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.settings_debug_info), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))

                        val logDir = context.getExternalFilesDir(null)?.absolutePath ?: "N/A"
                        Text(stringResource(R.string.settings_log_dir, logDir), style = MaterialTheme.typography.bodySmall)

                        val daemonPid = Shell.cmd("pidof daemon").exec()
                        val pidText = if (daemonPid.isSuccess && daemonPid.out.isNotEmpty())
                            daemonPid.out[0].trim() else "N/A"
                        Text(stringResource(R.string.settings_daemon_pid, pidText), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.settings_socket), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.settings_build, BuildConfig.BUILD_TYPE), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccentColorCard(context: Context, onAccentChanged: (AccentColor) -> Unit) {
    var selected by remember { mutableStateOf(AccentPreference.load(context)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.settings_accent_color), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            AccentOption(stringResource(R.string.accent_system_default), listOf(Color(0xFF4FC3F7), Color(0xFF0288D1)), selected == AccentColor.SYSTEM_DEFAULT) {
                selected = AccentColor.SYSTEM_DEFAULT
                AccentPreference.save(context, AccentColor.SYSTEM_DEFAULT)
                onAccentChanged(AccentColor.SYSTEM_DEFAULT)
            }
            Spacer(Modifier.height(6.dp))
            AccentOption(stringResource(R.string.accent_almost_black), listOf(Color(0xFF1A1A1A), Color(0xFF2C2C2C)), selected == AccentColor.ALMOST_BLACK) {
                selected = AccentColor.ALMOST_BLACK
                AccentPreference.save(context, AccentColor.ALMOST_BLACK)
                onAccentChanged(AccentColor.ALMOST_BLACK)
            }
            Spacer(Modifier.height(6.dp))
            AccentOption(stringResource(R.string.accent_white), listOf(Color(0xFFF5F5F5), Color(0xFFE0E0E0)), selected == AccentColor.WHITE) {
                selected = AccentColor.WHITE
                AccentPreference.save(context, AccentColor.WHITE)
                onAccentChanged(AccentColor.WHITE)
            }
        }
    }
}

@Composable
private fun AccentOption(
    label: String,
    swatches: List<Color>,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            swatches.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.settings_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
