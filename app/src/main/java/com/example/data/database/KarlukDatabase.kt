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
        SongCustomTitleEntity::class,
        TrashItemEntity::class
    ],
    version = 5,
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trash_items` (`trashId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `itemType` TEXT NOT NULL, `refId` INTEGER NOT NULL, `title` TEXT NOT NULL, `subtitle` TEXT NOT NULL, `uri` TEXT NOT NULL, `folderPath` TEXT NOT NULL, `folderName` TEXT NOT NULL, `isSafTrack` INTEGER NOT NULL, `formatLabel` TEXT NOT NULL, `fileSize` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `trashedAt` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `isFilteredMusic` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `isUserApprovedNasheed` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): KarlukDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KarlukDatabase::class.java,
                    "karluk_mp3_pro.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
