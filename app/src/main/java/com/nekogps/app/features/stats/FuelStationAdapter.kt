package com.nekogps.app.features.stats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * FuelStationAdapter - RecyclerView adapter for fuel station list.
 */
class FuelStationAdapter(
    private val onStationClick: (FuelStation) -> Unit = {}
) : ListAdapter<FuelStation, FuelStationAdapter.ViewHolder>(FuelStationDiffCallback()) {

    private var useMetric: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(com.nekogps.app.R.layout.item_fuel_station, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = getItem(position)
        holder.bind(station, useMetric)
        holder.itemView.setOnClickListener { onStationClick(station) }
    }

    fun setUseMetric(useMetric: Boolean) {
        this.useMetric = useMetric
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFuelIcon: TextView = itemView.findViewById(com.nekogps.app.R.id.tvFuelIcon)
        private val tvStationName: TextView = itemView.findViewById(com.nekogps.app.R.id.tvStationName)
        private val tvStationBrand: TextView = itemView.findViewById(com.nekogps.app.R.id.tvStationBrand)
        private val tvStationAddress: TextView = itemView.findViewById(com.nekogps.app.R.id.tvStationAddress)
        private val tvStationPrice: TextView = itemView.findViewById(com.nekogps.app.R.id.tvStationPrice)
        private val tvStationType: TextView = itemView.findViewById(com.nekogps.app.R.id.tvStationType)

        fun bind(station: FuelStation, useMetric: Boolean) {
            tvStationName.text = station.name
            tvStationBrand.text = station.brand
            tvStationAddress.text = station.address

            val price = station.pricePerLiter
            val priceText = if (useMetric) {
                "$%.2f/L".format(price)
            } else {
                "$%.2f/gal".format(price * 3.78541)
            }
            tvStationPrice.text = priceText
            tvStationPrice.setTextColor(
                if (price < 1.40) android.graphics.Color.parseColor("#4CAF50")
                else if (price < 1.60) android.graphics.Color.parseColor("#FF9800")
                else android.graphics.Color.parseColor("#F44336")
            )

            tvStationType.text = station.fuelType.displayName

            val icon = when (station.fuelType) {
                FuelStation.FuelType.REGULAR -> "\u26fd"
                FuelStation.FuelType.PREMIUM -> "\u1f3ce"
                FuelStation.FuelType.DIESEL -> "\u1f69b"
                FuelStation.FuelType.LPG -> "\u1f525"
                else -> "\u26fd"
            }
            tvFuelIcon.text = icon
        }
    }

    class FuelStationDiffCallback : DiffUtil.ItemCallback<FuelStation>() {
        override fun areItemsTheSame(oldItem: FuelStation, newItem: FuelStation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FuelStation, newItem: FuelStation): Boolean {
            return oldItem == newItem
        }
    }
}
