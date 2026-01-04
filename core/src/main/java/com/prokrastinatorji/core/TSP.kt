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
    lateinit var weights: Array<DoubleArray>
    var numberOfEvaluations = 0

    //samo za testiranje(lahko izbrises)
    constructor(dimension: Int, testMode: Boolean = true) : this("", 0) {
        if (testMode) {
            this.numberOfCities = dimension
            this.name = "TestProblem"
            for (i in 0 until dimension) {
                val city = City()
                city.id = i
                city.x = 0.0
                city.y = 0.0
                this.cities.add(city)
            }
        }
    }
    fun createOrderedTourForTest(): Tour {
        val tour = Tour(numberOfCities)
        tour.path = cities.toTypedArray()
        return tour
    }
    //konec stvari za testiranje

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