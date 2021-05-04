package ir.sambal.nabalad.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import ir.sambal.nabalad.database.entities.Bookmark

@Dao
interface BookmarkDao {

    @Insert
    suspend fun add(bookmark: Bookmark)

    @Delete
    suspend fun delete(bookmark: Bookmark)

    @Query("Select * from bookmark order by id desc limit :limit offset :offset")
    suspend fun loadBookmarks(offset: Int = 0, limit: Int = 10): List<Bookmark>

    @Query("Select * from bookmark where name  like '%' || :filter || '%' order by id desc limit :limit offset :offset")
    suspend fun loadBookmarksWithFilter(filter: String, offset: Int, limit: Int): List<Bookmark>

}