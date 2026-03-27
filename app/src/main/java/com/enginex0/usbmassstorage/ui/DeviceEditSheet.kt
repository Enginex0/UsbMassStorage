package com.enginex0.usbmassstorage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.enginex0.usbmassstorage.R
import com.enginex0.usbmassstorage.daemon.ActiveDevice
import com.enginex0.usbmassstorage.daemon.DeviceType

private fun ActiveDevice.currentType(): DeviceType = when {
    cdrom -> DeviceType.CDROM
    ro -> DeviceType.DISK_RO
    else -> DeviceType.DISK_RW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceEditSheet(
    device: ActiveDevice,
    onChangeType: (DeviceType) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val originalType = remember { device.currentType() }
    var selectedType by remember { mutableStateOf(originalType) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.edit_device_title),
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = device.file,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.add_device_type),
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == DeviceType.DISK_RW,
                    onClick = { selectedType = DeviceType.DISK_RW },
                    label = { Text(stringResource(R.string.type_read_write)) }
                )
                FilterChip(
                    selected = selectedType == DeviceType.DISK_RO,
                    onClick = { selectedType = DeviceType.DISK_RO },
                    label = { Text(stringResource(R.string.type_read_only)) }
                )
                FilterChip(
                    selected = selectedType == DeviceType.CDROM,
                    onClick = { selectedType = DeviceType.CDROM },
                    label = { Text(stringResource(R.string.type_cdrom)) }
                )
            }

            Button(
                onClick = { onChangeType(selectedType) },
                enabled = selectedType != originalType,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_apply))
            }

            Button(
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.action_remove))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
