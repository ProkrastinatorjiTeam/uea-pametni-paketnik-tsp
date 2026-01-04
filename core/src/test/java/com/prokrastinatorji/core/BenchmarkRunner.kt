package com.prokrastinatorji.core

import org.junit.Test
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class BenchmarkRunner {

    @Test
    fun runBenchmarks() {
        val files = listOf(
            "bays29.tsp",
            "eil101.tsp",
            "a280.tsp",
            "pr1002.tsp",
            "dca1389.tsp"
        )

        val teamName = "Prokrastinatorji"
        val runs = 30
        val popSize = 100
        val cr = 0.8
        val pm = 0.1

        // Create results directory
        val resultsDir = File("results")
        if (!resultsDir.exists()) {
            resultsDir.mkdirs()
        }

        // Use a thread pool to speed up runs
        // Number of threads = available processors (e.g., 8 or 16)
        val threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

        for (filename in files) {
            println("Running benchmark for $filename...")
            
            // Load problem once to get dimension
            val tempProblem = TSP(filename, 1000)
            val dimension = tempProblem.numberOfCities
            val maxFes = 1000 * dimension
            
            println("  Dimension: $dimension, MaxFes: $maxFes")

            val tasks = mutableListOf<Callable<Double>>()
            val completedCount = AtomicInteger(0)

            for (i in 1..runs) {
                tasks.add(Callable {
                    val problem = TSP(filename, maxFes)
                    val ga = GA(popSize, cr, pm)
                    val bestTour = ga.execute(problem)
                    
                    val current = completedCount.incrementAndGet()
                    print("\r  Progress: $current/$runs runs completed")
                    
                    bestTour.distance
                })
            }

            // Execute all 30 runs in parallel
            val futures = threadPool.invokeAll(tasks)
            val results = futures.map { it.get() }
            println() // New line after progress

            // Write results to file
            val outputFilename = "${teamName}_${filename.replace(".tsp", "")}.txt"
            val outputFile = File(resultsDir, outputFilename)
            
            val content = results.joinToString("\n")
            outputFile.writeText(content)
            
            println("  Saved results to ${outputFile.absolutePath}")
            
            // Calculate stats
            val min = results.minOrNull()
            val avg = results.average()
            val std = Math.sqrt(results.map { (it - avg) * (it - avg) }.sum() / results.size)
            
            println("  Stats: Min=$min, Avg=$avg, Std=$std")
            println("--------------------------------------------------")
        }
        
        threadPool.shutdown()
    }
}
