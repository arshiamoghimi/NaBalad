package ir.sambal.nabalad

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ir.sambal.nabalad.database.AppDatabase
import ir.sambal.nabalad.database.entities.Bookmark
import ir.sambal.nabalad.database.viewmodel.BookmarkViewModel
import ir.sambal.nabalad.helpers.EndlessRecyclerViewScrollListener

/**
 * A fragment representing a list of Items.
 */
class BookmarkFragment(private val db: AppDatabase) : Fragment() {

    private var viewModel: BookmarkViewModel? = null
    private var filter: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmarks, container, false)

        // Set the adapter
        val listView = view.findViewById<View>(R.id.list)
        if (listView is RecyclerView) {
            viewModel = BookmarkViewModel(db, listView)

            val layoutManager = LinearLayoutManager(context)
            listView.layoutManager = layoutManager
            listView.adapter = MyBookmarkRecyclerViewAdapter(this::onBookmarkDelete)

            val scrollListener = object : EndlessRecyclerViewScrollListener(layoutManager) {
                override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                    Log.i(TAG, "onLoadMore page: ${page}, totalItemsCount: $totalItemsCount")
                    viewModel?.loadMoreBookmarks(filter = filter) {
                        finishLoadMore()
                    }
                }
            }
            listView.addOnScrollListener(scrollListener)
        }
        viewModel?.loadTopBookmarks()

        view.findViewById<FloatingActionButton>(R.id.add_random_bookmark).setOnClickListener {
//            filter = "1"
//            viewModel?.loadTopBookmarks(filter = filter)
            viewModel?.addRandom()
        }
        return view
    }


    private fun onBookmarkDelete(bookmark: Bookmark) {
        viewModel?.deleteBookmark(bookmark)
    }


    companion object {

        const val TAG = "BOOKMARK_LIST"

        @JvmStatic
        fun newInstance(db: AppDatabase) =
            BookmarkFragment(db).apply {
            }
    }
}