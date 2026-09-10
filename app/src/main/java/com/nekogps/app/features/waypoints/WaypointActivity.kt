package com.nekogps.app.features.waypoints

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.nekogps.app.R
import com.nekogps.app.features.waypoints.WaypointEntity
import kotlinx.coroutines.launch

/**
 * WaypointActivity - displays list of waypoints with drag-and-drop reordering.
 * Shows total route distance and ETA.
 */
class WaypointActivity : AppCompatActivity() {
    private lateinit var waypointManager: WaypointManager
    private lateinit var adapter: WaypointAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvTotalDistance: TextView
    private lateinit var tvETA: TextView
    private lateinit var tvWaypointCount: TextView

    private var routeId: Long = 1L
    private var waypoints: List<WaypointEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waypoints)

        waypointManager = WaypointManager(this)
        initViews()
        setupRecyclerView()
        observeWaypoints()
    }

    private fun initViews() {
        tvTotalDistance = findViewById(R.id.tvTotalDistance)
        tvETA = findViewById(R.id.tvETA)
        tvWaypointCount = findViewById(R.id.tvWaypointCount)

        findViewById<FloatingActionButton>(R.id.fabAddWaypoint).setOnClickListener {
            showAddWaypointDialog()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvWaypoints)
        adapter = WaypointAdapter(
            onDeleteClick = { waypoint -> confirmDelete(waypoint) },
            onItemMoved = { from, to -> reorderWaypoints(from, to) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Setup drag-and-drop
        val callback = SimpleItemTouchHelperCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        adapter.setItemTouchHelper(touchHelper)
        touchHelper.attachToRecyclerView(recyclerView)
    }

    private fun observeWaypoints() {
        lifecycleScope.launch {
            waypointManager.getWaypointsForRoute(routeId).collect { list ->
                waypoints = list
                adapter.submitList(list)
                updateRouteSummary()
            }
        }
    }

    private fun updateRouteSummary() {
        val summary = waypointManager.getRouteSummary(waypoints)
        tvTotalDistance.text = "Distance: ${waypointManager.formatDistance(summary.totalDistanceMeters)}"
        tvETA.text = "ETA: ${waypointManager.formatETA(summary.estimatedTimeMinutes)}"
        tvWaypointCount.text = "${summary.waypointCount} waypoints"
    }

    private fun reorderWaypoints(from: Int, to: Int) {
        lifecycleScope.launch {
            waypointManager.reorderWaypoints(routeId, from, to)
        }
    }

    private fun confirmDelete(waypoint: WaypointEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Waypoint")
            .setMessage("Delete \"${waypoint.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    waypointManager.deleteWaypoint(waypoint)
                    Toast.makeText(this@WaypointActivity, "Deleted nya~", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddWaypointDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_waypoint, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etWaypointName)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etWaypointDescription)
        val etLat = dialogView.findViewById<TextInputEditText>(R.id.etWaypointLat)
        val etLon = dialogView.findViewById<TextInputEditText>(R.id.etWaypointLon)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Waypoint")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text?.toString()?.trim() ?: ""
                val desc = etDescription.text?.toString()?.trim() ?: ""
                val lat = etLat.text?.toString()?.toDoubleOrNull() ?: 0.0
                val lon = etLon.text?.toString()?.toDoubleOrNull() ?: 0.0

                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        waypointManager.addWaypoint(
                            routeId = routeId,
                            name = name,
                            description = desc,
                            latitude = lat,
                            longitude = lon
                        )
                        Toast.makeText(this@WaypointActivity, "Waypoint added nya~", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
