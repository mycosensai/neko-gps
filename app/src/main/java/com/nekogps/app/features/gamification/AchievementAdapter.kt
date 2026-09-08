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
import java.text.SimpleDateFormat
import java.util.*

/**
 * AchievementAdapter - RecyclerView adapter for displaying achievements.
 * Shows unlocked and locked achievements with their icons and descriptions.
 */
class AchievementAdapter(
    private val onCheckClick: () -> Unit = {}
) : ListAdapter<AchievementEntity, AchievementAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: TextView = itemView.findViewById(R.id.ivAchievementIcon)
        private val name: TextView = itemView.findViewById(R.id.tvAchievementName)
        private val description: TextView = itemView.findViewById(R.id.tvAchievementDescription)
        private val statusIcon: ImageView = itemView.findViewById(R.id.ivStatusIcon)
        private val dateText: TextView = itemView.findViewById(R.id.tvUnlockDate)

        fun bind(achievement: AchievementEntity) {
            icon.text = achievement.icon
            name.text = achievement.name
            description.text = achievement.description

            if (achievement.unlocked) {
                statusIcon.setImageResource(android.R.drawable.btn_star_big_on)
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                dateText.text = sdf.format(Date(achievement.unlockedAt))
                dateText.visibility = View.VISIBLE
            } else {
                statusIcon.setImageResource(android.R.drawable.btn_star_big_off)
                dateText.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AchievementEntity>() {
            override fun areItemsTheSame(oldItem: AchievementEntity, newItem: AchievementEntity) =
                oldItem.achievementKey == newItem.achievementKey

            override fun areContentsTheSame(oldItem: AchievementEntity, newItem: AchievementEntity) =
                oldItem == newItem
        }
    }
}
