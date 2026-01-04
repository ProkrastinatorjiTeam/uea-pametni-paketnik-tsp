package com.prokrastinatorji.core

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.*

class RealWorldTSP {

    data class Location(
        val name: String,
        val address: String,
        val description: String,
        var lat: Double = 0.0,
        var lng: Double = 0.0
    )

    interface GeocodingService {
        fun getCoordinates(address: String): Pair<Double, Double>
    }

    interface DistanceMatrixService {
        fun getDistance(from: Location, to: Location): Double
    }

    // Mock implementation for testing
    class MockGeocodingService : GeocodingService {
        override fun getCoordinates(address: String): Pair<Double, Double> {
            // Generate random coordinates roughly within Slovenia
            // Lat: 45.4 - 46.9
            // Lng: 13.3 - 16.6
            val lat = 45.4 + RandomUtils.nextDouble() * (46.9 - 45.4)
            val lng = 13.3 + RandomUtils.nextDouble() * (16.6 - 13.3)
            return Pair(lat, lng)
        }
    }

    // Mock implementation using Haversine distance (air distance)
    class MockDistanceMatrixService : DistanceMatrixService {
        override fun getDistance(from: Location, to: Location): Double {
            return haversine(from.lat, from.lng, to.lat, to.lng)
        }

        private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val R = 6371e3 // Earth radius in meters
            val phi1 = lat1 * PI / 180
            val phi2 = lat2 * PI / 180
            val deltaPhi = (lat2 - lat1) * PI / 180
            val deltaLambda = (lon2 - lon1) * PI / 180

            val a = sin(deltaPhi / 2).pow(2) +
                    cos(phi1) * cos(phi2) *
                    sin(deltaLambda / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            return R * c // Distance in meters
        }
    }

    fun loadProblemFromCsv(
        csvPath: String,
        geocodingService: GeocodingService = MockGeocodingService(),
        distanceMatrixService: DistanceMatrixService = MockDistanceMatrixService()
    ): TSP {
        val locations = mutableListOf<Location>()
        
        val inputStream = javaClass.classLoader?.getResourceAsStream(csvPath)
            ?: throw IllegalArgumentException("File $csvPath not found in resources")

        val reader = BufferedReader(InputStreamReader(inputStream))
        var line = reader.readLine() // Skip header if present
        
        // Check if first line is header
        if (line != null && line.startsWith("Mesto")) {
            line = reader.readLine()
        }

        while (line != null) {
            val parts = line.split(",") // Simple CSV split, might break if address contains commas
            if (parts.size >= 2) {
                val city = parts[0].trim()
                val address = parts[1].trim()
                val description = if (parts.size > 2) parts[2].trim() else ""
                
                val fullAddress = "$address, $city, Slovenia"
                val coords = geocodingService.getCoordinates(fullAddress)
                
                locations.add(Location(city, fullAddress, description, coords.first, coords.second))
            }
            line = reader.readLine()
        }

        // Create TSP object
        val tsp = TSP(1000 * locations.size) 
        
        tsp.name = "Direct4Me Real World"
        tsp.numberOfCities = locations.size
        tsp.cities.clear()
        
        // Fill cities with 1-based IDs
        locations.forEachIndexed { index, loc ->
            tsp.cities.add(tsp.City(index + 1, loc.lat, loc.lng))
        }
        
        if (tsp.cities.isNotEmpty()) {
            tsp.start = tsp.cities[0]
        }

        // Calculate Weights Matrix
        tsp.weights = Array(locations.size) { DoubleArray(locations.size) }
        for (i in locations.indices) {
            for (j in locations.indices) {
                if (i == j) {
                    tsp.weights!![i][j] = 0.0
                } else {
                    tsp.weights!![i][j] = distanceMatrixService.getDistance(locations[i], locations[j])
                }
            }
        }
        
        // Force TSP to use WEIGHTED distance
        tsp.distanceType = TSP.DistanceType.WEIGHTED
        
        return tsp
    }
}
