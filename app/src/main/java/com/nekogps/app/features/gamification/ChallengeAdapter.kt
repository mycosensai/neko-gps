package com.nekogps.app.features.gamification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.nekogps.app.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * ChallengeAdapter - RecyclerView adapter for location-based challenges.
 * Shows active (in-progress) and completed challenges with progress bars.
 */
class ChallengeAdapter(
    private val onProgressClick: (LocationChallengeEntity) -> Unit = {},
    private val onResetClick: (LocationChallengeEntity) -> Unit = {}
) : ListAdapter<LocationChallengeEntity, ChallengeAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_challenge, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: TextView = itemView.findViewById(R.id.ivChallengeIcon)
        private val name: TextView = itemView.findViewById(R.id.tvChallengeName)
        private val description: TextView = itemView.findViewById(R.id.tvChallengeDescription)
        private val progressText: TextView = itemView.findViewById(R.id.tvProgressText)
        private val progressBar: android.widget.ProgressBar = itemView.findViewById(R.id.pbChallengeProgress)
        private val btnProgress: MaterialButton = itemView.findViewById(R.id.btnProgress)
        private val btnReset: MaterialButton = itemView.findViewById(R.id.btnReset)

        fun bind(challenge: LocationChallengeEntity) {
            icon.text = challenge.icon
            name.text = challenge.name
            description.text = challenge.description

            val percent = if (challenge.targetValue > 0) {
                (challenge.currentValue / challenge.targetValue * MAX_PROGRESS_PERCENT)
                    .toInt()
                    .coerceIn(0, MAX_PROGRESS_PERCENT)
            } else 0

            progressBar.max = MAX_PROGRESS_PERCENT
            progressBar.progress = percent
            progressText.text = "${challenge.currentValue.toInt()} / ${challenge.targetValue.toInt()} ($percent%)"

            if (challenge.completed) {
                btnProgress.text = "✓ Done"
                btnProgress.isEnabled = false
                btnReset.visibility = View.VISIBLE
            } else {
                btnProgress.text = "+ Progress"
                btnProgress.isEnabled = true
                btnReset.visibility = View.GONE
            }

            btnProgress.setOnClickListener { onProgressClick(challenge) }
            btnReset.setOnClickListener { onResetClick(challenge) }
        }
    }

    companion object {
        private const val MAX_PROGRESS_PERCENT = 100
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LocationChallengeEntity>() {
            override fun areItemsTheSame(oldItem: LocationChallengeEntity, newItem: LocationChallengeEntity) =
                oldItem.challengeKey == newItem.challengeKey

            override fun areContentsTheSame(oldItem: LocationChallengeEntity, newItem: LocationChallengeEntity) =
                oldItem == newItem
        }
    }
}
