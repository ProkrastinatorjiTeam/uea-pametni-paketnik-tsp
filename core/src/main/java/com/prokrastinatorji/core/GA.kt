package com.prokrastinatorji.core

typealias NewBestTourListener = (TSP.Tour) -> Unit

class GA(
    private val popSize: Int,
    private val cr: Double, //crossover
    private val pm: Double, //mutatuon
) {
    private lateinit var population: MutableList<TSP.Tour>
    private lateinit var offspring: MutableList<TSP.Tour>
    private lateinit var problem: TSP

    private var newBestTourListener: NewBestTourListener? = null

    fun setNewBestTourListener(listener: NewBestTourListener) {
        this.newBestTourListener = listener
    }

    fun execute(problem: TSP): TSP.Tour {
        this.problem = problem

        population = mutableListOf()
        offspring = mutableListOf()
        var best: TSP.Tour? = null

        //INICIALIZACIJA
        repeat(popSize) {
            val newTour = problem.generateTour()
            problem.evaluate(newTour)
            population.add(newTour)

            if (best == null || newTour.distance < best!!.distance) {
                best = newTour.copy()

                newBestTourListener?.invoke(best)
            }
        }

        //GLAVNA ZANKA
        while (problem.numberOfEvaluations < problem.maxEvaluations) {

            //ELITIZEM
            val elite = population.minByOrNull { it.distance }!!
            offspring.add(elite.copy())

            //GLAVNA ZANKA GENERACIJE
            while (offspring.size < popSize) {
                val parent1 = tournamentSelection()
                var parent2 = tournamentSelection()

                while (parent1 === parent2) {
                    parent2 = tournamentSelection()
                }

                if (RandomUtils.nextDouble() < cr) {
                    val children = pmx(parent1, parent2)
                    offspring.add(children[0])
                    if (offspring.size < popSize) {
                        offspring.add(children[1])
                    }
                } else {
                    offspring.add(parent1.copy())
                    if (offspring.size < popSize) {
                        offspring.add(parent2.copy())
                    }
                }
            }

            for (off in offspring) {
                if (RandomUtils.nextDouble() < pm) {
                    swapMutation(off)
                }
            }

            for (tour in offspring) {
                if (tour.distance == Double.MAX_VALUE) {
                    problem.evaluate(tour)
                }
                if (tour.distance < best!!.distance) {
                    best = tour.copy()

                    newBestTourListener?.invoke(best)
                }
            }

            population = offspring.toMutableList()
            offspring.clear()
        }

        return best!!
    }

    private fun swapMutation(off: TSP.Tour) {
        val path = off.path
        val size = path.size

        if (size < 2) return

        val index1 = RandomUtils.nextInt(size)
        var index2 = RandomUtils.nextInt(size)
        while (index1 == index2) {
            index2 = RandomUtils.nextInt(size)
        }

        val tempCity = path[index1]
        path[index1] = path[index2]
        path[index2] = tempCity

        off.distance = Double.MAX_VALUE
    }

    private fun pmx(parent1: TSP.Tour, parent2: TSP.Tour): Array<TSP.Tour> {
        val size = parent1.dimension
        val p1Path = parent1.path
        val p2Path = parent2.path

        val child1Path = Array<TSP.City?>(size) { null }
        val child2Path = Array<TSP.City?>(size) { null }

        var cut1 = RandomUtils.nextInt(size)
        var cut2 = RandomUtils.nextInt(size)
        if (cut1 > cut2) {
            val temp = cut1; cut1 = cut2; cut2 = temp
        }

        for (i in cut1..cut2) {
            child1Path[i] = p1Path[i]
            child2Path[i] = p2Path[i]
        }

        for (i in 0 until size) {
            if (i >= cut1 && i <= cut2) continue

            //Potomec 1
            var cityToInsert = p2Path[i]
            while (child1Path.slice(cut1..cut2).contains(cityToInsert)) {
                val indexInP1 = p1Path.indexOfFirst { it.id == cityToInsert.id }

                cityToInsert = p2Path[indexInP1]
            }
            child1Path[i] = cityToInsert

            //Potomec 2
            cityToInsert = p1Path[i]
            while (child2Path.slice(cut1..cut2).contains(cityToInsert)) {
                val indexInP2 = p2Path.indexOfFirst { it.id == cityToInsert.id }
                cityToInsert = p1Path[indexInP2]
            }
            child2Path[i] = cityToInsert
        }

        val child1 = TSP.Tour(size)
        child1.path = child1Path.map { it!! }.toTypedArray()

        val child2 = TSP.Tour(size)
        child2.path = child2Path.map { it!! }.toTypedArray()

        return arrayOf(child1, child2)
    }

    private fun tournamentSelection(tournamentSize: Int = 2): TSP.Tour {
        val selectedIndices = mutableSetOf<Int>()
        while (selectedIndices.size < tournamentSize) {
            selectedIndices.add(RandomUtils.nextInt(population.size))
        }

        var bestContestant: TSP.Tour? = null
        for (index in selectedIndices) {
            val contestant = population[index]
            if (bestContestant == null || contestant.distance < bestContestant.distance) {
                bestContestant = contestant
            }
        }

        return bestContestant!!
    }
}