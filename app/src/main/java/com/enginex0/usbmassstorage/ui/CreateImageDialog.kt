package com.enginex0.usbmassstorage.ui

import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.enginex0.usbmassstorage.R
import com.enginex0.usbmassstorage.daemon.DeviceInfo
import com.enginex0.usbmassstorage.daemon.DeviceType
import com.enginex0.usbmassstorage.data.FileSystemType
import com.enginex0.usbmassstorage.data.FormatPreference
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "CreateImage"

private val IMAGE_EXTENSIONS = listOf(".img", ".iso", ".bin", ".raw")

private data class SizePreset(val label: String, val bytes: Long)

private val SIZE_PRESETS = listOf(
    SizePreset("256 MB", 256_000_000L),
    SizePreset("512 MB", 512_000_000L),
    SizePreset("1 GB", 1_000_000_000L),
    SizePreset("2 GB", 2_000_000_000L),
    SizePreset("4 GB", 4_000_000_000L),
    SizePreset("8 GB", 8_000_000_000L),
    SizePreset("16 GB", 16_000_000_000L),
    SizePreset("32 GB", 32_000_000_000L),
)

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

private fun sanitizeFilename(name: String): String =
    name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> "%.0f MB".format(bytes / 1_000_000.0)
    else -> "$bytes B"
}

private const val MODULE_DIR = "/data/adb/modules/usbmassstorage"

private suspend fun findBundledMkfs(): String? = withContext(Dispatchers.IO) {
    val abis = listOf("arm64-v8a", "armeabi-v7a", "x86_64")
    abis.map { "$MODULE_DIR/bin/$it/mkfs.fat" }
        .firstOrNull { Shell.cmd("[ -x '$it' ]").exec().isSuccess }
}

private suspend fun checkBinary(name: String): Boolean = withContext(Dispatchers.IO) {
    Shell.cmd("which $name 2>/dev/null").exec().isSuccess
}

private suspend fun formatFat32(path: String) = withContext(Dispatchers.IO) {
    val mkfs = findBundledMkfs()
        ?: throw IllegalStateException("mkfs.fat not found in module. Reinstall the module.")
    val fmt = Shell.cmd("$mkfs -F 32 -n USBDRIVE '$path'").exec()
    if (!fmt.isSuccess) throw IllegalStateException("FAT32 format failed: ${fmt.err.joinToString()}")
}

private suspend fun formatExfat(path: String) = withContext(Dispatchers.IO) {
    val fmt = Shell.cmd("mkfs.exfat '$path'").exec()
    if (!fmt.isSuccess) throw IllegalStateException("exFAT format failed: ${fmt.err.joinToString()}")
}

private suspend fun createAndFormat(
    baseDir: java.io.File,
    filename: String,
    sizeBytes: Long,
    fsType: FileSystemType
): String = withContext(Dispatchers.IO) {
    val dir = java.io.File(baseDir, "images").apply { mkdirs() }
    val file = java.io.File(dir, filename)
    val path = file.absolutePath

    java.io.RandomAccessFile(file, "rw").use { it.setLength(sizeBytes) }

    try {
        when (fsType) {
            FileSystemType.FAT32 -> formatFat32(path)
            FileSystemType.EXFAT -> formatExfat(path)
            FileSystemType.NONE -> {}
        }
    } catch (e: Exception) {
        file.delete()
        throw e
    }

    path
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateImageSheet(
    onDismiss: () -> Unit,
    onCreated: (DeviceInfo) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var filename by remember { mutableStateOf("disk1") }
    var selectedExt by remember { mutableStateOf(".img") }
    var selectedPresetIndex by remember { mutableStateOf(1) }
    var customSize by remember { mutableStateOf("") }
    var useCustom by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf(FormatPreference.load(context)) }
    var creating by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasVfat by remember { mutableStateOf(true) }
    var hasExfat by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val dir = java.io.File(context.getExternalFilesDir(null), "images")
        val existing = withContext(Dispatchers.IO) {
            dir.listFiles()?.map { it.nameWithoutExtension }?.toSet() ?: emptySet()
        }
        var n = 1
        while ("disk$n" in existing) n++
        filename = "disk$n"

        hasVfat = findBundledMkfs() != null
        hasExfat = checkBinary("mkfs.exfat")
        if (selectedFormat == FileSystemType.EXFAT && !hasExfat) {
            selectedFormat = if (hasVfat) FileSystemType.FAT32 else FileSystemType.NONE
        }
    }

    val sizeBytes = if (useCustom) parseSize(customSize) else SIZE_PRESETS[selectedPresetIndex].bytes
    val filenameRequiredMsg = stringResource(R.string.create_image_filename_required)
    val invalidSizeMsg = stringResource(R.string.create_image_invalid)
    val creatingMsg = stringResource(R.string.create_image_creating)
    val formattingMsg = stringResource(R.string.create_image_formatting)

    val glow = rememberInfiniteTransition(label = "header")
    val borderAlpha by glow.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "glow"
    )

    ModalBottomSheet(
        onDismissRequest = { if (!creating) onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .animateContentSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .then(
                        if (!creating) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                            CircleShape
                        ) else Modifier
                    )
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (creating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(72.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                }
                Icon(
                    Icons.Filled.Storage,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.create_image_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                stringResource(R.string.create_image_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(24.dp))

            if (creating) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        statusText ?: creatingMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = filename,
                        onValueChange = { filename = it },
                        label = { Text(stringResource(R.string.create_image_filename)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IMAGE_EXTENSIONS.forEach { ext ->
                            FilterChip(
                                selected = selectedExt == ext,
                                onClick = { selectedExt = ext },
                                label = { Text(ext) }
                            )
                        }
                    }

                    Text(
                        stringResource(R.string.create_image_pick_size),
                        style = MaterialTheme.typography.labelLarge
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SIZE_PRESETS.forEachIndexed { index, preset ->
                            FilterChip(
                                selected = !useCustom && selectedPresetIndex == index,
                                onClick = { useCustom = false; selectedPresetIndex = index },
                                label = { Text(preset.label) }
                            )
                        }
                        FilterChip(
                            selected = useCustom,
                            onClick = { useCustom = true },
                            label = { Text(stringResource(R.string.create_image_custom_size)) }
                        )
                    }

                    if (useCustom) {
                        OutlinedTextField(
                            value = customSize,
                            onValueChange = { customSize = it },
                            label = { Text(stringResource(R.string.create_image_size)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            shape = RoundedCornerShape(12.dp),
                            supportingText = {
                                val bytes = parseSize(customSize)
                                if (bytes != null) {
                                    Text("= ${formatBytes(bytes)}")
                                } else if (customSize.isNotBlank()) {
                                    Text(
                                        stringResource(R.string.create_image_invalid_size),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                    }

                    Text(
                        stringResource(R.string.create_image_format),
                        style = MaterialTheme.typography.labelLarge
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedFormat == FileSystemType.FAT32,
                            onClick = { selectedFormat = FileSystemType.FAT32 },
                            enabled = hasVfat,
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .size(6.dp)
                                            .background(
                                                if (hasVfat) Color(0xFF4CAF50)
                                                else MaterialTheme.colorScheme.error,
                                                CircleShape
                                            )
                                    )
                                    Text(stringResource(R.string.format_fat32))
                                }
                            }
                        )
                        FilterChip(
                            selected = selectedFormat == FileSystemType.EXFAT,
                            onClick = { selectedFormat = FileSystemType.EXFAT },
                            enabled = hasExfat,
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .size(6.dp)
                                            .background(
                                                if (hasExfat) Color(0xFF4CAF50)
                                                else MaterialTheme.colorScheme.error,
                                                CircleShape
                                            )
                                    )
                                    Text(stringResource(R.string.format_exfat))
                                }
                            }
                        )
                        FilterChip(
                            selected = selectedFormat == FileSystemType.NONE,
                            onClick = { selectedFormat = FileSystemType.NONE },
                            label = { Text(stringResource(R.string.format_none)) }
                        )
                    }

                    if (sizeBytes != null) {
                        val safeName = sanitizeFilename(filename).ifEmpty { "disk" }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "$safeName$selectedExt  \u00b7  ${formatBytes(sizeBytes)}  \u00b7  ${selectedFormat.name}",
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (error != null) {
                        Text(
                            error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (filename.isBlank()) { error = filenameRequiredMsg; return@Button }
                            if (sizeBytes == null) { error = invalidSizeMsg; return@Button }
                            error = null
                            creating = true
                            statusText = creatingMsg

                            val safeName = sanitizeFilename(filename).ifEmpty { "disk" }
                            val finalName = "$safeName$selectedExt"

                            val baseDir = context.getExternalFilesDir(null)
                            if (baseDir == null) {
                                error = "External storage unavailable"
                                creating = false
                                return@Button
                            }

                            scope.launch {
                                try {
                                    if (selectedFormat != FileSystemType.NONE) statusText = formattingMsg
                                    val path = createAndFormat(baseDir, finalName, sizeBytes, selectedFormat)
                                    val fsLabel = when (selectedFormat) {
                                        FileSystemType.FAT32 -> "VFAT"
                                        FileSystemType.EXFAT -> "EXFAT"
                                        FileSystemType.NONE -> null
                                    }
                                    Log.d(TAG, "Created: $path ($sizeBytes bytes, $selectedFormat)")
                                    onCreated(
                                        DeviceInfo(Uri.fromFile(java.io.File(path)), DeviceType.DISK_RW, fsLabel)
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Create failed", e)
                                    error = e.message ?: "Creation failed"
                                    creating = false
                                    statusText = null
                                }
                            }
                        },
                        enabled = sizeBytes != null,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.action_create_mount))
                    }
                }
            }
        }
    }
}
