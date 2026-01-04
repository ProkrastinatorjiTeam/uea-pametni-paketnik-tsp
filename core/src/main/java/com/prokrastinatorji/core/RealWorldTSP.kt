package com.prokrastinatorji.core

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
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

    // --- Services ---

    // 1. Nominatim (OpenStreetMap) - Free, No Key
    class NominatimGeocodingService : GeocodingService {
        private val client = OkHttpClient()
        private val gson = Gson()

        override fun getCoordinates(address: String): Pair<Double, Double> {
            // Respect usage policy: Max 1 request per second
            Thread.sleep(1100)

            val url = "https://nominatim.openstreetmap.org/search?q=${address.replace(" ", "+")}&format=json&limit=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "UEA-Project-Student/1.0") // Required by Nominatim
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Unexpected code $response")

                    val json = response.body?.string()
                    val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                    val results: List<Map<String, Any>> = gson.fromJson(json, listType)

                    if (results.isNotEmpty()) {
                        val lat = results[0]["lat"].toString().toDouble()
                        val lon = results[0]["lon"].toString().toDouble()
                        println("Geocoded: $address -> $lat, $lon")
                        return Pair(lat, lon)
                    }
                }
            } catch (e: Exception) {
                println("Error geocoding $address: ${e.message}")
            }
            return Pair(0.0, 0.0) // Failed
        }
    }

    // 2. Google Distance Matrix API
    class GoogleDistanceMatrixService(private val apiKey: String) : DistanceMatrixService {
        private val client = OkHttpClient()
        private val gson = Gson()

        override fun getDistance(from: Location, to: Location): Double {
            // Note: For 100x100, calling this one by one is inefficient and will hit rate limits.
            // Ideally, we should batch requests (origins=A|B|C&destinations=D|E|F).
            // But for simplicity in this assignment context, we'll implement single call logic
            // and rely on the Caching wrapper to store it.
            // WARNING: This will burn API quota fast. Use sparingly or implement batching.
            
            // Fallback to Haversine if key is invalid or empty
            if (apiKey.isBlank()) return HaversineDistanceService().getDistance(from, to)

            val url = "https://maps.googleapis.com/maps/api/distancematrix/json" +
                    "?origins=${from.lat},${from.lng}" +
                    "&destinations=${to.lat},${to.lng}" +
                    "&key=$apiKey"

            val request = Request.Builder().url(url).build()

            try {
                client.newCall(request).execute().use { response ->
                    val json = response.body?.string()
                    // Parse JSON manually or with Gson to find rows[0].elements[0].distance.value
                    // Simplified parsing:
                    val map: Map<String, Any> = gson.fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
                    val rows = map["rows"] as? List<Map<String, Any>>
                    val elements = rows?.get(0)?.get("elements") as? List<Map<String, Any>>
                    val distanceObj = elements?.get(0)?.get("distance") as? Map<String, Any>
                    val value = distanceObj?.get("value") as? Double // Meters

                    if (value != null) return value
                }
            } catch (e: Exception) {
                println("Error fetching distance: ${e.message}")
            }
            return HaversineDistanceService().getDistance(from, to) // Fallback
        }
    }

    // 3. Haversine (Fallback)
    class HaversineDistanceService : DistanceMatrixService {
        override fun getDistance(from: Location, to: Location): Double {
            val R = 6371e3
            val phi1 = from.lat * PI / 180
            val phi2 = to.lat * PI / 180
            val deltaPhi = (to.lat - from.lat) * PI / 180
            val deltaLambda = (to.lng - from.lng) * PI / 180

            val a = sin(deltaPhi / 2).pow(2) +
                    cos(phi1) * cos(phi2) *
                    sin(deltaLambda / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            return R * c
        }
    }

    // --- Caching Logic ---

    fun loadProblemFromCsv(
        csvPath: String,
        useRealApis: Boolean = false
    ): TSP {
        val cacheDir = File("cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val locationsFile = File(cacheDir, "locations.json")
        val distancesFile = File(cacheDir, "distances.json")
        val gson = Gson()

        // 1. Load Locations (from Cache or CSV+Geocode)
        val locations: MutableList<Location>
        if (locationsFile.exists()) {
            println("Loading locations from cache...")
            val type = object : TypeToken<List<Location>>() {}.type
            locations = gson.fromJson(locationsFile.readText(), type)
        } else {
            println("Cache not found. Parsing CSV and Geocoding...")
            locations = parseCsv(csvPath)
            
            val geocoder = if (useRealApis) NominatimGeocodingService() else MockGeocodingService()
            
            locations.forEachIndexed { index, loc ->
                print("Geocoding ${index + 1}/${locations.size}: ${loc.address}... ")
                val coords = geocoder.getCoordinates(loc.address)
                loc.lat = coords.first
                loc.lng = coords.second
                println("Done.")
            }
            
            // Save to cache
            locationsFile.writeText(gson.toJson(locations))
            println("Locations saved to cache.")
        }

        // 2. Load Distance Matrix (from Cache or API)
        val size = locations.size
        val weights: Array<DoubleArray>

        if (distancesFile.exists()) {
            println("Loading distance matrix from cache...")
            weights = gson.fromJson(distancesFile.readText(), Array<DoubleArray>::class.java)
        } else {
            println("Cache not found. Calculating distances...")
            weights = Array(size) { DoubleArray(size) }
            
            val apiKey = Secrets.getGoogleApiKey() ?: ""
            val distanceService = if (useRealApis && apiKey.isNotEmpty()) {
                GoogleDistanceMatrixService(apiKey)
            } else {
                if (useRealApis) println("WARNING: No Google API Key found. Using Haversine.")
                HaversineDistanceService()
            }

            // This is O(N^2). For 100 cities -> 10,000 calls.
            // If using Google API, this is BAD without batching.
            // For safety, if N > 10 and using Google, we should warn or fail.
            // But for Haversine it's instant.
            
            for (i in 0 until size) {
                for (j in 0 until size) {
                    if (i == j) {
                        weights[i][j] = 0.0
                    } else {
                        // Optimization: Matrix is symmetric?
                        // Real roads are NOT symmetric (one-way streets), but for approximation we often assume it.
                        // The assignment says: "Asymmetric Traveling Salesman Problem - ATSP".
                        // So we must calculate both directions.
                        weights[i][j] = distanceService.getDistance(locations[i], locations[j])
                    }
                }
                if (i % 10 == 0) println("Distance Matrix: Row $i/$size completed")
            }
            
            // Save to cache
            distancesFile.writeText(gson.toJson(weights))
            println("Distance matrix saved to cache.")
        }

        // 3. Create TSP Object
        val tsp = TSP(1000 * size)
        tsp.name = "Direct4Me Real World"
        tsp.numberOfCities = size
        tsp.cities.clear()
        
        locations.forEachIndexed { index, loc ->
            tsp.cities.add(tsp.City(index + 1, loc.lat, loc.lng))
        }
        
        if (tsp.cities.isNotEmpty()) tsp.start = tsp.cities[0]
        tsp.weights = weights
        tsp.distanceType = TSP.DistanceType.WEIGHTED
        
        return tsp
    }

    private fun parseCsv(csvPath: String): MutableList<Location> {
        val locations = mutableListOf<Location>()
        val inputStream = javaClass.classLoader?.getResourceAsStream(csvPath)
            ?: throw IllegalArgumentException("File $csvPath not found in resources")

        val reader = BufferedReader(InputStreamReader(inputStream))
        var line = reader.readLine()
        if (line != null && line.startsWith("Mesto")) line = reader.readLine()

        while (line != null) {
            // Handle CSV parsing better (e.g. quotes) if needed, but simple split for now
            val parts = line.split(",")
            if (parts.size >= 2) {
                val city = parts[0].trim()
                val address = parts[1].trim()
                val description = if (parts.size > 2) parts[2].trim() else ""
                val fullAddress = "$address, $city, Slovenia"
                locations.add(Location(city, fullAddress, description))
            }
            line = reader.readLine()
        }
        return locations
    }
    
    // Mock for fallback
    class MockGeocodingService : GeocodingService {
        override fun getCoordinates(address: String): Pair<Double, Double> {
            val lat = 45.4 + RandomUtils.nextDouble() * (46.9 - 45.4)
            val lng = 13.3 + RandomUtils.nextDouble() * (16.6 - 13.3)
            return Pair(lat, lng)
        }
    }
}
