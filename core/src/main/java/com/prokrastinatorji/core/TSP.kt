package com.prokrastinatorji.core

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.StringTokenizer
import kotlin.math.pow
import kotlin.math.sqrt

class TSP(
    var maxEvaluations: Int
) {
    enum class DistanceType { EUCLIDEAN, WEIGHTED }

    class City(
        var id: Int = 0,
        var x: Double = 0.0,
        var y: Double = 0.0
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is com.prokrastinatorji.core.TSP.City) return false
            return id == other.id
        }

        override fun hashCode(): Int {
            return id
        }
    }

    class Tour(val dimension: Int) {
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
    var start: City? = null
    val cities = mutableListOf<City>()
    var numberOfCities = 0
    var weights: Array<DoubleArray>? = null
    var numberOfEvaluations = 0
    var distanceType = DistanceType.EUCLIDEAN

    constructor() : this(0)

    constructor(path: String, maxEvaluations: Int) : this(maxEvaluations) {
        loadData(path)
    }

    fun evaluate(tour: Tour) {
        var dist = 0.0
        for (i in 0 until numberOfCities - 1) {
            dist += calculateDistance(tour.path[i], tour.path[i + 1])
        }
        dist += calculateDistance(tour.path[numberOfCities - 1], tour.path[0])

        tour.distance = dist
        numberOfEvaluations++
    }

    private fun calculateDistance(from: City, to: City): Double {
        return if (distanceType == DistanceType.WEIGHTED && weights != null) {
            val i = from.id - 1
            val j = to.id - 1
            if (i in weights!!.indices && j in weights!!.indices) {
                weights!![i][j]
            } else {
                Double.MAX_VALUE
            }
        } else {
            sqrt((from.x - to.x).pow(2) + (from.y - to.y).pow(2))
        }
    }

    fun generateTour(): Tour {
        val tour = Tour(numberOfCities)
        val shuffledCities = cities.toMutableList()

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
        var readingDisplayData = false

        val matrixValues = mutableListOf<Double>()

        while (line != null) {
            val trimmed = line.trim()

            if (trimmed == "NODE_COORD_SECTION") {
                readingCoords = true
                readingWeights = false
                readingDisplayData = false
                line = reader.readLine()
                continue
            } else if (trimmed == "EDGE_WEIGHT_SECTION") {
                readingWeights = true
                readingCoords = false
                readingDisplayData = false
                line = reader.readLine()
                continue
            } else if (trimmed == "DISPLAY_DATA_SECTION") {
                readingDisplayData = true
                readingCoords = false
                readingWeights = false
                line = reader.readLine()
                continue
            } else if (trimmed == "EOF") {
                readingCoords = false
                readingWeights = false
                readingDisplayData = false
                break
            }

            if (!readingCoords && !readingWeights && !readingDisplayData) {
                when {
                    trimmed.startsWith("NAME") -> name = trimmed.substringAfter(":").trim()
                    trimmed.startsWith("DIMENSION") -> {
                        numberOfCities = trimmed.substringAfter(":").trim().toInt()
                        if (cities.isEmpty()) {
                            for (i in 1..numberOfCities) {
                                cities.add(City(i, 0.0, 0.0))
                            }
                        }
                    }
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
                val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                if (parts.size >= 3) {
                    try {
                        val id = parts[0].toInt()
                        val x = parts[1].toDouble()
                        val y = parts[2].toDouble()
                        cities.find { it.id == id }?.let { it.x = x; it.y = y }
                    } catch (e: NumberFormatException) {}
                }
            } else if (readingWeights) {
                val tokenizer = StringTokenizer(trimmed)
                while (tokenizer.hasMoreTokens()) {
                    try {
                        matrixValues.add(tokenizer.nextToken().toDouble())
                    } catch (e: NumberFormatException) {}
                }
            } else if (readingDisplayData) {
                val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                if (parts.size >= 3) {
                    try {
                        val id = parts[0].toInt()
                        val x = parts[1].toDouble()
                        val y = parts[2].toDouble()
                        cities.find { it.id == id }?.let { it.x = x; it.y = y }
                    } catch (e: NumberFormatException) {}
                }
            }
            line = reader.readLine()
        }

        if (distanceType == DistanceType.WEIGHTED) {
            weights = Array(numberOfCities) { DoubleArray(numberOfCities) }
            var index = 0
            for (i in 0 until numberOfCities) {
                for (j in 0 until numberOfCities) {
                    if (index < matrixValues.size) {
                        weights!![i][j] = matrixValues[index++]
                    }
                }
            }
        }

        // Added check for empty list as suggested
        if (cities.isNotEmpty()) {
            start = cities[0]
        }
    }
}