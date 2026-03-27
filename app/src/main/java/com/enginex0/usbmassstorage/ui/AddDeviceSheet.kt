package com.enginex0.usbmassstorage.ui

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.enginex0.usbmassstorage.R
import com.enginex0.usbmassstorage.daemon.DeviceInfo
import com.enginex0.usbmassstorage.daemon.DeviceType

private const val TAG = "UsbMsUI"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceSheet(
    mounting: Boolean,
    onMount: (DeviceInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf(DeviceType.DISK_RW) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = object : ActivityResultContracts.OpenDocument() {
            override fun createIntent(context: android.content.Context, input: Array<String>): Intent {
                return super.createIntent(context, input).addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }
        }
    ) { uri ->
        if (uri != null) {
            Log.d(TAG, "AddDeviceSheet: file selected: $uri")
            selectedUri = uri

            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        selectedName = it.getString(nameIndex)
                    }
                }
            }
            if (selectedName == null) {
                selectedName = uri.lastPathSegment ?: "Unknown file"
            }
        }
    }

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
                text = stringResource(R.string.add_device_title),
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedButton(
                onClick = {
                    Log.d(TAG, "AddDeviceSheet: opening file picker")
                    filePicker.launch(arrayOf("*/*"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.FileOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(selectedName ?: stringResource(R.string.add_device_select_file))
            }

            Text(
                text = stringResource(R.string.add_device_type),
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == DeviceType.DISK_RW,
                    onClick = {
                        Log.d(TAG, "AddDeviceSheet: type changed to DISK_RW")
                        selectedType = DeviceType.DISK_RW
                    },
                    label = { Text(stringResource(R.string.type_read_write)) }
                )
                FilterChip(
                    selected = selectedType == DeviceType.DISK_RO,
                    onClick = {
                        Log.d(TAG, "AddDeviceSheet: type changed to DISK_RO")
                        selectedType = DeviceType.DISK_RO
                    },
                    label = { Text(stringResource(R.string.type_read_only)) }
                )
                FilterChip(
                    selected = selectedType == DeviceType.CDROM,
                    onClick = {
                        Log.d(TAG, "AddDeviceSheet: type changed to CDROM")
                        selectedType = DeviceType.CDROM
                    },
                    label = { Text(stringResource(R.string.type_cdrom)) }
                )
            }

            Button(
                onClick = {
                    val uri = selectedUri ?: return@Button
                    Log.d(TAG, "AddDeviceSheet: mounting $uri as $selectedType")
                    onMount(DeviceInfo(uri, selectedType))
                },
                enabled = selectedUri != null && !mounting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (mounting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp).width(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (mounting) stringResource(R.string.mounting) else stringResource(R.string.action_mount))
            }

            TextButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.create_new_image))
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showCreateDialog) {
        CreateImageDialog(
            onDismiss = { showCreateDialog = false },
            onCreated = { deviceInfo ->
                showCreateDialog = false
                onMount(deviceInfo)
            }
        )
    }
}
