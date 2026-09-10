package com.nekogps.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.nekogps.app.utils.DistanceCalculator
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

/**
 * Search for nearby points of interest using Nominatim API.
 * Categories: Restaurant, Hospital, Gas Station, Hotel, Park.
 * Results shown in RecyclerView; tapping shows details.
 */
class PoiSearchActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var cgCategories: ChipGroup
    private lateinit var recyclerViewResults: RecyclerView
    private lateinit var adapter: PoiResultAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val results = mutableListOf<PoiResult>()
    private var selectedCategory: String = "Restaurant"

    companion object {
        private const val NOMINATIM_URL = "https://nominatim.openstreetmap.org/search"
        private const val DEFAULT_LAT = 40.7128
        private const val DEFAULT_LNG = -74.0060
        private const val CONNECT_TIMEOUT_MS = 15000
        private const val READ_TIMEOUT_MS = 15000
        private const val POI_NAME_PREVIEW_LENGTH = 30
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_poi_search)

        etSearch = findViewById(R.id.etSearch)
        cgCategories = findViewById(R.id.cgCategories)
        recyclerViewResults = findViewById(R.id.rvPoiResults)

        setupCategories()
        setupRecyclerView()
        setupSearch()
    }

    private fun setupCategories() {
        // Chips are already defined in XML, just set up listeners
        findViewById<Chip>(R.id.chipRestaurant).setOnClickListener {
            selectedCategory = "Restaurant"
            performSearchIfQueryExists()
        }
        findViewById<Chip>(R.id.chipHospital).setOnClickListener {
            selectedCategory = "Hospital"
            performSearchIfQueryExists()
        }
        findViewById<Chip>(R.id.chipGas).setOnClickListener {
            selectedCategory = "Gas Station"
            performSearchIfQueryExists()
        }
        findViewById<Chip>(R.id.chipHotel).setOnClickListener {
            selectedCategory = "Hotel"
            performSearchIfQueryExists()
        }
        findViewById<Chip>(R.id.chipPark).setOnClickListener {
            selectedCategory = "Park"
            performSearchIfQueryExists()
        }
    }

    private fun performSearchIfQueryExists() {
        val query = etSearch.text.toString().trim()
        if (query.isNotEmpty()) {
            performSearch(query)
        }
    }

    private fun setupRecyclerView() {
        recyclerViewResults.layoutManager = LinearLayoutManager(this)
        adapter = PoiResultAdapter { result ->
            onPoiSelected(result)
        }
        recyclerViewResults.adapter = adapter
    }

    private fun setupSearch() {
        findViewById<View>(R.id.btnSearch).setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(this, "Enter a search term", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performSearch(query)
        }
    }

    private fun performSearch(query: String) {
        Toast.makeText(this, "Searching for: $query", Toast.LENGTH_SHORT).show()

        thread {
            try {
                val searchQuery = "$selectedCategory near $query"
                val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                val url = URL("$NOMINATIM_URL?q=$encodedQuery&format=json&limit=20")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "NekoGPS/1.0")
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val jsonArray = JSONArray(response)
                    results.clear()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        results.add(
                            PoiResult(
                                name = obj.optString("display_name", "Unknown"),
                                lat = obj.getDouble("lat"),
                                lng = obj.getDouble("lon"),
                                category = selectedCategory,
                                type = obj.optString("type", "unknown"),
                                importance = obj.optDouble("importance", 0.0)
                            )
                        )
                    }

                    handler.post {
                        adapter.submitList(results.toList())
                        if (results.isEmpty()) {
                            Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    handler.post {
                        Toast.makeText(this, "Search failed: HTTP $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
                connection.disconnect()
            } catch (e: IOException) {
                Log.w("PoiSearchActivity", "Search failed", e)
                handler.post {
                    Toast.makeText(this, "Search error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: JSONException) {
                Log.w("PoiSearchActivity", "Search failed", e)
                handler.post {
                    Toast.makeText(this, "Search error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onPoiSelected(result: PoiResult) {
        Toast.makeText(this, "Selected: ${result.name.take(POI_NAME_PREVIEW_LENGTH)}...", Toast.LENGTH_SHORT).show()
    }

    data class PoiResult(
        val name: String,
        val lat: Double,
        val lng: Double,
        val category: String,
        val type: String,
        val importance: Double
    )

    inner class PoiResultAdapter(
        private val onClick: (PoiResult) -> Unit
    ) : RecyclerView.Adapter<PoiResultAdapter.PoiViewHolder>() {

        private var items: List<PoiResult> = emptyList()

        fun submitList(newItems: List<PoiResult>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PoiViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_poi, parent, false)
            return PoiViewHolder(view)
        }

        override fun onBindViewHolder(holder: PoiViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class PoiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvName: TextView = itemView.findViewById(R.id.tvPoiName)
            private val tvCategory: TextView = itemView.findViewById(R.id.tvPoiType)
            private val tvDistance: TextView = itemView.findViewById(R.id.tvPoiDistance)

            fun bind(result: PoiResult) {
                tvName.text = result.name
                tvCategory.text = result.category

                val dist = DistanceCalculator.haversineDistance(
                    DEFAULT_LAT, DEFAULT_LNG, result.lat, result.lng
                )
                tvDistance.text = DistanceCalculator.formatDistance(dist)

                itemView.setOnClickListener { onClick(result) }
            }
        }
    }
}
