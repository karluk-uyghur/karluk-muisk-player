package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        ExcludedFolderEntity::class,
        SongBackgroundEntity::class,
        SongCustomTitleEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class KarlukDatabase : RoomDatabase() {
    abstract fun karlukDao(): KarlukDao

    companion object {
        @Volatile
        private var INSTANCE: KarlukDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `song_backgrounds` (`songId` INTEGER NOT NULL, `backgroundUri` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `song_custom_titles` (`songId` INTEGER NOT NULL, `customTitle` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))"
                )
            }
        }

        fun getInstance(context: Context): KarlukDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KarlukDatabase::class.java,
                    "karluk_mp3_pro.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
