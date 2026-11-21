package com.example.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "songs",
    indices = [Index(value = ["uri"], unique = true)]
)
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val mimeType: String,
    val formatLabel: String,
    val artworkUri: String?,
    val fileSize: Long,
    val dateAdded: Long,
    val trackNumber: Int?,
    val genre: String?,
    val folderPath: String,
    val folderName: String,
    val isSafTrack: Boolean
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("songId")]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

data class PlaylistWithSongsEntity(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<SongEntity>
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val songId: Long,
    val playCount: Int = 1,
    val lastPlayedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val songId: Long,
    val rawContent: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saf_folders")
data class SafFolderEntity(
    @PrimaryKey val treeUri: String,
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey val folderPath: String,
    val folderName: String,
    val excludedAt: Long = System.currentTimeMillis()
)
