package com.nekogps.app.features.gamification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nekogps.app.R
import com.google.android.material.button.MaterialButton

/**
 * LeaderboardAdapter - RecyclerView adapter for the local leaderboard.
 * Supports user entries and friend entries with different visual styles.
 */
class LeaderboardAdapter(
    private val onDeleteFriendClick: (LeaderboardEntryEntity) -> Unit = {}
) : ListAdapter<LeaderboardEntryEntity, LeaderboardAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rankIcon: TextView = itemView.findViewById(R.id.ivRankIcon)
        private val playerName: TextView = itemView.findViewById(R.id.tvPlayerName)
        private val playerDistance: TextView = itemView.findViewById(R.id.tvPlayerDistance)
        private val playerTracks: TextView = itemView.findViewById(R.id.tvPlayerTracks)
        private val deleteBtn: MaterialButton = itemView.findViewById(R.id.btnDeleteFriend)

        fun bind(entry: LeaderboardEntryEntity, rank: Int) {
            playerName.text = entry.playerName
            playerDistance.text = formatDistance(entry.totalDistanceMeters)
            playerTracks.text = "Tracks: ${entry.tracksRecorded}"

            // Set rank icon
            when {
                rank == RANK_FIRST_PLACE -> rankIcon.text = "🥇"
                rank == RANK_SECOND_PLACE -> rankIcon.text = "🥈"
                rank == RANK_THIRD_PLACE -> rankIcon.text = "🥉"
                else -> rankIcon.text = rank.toString()
            }

            if (entry.isUser) {
                playerName.setTextColor(0xFFcbb7fb.toInt()) // highlight user
                deleteBtn.visibility = View.GONE
            } else {
                deleteBtn.visibility = View.VISIBLE
                deleteBtn.setOnClickListener { onDeleteFriendClick(entry) }
            }
        }
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= METERS_PER_KILOMETER) {
            String.format(java.util.Locale.getDefault(), "%.1f km", meters / METERS_PER_KILOMETER)
        } else {
            String.format(java.util.Locale.getDefault(), "%.0f m", meters)
        }
    }

    companion object {
        private const val METERS_PER_KILOMETER = 1000
        private const val RANK_FIRST_PLACE = 1
        private const val RANK_SECOND_PLACE = 2
        private const val RANK_THIRD_PLACE = 3
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LeaderboardEntryEntity>() {
            override fun areItemsTheSame(oldItem: LeaderboardEntryEntity, newItem: LeaderboardEntryEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: LeaderboardEntryEntity, newItem: LeaderboardEntryEntity) =
                oldItem == newItem
        }
    }
}
