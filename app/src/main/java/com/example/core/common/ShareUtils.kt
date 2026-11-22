package com.example.core.common

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.domain.model.Song
import java.io.File
import java.io.FileOutputStream

object ShareUtils {
    fun shareSong(context: Context, song: Song): Boolean {
        return try {
            val rawUri = Uri.parse(song.uri)
            val mimeType = song.mimeType.ifBlank { "audio/*" }
            val shareUri: Uri = when (rawUri.scheme?.lowercase()) {
                "content" -> rawUri
                "file" -> {
                    val sourceFile = rawUri.path?.let { File(it) }
                    if (sourceFile == null || !sourceFile.exists()) return false
                    copyToSharedCache(context, song, sourceFile)
                }
                else -> {
                    // Fallback: try copying via ContentResolver stream into shared_audio cache
                    copyStreamToSharedCache(context, song, rawUri) ?: rawUri
                }
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_SUBJECT, song.title)
                putExtra(Intent.EXTRA_TEXT, song.title)
                clipData = ClipData.newRawUri(song.title, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(
                sendIntent,
                context.getString(R.string.action_share)
            ).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.share_failed),
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    private fun copyToSharedCache(context: Context, song: Song, sourceFile: File): Uri {
        val dir = File(context.cacheDir, "shared_audio").apply { if (!exists()) mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val ext = sourceFile.extension.ifBlank { song.formatLabel.lowercase().ifBlank { "mp3" } }
        val safeTitle = song.title.replace(Regex("[^\\p{L}\\p{N}._ -]"), "_").take(48).ifBlank { "nasheed" }
        val destFile = File(dir, "$safeTitle.$ext")
        sourceFile.inputStream().use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
    }

    private fun copyStreamToSharedCache(context: Context, song: Song, rawUri: Uri): Uri? {
        val dir = File(context.cacheDir, "shared_audio").apply { if (!exists()) mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val ext = song.formatLabel.lowercase().ifBlank { "mp3" }
        val safeTitle = song.title.replace(Regex("[^\\p{L}\\p{N}._ -]"), "_").take(48).ifBlank { "nasheed" }
        val destFile = File(dir, "$safeTitle.$ext")
        val copied = context.contentResolver.openInputStream(rawUri)?.use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
        } != null
        return if (copied && destFile.exists()) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
        } else {
            null
        }
    }
}
