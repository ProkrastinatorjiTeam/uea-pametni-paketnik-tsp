package com.prokrastinatorji.core

import kotlin.random.Random

class GA(
    private val popSize: Int,
    private val cr: Double, //crossover
    private val pm: Double, //mutatuon
) {
    private lateinit var population: MutableList<TSP.Tour>
    private lateinit var offspring: MutableList<TSP.Tour>
    private lateinit var problem: TSP

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

            //TODO: shrani najboljšega
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
                //TODO: preveri, da starša nista enaka

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

            //TODO ovrednoti populacijo in shrani najboljšega (best)
            //implementacijo lahko naredimo bolj učinkovito tako, da overdnotimo samo tiste, ki so se spremenili (mutirani in križani potomci)

            population = offspring.toMutableList()
            offspring.clear()
        }

        return best!!
    }

    fun swapMutation(off: TSP.Tour) {
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

    fun pmx(parent1: TSP.Tour, parent2: TSP.Tour): Array<TSP.Tour> {
        val size = parent1.dimension
        val p1Path = parent1.path
        val p2Path = parent2.path

        val child1Path = Array<TSP.City?>(size) { null }
        val child2Path = Array<TSP.City?>(size) { null }

        //var cut1 = RandomUtils.nextInt(size)
        //var cut2 = RandomUtils.nextInt(size)

        //fiksno sam za test
        var cut1 = 3
        var cut2 = 6
        if (cut1 > cut2) {
            val temp = cut1
            cut1 = cut2
            cut2 = temp
        }

        for (i in cut1..cut2) {
            child1Path[i] = p1Path[i]
            child2Path[i] = p2Path[i]
        }

        for (i in 0 until size) {
            if (i < cut1 || i > cut2) {
                var cityFromP2 = p2Path[i]
                while (child1Path.contains(cityFromP2)) {
                    val indexInChild = child1Path.indexOf(cityFromP2)
                    cityFromP2 = p2Path[indexInChild]
                }
                child1Path[i] = cityFromP2

                var cityFromP1 = p1Path[i]
                while (child2Path.contains(cityFromP1)) {
                    val indexInChild = child2Path.indexOf(cityFromP1)
                    cityFromP1 = p1Path[indexInChild]
                }
                child2Path[i] = cityFromP1
            }
        }

        val child1 = problem.Tour(size)
        child1.path = child1Path.map { it!! }.toTypedArray()

        val child2 = problem.Tour(size)
        child2.path = child2Path.map { it!! }.toTypedArray()


        return arrayOf(child1, child2)
    }

    fun tournamentSelection(tournamentSize: Int = 2): TSP.Tour {
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

    fun setPopulationForTesting(testPopulation: MutableList<TSP.Tour>) {
        this.population = testPopulation
    }

    fun setProblemForTesting(testProblem: TSP) {
        this.problem = testProblem
    }
}