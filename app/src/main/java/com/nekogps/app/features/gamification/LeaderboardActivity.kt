package com.nekogps.app.features.gamification

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.nekogps.app.R
import kotlinx.coroutines.launch

/**
 * LeaderboardActivity - displays a local leaderboard with distance traveled,
 * tracks recorded, etc. Supports manually added friend entries.
 */
class LeaderboardActivity : AppCompatActivity() {
    private lateinit var leaderboardManager: LeaderboardManager
    private lateinit var adapter: LeaderboardAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        leaderboardManager = LeaderboardManager(this)
        initViews()
        observeEntries()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rvLeaderboard)
        adapter = LeaderboardAdapter(
            onDeleteFriendClick = { friend -> confirmDeleteFriend(friend) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddFriend).setOnClickListener {
            showAddFriendDialog()
        }
    }

    private fun observeEntries() {
        lifecycleScope.launch {
            leaderboardManager.allEntries.collect { entries ->
                adapter.submitList(entries)
                updateStats(entries)
            }
        }
    }

    private fun updateStats(entries: List<LeaderboardEntryEntity>) {
        val userEntry = entries.find { it.isUser }
        val rank = entries.indexOfFirst { it.isUser } + 1
        val stats = findViewById<TextView>(R.id.tvStats)
        if (userEntry != null) {
            stats.text = "Rank #$rank | ${leaderboardManager.getDistanceString(userEntry.totalDistanceMeters)}"
        } else {
            stats.text = "No entry yet"
        }
    }

    private fun showAddFriendDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_friend, null)
        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFriendName)
        val etDistance = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFriendDistance)
        val etTracks = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFriendTracks)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Add Friend")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text?.toString()?.trim() ?: ""
                val distance = etDistance.text?.toString()?.toDoubleOrNull() ?: 0.0
                val tracks = etTracks.text?.toString()?.toIntOrNull() ?: 0
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        leaderboardManager.addFriend(name, distance, tracks)
                        Toast.makeText(this@LeaderboardActivity, "Friend added!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteFriend(friend: LeaderboardEntryEntity) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Remove ${friend.playerName}?")
            .setPositiveButton("Remove") { _, _ ->
                lifecycleScope.launch {
                    leaderboardManager.removeFriend(friend.playerName)
                    Toast.makeText(this@LeaderboardActivity, "Removed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
