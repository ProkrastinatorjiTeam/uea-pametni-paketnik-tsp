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
        // Changed to return full matrix to allow batching
        fun getFullMatrix(locations: List<Location>): Array<DoubleArray>
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
                .header("User-Agent", "UEA-Project-Student/1.0")
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

    // 2. Google Distance Matrix API (Batched)
    class GoogleDistanceMatrixService(private val apiKey: String) : DistanceMatrixService {
        private val client = OkHttpClient()
        private val gson = Gson()

        override fun getFullMatrix(locations: List<Location>): Array<DoubleArray> {
            val size = locations.size
            val matrix = Array(size) { DoubleArray(size) }
            
            if (apiKey.isBlank()) {
                println("WARNING: No Google API Key. Using Haversine.")
                val haversine = HaversineDistanceService()
                return haversine.getFullMatrix(locations)
            }

            // Batch size: 10x10 = 100 elements (Google limit is 100 elements per request)
            val batchSize = 10

            for (i in 0 until size step batchSize) {
                for (j in 0 until size step batchSize) {
                    // Prepare batch
                    val origins = locations.subList(i, min(i + batchSize, size))
                    val destinations = locations.subList(j, min(j + batchSize, size))
                    
                    val originsStr = origins.joinToString("|") { "${it.lat},${it.lng}" }
                    val destinationsStr = destinations.joinToString("|") { "${it.lat},${it.lng}" }

                    val url = "https://maps.googleapis.com/maps/api/distancematrix/json" +
                            "?origins=$originsStr" +
                            "&destinations=$destinationsStr" +
                            "&key=$apiKey"

                    val request = Request.Builder().url(url).build()
                    
                    println("Fetching Matrix Batch: Origins $i..${i+origins.size-1}, Dest $j..${j+destinations.size-1}")

                    try {
                        client.newCall(request).execute().use { response ->
                            val json = response.body?.string()
                            val map: Map<String, Any> = gson.fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
                            
                            val status = map["status"] as? String
                            if (status != "OK") {
                                println("API Error: $status. ${map["error_message"]}")
                                // Fallback to Haversine for this batch?
                                // For now, just leave 0.0 or implement fallback logic
                                return@use
                            }

                            val rows = map["rows"] as? List<Map<String, Any>>
                            
                            rows?.forEachIndexed { rowIndex, rowMap ->
                                val elements = rowMap["elements"] as? List<Map<String, Any>>
                                elements?.forEachIndexed { colIndex, elementMap ->
                                    val distanceObj = elementMap["distance"] as? Map<String, Any>
                                    val value = distanceObj?.get("value") as? Double // Meters
                                    
                                    if (value != null) {
                                        matrix[i + rowIndex][j + colIndex] = value
                                    } else {
                                        // Element status might be ZERO_RESULTS or NOT_FOUND
                                        // Use Haversine fallback
                                        matrix[i + rowIndex][j + colIndex] = HaversineDistanceService().calculate(
                                            locations[i + rowIndex], locations[j + colIndex]
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Error fetching batch: ${e.message}")
                    }
                    
                    // Respect rate limits slightly
                    Thread.sleep(100) 
                }
            }
            return matrix
        }
    }

    // 3. Haversine (Fallback)
    class HaversineDistanceService : DistanceMatrixService {
        override fun getFullMatrix(locations: List<Location>): Array<DoubleArray> {
            val size = locations.size
            val matrix = Array(size) { DoubleArray(size) }
            for (i in 0 until size) {
                for (j in 0 until size) {
                    matrix[i][j] = calculate(locations[i], locations[j])
                }
            }
            return matrix
        }

        fun calculate(from: Location, to: Location): Double {
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

        // 1. Load Locations
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
            
            locationsFile.writeText(gson.toJson(locations))
            println("Locations saved to cache.")
        }

        // 2. Load Distance Matrix
        val size = locations.size
        val weights: Array<DoubleArray>

        if (distancesFile.exists()) {
            println("Loading distance matrix from cache...")
            weights = gson.fromJson(distancesFile.readText(), Array<DoubleArray>::class.java)
        } else {
            println("Cache not found. Calculating distances...")
            
            val apiKey = Secrets.getGoogleApiKey() ?: ""
            val distanceService = if (useRealApis && apiKey.isNotEmpty()) {
                GoogleDistanceMatrixService(apiKey)
            } else {
                if (useRealApis) println("WARNING: No Google API Key found. Using Haversine.")
                HaversineDistanceService()
            }

            weights = distanceService.getFullMatrix(locations)
            
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
    
    class MockGeocodingService : GeocodingService {
        override fun getCoordinates(address: String): Pair<Double, Double> {
            val lat = 45.4 + RandomUtils.nextDouble() * (46.9 - 45.4)
            val lng = 13.3 + RandomUtils.nextDouble() * (16.6 - 13.3)
            return Pair(lat, lng)
        }
    }
}
