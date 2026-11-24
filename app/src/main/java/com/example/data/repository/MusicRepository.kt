package com.example.data.repository

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import com.example.data.database.TrashItemEntity
import com.example.data.media.AudioContentFilter
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

data class PermanentDeletePlan(
    val intentSender: IntentSender?,
    val itemsToFinalize: List<TrashItemEntity>,
    val deletedCount: Int
)

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

    private val hiddenFiltersFlow = combine(
        dao.observeExcludedFolders(),
        dao.observeTrashedSongIds()
    ) { excluded, trashedIds ->
        excluded.map { it.folderPath }.toHashSet() to trashedIds.toHashSet()
    }

    val allSongsFlow: Flow<List<Song>> = combine(
        dao.observeAllSongs(),
        dao.observeFavorites(),
        dao.observeHistory(),
        hiddenFiltersFlow,
        songOverridesFlow
    ) { songs, favorites, history, hiddenFilters, overrides ->
        val favIds = favorites.map { it.songId }.toHashSet()
        val historyMap = history.associateBy { it.songId }
        val (excludedPaths, trashedSongIds) = hiddenFilters
        val (bgMap, titleMap) = overrides

        songs
            .filter { !it.isFilteredMusic && it.folderPath !in excludedPaths && it.id !in trashedSongIds }
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

    val filteredMusicSongsFlow: Flow<List<Song>> = combine(
        dao.observeAllSongs(),
        dao.observeFavorites(),
        dao.observeHistory(),
        hiddenFiltersFlow,
        songOverridesFlow
    ) { songs, favorites, history, hiddenFilters, overrides ->
        val favIds = favorites.map { it.songId }.toHashSet()
        val historyMap = history.associateBy { it.songId }
        val (excludedPaths, trashedSongIds) = hiddenFilters
        val (bgMap, titleMap) = overrides

        songs
            .filter { it.isFilteredMusic && it.folderPath !in excludedPaths && it.id !in trashedSongIds }
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
        hiddenFiltersFlow,
        songOverridesFlow
    ) { list, favs, history, hiddenFilters, overrides ->
        val favIds = favs.map { it.songId }.toHashSet()
        val historyMap = history.associateBy { it.songId }
        val (excludedPaths, trashedSongIds) = hiddenFilters
        val (bgMap, titleMap) = overrides
        list.map { item ->
            Playlist(
                id = item.playlist.id,
                name = item.playlist.name,
                createdAt = item.playlist.createdAt,
                songs = item.songs
                    .filter { !it.isFilteredMusic && it.folderPath !in excludedPaths && it.id !in trashedSongIds }
                    .map { s ->
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
    val trashItemsFlow: Flow<List<TrashItemEntity>> = dao.observeTrashItems()
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
        val approvedIds = dao.getUserApprovedSongIdsOnce().toHashSet()
        val mediaStoreSongs = mediaScanner.scanMediaStore().map { entity ->
            if (entity.id in approvedIds) {
                entity.copy(isFilteredMusic = false, isUserApprovedNasheed = true)
            } else {
                entity
            }
        }
        if (mediaStoreSongs.isNotEmpty()) {
            dao.upsertSongs(mediaStoreSongs)
            dao.deleteMissingMediaStoreSongs(mediaStoreSongs.map { it.id })
        } else {
            dao.deleteAllMediaStoreSongs()
        }

        // Also refresh SAF folders that have persisted URI permissions
        val safFolders = dao.getSafFoldersOnce()
        for (folder in safFolders) {
            runCatching {
                val uri = Uri.parse(folder.treeUri)
                val result = mediaScanner.scanSafTree(uri, folder.displayName)
                val resolvedSafSongs = result.songs.map { entity ->
                    if (entity.id in approvedIds) {
                        entity.copy(isFilteredMusic = false, isUserApprovedNasheed = true)
                    } else {
                        entity
                    }
                }
                if (resolvedSafSongs.isNotEmpty()) {
                    dao.upsertSongs(resolvedSafSongs)
                }
                result.lyrics.forEach { dao.upsertLyrics(it) }
            }
        }

        // Route any unapproved stored tracks matching DJ/instrumental/music rules into Filtered Music
        val remainingStored = dao.getAllSongsOnce()
        for (song in remainingStored) {
            if (!song.isUserApprovedNasheed && !song.isFilteredMusic && AudioContentFilter.isExcludedMusicTrack(
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    genre = song.genre,
                    displayName = song.title,
                    folderPath = song.folderPath
                )
            ) {
                dao.markSongAsFilteredMusic(song.id)
            }
        }
        return dao.getAllSongsOnce().count { !it.isFilteredMusic }
    }

    suspend fun addSafFolder(treeUri: Uri): Int {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        val approvedIds = dao.getUserApprovedSongIdsOnce().toHashSet()
        val doc = DocumentFile.fromTreeUri(context, treeUri)
        val name = doc?.name ?: "Audio Directory"
        dao.insertSafFolder(SafFolderEntity(treeUri = treeUri.toString(), displayName = name))
        dao.deleteExcludedFolder(treeUri.toString())
        val result = mediaScanner.scanSafTree(treeUri, name)
        val resolvedSongs = result.songs.map { entity ->
            if (entity.id in approvedIds) {
                entity.copy(isFilteredMusic = false, isUserApprovedNasheed = true)
            } else {
                entity
            }
        }
        if (resolvedSongs.isNotEmpty()) {
            dao.upsertSongs(resolvedSongs)
        }
        result.lyrics.forEach { dao.upsertLyrics(it) }
        return resolvedSongs.count { !it.isFilteredMusic }
    }

    suspend fun removeSafFolder(treeUri: String): List<Long> {
        val safFolder = dao.getSafFoldersOnce().firstOrNull { it.treeUri == treeUri }
        val songsInFolder = dao.getSongsInFolderOnce(treeUri)
        val alreadyTrashed = dao.getTrashItemsOnce().any { it.itemType == "FOLDER" && it.folderPath == treeUri }
        if (!alreadyTrashed) {
            dao.insertTrashItem(
                TrashItemEntity(
                    itemType = "FOLDER",
                    refId = -1L,
                    title = safFolder?.displayName ?: "SAF Directory",
                    subtitle = songsInFolder.size.toString(),
                    uri = treeUri,
                    folderPath = treeUri,
                    folderName = safFolder?.displayName ?: "SAF Directory",
                    isSafTrack = true,
                    fileSize = songsInFolder.sumOf { it.fileSize }
                )
            )
        }
        dao.insertExcludedFolder(
            ExcludedFolderEntity(
                folderPath = treeUri,
                folderName = safFolder?.displayName ?: "SAF Directory"
            )
        )
        return songsInFolder.map { it.id }
    }

    data class ImportedFilesOutcome(
        val songs: List<Song>,
        val skippedMusicCount: Int
    )

    suspend fun importAudioFiles(uris: List<Uri>): ImportedFilesOutcome {
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
        }
        val approvedIds = dao.getUserApprovedSongIdsOnce().toHashSet()
        val result = mediaScanner.importSafFiles(uris)
        val resolvedEntities = result.songs.map { entity ->
            if (entity.id in approvedIds) {
                entity.copy(isFilteredMusic = false, isUserApprovedNasheed = true)
            } else {
                entity
            }
        }
        if (resolvedEntities.isNotEmpty()) {
            dao.upsertSongs(resolvedEntities)
        }
        val normalNasheeds = resolvedEntities.filter { !it.isFilteredMusic }.map { it.toDomain() }
        val filteredCount = resolvedEntities.count { it.isFilteredMusic }
        return ImportedFilesOutcome(
            songs = normalNasheeds,
            skippedMusicCount = filteredCount
        )
    }

    suspend fun importSingleExternalUri(uri: Uri): Song? {
        val existing = dao.getAllSongsOnce().firstOrNull { it.uri == uri.toString() }
        if (existing != null) {
            if (!existing.isUserApprovedNasheed && AudioContentFilter.isExcludedMusicTrack(
                    title = existing.title,
                    artist = existing.artist,
                    album = existing.album,
                    genre = existing.genre,
                    folderPath = existing.folderPath
                )
            ) {
                if (!existing.isFilteredMusic) {
                    dao.markSongAsFilteredMusic(existing.id)
                }
                return null
            }
            return existing.toDomain(isFavorite = dao.isFavorite(existing.id))
        }
        val entity = mediaScanner.extractSongFromUri(uri, filterMusicTracks = true) ?: return null
        dao.upsertSongs(listOf(entity))
        return if (entity.isFilteredMusic) null else entity.toDomain()
    }

    suspend fun approveFilteredSongsAsNasheed(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        dao.approveFilteredSongsAsNasheed(songIds.distinct())
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

    suspend fun moveRecentSong(songId: Long, delta: Int) {
        val trashedIds = dao.getTrashItemsOnce().filter { it.itemType == "SONG" }.map { it.refId }.toHashSet()
        val excludedPaths = dao.getAllSongsOnce().let { _ -> emptySet<String>() }
        val visibleSongIds = allSongsForOrdering()
        val currentHistory = dao.getHistoryOnce()
            .filter { it.songId !in trashedIds && it.songId in visibleSongIds }
            .toMutableList()
        if (currentHistory.size < 2) return

        // Ensure strictly distinct descending timestamps so swapping positions is deterministic
        val baseTime = System.currentTimeMillis()
        currentHistory.forEachIndexed { idx, entry ->
            val normalizedTime = baseTime - idx * 1000L
            if (entry.lastPlayedAt != normalizedTime) {
                currentHistory[idx] = entry.copy(lastPlayedAt = normalizedTime)
                dao.updateHistoryTimestamp(entry.songId, normalizedTime)
            }
        }

        val currentIdx = currentHistory.indexOfFirst { it.songId == songId }
        if (currentIdx < 0) return
        val targetIdx = (currentIdx + delta).coerceIn(0, currentHistory.lastIndex)
        if (targetIdx == currentIdx) return

        val entryA = currentHistory[currentIdx]
        val entryB = currentHistory[targetIdx]
        dao.updateHistoryTimestamp(entryA.songId, entryB.lastPlayedAt)
        dao.updateHistoryTimestamp(entryB.songId, entryA.lastPlayedAt)
    }

    private suspend fun allSongsForOrdering(): Set<Long> {
        return dao.getAllSongsOnce().map { it.id }.toHashSet()
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun removeSongFromHistory(songId: Long) {
        dao.deleteHistoryForSong(songId)
    }

    suspend fun removeSongsFromHistory(songIds: List<Long>) {
        songIds.forEach { dao.deleteHistoryForSong(it) }
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

    suspend fun excludeFolder(folderPath: String, folderName: String): List<Long> {
        dao.insertExcludedFolder(ExcludedFolderEntity(folderPath = folderPath, folderName = folderName))
        val songsInFolder = dao.getSongsInFolderOnce(folderPath)
        val alreadyTrashed = dao.getTrashItemsOnce().any { it.itemType == "FOLDER" && it.folderPath == folderPath }
        if (!alreadyTrashed) {
            dao.insertTrashItem(
                TrashItemEntity(
                    itemType = "FOLDER",
                    refId = -1L,
                    title = folderName,
                    subtitle = songsInFolder.size.toString(),
                    uri = folderPath,
                    folderPath = folderPath,
                    folderName = folderName,
                    isSafTrack = songsInFolder.any { it.isSafTrack },
                    fileSize = songsInFolder.sumOf { it.fileSize }
                )
            )
        }
        return songsInFolder.map { it.id }
    }

    suspend fun restoreFolder(folderPath: String) {
        dao.deleteExcludedFolder(folderPath)
        dao.getTrashItemsOnce()
            .filter { it.itemType == "FOLDER" && it.folderPath == folderPath }
            .forEach { dao.deleteTrashItem(it.trashId) }
    }

    suspend fun removeSongFromLibrary(songId: Long) {
        removeSongsFromLibrary(listOf(songId))
    }

    suspend fun removeSongsFromLibrary(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        val existingTrashedIds = dao.getTrashItemsOnce()
            .asSequence()
            .filter { it.itemType == "SONG" }
            .map { it.refId }
            .toHashSet()
        for (songId in songIds.distinct()) {
            if (songId in existingTrashedIds) continue
            val entity = dao.getSongById(songId) ?: continue
            val customTitle = dao.getSongCustomTitleOnce(songId)?.customTitle
            dao.insertTrashItem(
                TrashItemEntity(
                    itemType = "SONG",
                    refId = entity.id,
                    title = customTitle?.takeIf { it.isNotBlank() } ?: entity.title,
                    subtitle = entity.artist,
                    uri = entity.uri,
                    folderPath = entity.folderPath,
                    folderName = entity.folderName,
                    isSafTrack = entity.isSafTrack,
                    formatLabel = entity.formatLabel,
                    fileSize = entity.fileSize,
                    duration = entity.duration
                )
            )
            existingTrashedIds.add(songId)
        }
    }

    suspend fun restoreTrashItem(item: TrashItemEntity) {
        if (item.itemType == "FOLDER") {
            dao.deleteExcludedFolder(item.folderPath)
        }
        dao.deleteTrashItem(item.trashId)
    }

    suspend fun preparePermanentDelete(items: List<TrashItemEntity>): PermanentDeletePlan =
        withContext(Dispatchers.IO) {
            if (items.isEmpty()) {
                return@withContext PermanentDeletePlan(null, emptyList(), 0)
            }

            val mediaStoreUris = mutableListOf<Uri>()
            val safOrFileUris = mutableListOf<Uri>()
            val safTreeUris = mutableListOf<Uri>()

            for (item in items) {
                if (item.itemType == "SONG") {
                    val uri = runCatching { Uri.parse(item.uri) }.getOrNull()
                    if (uri != null && !uri.toString().isNullOrBlank()) {
                        if (!item.isSafTrack && item.uri.startsWith("content://media")) {
                            mediaStoreUris.add(uri)
                        } else {
                            safOrFileUris.add(uri)
                        }
                    }
                } else if (item.itemType == "FOLDER") {
                    val folderSongs = runCatching { dao.getSongsInFolderOnce(item.folderPath) }.getOrDefault(emptyList())
                    for (song in folderSongs) {
                        val sUri = runCatching { Uri.parse(song.uri) }.getOrNull() ?: continue
                        if (!song.isSafTrack && song.uri.startsWith("content://media")) {
                            mediaStoreUris.add(sUri)
                        } else {
                            safOrFileUris.add(sUri)
                        }
                    }
                    if (item.isSafTrack && item.uri.startsWith("content://")) {
                        runCatching { Uri.parse(item.uri) }.getOrNull()?.let { safTreeUris.add(it) }
                    }
                }
            }

            // Delete SAF / local files directly where write access is available
            var directDeletedCount = 0
            for (uri in safOrFileUris) {
                val ok = deleteNonMediaStoreUri(uri)
                if (ok) directDeletedCount++
            }
            for (treeUri in safTreeUris) {
                runCatching {
                    DocumentFile.fromTreeUri(context, treeUri)?.delete()
                }
            }

            // Verify MediaStore URIs actually exist before requesting system confirmation
            val validMediaUris = mediaStoreUris.distinct().filter { uriExistsInMediaStore(it) }
            if (validMediaUris.isNotEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, validMediaUris)
                        return@withContext PermanentDeletePlan(
                            intentSender = pendingIntent.intentSender,
                            itemsToFinalize = items,
                            deletedCount = directDeletedCount
                        )
                    } catch (_: Throwable) {
                        // Fall through to best-effort finalize if MediaStore rejects batch delete
                    }
                } else {
                    for (uri in validMediaUris) {
                        try {
                            val rows = context.contentResolver.delete(uri, null, null)
                            if (rows > 0) directDeletedCount++
                        } catch (sec: SecurityException) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && sec is RecoverableSecurityException) {
                                return@withContext PermanentDeletePlan(
                                    intentSender = sec.userAction.actionIntent.intentSender,
                                    itemsToFinalize = items,
                                    deletedCount = directDeletedCount
                                )
                            }
                        } catch (_: Throwable) {
                        }
                    }
                }
            }

            // No user confirmation prompt needed -> finalize DB removal right away
            finalizePermanentDelete(items)
            PermanentDeletePlan(
                intentSender = null,
                itemsToFinalize = emptyList(),
                deletedCount = items.size
            )
        }

    private fun uriExistsInMediaStore(uri: Uri): Boolean {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns._ID),
                null,
                null,
                null
            )?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun finalizePermanentDelete(items: List<TrashItemEntity>) = withContext(Dispatchers.IO) {
        for (item in items) {
            runCatching {
                if (item.itemType == "SONG") {
                    val songId = item.refId
                    clearSongBackground(songId)
                    dao.deleteSongCustomTitle(songId)
                    dao.deleteFavorite(songId)
                    dao.deleteHistoryForSong(songId)
                    dao.deleteLyrics(songId)
                    dao.removeSongFromAllPlaylists(songId)
                    dao.deleteSongById(songId)
                } else if (item.itemType == "FOLDER") {
                    val songs = dao.getSongsInFolderOnce(item.folderPath)
                    songs.forEach { song ->
                        clearSongBackground(song.id)
                        dao.deleteSongCustomTitle(song.id)
                        dao.deleteFavorite(song.id)
                        dao.deleteHistoryForSong(song.id)
                        dao.deleteLyrics(song.id)
                        dao.removeSongFromAllPlaylists(song.id)
                    }
                    dao.deleteSongsInFolder(item.folderPath)
                    dao.deleteExcludedFolder(item.folderPath)
                    dao.deleteSafFolder(item.folderPath)
                }
            }
            runCatching { dao.deleteTrashItem(item.trashId) }
        }
    }

    private fun deleteNonMediaStoreUri(uri: Uri): Boolean {
        return try {
            when (uri.scheme) {
                "file" -> {
                    val path = uri.path ?: return false
                    File(path).delete()
                }
                "content" -> {
                    val doc = DocumentFile.fromSingleUri(context, uri)
                    if (doc != null && doc.exists()) {
                        doc.delete()
                    } else {
                        context.contentResolver.delete(uri, null, null) > 0
                    }
                }
                else -> false
            }
        } catch (_: Throwable) {
            false
        }
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
        customBackgroundUri = customBackgroundUri,
        isFilteredMusic = isFilteredMusic,
        isUserApprovedNasheed = isUserApprovedNasheed
    )
}
