package com.nekogps.app.features.waypoints

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.nekogps.app.R
import com.nekogps.app.features.waypoints.WaypointEntity

/**
 * WaypointAdapter - RecyclerView adapter with drag-and-drop reordering support.
 */
class WaypointAdapter(
    private val onDeleteClick: (WaypointEntity) -> Unit,
    private val onItemMoved: (Int, Int) -> Unit
) : ListAdapter<WaypointEntity, WaypointAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var itemTouchHelper: ItemTouchHelper? = null

    fun setItemTouchHelper(touchHelper: ItemTouchHelper) {
        itemTouchHelper = touchHelper
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_waypoint, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        onItemMoved(fromPosition, toPosition)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrder: TextView = itemView.findViewById(R.id.tvWaypointOrder)
        private val tvName: TextView = itemView.findViewById(R.id.tvWaypointName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvWaypointDescription)
        private val tvCoordinates: TextView = itemView.findViewById(R.id.tvWaypointCoordinates)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDeleteWaypoint)
        private val dragHandle: View = itemView.findViewById(R.id.dragHandle)

        init {
            dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper?.startDrag(this@ViewHolder)
                }
                false
            }
        }

        fun bind(waypoint: WaypointEntity) {
            tvOrder.text = "${waypoint.order + 1}"
            tvName.text = waypoint.name
            tvDescription.text = waypoint.description.ifEmpty { "No description" }
            tvCoordinates.text = String.format(
                java.util.Locale.getDefault(),
                "%.5f, %.5f",
                waypoint.latitude,
                waypoint.longitude
            )

            btnDelete.setOnClickListener { onDeleteClick(waypoint) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WaypointEntity>() {
            override fun areItemsTheSame(oldItem: WaypointEntity, newItem: WaypointEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: WaypointEntity, newItem: WaypointEntity) =
                oldItem == newItem
        }
    }
}

/**
 * SimpleItemTouchHelperCallback - handles drag-and-drop reordering.
 */
class SimpleItemTouchHelperCallback(
    private val adapter: WaypointAdapter
) : ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean = false

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
}
