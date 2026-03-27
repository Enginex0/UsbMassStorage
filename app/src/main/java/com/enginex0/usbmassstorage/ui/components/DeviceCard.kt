package com.enginex0.usbmassstorage.ui.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Eject
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.enginex0.usbmassstorage.R
import com.enginex0.usbmassstorage.daemon.ActiveDevice
import com.enginex0.usbmassstorage.util.formatted

@Composable
fun DeviceCard(
    device: ActiveDevice,
    onEject: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (icon, label) = when {
        device.cdrom -> Icons.Filled.Album to stringResource(R.string.device_type_cdrom)
        device.ro -> Icons.Filled.Lock to stringResource(R.string.device_type_readonly)
        else -> Icons.Filled.Storage to stringResource(R.string.device_type_readwrite)
    }

    val displayPath = Uri.parse("file://${device.file}").formatted

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayPath,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = onEject) {
                Icon(
                    imageVector = Icons.Filled.Eject,
                    contentDescription = stringResource(R.string.action_eject),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
