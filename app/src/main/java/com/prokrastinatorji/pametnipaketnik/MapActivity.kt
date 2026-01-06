package com.prokrastinatorji.pametnipaketnik

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var optimizedRoute: ArrayList<City>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        optimizedRoute = intent.getParcelableArrayListExtra("optimized_route")

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val route = optimizedRoute ?: return
        if (route.isEmpty()) return

        drawMarkersAndRoute(route)
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

        //Close loop
        if (route.size > 1) {
            val firstCityLocation = LatLng(route.first().latitude, route.first().longitude)
            polylineOptions.add(firstCityLocation)
        }
        
        mMap.addPolyline(polylineOptions)

        //Zoom
        val bounds = boundsBuilder.build()
        val padding = 150
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)

        mMap.setOnMapLoadedCallback {
            mMap.moveCamera(cameraUpdate)
        }
    }
}