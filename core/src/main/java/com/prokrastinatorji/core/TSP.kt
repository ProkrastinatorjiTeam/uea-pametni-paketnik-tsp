package com.prokrastinatorji.core

class TSP(
    val name: String,
    val cities: List<City>,
    private val distanceMatrix: Array<DoubleArray>
) {
    val dimension: Int
        get() = cities.size

    fun getDistance(city1: City, city2: City): Double {
        return distanceMatrix[city1.id - 1][city2.id - 1]
    }

    fun calculateDistance(tour: Tour): Double {
        val cities = tour.cities
        if (cities.isEmpty()) return 0.0

        var totalDistance = 0.0
        for (i in 0 until cities.size - 1) {
            totalDistance += getDistance(cities[i], cities[i + 1])
        }
        totalDistance += getDistance(cities.last(), cities.first())
        return totalDistance
    }
}