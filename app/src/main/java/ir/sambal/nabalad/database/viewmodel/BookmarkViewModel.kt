package ir.sambal.nabalad.database.viewmodel

import android.util.Log
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import ir.sambal.nabalad.MyBookmarkRecyclerViewAdapter
import ir.sambal.nabalad.database.AppDatabase
import ir.sambal.nabalad.database.entities.Bookmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class BookmarkViewModel(
    private val db: AppDatabase,
    private val bookmarkListView: RecyclerView,
    private val notFoundView: TextView
) :
    ViewModel() {
    private fun loadTopBookmarksFrom(
        offset: Int,
        filter: String? = null,
        callback: () -> Unit = {}
    ) {
        var bookmarks: List<Bookmark>
        viewModelScope.launch(Dispatchers.IO) {
            if (filter == null || filter.isBlank()) {
                bookmarks = db.bookmarkDao().loadBookmarks(offset, PER_PAGE)
            } else {
                bookmarks = db.bookmarkDao().loadBookmarksWithFilter(filter, offset, PER_PAGE)
            }
            updateBookmarks(offset, bookmarks)
            viewModelScope.launch(Dispatchers.Main) {
                callback()
            }
        }
    }

    fun loadTopBookmarks(filter: String? = null) {
        viewModelScope.launch(Dispatchers.Main) {
            val adapter = bookmarkListView.adapter as MyBookmarkRecyclerViewAdapter
            notFoundView.visibility = TextView.VISIBLE
            adapter.values.clear()
            adapter.notifyDataSetChanged()
        }
        loadTopBookmarksFrom(0, filter)
    }

    fun loadMoreBookmarks(filter: String? = null, callback: () -> Unit) {
        Log.v(
            "BOOKMARK_LIST",
            "itemCount: ${(bookmarkListView.adapter as MyBookmarkRecyclerViewAdapter).itemCount}"
        )
        loadTopBookmarksFrom(
            (bookmarkListView.adapter as MyBookmarkRecyclerViewAdapter).itemCount,
            filter,
            callback
        )
    }

    private fun updateBookmarks(offset: Int, bookmarks: List<Bookmark>) {
        viewModelScope.launch(Dispatchers.Main) {
            if (bookmarks.isNotEmpty()) {
                notFoundView.visibility = TextView.INVISIBLE
            }
            (bookmarkListView.adapter as MyBookmarkRecyclerViewAdapter).update(
                offset,
                bookmarks
            )
        }
    }

    private fun findBookmarkPosition(bookmark: Bookmark, callback: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            val adapter = bookmarkListView.adapter as MyBookmarkRecyclerViewAdapter
            for ((index, item) in adapter.values.withIndex()) {
                if (item == bookmark) {
                    callback(index)
                    break
                }
            }
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        findBookmarkPosition(bookmark) { index ->
            viewModelScope.launch(Dispatchers.IO) {
                db.bookmarkDao().delete(bookmark)
                deleteViewItem(index)
            }
        }
    }

    private fun deleteViewItem(index: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            val adapter = bookmarkListView.adapter as MyBookmarkRecyclerViewAdapter
            adapter.remove(index)
        }
    }


    companion object {
        const val PER_PAGE = 10
    }
}