package com.prokrastinatorji.core

import java.io.File
import java.util.Locale
import kotlin.math.sqrt

fun main() {
    RandomUtils.setSeedFromTime()

    runBenchmarkTests()

    runRealWorldTest()

    println("\nDone")
}

fun runBenchmarkTests() {
    println("\nZačenjam Benchmark teste")

    val files = listOf(
        "bays29.tsp", "eil101.tsp", "a280.tsp", "pr1002.tsp", "dca1389.tsp"
    )

    val teamName = "Prokrastinatorji"
    val runs = 30
    val popSize = 100
    val cr = 0.8
    val pm = 0.1

    val resultsDir = File("results")
    resultsDir.mkdirs()

    for (filename in files) {
        val tempProblem = TSP(filename, 1)
        val dimension = tempProblem.numberOfCities
        val maxFes = 1000 * dimension

        println("\nProblem: $filename (d=$dimension, maxFes=$maxFes)")

        val results = mutableListOf<Double>()

        for (i in 1..runs) {
            val problem = TSP(filename, maxFes)
            val ga = GA(popSize, cr, pm)
            val bestTour = ga.execute(problem)
            results.add(bestTour.distance)

            print("\r  Postopek: $i/$runs")
        }
        println("\r  Postopek: $runs/$runs ... Končano.")

        val outputFilename = "${teamName}_${filename.replace(".tsp", "")}.txt"
        val outputFile = File(resultsDir, outputFilename)
        outputFile.writeText(results.joinToString("\n") { String.format(Locale.US, "%.10f", it) })
        println("  Rezultati shranjeni v ${outputFile.path}")

        val min = results.minOrNull() ?: Double.NaN
        val avg = results.average()
        val std = sqrt(results.sumOf { (it - avg) * (it - avg) } / results.size)
        println("  Statistika: Min=%.4f, Avg=%.4f, Std=%.4f".format(Locale.US, min, avg, std))
    }
}

fun runRealWorldTest() {
    println("\nZačenjam test na realnem problemu (Direct4me)")

    val realWorldTSP = RealWorldTSP()
    val problem = realWorldTSP.loadProblemFromCsv("Direct4me.csv", useRealApis = true)

    println("Problem naložen: ${problem.name} (Št. mest: ${problem.numberOfCities})")

    val runs = 30
    val popSize = 100
    val cr = 0.8
    val pm = 0.1

    val results = mutableListOf<Double>()

    for (i in 1..runs) {
        problem.numberOfEvaluations = 0
        val ga = GA(popSize, cr, pm)
        val bestTour = ga.execute(problem)
        results.add(bestTour.distance)
        println("  Zagon $i/$runs: Najdena razdalja = %.2f m".format(Locale.US, bestTour.distance))
    }

    val min = results.minOrNull() ?: Double.NaN
    val avg = results.average()
    println("\nStatistika za realni problem")
    println("  Najboljša razdalja (Min): %.2f m".format(Locale.US, min))
    println("  Povprečna razdalja (Avg): %.2f m".format(Locale.US, avg))

    val teamName = "Prokrastinatorji"
    val resultsDir = File("results")
    val outputFile = File(resultsDir, "${teamName}_Direct4me.txt")
    outputFile.writeText(results.joinToString("\n") { String.format(Locale.US, "%.4f", it) })
    println("  Rezultati shranjeni v ${outputFile.path}")
}