package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        LyricsEntity::class,
        SearchHistoryEntity::class,
        SafFolderEntity::class,
        ExcludedFolderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KarlukDatabase : RoomDatabase() {
    abstract fun karlukDao(): KarlukDao

    companion object {
        @Volatile
        private var INSTANCE: KarlukDatabase? = null

        fun getInstance(context: Context): KarlukDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KarlukDatabase::class.java,
                    "karluk_mp3_pro.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
