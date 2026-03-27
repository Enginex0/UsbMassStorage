package com.enginex0.usbmassstorage.daemon

import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import java.io.Closeable
import java.io.FileDescriptor
import java.io.IOException

private const val TAG = "MsdClient"

class DaemonException(message: String, cause: Throwable? = null) : Exception(message, cause)

class DaemonClient : Closeable {
    private val socket = LocalSocket()
    private val input get() = socket.inputStream
    private val output get() = socket.outputStream

    init {
        try {
            Log.d(TAG, "Connecting to abstract socket '${MsdProtocol.SOCKET_NAME}'")
            socket.connect(
                LocalSocketAddress(MsdProtocol.SOCKET_NAME, LocalSocketAddress.Namespace.ABSTRACT)
            )
            socket.soTimeout = 5_000
            Log.d(TAG, "Connected, starting version handshake")
            handshake()
            Log.d(TAG, "Handshake complete")
        } catch (e: IOException) {
            Log.e(TAG, "Connection failed", e)
            socket.close()
            throw IOException("Failed to connect to MSD daemon", e)
        }
    }

    private fun handshake() {
        output.writeU8(MsdProtocol.PROTOCOL_VERSION)
        output.flush()
        Log.d(TAG, "Sent protocol version ${MsdProtocol.PROTOCOL_VERSION}")

        val ack = input.readU8()
        Log.d(TAG, "Received handshake ack: $ack")
        when (ack) {
            1 -> {}
            0 -> throw IOException(
                "Daemon rejected protocol version ${MsdProtocol.PROTOCOL_VERSION}"
            )
            else -> throw IOException("Invalid handshake ack: $ack")
        }
    }

    private fun readResponseId(): Int {
        val id = input.readU8()
        Log.d(TAG, "Response message ID: $id")
        if (id == MsdProtocol.MSG_ERROR_RESPONSE) {
            val errorMsg = input.readLengthPrefixedString()
            Log.e(TAG, "Daemon error: $errorMsg")
            throw DaemonException(errorMsg)
        }
        return id
    }

    fun getFunctions(): List<UsbFunction> {
        Log.d(TAG, "Sending GetFunctionsRequest")
        output.writeU8(MsdProtocol.MSG_GET_FUNCTIONS_REQUEST)
        output.flush()

        val id = readResponseId()
        if (id != MsdProtocol.MSG_GET_FUNCTIONS_RESPONSE) {
            throw IOException("Expected GetFunctionsResponse (${MsdProtocol.MSG_GET_FUNCTIONS_RESPONSE}), got $id")
        }

        val count = input.readU8()
        Log.d(TAG, "GetFunctionsResponse: $count functions")
        val functions = ArrayList<UsbFunction>(count)
        for (i in 0 until count) {
            val config = input.readLengthPrefixedString()
            val function = input.readLengthPrefixedString()
            functions.add(UsbFunction(config, function))
        }
        return functions
    }

    fun setMassStorage(devices: List<Pair<FileDescriptor, DeviceType>>) {
        Log.d(TAG, "Sending SetMassStorageRequest with ${devices.size} devices")
        output.writeU8(MsdProtocol.MSG_SET_MASS_STORAGE_REQUEST)
        output.writeU8(devices.size)

        for ((index, pair) in devices.withIndex()) {
            val (fd, type) = pair
            val cdrom = type == DeviceType.CDROM
            val ro = type != DeviceType.DISK_RW

            Log.d(TAG, "Sending FD for device $index (cdrom=$cdrom, ro=$ro)")
            socket.setFileDescriptorsForSend(arrayOf(fd))
            output.writeU8(0)
            output.flush()

            output.writeU8(if (cdrom) 1 else 0)
            output.writeU8(if (ro) 1 else 0)
        }
        output.flush()

        val id = readResponseId()
        if (id != MsdProtocol.MSG_SET_MASS_STORAGE_RESPONSE) {
            throw IOException("Expected SetMassStorageResponse (${MsdProtocol.MSG_SET_MASS_STORAGE_RESPONSE}), got $id")
        }
        Log.d(TAG, "SetMassStorageResponse received")
    }

    fun getMassStorage(): List<ActiveDevice> {
        Log.d(TAG, "Sending GetMassStorageRequest")
        output.writeU8(MsdProtocol.MSG_GET_MASS_STORAGE_REQUEST)
        output.flush()

        val id = readResponseId()
        if (id != MsdProtocol.MSG_GET_MASS_STORAGE_RESPONSE) {
            throw IOException("Expected GetMassStorageResponse (${MsdProtocol.MSG_GET_MASS_STORAGE_RESPONSE}), got $id")
        }

        val count = input.readU8()
        Log.d(TAG, "GetMassStorageResponse: $count devices")
        val devices = ArrayList<ActiveDevice>(count)
        for (i in 0 until count) {
            val file = input.readLengthPrefixedString()
            val cdrom = input.readU8() != 0
            val ro = input.readU8() != 0
            devices.add(ActiveDevice(file, cdrom, ro))
        }
        return devices
    }

    override fun close() {
        Log.d(TAG, "Closing daemon client")
        socket.close()
    }
}
