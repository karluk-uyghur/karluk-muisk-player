package com.example.data.media

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.example.data.database.LyricsEntity
import com.example.data.database.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

class MediaScanner(private val context: Context) {

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf(
            "mp3", "opus", "aac", "m4a", "flac", "wav", "ogg", "oga", "webm", "3gp", "amr", "mid", "midi"
        )
    }

    private val artworkCacheDir: File by lazy {
        File(context.cacheDir, "artwork_cache").apply { if (!exists()) mkdirs() }
    }

    fun getArtworkCacheBytes(): Long {
        return artworkCacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun clearArtworkCache() {
        artworkCacheDir.listFiles()?.forEach { it.delete() }
    }

    suspend fun scanMediaStore(): List<SongEntity> = withContext(Dispatchers.IO) {
        val requiredPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(context, requiredPerm) != PackageManager.PERMISSION_GRANTED) {
            return@withContext emptyList()
        }

        val songs = mutableListOf<SongEntity>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.IS_RINGTONE)
            add(MediaStore.Audio.Media.IS_NOTIFICATION)
            add(MediaStore.Audio.Media.IS_ALARM)
            @Suppress("DEPRECATION")
            add(MediaStore.Audio.Media.DATA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.GENRE)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val displayCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val ringtoneCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_RINGTONE)
                val notifCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_NOTIFICATION)
                val alarmCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_ALARM)
                @Suppress("DEPRECATION")
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1
                val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val displayName = cursor.getString(displayCol).orEmpty()
                    val mimeType = cursor.getString(mimeCol).orEmpty()
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)

                    val isRingtone = if (ringtoneCol >= 0) cursor.getInt(ringtoneCol) != 0 else false
                    val isNotif = if (notifCol >= 0) cursor.getInt(notifCol) != 0 else false
                    val isAlarm = if (alarmCol >= 0) cursor.getInt(alarmCol) != 0 else false
                    if (isRingtone || isNotif || isAlarm) continue

                    val rawPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    if (!rawPath.isNullOrBlank() && (
                            rawPath.startsWith("/system/") ||
                            rawPath.startsWith("/product/") ||
                            rawPath.startsWith("/vendor/") ||
                            rawPath.startsWith("/apex/")
                        )
                    ) {
                        continue
                    }

                    val ext = displayName.substringAfterLast('.', "").lowercase()
                    val isSupportedAudio = mimeType.startsWith("audio/") ||
                        mimeType == "application/ogg" ||
                        ext in SUPPORTED_EXTENSIONS

                    // Skip zero-byte files or tiny system sound effects
                    if (!isSupportedAudio || size <= 0L) continue

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val albumId = cursor.getLong(albumIdCol)
                    val artworkUri = if (albumId > 0) {
                        Uri.parse("content://media/external/audio/albumart/$albumId").toString()
                    } else null

                    val relPath = if (relPathCol >= 0) cursor.getString(relPathCol) else null
                    val folderPath = when {
                        !rawPath.isNullOrBlank() -> File(rawPath).parent ?: "Device"
                        !relPath.isNullOrBlank() -> relPath.trimEnd('/')
                        else -> "Audio"
                    }
                    val folderName = folderPath.substringAfterLast('/').ifBlank { "Audio" }

                    val rawTitle = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: displayName.substringBeforeLast('.')
                    val artist = cursor.getString(artistCol)
                        ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Unknown Artist"
                    val album = cursor.getString(albumCol)
                        ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Unknown Album"
                    val trackNum = cursor.getInt(trackCol).takeIf { it > 0 }
                    val genre = if (genreCol >= 0) cursor.getString(genreCol)?.takeIf { it.isNotBlank() } else null

                    val isFilteredMusicTrack = AudioContentFilter.isExcludedMusicTrack(
                        title = rawTitle,
                        artist = artist,
                        album = album,
                        genre = genre,
                        displayName = displayName,
                        folderPath = folderPath
                    )

                    songs.add(
                        SongEntity(
                            id = id,
                            title = rawTitle,
                            artist = artist,
                            album = album,
                            duration = duration.coerceAtLeast(0L),
                            uri = contentUri.toString(),
                            mimeType = mimeType.ifBlank { guessMimeType(ext) },
                            formatLabel = resolveFormatLabel(ext, mimeType),
                            artworkUri = artworkUri,
                            fileSize = size,
                            dateAdded = cursor.getLong(dateCol) * 1000L,
                            trackNumber = trackNum,
                            genre = genre,
                            folderPath = folderPath,
                            folderName = folderName,
                            isSafTrack = false,
                            isFilteredMusic = isFilteredMusicTrack,
                            isUserApprovedNasheed = false
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted; SAF tracks will still function seamlessly
        } catch (_: Exception) {
        }

        songs
    }

    data class SafScanResult(
        val songs: List<SongEntity>,
        val lyrics: List<LyricsEntity>
    )

    suspend fun scanSafTree(treeUri: Uri, folderDisplayName: String): SafScanResult = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext SafScanResult(emptyList(), emptyList())

        val collectedSongs = mutableListOf<SongEntity>()
        val collectedLyrics = mutableListOf<LyricsEntity>()
        val lrcFilesByBaseName = mutableMapOf<String, DocumentFile>()
        val audioDocs = mutableListOf<Pair<DocumentFile, String>>()

        fun traverse(dir: DocumentFile, currentFolderName: String) {
            val children = try {
                dir.listFiles()
            } catch (_: Exception) {
                emptyArray()
            }
            children.forEach { child ->
                if (child.isDirectory) {
                    traverse(child, child.name ?: currentFolderName)
                } else if (child.isFile) {
                    val name = child.name ?: return@forEach
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val mime = child.type.orEmpty()
                    if (ext == "lrc") {
                        lrcFilesByBaseName[name.substringBeforeLast('.').lowercase()] = child
                    } else if (ext in SUPPORTED_EXTENSIONS || mime.startsWith("audio/") || mime == "application/ogg") {
                        audioDocs.add(child to currentFolderName)
                    }
                }
            }
        }

        traverse(root, folderDisplayName)

        audioDocs.forEach { (doc, subFolderName) ->
            val song = extractSongFromUri(
                uri = doc.uri,
                fallbackName = doc.name,
                fallbackSize = doc.length(),
                folderPath = treeUri.toString(),
                folderName = subFolderName,
                filterMusicTracks = true
            )
            if (song != null) {
                collectedSongs.add(song)
                val baseName = (doc.name ?: song.title).substringBeforeLast('.').lowercase()
                val lrcDoc = lrcFilesByBaseName[baseName]
                if (lrcDoc != null) {
                    val lrcText = readTextFromUri(lrcDoc.uri)
                    if (!lrcText.isNullOrBlank()) {
                        collectedLyrics.add(LyricsEntity(songId = song.id, rawContent = lrcText))
                    }
                }
            }
        }

        SafScanResult(collectedSongs, collectedLyrics)
    }

    data class SafFileImportResult(
        val songs: List<SongEntity>,
        val skippedMusicCount: Int
    )

    suspend fun importSafFiles(uris: List<Uri>): SafFileImportResult = withContext(Dispatchers.IO) {
        val imported = uris.mapNotNull { uri ->
            extractSongFromUri(
                uri = uri,
                fallbackName = null,
                fallbackSize = 0L,
                folderPath = "saf_imports",
                folderName = "Imported Files",
                filterMusicTracks = true
            )
        }
        val filteredCount = imported.count { it.isFilteredMusic }
        SafFileImportResult(songs = imported, skippedMusicCount = filteredCount)
    }

    fun extractSongFromUri(
        uri: Uri,
        fallbackName: String? = null,
        fallbackSize: Long = 0L,
        folderPath: String = "external",
        folderName: String = "External",
        filterMusicTracks: Boolean = true
    ): SongEntity? {
        val uriString = uri.toString()
        val id = -abs(uriString.hashCode().toLong()).let { if (it == 0L) -1L else it }

        var displayName = fallbackName ?: ""
        var fileSize = fallbackSize
        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0 && displayName.isBlank()) {
                        displayName = c.getString(nameIdx).orEmpty()
                    }
                    if (sizeIdx >= 0 && fileSize <= 0L) {
                        fileSize = c.getLong(sizeIdx)
                    }
                }
            }
        } catch (_: Exception) {
        }

        val ext = displayName.substringAfterLast('.', "").lowercase()
        val resolvedMime = context.contentResolver.getType(uri).orEmpty().ifBlank { guessMimeType(ext) }

        val retriever = MediaMetadataRetriever()
        var title = displayName.substringBeforeLast('.').ifBlank { "Nasheed" }
        var artist = "Unknown Artist"
        var album = "Unknown Album"
        var duration = 0L
        var trackNum: Int? = null
        var genre: String? = null
        var artworkUri: String? = null

        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }?.let { artist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }?.let { album = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.let { duration = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.toIntOrNull()?.let { trackNum = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.takeIf { it.isNotBlank() }?.let { genre = it }

            val artBytes = retriever.embeddedPicture
            if (artBytes != null && artBytes.isNotEmpty()) {
                val artFile = File(artworkCacheDir, "art_${abs(id)}.jpg")
                FileOutputStream(artFile).use { it.write(artBytes) }
                artworkUri = Uri.fromFile(artFile).toString()
            }
        } catch (_: Exception) {
            // Still allow ExoPlayer to attempt playback even if MediaMetadataRetriever can't read exotic container tags
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        val isFilteredMusicTrack = filterMusicTracks && AudioContentFilter.isExcludedMusicTrack(
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            displayName = displayName,
            folderPath = folderPath
        )

        return SongEntity(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            uri = uriString,
            mimeType = resolvedMime,
            formatLabel = resolveFormatLabel(ext, resolvedMime),
            artworkUri = artworkUri,
            fileSize = fileSize,
            dateAdded = System.currentTimeMillis(),
            trackNumber = trackNum,
            genre = genre,
            folderPath = folderPath,
            folderName = folderName,
            isSafTrack = true,
            isFilteredMusic = isFilteredMusicTrack,
            isUserApprovedNasheed = false
        )
    }

    fun readTextFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveFormatLabel(ext: String, mime: String): String {
        val cleanExt = ext.uppercase()
        if (cleanExt in setOf("MP3", "OPUS", "AAC", "M4A", "FLAC", "WAV", "OGG", "OGA", "WEBM", "AMR", "3GP")) {
            return if (cleanExt == "OGA") "OGG" else cleanExt
        }
        return when {
            mime.contains("mpeg", true) || mime.contains("mp3", true) -> "MP3"
            mime.contains("opus", true) -> "OPUS"
            mime.contains("flac", true) -> "FLAC"
            mime.contains("mp4", true) || mime.contains("m4a", true) -> "M4A"
            mime.contains("aac", true) -> "AAC"
            mime.contains("wav", true) || mime.contains("wave", true) -> "WAV"
            mime.contains("ogg", true) || mime.contains("vorbis", true) -> "OGG"
            else -> "AUDIO"
        }
    }

    private fun guessMimeType(ext: String): String = when (ext) {
        "mp3" -> "audio/mpeg"
        "opus" -> "audio/opus"
        "aac" -> "audio/aac"
        "m4a" -> "audio/mp4"
        "flac" -> "audio/flac"
        "wav" -> "audio/wav"
        "ogg", "oga" -> "audio/ogg"
        "webm" -> "audio/webm"
        else -> "audio/*"
    }
}
