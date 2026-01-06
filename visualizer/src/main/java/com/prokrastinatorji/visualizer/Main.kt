package com.prokrastinatorji.visualizer

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.prokrastinatorji.core.GA
import com.prokrastinatorji.core.TSP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() = application {
    //RandomUtils.setSeedFromTime()

    /*val files = listOf(
        "bays29.tsp",
        "eil101.tsp",
        "a280.tsp",
        "pr1002.tsp",
        "dca1389.tsp"
    )*/


    val filename = "a280.tsp"
    val tempProblem = TSP(filename, 1000)
    val dimension = tempProblem.numberOfCities
    println("Število mest: $dimension")
    val maxFes = 1000 * dimension
    val problem = remember { TSP(filename, maxFes) }

    /*
    val problem = remember {
        println("Nalagam realni problem..")
        val realWorldLoader = RealWorldTSP()

        realWorldLoader.loadProblemFromCsv("Direct4me.csv", useRealApis = true)
    }*/

    val ga = remember { GA(popSize = 100, cr = 0.8, pm = 0.1) }

    var bestTourState by remember { mutableStateOf<TSP.Tour?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ga.setNewBestTourListener { newBestTour ->
            bestTourState = newBestTour
        }

        coroutineScope.launch(Dispatchers.Default) {
            println("Začenjam algoritem")
            val finalTour = ga.execute(problem)
            println("Algoritem je končal. Končna razdalja: ${finalTour.distance}")
        }
    }

    Window(onCloseRequest = ::exitApplication, title = "TSP Visualizer - ${problem.name}") {
        MaterialTheme {
            TSPVisualizer(tour = bestTourState)
        }
    }
}