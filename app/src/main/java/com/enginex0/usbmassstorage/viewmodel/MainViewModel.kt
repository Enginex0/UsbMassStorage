package com.enginex0.usbmassstorage.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.system.Os
import android.system.OsConstants
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enginex0.usbmassstorage.daemon.ActiveDevice
import com.enginex0.usbmassstorage.daemon.DaemonClient
import com.enginex0.usbmassstorage.daemon.DeviceInfo
import com.enginex0.usbmassstorage.daemon.DeviceType
import com.enginex0.usbmassstorage.daemon.UsbFunction
import com.enginex0.usbmassstorage.data.DeviceStore
import com.enginex0.usbmassstorage.util.formatted
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import android.os.ParcelFileDescriptor

private const val TAG = "UsbMsVM"

sealed interface Alert {
    val message: String

    data class QueryFailure(override val message: String) : Alert
    data class MountFailure(override val message: String) : Alert
    data class ImageFailure(override val message: String) : Alert
    data class NotLocalFile(override val message: String = "File is not locally accessible (AppFuse proxy)") : Alert
    data class InvalidFile(override val message: String = "Selected file is not a regular file") : Alert
}

fun Throwable.toAlertMessage(): String {
    val chain = generateSequence(this) { it.cause }
    return chain.joinToString(" -> ") { "${it.javaClass.simpleName} (${it.message})" }
}

data class UiState(
    val connected: Boolean = false,
    val connecting: Boolean = true,
    val mounting: Boolean = false,
    val rootGranted: Boolean = false,
    val alerts: List<Alert> = emptyList(),
    val functions: List<UsbFunction> = emptyList(),
    val activeDevices: List<ActiveDevice> = emptyList(),
    val savedDevices: List<DeviceInfo> = emptyList(),
    val daemonRunning: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var client: DaemonClient? = null
    private val daemonLock = Any()
    private val store = DeviceStore(application)
    private var reconnectJob: Job? = null
    private var activeJob: Job? = null

    init {
        Log.d(TAG, "ViewModel init, connecting to daemon")
        _uiState.update { it.copy(savedDevices = store.load()) }
        refresh()
    }

    // Daemon connections die when MIUI/HyperOS kills the background process;
    // serialize access and retry once with a fresh socket on IOException.
    private fun <T> withDaemon(block: (DaemonClient) -> T): T = synchronized(daemonLock) {
        var c = client ?: DaemonClient().also { client = it }
        try {
            return@synchronized block(c)
        } catch (_: java.io.IOException) {
            Log.w(TAG, "withDaemon: stale connection, reconnecting")
            try { c.close() } catch (_: Exception) {}
        }
        c = DaemonClient()
        client = c
        try {
            block(c)
        } catch (e: java.io.IOException) {
            try { c.close() } catch (_: Exception) {}
            client = null
            throw e
        }
    }

    fun refresh() {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update { it.copy(connecting = true) }
            Log.d(TAG, "refresh: starting daemon connection")

            val hasRoot = withContext(Dispatchers.IO) {
                Shell.getShell().isRoot
            }
            Log.d(TAG, "refresh: rootGranted=$hasRoot")

            try {
                val (functions, devices) = withContext(Dispatchers.IO) {
                    synchronized(daemonLock) {
                        client?.close()
                        client = null
                        val c = DaemonClient()
                        client = c
                        c.getFunctions() to c.getMassStorage()
                    }
                }

                _uiState.update {
                    it.copy(
                        connected = true,
                        connecting = false,
                        rootGranted = hasRoot,
                        functions = functions,
                        activeDevices = enrichWithSize(devices),
                        daemonRunning = true
                    )
                }
                Log.d(TAG, "refresh: connected, ${functions.size} functions, ${devices.size} devices")
                reconnectJob?.cancel()
                reconnectJob = null

                val saved = _uiState.value.savedDevices
                if (devices.isEmpty() && saved.isNotEmpty()) {
                    Log.d(TAG, "refresh: auto-remounting ${saved.size} saved devices")
                    setMassStorage(getApplication(), saved)
                }
            } catch (e: Exception) {
                Log.e(TAG, "refresh: connection failed", e)
                synchronized(daemonLock) {
                    client?.close()
                    client = null
                }
                pushAlert(Alert.QueryFailure(e.toAlertMessage()))
                val daemonAlive = withContext(Dispatchers.IO) { checkDaemonStatus() }
                _uiState.update {
                    it.copy(
                        connected = false,
                        connecting = false,
                        rootGranted = hasRoot,
                        daemonRunning = daemonAlive
                    )
                }
                startReconnect()
            }
        }
    }

    fun setMassStorage(context: Context, devices: List<DeviceInfo>) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            Log.d(TAG, "setMassStorage: mounting ${devices.size} devices")
            _uiState.update { it.copy(mounting = true) }

            try {
                withContext(Dispatchers.IO) {
                    val fdPairs = mutableListOf<Pair<FileDescriptor, DeviceType>>()
                    val pfds = mutableListOf<ParcelFileDescriptor>()

                    try {
                        for (info in devices) {
                            val mode = if (info.type == DeviceType.DISK_RW) "rw" else "r"
                            val pfd = if (info.uri.scheme == "file") {
                                val file = java.io.File(info.uri.path!!)
                                val flags = if (info.type == DeviceType.DISK_RW)
                                    ParcelFileDescriptor.MODE_READ_WRITE
                                else ParcelFileDescriptor.MODE_READ_ONLY
                                ParcelFileDescriptor.open(file, flags)
                            } else {
                                context.contentResolver.openFileDescriptor(info.uri, mode)
                                    ?: throw IllegalStateException("Failed to open FD for ${info.uri}")
                            }
                            pfds.add(pfd)

                            validateFd(pfd.fileDescriptor)

                            fdPairs.add(pfd.fileDescriptor to info.type)
                        }

                        withDaemon { c -> c.setMassStorage(fdPairs) }
                    } finally {
                        pfds.forEach { pfd ->
                            try { pfd.close() } catch (_: Exception) {}
                        }
                    }
                }

                Log.d(TAG, "setMassStorage: success, refreshing state")
                store.save(devices)
                _uiState.update { it.copy(savedDevices = devices) }
                refreshDevices()

                if (_uiState.value.activeDevices.isEmpty() && devices.isNotEmpty()) {
                    Log.d(TAG, "setMassStorage: daemon returned 0 devices, retrying after delay")
                    delay(500)
                    refreshDevices()
                }

                if (_uiState.value.activeDevices.isEmpty() && devices.isNotEmpty()) {
                    val synthetic = devices.map { info ->
                        ActiveDevice(
                            file = info.uri.formatted,
                            cdrom = info.type == DeviceType.CDROM,
                            ro = info.type != DeviceType.DISK_RW
                        )
                    }
                    _uiState.update { it.copy(activeDevices = synthetic) }
                    Log.w(TAG, "setMassStorage: using ${synthetic.size} synthetic devices as fallback")
                }

                _uiState.update { it.copy(mounting = false) }
            } catch (e: AppFuseException) {
                Log.e(TAG, "setMassStorage: AppFuse file rejected", e)
                pushAlert(Alert.NotLocalFile())
                _uiState.update { it.copy(mounting = false) }
            } catch (e: InvalidFileException) {
                Log.e(TAG, "setMassStorage: invalid file", e)
                pushAlert(Alert.InvalidFile())
                _uiState.update { it.copy(mounting = false) }
            } catch (e: Exception) {
                Log.e(TAG, "setMassStorage: failed", e)
                pushAlert(Alert.MountFailure(e.toAlertMessage()))
                _uiState.update { it.copy(mounting = false) }
            }
        }
    }

    fun addDevice(context: Context, device: DeviceInfo) {
        val existing = _uiState.value.activeDevices.map { it.toDeviceInfo() }
        setMassStorage(context, existing + device)
    }

    fun clearDevices() {
        viewModelScope.launch {
            Log.d(TAG, "clearDevices: unmounting all")

            try {
                withContext(Dispatchers.IO) {
                    withDaemon { c -> c.setMassStorage(emptyList()) }
                }
                Log.d(TAG, "clearDevices: success")
                store.clear()
                _uiState.update { it.copy(savedDevices = emptyList()) }
                refreshDevices()
            } catch (e: Exception) {
                Log.e(TAG, "clearDevices: failed", e)
                pushAlert(Alert.MountFailure(e.toAlertMessage()))
            }
        }
    }

    fun ejectDevice(context: Context, index: Int) {
        val current = _uiState.value.activeDevices
        if (index < 0 || index >= current.size) return
        Log.d(TAG, "ejectDevice: ejecting index $index (${current[index].file})")

        val remaining = current.filterIndexed { i, _ -> i != index }.map { it.toDeviceInfo() }
        if (remaining.isEmpty()) {
            clearDevices()
        } else {
            setMassStorage(context, remaining)
        }
    }

    fun updateDeviceType(context: Context, index: Int, newType: DeviceType) {
        val current = _uiState.value.activeDevices
        if (index < 0 || index >= current.size) return
        Log.d(TAG, "updateDeviceType: index $index -> $newType")

        val devices = current.mapIndexed { i, dev ->
            val info = dev.toDeviceInfo()
            if (i == index) info.copy(type = newType) else info
        }
        setMassStorage(context, devices)
    }

    fun removeDevice(context: Context, index: Int) {
        val active = _uiState.value.activeDevices
        if (index < 0 || index >= active.size) return
        val targetUri = active[index].toDeviceInfo().uri

        ejectDevice(context, index)

        viewModelScope.launch {
            val saved = _uiState.value.savedDevices.toMutableList()
            val savedIndex = saved.indexOfFirst { it.uri == targetUri }
            if (savedIndex >= 0) {
                val removed = saved.removeAt(savedIndex)
                withContext(Dispatchers.IO) { store.save(saved) }
                _uiState.update { it.copy(savedDevices = saved) }
                releasePersistablePermission(context, removed.uri)
            }
        }
    }

    fun acknowledgeAlert() {
        _uiState.update { state ->
            state.copy(alerts = state.alerts.drop(1))
        }
    }

    fun restartDaemon() {
        viewModelScope.launch {
            Log.d(TAG, "restartDaemon: killing and relaunching")
            _uiState.update { it.copy(connecting = true) }

            withContext(Dispatchers.IO) {
                synchronized(daemonLock) {
                    client?.close()
                    client = null
                }
                Shell.cmd("pkill -f '/bin/.*/daemon' 2>/dev/null; sleep 0.5; rm -f /dev/usbms_svc_lock; sh /data/adb/modules/usbmassstorage/service.sh &").exec()
            }

            delay(1000)
            refresh()
        }
    }

    fun stopDaemon() {
        viewModelScope.launch {
            Log.d(TAG, "stopDaemon: hard kill")
            reconnectJob?.cancel()
            reconnectJob = null

            withContext(Dispatchers.IO) {
                synchronized(daemonLock) {
                    client?.close()
                    client = null
                }
                Shell.cmd(
                    "DPID=\$(pidof daemon);" +
                    "[ -n \"\$DPID\" ] && kill \$(awk '{print \$4}' /proc/\$DPID/stat 2>/dev/null) 2>/dev/null;" +
                    "pkill -f '/bin/.*/daemon' 2>/dev/null;" +
                    "rm -f /dev/usbms_svc_lock"
                ).exec()
            }

            store.clear()
            _uiState.update {
                it.copy(
                    connected = false,
                    connecting = false,
                    daemonRunning = false,
                    activeDevices = emptyList(),
                    savedDevices = emptyList()
                )
            }
        }
    }

    fun checkDaemonStatus(): Boolean {
        val result = Shell.cmd("pidof daemon").exec()
        val running = result.isSuccess && result.out.isNotEmpty()
        Log.d(TAG, "checkDaemonStatus: running=$running")
        return running
    }

    private fun startReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = viewModelScope.launch {
            var interval = 3000L
            while (true) {
                delay(interval)
                Log.d(TAG, "reconnect: attempting (interval=${interval}ms)")
                val alive = withContext(Dispatchers.IO) { checkDaemonStatus() }
                if (alive) {
                    refresh()
                    break
                }
                interval = (interval * 2).coerceAtMost(30_000L)
            }
        }
    }

    fun takePersistablePermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            Log.d(TAG, "takePersistablePermission: granted for $uri")
        } catch (_: SecurityException) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                Log.d(TAG, "takePersistablePermission: read-only granted for $uri")
            } catch (e2: SecurityException) {
                Log.w(TAG, "takePersistablePermission: failed for $uri", e2)
            }
        }
    }

    private fun releasePersistablePermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            Log.d(TAG, "releasePersistablePermission: released for $uri")
        } catch (e: SecurityException) {
            Log.w(TAG, "releasePersistablePermission: failed for $uri", e)
        }
    }

    private fun pushAlert(alert: Alert) {
        _uiState.update { it.copy(alerts = it.alerts + alert) }
    }

    @Throws(AppFuseException::class, InvalidFileException::class)
    private fun validateFd(fd: FileDescriptor) {
        val fdInt = getFdInt(fd)
        if (fdInt >= 0) {
            try {
                val link = Os.readlink("/proc/self/fd/$fdInt")
                if (link.startsWith("/mnt/appfuse/")) {
                    throw AppFuseException("File descriptor points to AppFuse proxy: $link")
                }
            } catch (_: android.system.ErrnoException) {
                Log.w(TAG, "validateFd: readlink failed for fd $fdInt")
            }
        }

        try {
            val stat = Os.fstat(fd)
            if (!OsConstants.S_ISREG(stat.st_mode)) {
                throw InvalidFileException("Not a regular file (mode=${stat.st_mode})")
            }
        } catch (_: android.system.ErrnoException) {
            Log.w(TAG, "validateFd: fstat failed")
        }
    }

    private fun getFdInt(fd: FileDescriptor): Int {
        return try {
            val field = FileDescriptor::class.java.getDeclaredField("descriptor")
            field.isAccessible = true
            field.getInt(fd)
        } catch (_: Exception) {
            -1
        }
    }

    private suspend fun refreshDevices() {
        try {
            val devices = withContext(Dispatchers.IO) {
                val raw = withDaemon { c -> c.getMassStorage() }
                enrichWithSize(raw)
            }
            _uiState.update { it.copy(activeDevices = devices) }
            Log.d(TAG, "refreshDevices: ${devices.size} active devices")
        } catch (e: Exception) {
            Log.e(TAG, "refreshDevices: failed", e)
        }
    }

    private fun enrichWithSize(devices: List<ActiveDevice>): List<ActiveDevice> =
        devices.map { dev ->
            val path = normalizeMediaPath(dev.file)
            val size = try { java.io.File(path).length() } catch (_: Exception) { -1L }
            val fs = try {
                val r = Shell.cmd("blkid -s TYPE -o value '$path'").exec()
                if (r.isSuccess && r.out.isNotEmpty()) r.out[0].trim().uppercase() else null
            } catch (_: Exception) { null }
            dev.copy(file = path, size = size, fsType = fs)
        }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: closing client")
        reconnectJob?.cancel()
        synchronized(daemonLock) {
            client?.close()
            client = null
        }
    }
}

private fun normalizeMediaPath(path: String): String {
    if (path.startsWith("/data/media/")) {
        return "/storage/emulated/${path.removePrefix("/data/media/")}"
    }
    return path
}

private class AppFuseException(message: String) : Exception(message)
private class InvalidFileException(message: String) : Exception(message)

private fun ActiveDevice.toDeviceInfo(): DeviceInfo = DeviceInfo(
    Uri.parse("file://$file"),
    when {
        cdrom -> DeviceType.CDROM
        ro -> DeviceType.DISK_RO
        else -> DeviceType.DISK_RW
    }
)
