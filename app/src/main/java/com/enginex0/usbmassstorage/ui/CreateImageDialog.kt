package com.enginex0.usbmassstorage.ui

import android.content.Intent
import android.net.Uri
import android.system.Os
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.enginex0.usbmassstorage.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "CreateImage"

private val SIZE_PATTERN = Regex(
    """^\s*(\d+(?:\.\d+)?)\s*(B|KB|MB|GB|KiB|MiB|GiB)?\s*$""",
    RegexOption.IGNORE_CASE
)

private fun parseSize(input: String): Long? {
    val match = SIZE_PATTERN.matchEntire(input) ?: return null
    val value = match.groupValues[1].toDoubleOrNull() ?: return null
    val unit = match.groupValues[2].uppercase().ifEmpty { "MB" }
    val multiplier = when (unit) {
        "B" -> 1L
        "KB" -> 1_000L
        "MB" -> 1_000_000L
        "GB" -> 1_000_000_000L
        "KIB" -> 1_024L
        "MIB" -> 1_048_576L
        "GIB" -> 1_073_741_824L
        else -> return null
    }
    val bytes = (value * multiplier).toLong()
    return if (bytes > 0) bytes else null
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.2f GiB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.2f MiB".format(bytes / 1_048_576.0)
    bytes >= 1_024L -> "%.2f KiB".format(bytes / 1_024.0)
    else -> "$bytes B"
}

private fun sanitizeFilename(name: String): String =
    name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")

@Composable
fun CreateImageDialog(
    onDismiss: () -> Unit,
    onCreated: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var filename by remember { mutableStateOf("disk.img") }
    var sizeInput by remember { mutableStateOf("512 MB") }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val parsedBytes = parseSize(sizeInput)
    val suggestedName = sanitizeFilename(filename).ifEmpty { "disk.img" }
    val filenameRequiredMsg = stringResource(R.string.create_image_filename_required)
    val invalidSizeMsg = stringResource(R.string.create_image_invalid)

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = object : ActivityResultContracts.CreateDocument("application/octet-stream") {
            override fun createIntent(context: android.content.Context, input: String): Intent {
                return super.createIntent(context, input).addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }
        }
    ) { uri ->
        if (uri == null) {
            creating = false
            return@rememberLauncherForActivityResult
        }
        val sizeBytes = parsedBytes ?: run {
            error = invalidSizeMsg
            creating = false
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openFileDescriptor(uri, "rwt")?.use { pfd ->
                        Os.ftruncate(pfd.fileDescriptor, sizeBytes)
                    } ?: throw IllegalStateException("Failed to open file descriptor")
                }

                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                Log.d(TAG, "Created image: $uri ($sizeBytes bytes)")
                onCreated(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create image", e)
                error = e.message ?: "Creation failed"
                creating = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        title = { Text(stringResource(R.string.create_image_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text(stringResource(R.string.create_image_filename)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = sizeInput,
                    onValueChange = { sizeInput = it },
                    label = { Text(stringResource(R.string.create_image_size)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    supportingText = {
                        val bytes = parsedBytes
                        if (bytes != null) {
                            Text(stringResource(R.string.create_image_size_display, formatBytes(bytes)))
                        } else if (sizeInput.isNotBlank()) {
                            Text(stringResource(R.string.create_image_invalid_size), color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (filename.isBlank()) {
                        error = filenameRequiredMsg
                        return@Button
                    }
                    if (parsedBytes == null) {
                        error = invalidSizeMsg
                        return@Button
                    }
                    error = null
                    creating = true
                    createDocLauncher.launch(suggestedName)
                },
                enabled = !creating
            ) {
                if (creating) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp).width(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.action_create))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
