package com.enginex0.usbmassstorage.util

import android.net.Uri

private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

val Uri.formatted: String
    get() = when {
        scheme == "file" -> path ?: toString()

        scheme == "content" && authority == EXTERNAL_STORAGE_AUTHORITY -> {
            val docId = lastPathSegment
                ?.removePrefix("primary:")
                ?.replace(':', '/')
            docId ?: toString()
        }

        scheme == "content" -> "[$authority] ${lastPathSegment ?: path ?: ""}"

        else -> toString()
    }
