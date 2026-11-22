package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.database.ExcludedFolderEntity
import com.example.data.database.FavoriteEntity
import com.example.data.database.HistoryEntity
import com.example.data.database.KarlukDao
import com.example.data.database.LyricsEntity
import com.example.data.database.PlaylistEntity
import com.example.data.database.PlaylistSongCrossRef
import com.example.data.database.SafFolderEntity
import com.example.data.database.SearchHistoryEntity
import com.example.data.database.SongBackgroundEntity
import com.example.data.database.SongCustomTitleEntity
import com.example.data.database.SongEntity
import com.example.data.media.MediaScanner
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.FolderItem
import com.example.domain.model.LrcParser
import com.example.domain.model.LyricsData
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.domain.model.StorageStats
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val dao: KarlukDao,
    private val mediaScanner: MediaScanner
) {

    private val songOverridesFlow = combine(
        dao.observeSongBackgrounds(),
        dao.observeSongCustomTitles()
    ) { backgrounds, titles ->
        backgrounds.associateBy { it.songId } to titles.associateBy { it.songId }
    }

    val allSongsFlow: Flow<List<Song>> = combine(
        dao.observeAllSongs(),
        dao.observeFavorites(),
        dao.observeHistory(),
        dao.observeExcludedFolders(),
        songOverridesFlow
    ) { songs, favorites, history, excluded, overrides ->
        val favIds = favorites.map { it.songId }.toHashSet()
        val historyMap = history.associateBy { it.songId }
        val excludedPaths = excluded.map { it.folderPath }.toHashSet()
        val (bgMap, titleMap) = overrides

        songs
            .filter { it.folderPath !in excludedPaths }
            .map { entity ->
                val hist = historyMap[entity.id]
                entity.toDomain(
                    isFavorite = entity.id in favIds,
                    playCount = hist?.playCount ?: 0,
                    lastPlayedAt = hist?.lastPlayedAt ?: 0L,
                    customBackgroundUri = bgMap[entity.id]?.backgroundUri,
                    customTitle = titleMap[entity.id]?.customTitle
                )
            }
    }

    val favoriteSongsFlow: Flow<List<Song>> = combine(
        allSongsFlow,
        dao.observeFavorites()
    ) { songs, favs ->
        val favOrder = favs.mapIndexed { idx, f -> f.songId to idx }.toMap()
        songs.filter { it.id in favOrder }.sortedBy { favOrder[it.id] ?: Int.MAX_VALUE }
    }

    val historySongsFlow: Flow<List<Song>> = combine(
        allSongsFlow,
        dao.observeHistory()
    ) { songs, history ->
        val songMap = songs.associateBy { it.id }
        history.mapNotNull { h -> songMap[h.songId] }
    }

    val albumsFlow: Flow<List<Album>> = allSongsFlow.map { songs ->
        songs.groupBy { it.album }
            .map { (albumName, albumSongs) ->
                Album(
                    name = albumName,
                    artist = albumSongs.firstOrNull()?.artist ?: "Unknown Artist",
                    artworkUri = albumSongs.firstNotNullOfOrNull { it.artworkUri },
                    songCount = albumSongs.size,
                    totalDuration = albumSongs.sumOf { it.duration }
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val artistsFlow: Flow<List<Artist>> = allSongsFlow.map { songs ->
        songs.groupBy { it.artist }
            .map { (artistName, artistSongs) ->
                Artist(
                    name = artistName,
                    songCount = artistSongs.size,
                    albumCount = artistSongs.map { it.album }.distinct().size,
                    artworkUri = artistSongs.firstNotNullOfOrNull { it.artworkUri }
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val foldersFlow: Flow<List<FolderItem>> = allSongsFlow.map { songs ->
        songs.groupBy { it.folderPath }
            .map { (path, folderSongs) ->
                FolderItem(
                    path = path,
                    name = folderSongs.firstOrNull()?.folderName ?: path,
                    songCount = folderSongs.size,
                    isSaf = folderSongs.any { it.isSafTrack }
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val playlistsFlow: Flow<List<Playlist>> = combine(
        dao.observePlaylistsWithSongs(),
        dao.observeFavorites(),
        dao.observeHistory(),
        songOverridesFlow
    ) { list, favs, history, overrides ->
        val favIds = favs.map { it.songId }.toHashSet()
        val historyMap = history.associateBy { it.songId }
        val (bgMap, titleMap) = overrides
        list.map { item ->
            Playlist(
                id = item.playlist.id,
                name = item.playlist.name,
                createdAt = item.playlist.createdAt,
                songs = item.songs.map { s ->
                    val hist = historyMap[s.id]
                    s.toDomain(
                        isFavorite = s.id in favIds,
                        playCount = hist?.playCount ?: 0,
                        lastPlayedAt = hist?.lastPlayedAt ?: 0L,
                        customBackgroundUri = bgMap[s.id]?.backgroundUri,
                        customTitle = titleMap[s.id]?.customTitle
                    )
                }
            )
        }
    }

    val safFoldersFlow: Flow<List<SafFolderEntity>> = dao.observeSafFolders()
    val excludedFoldersFlow: Flow<List<ExcludedFolderEntity>> = dao.observeExcludedFolders()
    val searchHistoryFlow: Flow<List<String>> = dao.observeSearchHistory().map { list -> list.map { it.query } }

    val storageStatsFlow: Flow<StorageStats> = combine(
        allSongsFlow,
        safFoldersFlow
    ) { songs, safFolders ->
        val formats = songs.groupingBy { it.formatLabel }.eachCount()
        StorageStats(
            totalTracks = songs.size,
            totalSizeBytes = songs.sumOf { it.fileSize },
            artworkCacheBytes = mediaScanner.getArtworkCacheBytes(),
            formatCounts = formats,
            safFoldersCount = safFolders.size
        )
    }

    suspend fun rescanAllMedia(): Int {
        val mediaStoreSongs = mediaScanner.scanMediaStore()
        if (mediaStoreSongs.isNotEmpty()) {
            dao.upsertSongs(mediaStoreSongs)
            dao.deleteMissingMediaStoreSongs(mediaStoreSongs.map { it.id })
        }

        // Also refresh SAF folders that have persisted URI permissions
        val safFolders = dao.getSafFoldersOnce()
        var safCount = 0
        for (folder in safFolders) {
            val uri = Uri.parse(folder.treeUri)
            val result = mediaScanner.scanSafTree(uri, folder.displayName)
            if (result.songs.isNotEmpty()) {
                dao.upsertSongs(result.songs)
                safCount += result.songs.size
            }
            result.lyrics.forEach { dao.upsertLyrics(it) }
        }
        return mediaStoreSongs.size + safCount
    }

    suspend fun addSafFolder(treeUri: Uri): Int {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        val doc = DocumentFile.fromTreeUri(context, treeUri)
        val name = doc?.name ?: "Audio Directory"
        dao.insertSafFolder(SafFolderEntity(treeUri = treeUri.toString(), displayName = name))
        val result = mediaScanner.scanSafTree(treeUri, name)
        if (result.songs.isNotEmpty()) {
            dao.upsertSongs(result.songs)
        }
        result.lyrics.forEach { dao.upsertLyrics(it) }
        return result.songs.size
    }

    suspend fun removeSafFolder(treeUri: String) {
        dao.deleteSafFolder(treeUri)
        dao.deleteSafSongsInFolder(treeUri)
    }

    suspend fun importAudioFiles(uris: List<Uri>): List<Song> {
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
        }
        val entities = mediaScanner.importSafFiles(uris)
        if (entities.isNotEmpty()) {
            dao.upsertSongs(entities)
        }
        return entities.map { it.toDomain() }
    }

    suspend fun importSingleExternalUri(uri: Uri): Song? {
        val existing = dao.getAllSongsOnce().firstOrNull { it.uri == uri.toString() }
        if (existing != null) {
            return existing.toDomain(isFavorite = dao.isFavorite(existing.id))
        }
        val entity = mediaScanner.extractSongFromUri(uri) ?: return null
        dao.upsertSongs(listOf(entity))
        return entity.toDomain()
    }

    suspend fun toggleFavorite(songId: Long) {
        if (dao.isFavorite(songId)) {
            dao.deleteFavorite(songId)
        } else {
            dao.insertFavorite(FavoriteEntity(songId = songId))
        }
    }

    suspend fun setFavorites(songIds: List<Long>, favorite: Boolean) {
        songIds.forEach { id ->
            if (favorite) dao.insertFavorite(FavoriteEntity(songId = id))
            else dao.deleteFavorite(id)
        }
    }

    suspend fun recordPlayHistory(songId: Long) {
        val prev = dao.getHistoryForSong(songId)
        dao.upsertHistory(
            HistoryEntity(
                songId = songId,
                playCount = (prev?.playCount ?: 0) + 1,
                lastPlayedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun createPlaylist(name: String, initialSongIds: List<Long> = emptyList()): Long {
        val cleanName = name.trim().ifBlank { "Playlist" }
        val playlistId = dao.insertPlaylist(PlaylistEntity(name = cleanName))
        initialSongIds.forEach { songId ->
            dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = songId))
        }
        return playlistId
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        val clean = newName.trim()
        if (clean.isNotBlank()) dao.renamePlaylist(playlistId, clean)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        songIds.forEach { songId ->
            dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = songId))
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        dao.removeSongFromPlaylist(playlistId, songId)
    }

    fun observeLyrics(songId: Long): Flow<LyricsData?> {
        return dao.observeLyricsForSong(songId).map { entity ->
            entity?.let { LrcParser.parse(songId, it.rawContent) }
        }
    }

    suspend fun saveLyrics(songId: Long, rawContent: String) {
        if (rawContent.isBlank()) {
            dao.deleteLyrics(songId)
        } else {
            dao.upsertLyrics(LyricsEntity(songId = songId, rawContent = rawContent))
        }
    }

    suspend fun importLrcFileForSong(songId: Long, lrcUri: Uri): Boolean {
        val text = mediaScanner.readTextFromUri(lrcUri) ?: return false
        if (text.isBlank()) return false
        dao.upsertLyrics(LyricsEntity(songId = songId, rawContent = text))
        return true
    }

    suspend fun saveSearchQuery(query: String) {
        val clean = query.trim()
        if (clean.isNotBlank()) {
            dao.upsertSearchHistory(SearchHistoryEntity(query = clean))
        }
    }

    suspend fun deleteSearchQuery(query: String) {
        dao.deleteSearchQuery(query)
    }

    suspend fun clearSearchHistory() {
        dao.clearSearchHistory()
    }

    suspend fun excludeFolder(folderPath: String, folderName: String) {
        dao.insertExcludedFolder(ExcludedFolderEntity(folderPath = folderPath, folderName = folderName))
    }

    suspend fun restoreFolder(folderPath: String) {
        dao.deleteExcludedFolder(folderPath)
    }

    suspend fun removeSongFromLibrary(songId: Long) {
        clearSongBackground(songId)
        dao.deleteSongCustomTitle(songId)
        dao.deleteSongById(songId)
    }

    suspend fun renameSong(songId: Long, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isNotBlank()) {
            dao.upsertSongCustomTitle(SongCustomTitleEntity(songId = songId, customTitle = clean))
        }
    }

    suspend fun saveSongBackgroundFromUri(songId: Long, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        val bgDir = File(context.filesDir, "song_backgrounds").apply { if (!exists()) mkdirs() }
        val prefix = "song_${songId}_"
        val destFile = File(bgDir, "${prefix}${System.currentTimeMillis()}.jpg")
        val copied = try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } != null
        } catch (_: Exception) {
            false
        }
        if (!copied || !destFile.exists() || destFile.length() <= 0L) {
            destFile.delete()
            return@withContext null
        }
        bgDir.listFiles()?.forEach { file ->
            if (file.name.startsWith(prefix) && file.absolutePath != destFile.absolutePath) {
                file.delete()
            }
        }
        val savedUri = Uri.fromFile(destFile).toString()
        dao.upsertSongBackground(SongBackgroundEntity(songId = songId, backgroundUri = savedUri))
        savedUri
    }

    suspend fun clearSongBackground(songId: Long) = withContext(Dispatchers.IO) {
        val bgDir = File(context.filesDir, "song_backgrounds")
        val prefix = "song_${songId}_"
        if (bgDir.exists()) {
            bgDir.listFiles()?.forEach { file ->
                if (file.name.startsWith(prefix)) {
                    file.delete()
                }
            }
        }
        dao.deleteSongBackground(songId)
    }

    suspend fun getSongById(songId: Long): Song? {
        return dao.getSongById(songId)?.toDomain(
            isFavorite = dao.isFavorite(songId),
            customBackgroundUri = dao.getSongBackgroundOnce(songId)?.backgroundUri,
            customTitle = dao.getSongCustomTitleOnce(songId)?.customTitle
        )
    }

    fun clearArtworkCache() {
        mediaScanner.clearArtworkCache()
    }

    private fun SongEntity.toDomain(
        isFavorite: Boolean = false,
        playCount: Int = 0,
        lastPlayedAt: Long = 0L,
        customBackgroundUri: String? = null,
        customTitle: String? = null
    ): Song = Song(
        id = id,
        title = customTitle?.takeIf { it.isNotBlank() } ?: title,
        artist = artist,
        album = album,
        duration = duration,
        uri = uri,
        mimeType = mimeType,
        formatLabel = formatLabel,
        artworkUri = artworkUri,
        fileSize = fileSize,
        dateAdded = dateAdded,
        trackNumber = trackNumber,
        genre = genre,
        folderPath = folderPath,
        folderName = folderName,
        isSafTrack = isSafTrack,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        customBackgroundUri = customBackgroundUri
    )
}
