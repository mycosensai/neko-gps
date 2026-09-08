package com.nekogps.app.features.bookmarks

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.nekogps.app.MapsActivity
import com.nekogps.app.R
import kotlinx.coroutines.launch

/**
 * BookmarkActivity - displays list of saved bookmarks with navigation and delete options.
 */
class BookmarkActivity : AppCompatActivity() {
    private lateinit var bookmarkManager: BookmarkManager
    private lateinit var adapter: BookmarkAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)

        bookmarkManager = BookmarkManager(this)
        setupRecyclerView()
        setupFab()
        observeBookmarks()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvBookmarks)
        adapter = BookmarkAdapter(
            onNavigateClick = { bookmark -> navigateToBookmark(bookmark) },
            onDeleteClick = { bookmark -> confirmDelete(bookmark) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fabAddBookmark).setOnClickListener {
            showAddBookmarkDialog()
        }
    }

    private fun observeBookmarks() {
        lifecycleScope.launch {
            bookmarkManager.allBookmarks.collect { bookmarks ->
                adapter.submitList(bookmarks)
            }
        }
    }

    private fun navigateToBookmark(bookmark: BookmarkEntity) {
        val intent = Intent(this, MapsActivity::class.java).apply {
            putExtra("navigate_to_lat", bookmark.latitude)
            putExtra("navigate_to_lon", bookmark.longitude)
            putExtra("navigate_to_name", bookmark.name)
        }
        startActivity(intent)
    }

    private fun confirmDelete(bookmark: BookmarkEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Bookmark")
            .setMessage("Delete \"${bookmark.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    bookmarkManager.deleteBookmark(bookmark)
                    Toast.makeText(this@BookmarkActivity, "Deleted nya~", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddBookmarkDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_bookmark, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etBookmarkName)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etBookmarkDescription)
        val etLat = dialogView.findViewById<TextInputEditText>(R.id.etBookmarkLat)
        val etLon = dialogView.findViewById<TextInputEditText>(R.id.etBookmarkLon)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Bookmark")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text?.toString()?.trim() ?: ""
                val desc = etDescription.text?.toString()?.trim() ?: ""
                val lat = etLat.text?.toString()?.toDoubleOrNull() ?: 0.0
                val lon = etLon.text?.toString()?.toDoubleOrNull() ?: 0.0

                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        bookmarkManager.addBookmark(
                            name = name,
                            description = desc,
                            category = BookmarkCategory.CUSTOM,
                            latitude = lat,
                            longitude = lon
                        )
                        Toast.makeText(this@BookmarkActivity, "Saved nya~", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
