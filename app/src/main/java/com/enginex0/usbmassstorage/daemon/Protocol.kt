package com.enginex0.usbmassstorage.daemon

import android.net.Uri
import android.util.Log
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

private const val TAG = "MsdProto"

object MsdProtocol {
    const val PROTOCOL_VERSION: Int = 1
    const val SOCKET_NAME: String = "msdd"

    const val MSG_ERROR_RESPONSE: Int = 1
    const val MSG_GET_FUNCTIONS_REQUEST: Int = 2
    const val MSG_GET_FUNCTIONS_RESPONSE: Int = 3
    const val MSG_SET_MASS_STORAGE_REQUEST: Int = 4
    const val MSG_SET_MASS_STORAGE_RESPONSE: Int = 5
    const val MSG_GET_MASS_STORAGE_REQUEST: Int = 6
    const val MSG_GET_MASS_STORAGE_RESPONSE: Int = 7
}

fun InputStream.readU8(): Int {
    val b = read()
    if (b < 0) throw EOFException("Unexpected end of stream reading u8")
    Log.d(TAG, "readU8: $b")
    return b
}

fun InputStream.readU16LE(): Int {
    val low = read()
    if (low < 0) throw EOFException("Unexpected end of stream reading u16 low byte")
    val high = read()
    if (high < 0) throw EOFException("Unexpected end of stream reading u16 high byte")
    val value = low or (high shl 8)
    Log.d(TAG, "readU16LE: $value")
    return value
}

fun InputStream.readLengthPrefixed(): ByteArray {
    val size = readU16LE()
    val buf = ByteArray(size)
    var offset = 0
    while (offset < size) {
        val n = read(buf, offset, size - offset)
        if (n < 0) throw EOFException("Unexpected end of stream reading $size bytes of prefixed data")
        offset += n
    }
    return buf
}

fun InputStream.readLengthPrefixedString(): String {
    return String(readLengthPrefixed(), Charsets.UTF_8)
}

fun OutputStream.writeU8(value: Int) {
    write(value and 0xFF)
}

fun OutputStream.writeU16LE(value: Int) {
    write(value and 0xFF)
    write((value shr 8) and 0xFF)
}

fun OutputStream.writeLengthPrefixed(data: ByteArray) {
    require(data.size <= 0xFFFF) { "Data length ${data.size} exceeds u16 bounds" }
    writeU16LE(data.size)
    write(data)
}

fun OutputStream.writeLengthPrefixedString(s: String) {
    writeLengthPrefixed(s.toByteArray(Charsets.UTF_8))
}

data class UsbFunction(val config: String, val function: String)

data class ActiveDevice(val file: String, val cdrom: Boolean, val ro: Boolean, val size: Long = -1, val fsType: String? = null)

enum class DeviceType {
    DISK_RW,
    DISK_RO,
    CDROM,
}

data class DeviceInfo(val uri: Uri, val type: DeviceType, val fsType: String? = null)
