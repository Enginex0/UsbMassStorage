package com.enginex0.usbmassstorage.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Eject
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.enginex0.usbmassstorage.R
import com.enginex0.usbmassstorage.daemon.ActiveDevice

@Composable
fun DeviceCard(
    device: ActiveDevice,
    index: Int,
    onEject: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showEjectConfirm by rememberSaveable { mutableStateOf(false) }

    if (showEjectConfirm) {
        AlertDialog(
            onDismissRequest = { showEjectConfirm = false },
            title = { Text(stringResource(R.string.eject_confirm_title)) },
            text = { Text(stringResource(R.string.eject_confirm_message, device.file.substringAfterLast('/'))) },
            confirmButton = {
                TextButton(onClick = {
                    showEjectConfirm = false
                    onEject()
                }) { Text(stringResource(R.string.action_eject)) }
            },
            dismissButton = {
                TextButton(onClick = { showEjectConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val (icon, typeLabel, iconTint) = when {
        device.cdrom -> Triple(Icons.Filled.Album, stringResource(R.string.device_type_cdrom), MaterialTheme.colorScheme.secondary)
        device.ro -> Triple(Icons.Filled.Lock, stringResource(R.string.device_type_readonly), MaterialTheme.colorScheme.tertiary)
        else -> Triple(Icons.Filled.Storage, stringResource(R.string.device_type_readwrite), MaterialTheme.colorScheme.primary)
    }

    val filename = device.file.substringAfterLast('/')
    val directory = device.file.substringBeforeLast('/', "")
    val sizeText = if (device.size > 0) formatSize(device.size) else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = typeLabel,
                    modifier = Modifier.size(28.dp),
                    tint = iconTint
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = filename,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                    }

                    val subtitle = buildString {
                        append(typeLabel)
                        if (sizeText != null) append("  \u2022  $sizeText")
                        append("  \u2022  LUN $index")
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(4.dp))

                IconButton(onClick = { showEjectConfirm = true }) {
                    Icon(
                        imageVector = Icons.Filled.Eject,
                        contentDescription = stringResource(R.string.action_eject),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))

                InfoRow(stringResource(R.string.device_label_path), device.file)
                Spacer(Modifier.height(6.dp))
                if (directory.isNotEmpty()) {
                    InfoRow(stringResource(R.string.device_label_dir), directory)
                    Spacer(Modifier.height(6.dp))
                }
                InfoRow(stringResource(R.string.device_label_type), typeLabel)
                Spacer(Modifier.height(6.dp))
                InfoRow(
                    stringResource(R.string.device_label_access),
                    if (device.ro || device.cdrom) stringResource(R.string.device_access_ro)
                    else stringResource(R.string.device_access_rw)
                )
                Spacer(Modifier.height(6.dp))
                if (sizeText != null) {
                    InfoRow(stringResource(R.string.device_label_size), sizeText)
                    Spacer(Modifier.height(6.dp))
                }
                if (device.fsType != null) {
                    InfoRow(stringResource(R.string.device_label_format), device.fsType)
                    Spacer(Modifier.height(6.dp))
                }
                InfoRow(stringResource(R.string.device_label_lun), index.toString())
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(start = 16.dp)
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000L -> "%.1f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
