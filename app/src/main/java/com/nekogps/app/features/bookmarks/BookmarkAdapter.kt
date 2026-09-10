package com.nekogps.app.features.bookmarks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.nekogps.app.R

class BookmarkAdapter(
    private val onNavigateClick: (BookmarkEntity) -> Unit,
    private val onDeleteClick: (BookmarkEntity) -> Unit
) : ListAdapter<BookmarkEntity, BookmarkAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvBookmarkName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvBookmarkDescription)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvBookmarkCategory)
        private val tvCoordinates: TextView = itemView.findViewById(R.id.tvBookmarkCoordinates)
        private val btnNavigate: MaterialButton = itemView.findViewById(R.id.btnNavigateToBookmark)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDeleteBookmark)

        fun bind(bookmark: BookmarkEntity) {
            tvName.text = bookmark.name
            tvDescription.text = bookmark.description.ifEmpty { bookmark.address.ifEmpty { "No description" } }
            tvCategory.text = "${bookmark.category.emoji} ${bookmark.category.displayName}"
            tvCoordinates.text = String.format(
                java.util.Locale.getDefault(),
                "%.5f, %.5f",
                bookmark.latitude,
                bookmark.longitude
            )

            btnNavigate.setOnClickListener { onNavigateClick(bookmark) }
            btnDelete.setOnClickListener { onDeleteClick(bookmark) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BookmarkEntity>() {
            override fun areItemsTheSame(oldItem: BookmarkEntity, newItem: BookmarkEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: BookmarkEntity, newItem: BookmarkEntity) =
                oldItem == newItem
        }
    }
}
