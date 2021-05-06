package ir.sambal.nabalad

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mancj.materialsearchbar.MaterialSearchBar
import ir.sambal.nabalad.database.AppDatabase
import ir.sambal.nabalad.database.entities.Bookmark
import ir.sambal.nabalad.database.viewmodel.BookmarkViewModel
import ir.sambal.nabalad.helpers.EndlessRecyclerViewScrollListener


class BookmarkFragment(private val db: AppDatabase) : Fragment(),
    MaterialSearchBar.OnSearchActionListener {

    private var viewModel: BookmarkViewModel? = null
    private var filter: String? = null
    private lateinit var searchBar: MaterialSearchBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmarks, container, false)

        // Set the adapter
        val notFoundView = view.findViewById<TextView>(R.id.bookmark_not_found_error_text)
        val listView = view.findViewById<RecyclerView>(R.id.list)
        viewModel = BookmarkViewModel(db, listView, notFoundView)

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
        viewModel?.loadTopBookmarks()

        view.findViewById<FloatingActionButton>(R.id.add_random_bookmark).setOnClickListener {
            viewModel?.addRandom()
        }

        searchBar = view.findViewById<MaterialSearchBar>(R.id.search_bar)
        searchBar.setOnSearchActionListener(this)
        searchBar.setSpeechMode(true)

        return view
    }


    private fun onBookmarkDelete(bookmark: Bookmark) {
        viewModel?.deleteBookmark(bookmark)
    }

    private fun doSearch(filter: String?) {
        if (!filter.equals(this.filter)) {
            this.filter = filter
            viewModel?.loadTopBookmarks(filter = filter)
        }
    }

    override fun onButtonClicked(buttonCode: Int) {
        when (buttonCode) {
            MaterialSearchBar.BUTTON_NAVIGATION -> {
                displaySpeechRecognizer()
            }
            MaterialSearchBar.BUTTON_SPEECH -> {
                searchBar.closeSearch()
                doSearch(null)
            }
        }
    }


    private fun displaySpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
        }
        startActivityForResult(intent, SPEECH_REQUEST_CODE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).let { results ->
                    results!![0]
                }
            if (spokenText != null) {
                doSearch(spokenText)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSearchStateChanged(enabled: Boolean) {
        if (!enabled && filter != null) {
            filter = null
            viewModel?.loadTopBookmarks(filter = filter)
        }
    }

    override fun onSearchConfirmed(text: CharSequence?) {
        doSearch(text.toString())
    }

    companion object {

        const val TAG = "BOOKMARK_LIST"
        const val SPEECH_REQUEST_CODE = 2

        @JvmStatic
        fun newInstance(db: AppDatabase) =
            BookmarkFragment(db).apply {
            }
    }
}