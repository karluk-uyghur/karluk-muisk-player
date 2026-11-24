package com.example.core.common

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.R
import com.example.domain.model.Song
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShareUtils {

    suspend fun shareApp(context: Context): Boolean {
        return try {
            val chooserTitle = context.getString(R.string.share_app_chooser_title)
            val shareText = context.getString(R.string.share_app_message)
            val apkUri = withContext(Dispatchers.IO) {
                prepareSharedApkUri(context)
            }

            val sendIntent = if (apkUri != null) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, apkUri)
                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
                    putExtra(Intent.EXTRA_TITLE, "Karluk_MP3_Pro_v${BuildConfig.VERSION_NAME}.apk")
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    clipData = ClipData.newRawUri(context.getString(R.string.app_name), apkUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
            }

            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                if (apkUri != null) {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
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

    private fun prepareSharedApkUri(context: Context): Uri? {
        return runCatching {
            val sourcePath = context.applicationInfo.publicSourceDir
                ?.takeIf { it.isNotBlank() }
                ?: context.applicationInfo.sourceDir
                ?.takeIf { it.isNotBlank() }
                ?: return null
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists() || !sourceFile.canRead()) return null

            val dir = File(context.cacheDir, "shared_apk").apply { if (!exists()) mkdirs() }
            val targetName = "Karluk_MP3_Pro_v${BuildConfig.VERSION_NAME}.apk"
            val destFile = File(dir, targetName)
            if (!destFile.exists() || destFile.length() != sourceFile.length()) {
                dir.listFiles()?.forEach { file ->
                    if (file.name != targetName) file.delete()
                }
                sourceFile.inputStream().use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
        }.getOrNull()
    }

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
