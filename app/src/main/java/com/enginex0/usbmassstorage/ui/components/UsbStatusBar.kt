package com.enginex0.usbmassstorage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.enginex0.usbmassstorage.R
import com.enginex0.usbmassstorage.daemon.UsbFunction

@Composable
fun UsbStatusBar(
    connected: Boolean,
    rootGranted: Boolean,
    functions: List<UsbFunction>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StatusRow(
            active = rootGranted,
            activeText = stringResource(R.string.status_root_granted),
            inactiveText = stringResource(R.string.status_root_not_granted),
            activeDescription = stringResource(R.string.desc_root_granted),
            inactiveDescription = stringResource(R.string.desc_root_not_granted)
        )

        val daemonLabel = if (connected) {
            val names = functions.joinToString(", ") { it.function }
            if (names.isNotEmpty()) stringResource(R.string.status_daemon_prefix, names) else stringResource(R.string.status_daemon_connected)
        } else {
            stringResource(R.string.status_daemon_disconnected)
        }

        StatusRow(
            active = connected,
            activeText = daemonLabel,
            inactiveText = daemonLabel,
            activeDescription = stringResource(R.string.desc_daemon_connected),
            inactiveDescription = stringResource(R.string.desc_daemon_disconnected)
        )
    }
}

@Composable
private fun StatusRow(
    active: Boolean,
    activeText: String,
    inactiveText: String,
    activeDescription: String,
    inactiveDescription: String
) {
    val bg = if (active) MaterialTheme.colorScheme.primaryContainer
             else MaterialTheme.colorScheme.errorContainer
    val fg = if (active) MaterialTheme.colorScheme.onPrimaryContainer
             else MaterialTheme.colorScheme.onErrorContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (active) Icons.Filled.CheckCircle else Icons.Filled.Error,
            contentDescription = if (active) activeDescription else inactiveDescription,
            modifier = Modifier.size(18.dp),
            tint = fg
        )
        Text(
            text = if (active) activeText else inactiveText,
            style = MaterialTheme.typography.bodySmall,
            color = fg
        )
    }
}
