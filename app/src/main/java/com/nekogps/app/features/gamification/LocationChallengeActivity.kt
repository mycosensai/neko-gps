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
 * LocationChallengeActivity - displays location-based challenges.
 * Shows active challenges and completed ones with progress tracking.
 */
class LocationChallengeActivity : AppCompatActivity() {
    private lateinit var challengeManager: LocationChallengeManager
    private lateinit var adapter: ChallengeAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_challenges)

        challengeManager = LocationChallengeManager(this)
        initViews()
        observeChallenges()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rvChallenges)
        adapter = ChallengeAdapter(
            onProgressClick = { challenge -> updateProgress(challenge) },
            onResetClick = { challenge -> resetChallenge(challenge) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabInitChallenges).setOnClickListener {
            lifecycleScope.launch {
                challengeManager.initializeChallenges()
                Toast.makeText(this@LocationChallengeActivity, "Challenges initialized!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeChallenges() {
        lifecycleScope.launch {
            challengeManager.allChallenges.collect { challenges ->
                adapter.submitList(challenges)
                updateStats(challenges)
            }
        }
    }

    private fun updateStats(challenges: List<LocationChallengeEntity>) {
        val active = challenges.count { !it.completed }
        val completed = challenges.count { it.completed }
        findViewById<TextView>(R.id.tvStats).text = "$completed / ${challenges.size} Completed"
    }

    private fun updateProgress(challenge: LocationChallengeEntity) {
        // Increment progress by target/10 for demo purposes
        val increment = challenge.targetValue / 10
        lifecycleScope.launch {
            val newProgress = challenge.currentValue + increment
            val completed = challengeManager.updateProgress(challenge.challengeKey, newProgress)
            if (completed) {
                Toast.makeText(this@LocationChallengeActivity, "Challenge completed! 🎉", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun resetChallenge(challenge: LocationChallengeEntity) {
        lifecycleScope.launch {
            challengeManager.resetChallenge(challenge.challengeKey)
            Toast.makeText(this@LocationChallengeActivity, "Challenge reset", Toast.LENGTH_SHORT).show()
        }
    }
}
