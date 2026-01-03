package com.prokrastinatorji.core

class TSP(
    path: String,
    val maxEvaluations: Int
) {
    enum class DistanceType { EUCLIDEAN, WEIGHTED }

    inner class City {
        var id: Int = 0
        var x: Double = 0.0
        var y: Double = 0.0
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
    lateinit var weights: Array<DoubleArray>
    var numberOfEvaluations = 0

    init {
        loadData(path)
    }

    fun evaluate(tour: Tour) {
        //TODO
        numberOfEvaluations++
    }

    private fun calculateDistance(from: City, to: City): Double {
        //TODO
        return 0.0
    }

    fun generateTour(): Tour {
        //TODO
        return Tour(numberOfCities)
    }

    private fun loadData(path: String) {
        //TODO:
    }
}