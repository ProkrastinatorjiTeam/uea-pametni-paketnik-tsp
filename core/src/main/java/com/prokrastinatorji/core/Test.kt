package com.prokrastinatorji.core

fun main() {
    println("ačenjam testiranje")
    RandomUtils.setSeed(12345L)

    val ga = GA(popSize = 10, cr = 0.8, pm = 0.1)
    val testTSP = TSP(10)
    ga.setProblemForTesting(testTSP)

    println("\nTestiranje Swap Mutacije")
    testSwapMutation(ga, testTSP)

    println("\nTestiranje Tournament Selekcije")
    testTournamentSelection(ga, testTSP)

    println("\nTestiranje PMX Križanja")
    testPMX(ga, testTSP)
}

fun testSwapMutation(ga: GA, problem: TSP) {
    val originalTour = problem.createOrderedTourForTest()

    println("Originalna pot: ${originalTour.path.map { it.id }.joinToString(", ")}")

    val tourToMutate = originalTour.copy()
    ga.swapMutation(tourToMutate)

    println("Mutirana pot:   ${tourToMutate.path.map { it.id }.joinToString(", ")}")

    val originalIds = (0 until problem.numberOfCities).toList()
    if (originalIds != tourToMutate.path.map { it.id }) {
        println("Mutacija je spremenila vrstni red.")
    } else {
        println("Mutacija ni spremenila vrstnega reda.")
    }
}

fun testTournamentSelection(ga: GA, problem: TSP) {
    val tour1 = problem.Tour(problem.numberOfCities).apply { distance = 100.0 }
    val tour2 = problem.Tour(problem.numberOfCities).apply { distance = 50.0 }
    val tour3 = problem.Tour(problem.numberOfCities).apply { distance = 75.0 }

    val population = mutableListOf(tour1, tour2, tour3)
    ga.setPopulationForTesting(population)

    println("Populacija z razdaljami: [100.0, 50.0, 75.0]")

    var bestWins = 0
    val numberOfRuns = 1000

    repeat(numberOfRuns) {
        val winner = ga.tournamentSelection(tournamentSize = 2)
        if (winner.distance == 50.0) {
            bestWins++
        }
    }

    println("V $numberOfRuns turnirjih (velikosti 2) je zmagal najboljši posameznik (distanca 50.0) $bestWins-krat.")

    if (bestWins > 0) {
        println("Selekcija deluje in daje prednost boljšim rešitvam.")
    } else {
        println("Najboljši ni nikoli zmagal.")
    }
}

fun testPMX(ga: GA, problem: TSP) {
    //Starš 1: [8, 4, 7, 3, 6, 2, 5, 1, 9, 0]
    //Starš 2: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
    val dimension = 10

    val testProblem = if (problem.numberOfCities >= dimension) problem else TSP(dimension)
    ga.setProblemForTesting(testProblem)

    val p1 = testProblem.Tour(dimension)
    val p2 = testProblem.Tour(dimension)

    val p1PathIds = listOf(8, 4, 7, 3, 6, 2, 5, 1, 9, 0)
    val p2PathIds = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

    p1.path = p1PathIds.map { id -> testProblem.City().apply { this.id = id } }.toTypedArray()
    p2.path = p2PathIds.map { id -> testProblem.City().apply { this.id = id } }.toTypedArray()

    println("Starš 1: ${p1.path.map { it.id }.joinToString(", ")}")
    println("Starš 2: ${p2.path.map { it.id }.joinToString(", ")}")

    println("Uporabljam fiksna presečišča: od indeksa 3 do 6")

    val children = ga.pmx(p1, p2)
    val child1 = children[0]
    val child2 = children[1]

    println("Potomec 1: ${child1.path.map { it.id }.joinToString(", ")}")
    println("Potomec 2: ${child2.path.map { it.id }.joinToString(", ")}")

    //Pričakovani rezultati za presečišča 3-6
    //Sredinski del P1: [3, 6, 2, 5] -> preslikave: 3<->3, 6<->5, 2<->4, 5<->6
    //Potomec 1 pričakovan: [0, 1, 4, 3, 6, 2, 5, 7, 8, 9]
    //Sredinski del P2: [3, 4, 5, 6] -> preslikave: 3<->3, 4<->6, 5<->2, 6<->5
    //Potomec 2 pričakovan: [8, 6, 7, 3, 4, 5, 6, 1, 9, 0
    //Ročni izračun za Potomec 2: [8, 2, 7, 3, 4, 5, 6, 1, 9, 0]

    val expectedChild1Ids = listOf(0, 1, 4, 3, 6, 2, 5, 7, 8, 9)
    val actualChild1Ids = child1.path.map { it.id }

    if (expectedChild1Ids == actualChild1Ids) {
        println("Potomec 1 je bil pravilno ustvarjen (z ročno nastavljenimi presečišči).")
    } else {
        println("Potomec 1 NI pravilen. Pričakovano: $expectedChild1Ids, Dejansko: $actualChild1Ids")
    }

    if (actualChild1Ids.toSet().size == dimension) {
        println("Potomec 1 vsebuje vsa mesta brez duplikatov.")
    } else {
        println("Potomec 1 ima duplikate ali manjkajoča mesta.")
    }
}