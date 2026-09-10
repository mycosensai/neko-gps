package com.nekogps.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nekogps.app.features.ElevationProfileView
import com.nekogps.app.utils.DistanceCalculator
import org.osmdroid.util.GeoPoint
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Displays recorded GPS tracks with RecyclerView.
 * Each item shows track name, date, distance, duration, and point count.
 * Includes elevation profile view for selected tracks.
 */
class TracksActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TracksAdapter
    private lateinit var btnExportAll: Button
    private lateinit var tvTotalTracks: TextView
    private lateinit var tvTotalDistance: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var elevationProfileView: ElevationProfileView
    private lateinit var tvElevationStats: TextView

    companion object {
        private const val TRACKS_PREF = "nekogps_tracks"
        private const val MILLIS_PER_HOUR = 3600000L
        private const val MILLIS_PER_MINUTE = 60000L
        private const val MILLIS_PER_SECOND = 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracks)

        recyclerView = findViewById(R.id.rvTracks)
        btnExportAll = findViewById(R.id.btnExportAll)
        tvTotalTracks = findViewById(R.id.tvTotalTracks)
        tvTotalDistance = findViewById(R.id.tvTotalDistance)
        tvEmpty = findViewById(R.id.tvEmpty)
        elevationProfileView = findViewById(R.id.elevationProfileView)
        tvElevationStats = findViewById(R.id.tvElevationStats)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TracksAdapter { track ->
            showTrackElevation(track)
        }
        recyclerView.adapter = adapter

        btnExportAll.setOnClickListener {
            Toast.makeText(this, "Export all tracks coming soon", Toast.LENGTH_SHORT).show()
        }

        loadTracks()
    }

    override fun onResume() {
        super.onResume()
        loadTracks()
    }

    private fun showTrackElevation(track: TrackData) {
        if (track.points.isNotEmpty()) {
            elevationProfileView.setTrackFromGeoPoints(track.points)
            elevationProfileView.visibility = View.VISIBLE
            tvElevationStats.visibility = View.VISIBLE
            val ascent = elevationProfileView.getTotalAscent().toInt()
            val descent = elevationProfileView.getTotalDescent().toInt()
            tvElevationStats.text = "↑ ${ascent}m ascent | ↓ ${descent}m descent"
        }
    }

    private fun loadTracks() {
        val tracks = getStoredTracks()
        adapter.submitList(tracks)

        // Update stats
        tvTotalTracks.text = tracks.size.toString()
        val totalDistance = tracks.sumOf { it.distance }
        tvTotalDistance.text = DistanceCalculator.formatDistance(totalDistance)

        // Show/hide empty state
        if (tracks.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            elevationProfileView.visibility = View.GONE
            tvElevationStats.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun getStoredTracks(): List<TrackData> {
        val prefs = getSharedPreferences(TRACKS_PREF, Context.MODE_PRIVATE)
        val tracksJson = prefs.getString("tracks", "[]") ?: "[]"
        val tracks = mutableListOf<TrackData>()

        try {
            val jsonArray = JSONArray(tracksJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val pointsArray = obj.getJSONArray("points")
                val points = mutableListOf<GeoPoint>()
                for (j in 0 until pointsArray.length()) {
                    val ptObj = pointsArray.getJSONObject(j)
                    points.add(GeoPoint(ptObj.getDouble("lat"), ptObj.getDouble("lng")))
                }

                tracks.add(
                    TrackData(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        date = obj.getLong("date"),
                        distance = obj.getDouble("distance"),
                        duration = obj.getLong("duration"),
                        points = points
                    )
                )
            }
        } catch (e: JSONException) {
            Log.w("TracksActivity", "getStoredTracks: suppressed Exception", e)
        }

        return tracks
    }

    data class TrackData(
        val id: String,
        val name: String,
        val date: Long,
        val distance: Double,
        val duration: Long,
        val points: List<GeoPoint>
    )

    /**
     * RecyclerView adapter for displaying track items.
     */
    inner class TracksAdapter(
        private val onClick: (TrackData) -> Unit
    ) : RecyclerView.Adapter<TracksAdapter.TrackViewHolder>() {

        private var tracks: List<TrackData> = emptyList()

        fun submitList(newTracks: List<TrackData>) {
            tracks = newTracks
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_track, parent, false)
            return TrackViewHolder(view)
        }

        override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
            holder.bind(tracks[position])
        }

        override fun getItemCount(): Int = tracks.size

        inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvName: TextView = itemView.findViewById(R.id.tvTrackName)
            private val tvDate: TextView = itemView.findViewById(R.id.tvTrackDate)
            private val tvDistance: TextView = itemView.findViewById(R.id.tvTrackDistance)
            private val tvDuration: TextView = itemView.findViewById(R.id.tvTrackDuration)
            private val tvPointCount: TextView = itemView.findViewById(R.id.tvTrackPoints)

            fun bind(track: TrackData) {
                tvName.text = track.name
                tvDate.text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .format(Date(track.date))
                tvDistance.text = DistanceCalculator.formatDistance(track.distance)
                tvDuration.text = formatDuration(track.duration)
                tvPointCount.text = "${track.points.size} points"

                itemView.setOnClickListener { onClick(track) }
            }

            private fun formatDuration(millis: Long): String {
                val hours = millis / MILLIS_PER_HOUR
                val minutes = (millis % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE
                val seconds = (millis % MILLIS_PER_MINUTE) / MILLIS_PER_SECOND
                return when {
                    hours > 0 -> String.format(Locale.US, "%dh %dm %ds", hours, minutes, seconds)
                    minutes > 0 -> String.format(Locale.US, "%dm %ds", minutes, seconds)
                    else -> String.format(Locale.US, "%ds", seconds)
                }
            }
        }
    }
}
