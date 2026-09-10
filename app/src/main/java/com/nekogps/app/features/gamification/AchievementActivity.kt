package com.nekogps.app.features.gamification

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nekogps.app.R
import kotlinx.coroutines.launch

/**
 * AchievementActivity - displays unlocked and locked achievements.
 * Users can see their progress and unlock milestones.
 */
class AchievementActivity : AppCompatActivity() {
    private lateinit var achievementManager: AchievementManager
    private lateinit var adapter: AchievementAdapter
    private lateinit var recyclerView: RecyclerView

    private var achievements = listOf<AchievementEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        achievementManager = AchievementManager(this)
        initViews()
        observeAchievements()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rvAchievements)
        adapter = AchievementAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabCheckAll).setOnClickListener {
            checkAllAchievements()
        }
    }

    private fun observeAchievements() {
        lifecycleScope.launch {
            achievementManager.allAchievements.collect { list ->
                achievements = list
                adapter.submitList(list)
                updateStats()
            }
        }
    }

    private fun updateStats() {
        val unlocked = achievements.count { it.unlocked }
        val total = achievements.size
        findViewById<TextView>(R.id.tvStats).text = "$unlocked / $total Unlocked"
    }

    private fun checkAllAchievements() {
        Toast.makeText(this, "Checking achievements...", Toast.LENGTH_SHORT).show()
    }
}
