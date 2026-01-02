package com.prokrastinatorji.core

class EvolutionaryAlgorithm(
    private val problem: TSP,
    private val populationSize: Int,
    private val crossoverRate: Double,
    private val mutationRate: Double,
    private val elitism: Boolean = true
) {
    fun run(maxEvaluations: Int): Tour{
        //TODO: začetna populacija, glavna zanka(dokler fes<maxFes), Elitizem, selekcija, krizanje, mutacija.,vrne najboljso najdeno rešitev

        return Tour(emptyList(), Double.MAX_VALUE)
    }

    private fun tournamentSelection(population: List<Tour>): Tour{
        //TODO: implementacija turnirske selekcije

        return population.first()
    }

    private fun partiallyMappedCrossover(parent1: Tour, parent2: Tour): Tour{
        //TODO: Implementacija PMX krizanja

        return parent1
    }

    private fun swapMutation(tour: Tour) : Tour{
        //TODO: Implementacija mutacije z zamenjavo

        return tour
    }
}