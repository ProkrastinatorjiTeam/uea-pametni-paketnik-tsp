package com.prokrastinatorji.pametnipaketnik

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.util.Locale

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var textViewResult: TextView
    private var optimizedRoute: ArrayList<City>? = null
    private var totalDistance: Double = 0.0
    private var isTimeOptimization: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Get data from Intent
        optimizedRoute = intent.getParcelableArrayListExtra("optimized_route")
        totalDistance = intent.getDoubleExtra("total_distance", 0.0)
        isTimeOptimization = intent.getBooleanExtra("is_time_optimization", false)

        // Initialize views
        textViewResult = findViewById(R.id.textViewResult)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val route = optimizedRoute ?: return
        if (route.isEmpty()) return

        drawMarkersAndRoute(route)
        displayResult()
    }

    private fun drawMarkersAndRoute(route: List<City>) {
        val boundsBuilder = LatLngBounds.Builder()
        val polylineOptions = PolylineOptions().color(Color.BLUE).width(5f)

        for (city in route) {
            val location = LatLng(city.latitude, city.longitude)
            mMap.addMarker(MarkerOptions().position(location).title(city.name))
            boundsBuilder.include(location)
            polylineOptions.add(location)
        }

        if (route.size > 1) {
            val firstCityLocation = LatLng(route.first().latitude, route.first().longitude)
            polylineOptions.add(firstCityLocation)
        }
        
        mMap.addPolyline(polylineOptions)

        val bounds = boundsBuilder.build()
        val padding = 200 // Increased padding to not overlap with the result text view
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        
        mMap.setOnMapLoadedCallback {
            mMap.moveCamera(cameraUpdate)
        }
    }

    private fun displayResult() {
        val label = if (isTimeOptimization) "Skupni čas" else "Skupna dolžina"
        // The distance from the algorithm is based on Euclidean distance of coordinates,
        // which is not in km. We display it as a raw value.
        // To get real km, Haversine formula should be used in the core TSP problem.
        val unit = if (isTimeOptimization) "enot časa" else "enot razdalje"
        
        val resultText = String.format(Locale.getDefault(), "%s: %.2f %s", label, totalDistance, unit)
        textViewResult.text = resultText
    }
}