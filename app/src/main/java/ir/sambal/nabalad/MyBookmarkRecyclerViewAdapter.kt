package ir.sambal.nabalad

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import ir.sambal.nabalad.database.entities.Bookmark


class MyBookmarkRecyclerViewAdapter(
    private val onDeleteHandler: (Bookmark) -> Unit,
    private val onClickHandler: (Bookmark) -> Unit
) :
    RecyclerView.Adapter<MyBookmarkRecyclerViewAdapter.ViewHolder>() {

    var values: ArrayList<Bookmark> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_bookmarks_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.titleView.text = item.name
        holder.locationView.text = item.latLng()
        holder.cardView.setOnClickListener {
            Log.i(
                "BOOKMARK_LIST",
                "click " + item.id.toString()
            )
            onClickHandler(item)
        }
        holder.removeButtonView.setOnClickListener {
            Log.i(
                "BOOKMARK_LIST",
                "remove " + item.id.toString()
            )
            onDeleteHandler(item)
        }
    }

    override fun getItemCount(): Int = values.size

    fun update(from: Int, bookmarks: List<Bookmark>) {
        for ((index, bookmark) in bookmarks.withIndex()) {
            val i = from + index
            if (i < values.size) {
                values[i] = bookmark
            } else {
                values.add(bookmark)
            }
            notifyItemChanged(i)
        }
    }

    fun remove(index: Int) {
        values.removeAt(index)
        notifyItemRemoved(index)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.findViewById(R.id.title)
        val locationView: TextView = view.findViewById(R.id.location)
        val cardView: CardView = view.findViewById(R.id.card)
        val removeButtonView: ImageButton = view.findViewById(R.id.removeButton)

        override fun toString(): String {
            return super.toString() + " '" + titleView.text + "'"
        }
    }
}