package com.prokrastinatorji.core

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.StringTokenizer
import kotlin.math.pow
import kotlin.math.sqrt

class TSP(
    val maxEvaluations: Int
) {
    enum class DistanceType { EUCLIDEAN, WEIGHTED }

    inner class City(
        var id: Int = 0,
        var x: Double = 0.0,
        var y: Double = 0.0
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is City) return false
            return id == other.id
        }

        override fun hashCode(): Int {
            return id
        }
    }

    inner class Tour(val dimension: Int) {
        var distance: Double = Double.MAX_VALUE
        var path: Array<City> = Array(dimension) { City() }

        constructor(tour: Tour) : this(tour.dimension) {
            this.distance = tour.distance
            this.path = tour.path.clone()
        }

        fun copy(): Tour {
            return Tour(this)
        }
    }

    var name: String? = null
    lateinit var start: City
    val cities = mutableListOf<City>()
    var numberOfCities = 0
    var weights: Array<DoubleArray>? = null
    var numberOfEvaluations = 0
    var distanceType = DistanceType.EUCLIDEAN

    // Constructor for file loading
    constructor(path: String, maxEvaluations: Int) : this(maxEvaluations) {
        loadData(path)
    }

    fun evaluate(tour: Tour) {
        var dist = 0.0
        for (i in 0 until numberOfCities - 1) {
            dist += calculateDistance(tour.path[i], tour.path[i + 1])
        }
        // Return to start
        dist += calculateDistance(tour.path[numberOfCities - 1], tour.path[0])
        
        tour.distance = dist
        numberOfEvaluations++
    }

    private fun calculateDistance(from: City, to: City): Double {
        return if (distanceType == DistanceType.WEIGHTED && weights != null) {
            // In .tsp files, indices are often 1-based.
            // We assume the City ID corresponds to the index in the weights matrix + 1.
            // So we subtract 1 to get 0-based array index.
            val i = from.id - 1
            val j = to.id - 1
            if (i in weights!!.indices && j in weights!!.indices) {
                weights!![i][j]
            } else {
                Double.MAX_VALUE // Error fallback
            }
        } else {
            // Euclidean
            sqrt((from.x - to.x).pow(2) + (from.y - to.y).pow(2))
        }
    }

    fun generateTour(): Tour {
        val tour = Tour(numberOfCities)
        // Create a shuffled list of cities
        val shuffledCities = cities.toMutableList()
        
        // Fisher-Yates shuffle using RandomUtils
        for (i in shuffledCities.size - 1 downTo 1) {
            val j = RandomUtils.nextInt(i + 1)
            val temp = shuffledCities[i]
            shuffledCities[i] = shuffledCities[j]
            shuffledCities[j] = temp
        }

        tour.path = shuffledCities.toTypedArray()
        return tour
    }

    private fun loadData(path: String) {
        val inputStream = javaClass.classLoader?.getResourceAsStream(path)
            ?: throw IllegalArgumentException("File $path not found in resources")

        val reader = BufferedReader(InputStreamReader(inputStream))
        var line = reader.readLine()
        var readingCoords = false
        var readingWeights = false
        
        // Temporary buffer for matrix reading
        val matrixValues = mutableListOf<Double>()

        while (line != null) {
            val trimmed = line.trim()
            
            // Check for section headers or EOF
            if (trimmed == "NODE_COORD_SECTION") {
                readingCoords = true
                readingWeights = false
                line = reader.readLine()
                continue
            } else if (trimmed == "EDGE_WEIGHT_SECTION") {
                readingWeights = true
                readingCoords = false
                line = reader.readLine()
                continue
            } else if (trimmed == "DISPLAY_DATA_SECTION" || trimmed == "EOF") {
                // Stop reading data
                readingCoords = false
                readingWeights = false
                break
            }

            if (!readingCoords && !readingWeights) {
                // Header parsing
                when {
                    trimmed.startsWith("NAME") -> name = trimmed.substringAfter(":").trim()
                    trimmed.startsWith("DIMENSION") -> numberOfCities = trimmed.substringAfter(":").trim().toInt()
                    trimmed.startsWith("EDGE_WEIGHT_TYPE") -> {
                        val type = trimmed.substringAfter(":").trim()
                        if (type == "EXPLICIT") {
                            distanceType = DistanceType.WEIGHTED
                        } else if (type == "EUC_2D") {
                            distanceType = DistanceType.EUCLIDEAN
                        }
                    }
                }
            } else if (readingCoords) {
                // Parse City: ID X Y
                val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                if (parts.size >= 3) {
                    try {
                        val id = parts[0].toInt()
                        val x = parts[1].toDouble()
                        val y = parts[2].toDouble()
                        cities.add(City(id, x, y))
                    } catch (e: NumberFormatException) {
                        // Ignore lines that don't parse (safety)
                    }
                }
            } else if (readingWeights) {
                // Parse Matrix
                val tokenizer = StringTokenizer(trimmed)
                while (tokenizer.hasMoreTokens()) {
                    val token = tokenizer.nextToken()
                    try {
                        matrixValues.add(token.toDouble())
                    } catch (e: NumberFormatException) {
                        // If we hit a non-number token while reading weights, it might be the start of a new section
                        // e.g. "DISPLAY_DATA_SECTION"
                        if (token == "DISPLAY_DATA_SECTION" || token == "EOF") {
                            readingWeights = false
                            break
                        }
                    }
                }
            }

            line = reader.readLine()
        }
        
        // Post-processing
        if (distanceType == DistanceType.WEIGHTED) {
            // Convert flat list to 2D array
            weights = Array(numberOfCities) { DoubleArray(numberOfCities) }
            var index = 0
            for (i in 0 until numberOfCities) {
                for (j in 0 until numberOfCities) {
                    if (index < matrixValues.size) {
                        weights!![i][j] = matrixValues[index++]
                    }
                }
            }
            // Create dummy cities for weighted graph if they weren't loaded
            if (cities.isEmpty()) {
                for (i in 1..numberOfCities) {
                    cities.add(City(i, 0.0, 0.0))
                }
            }
        }

        if (cities.isNotEmpty()) {
            start = cities[0]
        }
    }
}
