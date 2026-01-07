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
    private var totalValue: Double = 0.0
    private var isTimeOptimization: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        optimizedRoute = intent.getParcelableArrayListExtra("optimized_route")
        totalValue = intent.getDoubleExtra("total_distance", 0.0)
        isTimeOptimization = intent.getBooleanExtra("is_time_optimization", false)

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
            mMap.addMarker(MarkerOptions().position(location).title("${city.name}, ${city.address}"))
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
        val resultText = if (isTimeOptimization) {
            val minutes = totalValue / 60
            String.format(Locale.getDefault(), "Skupni čas: %.0f min", minutes)
        } else {
            val kilometers = totalValue / 1000
            String.format(Locale.getDefault(), "Skupna dolžina: %.2f km", kilometers)
        }
        textViewResult.text = resultText
    }
}