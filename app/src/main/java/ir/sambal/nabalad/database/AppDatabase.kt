package ir.sambal.nabalad.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ir.sambal.nabalad.database.dao.BookmarkDao
import ir.sambal.nabalad.database.entities.Bookmark

@Database(entities = [Bookmark::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
}