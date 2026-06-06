package de.pdfwerkzeugkasten.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.text.DecimalFormat

fun Context.displayName(uri: Uri): String = runCatching { contentResolver.query(uri, null, null, null, null)?.use { c -> val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (c.moveToFirst() && i >= 0) c.getString(i) else null } }.getOrNull() ?: uri.lastPathSegment ?: "dokument.pdf"
fun Context.sizeBytes(uri: Uri): Long = runCatching { contentResolver.query(uri, null, null, null, null)?.use { c -> val i = c.getColumnIndex(OpenableColumns.SIZE); if (c.moveToFirst() && i >= 0) c.getLong(i) else -1L } ?: -1L }.getOrDefault(-1L)
fun Long.humanSize(): String { if (this <= 0) return "—"; val units = listOf("B", "KB", "MB", "GB"); var v = toDouble(); var u = 0; while (v >= 1024 && u < units.lastIndex) { v /= 1024; u++ }; return DecimalFormat("#,##0.#").format(v) + " " + units[u] }
fun Context.shareable(uri: Uri): Uri = if (uri.scheme == "file") FileProvider.getUriForFile(this, "$packageName.fileprovider", File(uri.path!!)) else uri
