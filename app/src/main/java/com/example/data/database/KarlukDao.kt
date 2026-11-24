package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface KarlukDao {

    // Songs
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsOnce(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Long): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE isSafTrack = 0 AND id NOT IN (:validIds)")
    suspend fun deleteMissingMediaStoreSongs(validIds: List<Long>)

    @Query("DELETE FROM songs WHERE isSafTrack = 0")
    suspend fun deleteAllMediaStoreSongs()

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSongById(songId: Long)

    @Query("DELETE FROM songs WHERE folderPath = :folderPath AND isSafTrack = 1")
    suspend fun deleteSafSongsInFolder(folderPath: String)

    @Query("SELECT id FROM songs WHERE isUserApprovedNasheed = 1")
    suspend fun getUserApprovedSongIdsOnce(): List<Long>

    @Query("UPDATE songs SET isFilteredMusic = 0, isUserApprovedNasheed = 1 WHERE id IN (:songIds)")
    suspend fun approveFilteredSongsAsNasheed(songIds: List<Long>)

    @Query("UPDATE songs SET isFilteredMusic = 1 WHERE id = :songId AND isUserApprovedNasheed = 0")
    suspend fun markSongAsFilteredMusic(songId: Long)

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun deleteFavorite(songId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    suspend fun isFavorite(songId: Long): Boolean

    // Playlists
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylistsWithSongs(): Flow<List<PlaylistWithSongsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    // History
    @Query("SELECT * FROM history ORDER BY lastPlayedAt DESC")
    fun observeHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE songId = :songId LIMIT 1")
    suspend fun getHistoryForSong(songId: Long): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(history: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    // Lyrics
    @Query("SELECT * FROM lyrics WHERE songId = :songId LIMIT 1")
    fun observeLyricsForSong(songId: Long): Flow<LyricsEntity?>

    @Query("SELECT * FROM lyrics WHERE songId = :songId LIMIT 1")
    suspend fun getLyricsForSongOnce(songId: Long): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLyrics(lyrics: LyricsEntity)

    @Query("DELETE FROM lyrics WHERE songId = :songId")
    suspend fun deleteLyrics(songId: Long)

    // Search History
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 15")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchHistory(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // SAF Folders
    @Query("SELECT * FROM saf_folders ORDER BY addedAt DESC")
    fun observeSafFolders(): Flow<List<SafFolderEntity>>

    @Query("SELECT * FROM saf_folders")
    suspend fun getSafFoldersOnce(): List<SafFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSafFolder(folder: SafFolderEntity)

    @Query("DELETE FROM saf_folders WHERE treeUri = :treeUri")
    suspend fun deleteSafFolder(treeUri: String)

    // Excluded Folders
    @Query("SELECT * FROM excluded_folders ORDER BY excludedAt DESC")
    fun observeExcludedFolders(): Flow<List<ExcludedFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExcludedFolder(folder: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE folderPath = :folderPath")
    suspend fun deleteExcludedFolder(folderPath: String)

    // Per-Nasheed Custom Backgrounds
    @Query("SELECT * FROM song_backgrounds")
    fun observeSongBackgrounds(): Flow<List<SongBackgroundEntity>>

    @Query("SELECT * FROM song_backgrounds WHERE songId = :songId LIMIT 1")
    suspend fun getSongBackgroundOnce(songId: Long): SongBackgroundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongBackground(bg: SongBackgroundEntity)

    @Query("DELETE FROM song_backgrounds WHERE songId = :songId")
    suspend fun deleteSongBackground(songId: Long)

    // Per-Nasheed Custom Titles (Rename)
    @Query("SELECT * FROM song_custom_titles")
    fun observeSongCustomTitles(): Flow<List<SongCustomTitleEntity>>

    @Query("SELECT * FROM song_custom_titles WHERE songId = :songId LIMIT 1")
    suspend fun getSongCustomTitleOnce(songId: Long): SongCustomTitleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongCustomTitle(titleEntry: SongCustomTitleEntity)

    @Query("DELETE FROM song_custom_titles WHERE songId = :songId")
    suspend fun deleteSongCustomTitle(songId: Long)

    // Song queries for folder / permanent delete
    @Query("SELECT * FROM songs WHERE folderPath = :folderPath")
    suspend fun getSongsInFolderOnce(folderPath: String): List<SongEntity>

    @Query("DELETE FROM songs WHERE folderPath = :folderPath")
    suspend fun deleteSongsInFolder(folderPath: String)

    // History ordering support
    @Query("UPDATE history SET lastPlayedAt = :timestamp WHERE songId = :songId")
    suspend fun updateHistoryTimestamp(songId: Long, timestamp: Long)

    @Query("SELECT * FROM history ORDER BY lastPlayedAt DESC")
    suspend fun getHistoryOnce(): List<HistoryEntity>

    @Query("DELETE FROM history WHERE songId = :songId")
    suspend fun deleteHistoryForSong(songId: Long)

    @Query("DELETE FROM playlist_song_cross_ref WHERE songId = :songId")
    suspend fun removeSongFromAllPlaylists(songId: Long)

    // Recycle Bin (Trash)
    @Query("SELECT * FROM trash_items ORDER BY trashedAt DESC")
    fun observeTrashItems(): Flow<List<TrashItemEntity>>

    @Query("SELECT * FROM trash_items ORDER BY trashedAt DESC")
    suspend fun getTrashItemsOnce(): List<TrashItemEntity>

    @Query("SELECT refId FROM trash_items WHERE itemType = 'SONG'")
    fun observeTrashedSongIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItem(item: TrashItemEntity)

    @Query("DELETE FROM trash_items WHERE trashId = :trashId")
    suspend fun deleteTrashItem(trashId: Long)

    @Query("DELETE FROM trash_items")
    suspend fun clearAllTrash()
}
