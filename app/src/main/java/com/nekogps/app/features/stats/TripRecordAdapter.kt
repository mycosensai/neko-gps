package com.nekogps.app.features.stats

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nekogps.app.R

/**
 * TripRecordAdapter - RecyclerView adapter for recent trip records.
 */
class TripRecordAdapter(
    private val context: Context,
    private val onTripClick: (TripRecord) -> Unit = {}
) : ListAdapter<TripRecord, TripRecordAdapter.ViewHolder>(TripRecordDiffCallback()) {

    companion object {
        private const val SECONDS_PER_HOUR = 3600
        private const val SECONDS_PER_MINUTE = 60
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(com.nekogps.app.R.layout.item_trip_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = getItem(position)
        holder.bind(trip)
        holder.itemView.setOnClickListener { onTripClick(trip) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTripIcon: TextView = itemView.findViewById(com.nekogps.app.R.id.tvTripIcon)
        private val tvTripDate: TextView = itemView.findViewById(com.nekogps.app.R.id.tvTripDate)
        private val tvTripDistance: TextView = itemView.findViewById(com.nekogps.app.R.id.tvTripDistance)
        private val tvTripTime: TextView = itemView.findViewById(com.nekogps.app.R.id.tvTripTime)
        private val tvTripSpeed: TextView = itemView.findViewById(com.nekogps.app.R.id.tvTripSpeed)

        fun bind(trip: TripRecord) {
            tvTripDate.text = formatDate(trip.startTime)
            tvTripDistance.text = OdometerManager.formatDistance(trip.totalDistanceMeters)

            val hours = trip.totalTimeSeconds / SECONDS_PER_HOUR
            val mins = (trip.totalTimeSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
            tvTripTime.text = "${hours}h ${mins}m"

            tvTripSpeed.text = "%.0f km/h".format(trip.averageSpeedKmh)
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
    }

    class TripRecordDiffCallback : DiffUtil.ItemCallback<TripRecord>() {
        override fun areItemsTheSame(oldItem: TripRecord, newItem: TripRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TripRecord, newItem: TripRecord): Boolean {
            return oldItem == newItem
        }
    }
}
