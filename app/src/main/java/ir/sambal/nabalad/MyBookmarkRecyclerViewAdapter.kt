package ir.sambal.nabalad

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView

import ir.sambal.nabalad.dummy.DummyContent.DummyItem

class MyBookmarkRecyclerViewAdapter(
    private val values: List<DummyItem>
) : RecyclerView.Adapter<MyBookmarkRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_bookmarks_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.titleView.text = item.title
        holder.locationView.text = item.latLong()
        holder.cardView.setOnClickListener { v ->
            Log.i(
                "BOOKMARK_LIST",
                "click " + item.id.toString()
            )
        }
        holder.removeButtonView.setOnClickListener { v ->
            Log.i(
                "BOOKMARK_LIST",
                "remove " + item.id.toString()
            )
        }
    }

    override fun getItemCount(): Int = values.size

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