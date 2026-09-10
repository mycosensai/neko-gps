package com.nekogps.app.features.routing

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nekogps.app.R
import com.nekogps.app.databinding.ActivityAlternativeRoutesBinding
import com.nekogps.app.utils.DistanceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class AlternativeRoutesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlternativeRoutesBinding
    private val alternativeRoutes by lazy { AlternativeRoutes() }
    private val routeOptionsManager by lazy { RouteOptionsManager(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var routeAdapter: RouteOptionAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlternativeRoutesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
        setupRecyclerView()
        setupAvoidanceToggles()
        setupSelectRouteButton()
        calculateRoutes()
    }

    private fun setupViews() {
        binding.tvTitle.text = getString(R.string.alternative_routes)
    }

    private fun setupRecyclerView() {
        binding.rvRoutes.layoutManager = LinearLayoutManager(this)
        routeAdapter = RouteOptionAdapter { route -> selectRoute(route) }
        binding.rvRoutes.adapter = routeAdapter
    }

    private fun setupAvoidanceToggles() {
        binding.switchAvoidTolls.setOnCheckedChangeListener { _, _ -> calculateRoutes() }
        binding.switchAvoidHighways.setOnCheckedChangeListener { _, _ -> calculateRoutes() }
        binding.switchAvoidFerries.setOnCheckedChangeListener { _, _ -> calculateRoutes() }
    }

    private fun setupSelectRouteButton() {
        binding.btnSelectRoute.setOnClickListener {
            Toast.makeText(this, R.string.select_route_round_trip, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun calculateRoutes() {
        val origin = GeoPoint(DEFAULT_ORIGIN_LATITUDE, DEFAULT_ORIGIN_LONGITUDE)
        val destination = GeoPoint(DEFAULT_DESTINATION_LATITUDE, DEFAULT_DESTINATION_LONGITUDE)
        scope.launch {
            val result = alternativeRoutes.calculateAlternatives(
                origin = origin,
                destination = destination,
                routeOptionsManager = routeOptionsManager
            )
            routeAdapter?.submitList(result.routes)
            binding.tvTitle.text = getString(R.string.route_found, result.totalCalculated)
        }
    }

    private fun selectRoute(route: AlternativeRoutes.AlternativeRoute) {
        val distance = DistanceCalculator.formatDistance(route.distanceMeters)
        val eta = DistanceCalculator.formatETA(route.etaMinutes)
        Toast.makeText(this, "${route.name}: $distance, $eta", Toast.LENGTH_SHORT).show()
    }

    inner class RouteOptionAdapter(
        private val onSelectClick: (AlternativeRoutes.AlternativeRoute) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<
            AlternativeRoutes.AlternativeRoute,
            RouteOptionAdapter.ViewHolder
            >(DIFF_CALLBACK) {
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_route_option, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) { holder.bind(getItem(position)) }
        inner class ViewHolder(
            itemView: android.view.View
        ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            private val tvRouteName: android.widget.TextView = itemView.findViewById(R.id.tvRouteName)
            private val tvRouteDistance: android.widget.TextView = itemView.findViewById(R.id.tvRouteDistance)
            private val tvRouteEta: android.widget.TextView = itemView.findViewById(R.id.tvRouteEta)
            private val tvRoutePreference: android.widget.TextView = itemView.findViewById(R.id.tvRoutePreference)
            private val btnSelectRoute: android.widget.Button = itemView.findViewById(R.id.btnSelectRoute)
            fun bind(route: AlternativeRoutes.AlternativeRoute) {
                tvRouteName.text = route.name
                tvRouteDistance.text = DistanceCalculator.formatDistance(route.distanceMeters)
                tvRouteEta.text = DistanceCalculator.formatETA(route.etaMinutes)
                tvRoutePreference.text = route.preference.label
                btnSelectRoute.setOnClickListener { onSelectClick(route) }
            }
        }
    }

    companion object {
        private const val DEFAULT_ORIGIN_LATITUDE = 35.6762
        private const val DEFAULT_ORIGIN_LONGITUDE = 139.6503
        private const val DEFAULT_DESTINATION_LATITUDE = 35.6586
        private const val DEFAULT_DESTINATION_LONGITUDE = 139.7454
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AlternativeRoutes.AlternativeRoute>() {
            override fun areItemsTheSame(
                old: AlternativeRoutes.AlternativeRoute,
                new: AlternativeRoutes.AlternativeRoute
            ) = old.id == new.id
            override fun areContentsTheSame(
                old: AlternativeRoutes.AlternativeRoute,
                new: AlternativeRoutes.AlternativeRoute
            ) = old == new
        }
    }
}
