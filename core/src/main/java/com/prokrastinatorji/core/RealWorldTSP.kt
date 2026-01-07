package com.prokrastinatorji.core

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.math.*

class RealWorldTSP {

    enum class Metric { DISTANCE, TIME }

    data class Location(
        val name: String,
        val address: String,
        val description: String,
        var lat: Double = 0.0,
        var lng: Double = 0.0
    )

    interface GeocodingService {
        fun getCoordinates(address: String, city: String): Pair<Double, Double>
    }

    interface DistanceMatrixService {
        fun getFullMatrices(locations: List<Location>): Pair<Array<DoubleArray>, Array<DoubleArray>>
    }

    class GoogleGeocodingService(private val apiKey: String) : GeocodingService {
        private val client = OkHttpClient()
        private val gson = Gson()

        override fun getCoordinates(address: String, city: String): Pair<Double, Double> {
            if (apiKey.isBlank()) return Pair(0.0, 0.0)

            var coords = fetch(address)
            if (coords.first == 0.0 && coords.second == 0.0) {
                println("Geocoding fallback for '$address' -> '$city, Slovenia'")
                coords = fetch("$city, Slovenia")
            }
            return coords
        }

        private fun fetch(query: String): Pair<Double, Double> {
            Thread.sleep(50)
            val url = "https://maps.googleapis.com/maps/api/geocode/json".toHttpUrl().newBuilder()
                .addQueryParameter("address", query)
                .addQueryParameter("key", apiKey)
                .build()

            val request = Request.Builder().url(url).build()
            try {
                client.newCall(request).execute().use { response ->
                    val json = response.body?.string()
                    val result = gson.fromJson(json, GeocodingResult::class.java)
                    if (result.status == "OK" && result.results.isNotEmpty()) {
                        val location = result.results[0].geometry.location
                        println("Geocoded: $query -> ${location.lat}, ${location.lng}")
                        return Pair(location.lat, location.lng)
                    } else {
                        println("Geocoding failed for '$query': ${result.status}")
                    }
                }
            } catch (e: Exception) {
                println("Error geocoding '$query': ${e.message}")
            }
            return Pair(0.0, 0.0)
        }

        data class GeocodingResult(val results: List<Result>, val status: String)
        data class Result(val geometry: Geometry)
        data class Geometry(val location: LatLng)
        data class LatLng(val lat: Double, val lng: Double)
    }

    class GoogleDistanceMatrixService(private val apiKey: String) : DistanceMatrixService {
        private val client = OkHttpClient()
        private val gson = Gson()

        override fun getFullMatrices(locations: List<Location>): Pair<Array<DoubleArray>, Array<DoubleArray>> {
            val size = locations.size
            val distMatrix = Array(size) { DoubleArray(size) }
            val durMatrix = Array(size) { DoubleArray(size) }
            
            if (apiKey.isBlank()) {
                return HaversineDistanceService().getFullMatrices(locations)
            }

            val batchSize = 10
            for (i in 0 until size step batchSize) {
                for (j in 0 until size step batchSize) {
                    val origins = locations.subList(i, min(i + batchSize, size))
                    val destinations = locations.subList(j, min(j + batchSize, size))
                    
                    val originsStr = origins.joinToString("|") { "${it.lat},${it.lng}" }
                    val destinationsStr = destinations.joinToString("|") { "${it.lat},${it.lng}" }

                    val url = "https://maps.googleapis.com/maps/api/distancematrix/json".toHttpUrl().newBuilder()
                            .addQueryParameter("origins", originsStr)
                            .addQueryParameter("destinations", destinationsStr)
                            .addQueryParameter("key", apiKey).build()

                    println("Fetching Matrix Batch: Origins $i..${i+origins.size-1}, Dest $j..${j+destinations.size-1}")
                    var batchSuccess = false
                    try {
                        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                            val result: DistanceMatrixResult = gson.fromJson(response.body?.string(), DistanceMatrixResult::class.java)
                            if (result.status == "OK") {
                                result.rows.forEachIndexed { rowIndex, row ->
                                    row.elements.forEachIndexed { colIndex, element ->
                                        distMatrix[i + rowIndex][j + colIndex] = element.distance?.value?.toDouble() ?: 0.0
                                        durMatrix[i + rowIndex][j + colIndex] = element.duration?.value?.toDouble() ?: 0.0
                                    }
                                }
                                batchSuccess = true
                            } else {
                                println("API Error: ${result.status}. ${result.error_message}")
                            }
                        }
                    } catch (e: Exception) {
                        println("Error fetching batch: ${e.message}")
                    }
                    if (!batchSuccess) {
                        println("Batch failed. Running fallback for this batch.")
                        for (rowIndex in origins.indices) {
                            for (colIndex in destinations.indices) {
                                val dist = HaversineDistanceService().calculate(locations[i + rowIndex], locations[j + colIndex])
                                distMatrix[i + rowIndex][j + colIndex] = dist
                                durMatrix[i + rowIndex][j + colIndex] = dist / 16.6
                            }
                        }
                    }
                }
            }
            return Pair(distMatrix, durMatrix)
        }

        data class DistanceMatrixResult(val rows: List<Row>, val status: String, val error_message: String?)
        data class Row(val elements: List<Element>)
        data class Element(val distance: Value?, val duration: Value?, val status: String)
        data class Value(val value: Int)
    }

    class HaversineDistanceService : DistanceMatrixService {
        override fun getFullMatrices(locations: List<Location>): Pair<Array<DoubleArray>, Array<DoubleArray>> {
            val size = locations.size
            val distMatrix = Array(size) { DoubleArray(size) }
            val durMatrix = Array(size) { DoubleArray(size) }
            
            for (i in 0 until size) {
                for (j in 0 until size) {
                    val dist = calculate(locations[i], locations[j])
                    distMatrix[i][j] = dist
                    durMatrix[i][j] = dist / 16.66
                }
            }
            return Pair(distMatrix, durMatrix)
        }

        fun calculate(from: Location, to: Location): Double {
            val R = 6371e3
            val phi1 = from.lat * PI / 180
            val phi2 = to.lat * PI / 180
            val deltaPhi = (to.lat - from.lat) * PI / 180
            val deltaLambda = (to.lng - from.lng) * PI / 180

            val a = sin(deltaPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(deltaLambda / 2).pow(2)
            return R * (2 * atan2(sqrt(a), sqrt(1 - a)))
        }
    }

    fun loadProblemFromCsv(
        csvPath: String,
        useRealApis: Boolean = false,
        metric: Metric = Metric.DISTANCE
    ): TSP {
        val cacheDir = File("cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val locationsFile = File(cacheDir, "locations.json")
        val distancesFile = File(cacheDir, "distances.json")
        val durationsFile = File(cacheDir, "durations.json")
        val gson = Gson()

        val locations: MutableList<Location>
        if (locationsFile.exists()) {
            println("Loading locations from cache...")
            locations = gson.fromJson(locationsFile.readText(), object : TypeToken<List<Location>>() {}.type)
        } else {
            println("Cache not found. Parsing CSV and Geocoding...")
            locations = parseCsv(csvPath)
            val apiKey = Secrets.getGoogleApiKey() ?: ""
            val geocoder = if (useRealApis && apiKey.isNotEmpty()) GoogleGeocodingService(apiKey) else MockGeocodingService()

            locations.forEachIndexed { index, loc ->
                val coords = geocoder.getCoordinates(loc.address, loc.name)
                loc.lat = coords.first
                loc.lng = coords.second

                if (loc.lat == 0.0 && loc.lng == 0.0) {
                    println("  WARNING: Failed to geocode ${loc.address}. Setting to Ljubljana default.")
                    loc.lat = 46.056947
                    loc.lng = 14.505751
                }
            }

            locationsFile.writeText(gson.toJson(locations))
            println("Locations saved to cache.")
        }

        val size = locations.size
        var distMatrix: Array<DoubleArray>?
        var durMatrix: Array<DoubleArray>?

        if (distancesFile.exists() && durationsFile.exists()) {
            println("Loading matrices from cache...")
            distMatrix = gson.fromJson(distancesFile.readText(), Array<DoubleArray>::class.java)
            durMatrix = gson.fromJson(durationsFile.readText(), Array<DoubleArray>::class.java)
        } else {
            println("Cache not found. Calculating matrices...")

            val apiKey = Secrets.getGoogleApiKey() ?: ""
            val matrixService = if (useRealApis && apiKey.isNotEmpty()) GoogleDistanceMatrixService(apiKey) else HaversineDistanceService()

            val matrices = matrixService.getFullMatrices(locations)
            distMatrix = matrices.first
            durMatrix = matrices.second

            distancesFile.writeText(gson.toJson(distMatrix))
            durationsFile.writeText(gson.toJson(durationsFile))
            println("Matrices saved to cache.")
        }

        val tsp = TSP(1000 * size)
        tsp.name = "Direct4Me Real World"
        tsp.numberOfCities = size
        tsp.cities.clear()
        
        locations.forEachIndexed { index, loc ->
            tsp.cities.add(TSP.City(index + 1, loc.lat, loc.lng))
        }
        
        if (tsp.cities.isNotEmpty()) tsp.start = tsp.cities[0]

        tsp.weights = if (metric == Metric.DISTANCE) distMatrix else durMatrix
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
        override fun getCoordinates(address: String, city: String): Pair<Double, Double> {
            val lat = 45.4 + RandomUtils.nextDouble() * (46.9 - 45.4)
            val lng = 13.3 + RandomUtils.nextDouble() * (16.6 - 13.3)
            return Pair(lat, lng)
        }
    }
}
