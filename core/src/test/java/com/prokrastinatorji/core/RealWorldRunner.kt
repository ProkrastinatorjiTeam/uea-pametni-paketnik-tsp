package com.prokrastinatorji.core

import org.junit.Test
import java.io.File

class RealWorldRunner {

    @Test
    fun runRealWorldProblem() {
        println("Loading Real World Problem from Direct4me.csv...")
        
        val realWorldTSP = RealWorldTSP()
        // Load problem (uses Real APIs if available in secrets.properties)
        // Set useRealApis = true to attempt using Google Matrix and Nominatim
        val problem = realWorldTSP.loadProblemFromCsv("Direct4me.csv", useRealApis = true)
        
        println("Problem loaded: ${problem.name}")
        println("Number of cities: ${problem.numberOfCities}")
        
        val runs = 5 // Fewer runs for quick check
        val popSize = 100
        val cr = 0.8
        val pm = 0.1
        
        println("Max Evaluations: ${problem.maxEvaluations}")
        
        val results = mutableListOf<Double>()
        
        for (i in 1..runs) {
            // Reset problem state
            problem.numberOfEvaluations = 0
            
            val ga = GA(popSize, cr, pm)
            val bestTour = ga.execute(problem)
            
            results.add(bestTour.distance)
            println("Run $i/$runs: Distance = ${bestTour.distance} meters")
        }
        
        val min = results.minOrNull()
        val avg = results.average()
        
        println("--------------------------------------------------")
        println("Real World Results:")
        println("Min Distance: $min meters")
        println("Avg Distance: $avg meters")
        println("--------------------------------------------------")
        
        // Save to file
        val resultsDir = File("results")
        if (!resultsDir.exists()) resultsDir.mkdirs()
        val outputFile = File(resultsDir, "Prokrastinatorji_Direct4me.txt")
        outputFile.writeText(results.joinToString("\n"))
        println("Saved results to ${outputFile.absolutePath}")
    }
}
