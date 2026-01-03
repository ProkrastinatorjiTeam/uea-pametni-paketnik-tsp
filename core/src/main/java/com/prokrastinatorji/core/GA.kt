package com.prokrastinatorji.core

import kotlin.random.Random

class GA(
    private val popSize: Int,
    private val cr: Double, //crossover
    private val pm: Double, //mutatuon
) {
    private lateinit var population: MutableList<TSP.Tour>
    private lateinit var offspring: MutableList<TSP.Tour>

    fun execute(problem: TSP): TSP.Tour {
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

        return TODO("Provide the return value")
    }

    private fun tournamentSelection(tournamentSize: Int = 2): TSP.Tour {
        val selectedIndices = mutableListOf<Int>()
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