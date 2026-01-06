package com.prokrastinatorji.pametnipaketnik

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.prokrastinatorji.core.GA
import com.prokrastinatorji.core.TSP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewCities: RecyclerView
    private lateinit var cityAdapter: CityAdapter
    private lateinit var progressBar: ProgressBar
    private val cities = mutableListOf<City>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerViewCities = findViewById(R.id.recyclerViewCities)
        progressBar = findViewById(R.id.progressBar)
        val buttonStart = findViewById<Button>(R.id.buttonStart)
        val editTextPopulationSize = findViewById<TextInputEditText>(R.id.editTextPopulationSize)
        val editTextCrossover = findViewById<TextInputEditText>(R.id.editTextCrossover)
        val editTextMutation = findViewById<TextInputEditText>(R.id.editTextMutation)


        recyclerViewCities.layoutManager = LinearLayoutManager(this)
        cities.addAll(getDummyCities())
        cityAdapter = CityAdapter(cities)
        recyclerViewCities.adapter = cityAdapter

        buttonStart.setOnClickListener {
            val selectedCities = cities.filter { it.isSelected }
            if (selectedCities.isEmpty()) {
                Toast.makeText(this, "Izberite vsaj eno mesto.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val populationSize = editTextPopulationSize.text.toString().toIntOrNull() ?: 100
            val crossoverProbability = editTextCrossover.text.toString().toDoubleOrNull() ?: 0.8
            val mutationProbability = editTextMutation.text.toString().toDoubleOrNull() ?: 0.1

            progressBar.visibility = View.VISIBLE
            buttonStart.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                // --- Real algorithm execution ---
                
                // 1. Manually create and configure the TSP problem
                val tspProblem = TSP()
                tspProblem.maxEvaluations = 1000 * selectedCities.size
                tspProblem.numberOfCities = selectedCities.size
                tspProblem.distanceType = TSP.DistanceType.EUCLIDEAN
                
                // Convert app-level City to core-level TSP.City and add to problem
                selectedCities.forEachIndexed { index, appCity ->
                    val coreCity = TSP.City(
                        id = index + 1,
                        x = appCity.longitude,
                        y = appCity.latitude
                    )
                    tspProblem.cities.add(coreCity)
                }
                if (tspProblem.cities.isNotEmpty()) {
                    tspProblem.start = tspProblem.cities[0]
                }

                // 2. Initialize and run the evolutionary algorithm
                val algorithm = GA(populationSize, crossoverProbability, mutationProbability)
                val resultTour = algorithm.execute(tspProblem)

                // 3. Convert the result back to a list of app-level City objects
                val optimizedRoute = resultTour.path.map { coreCity ->
                    selectedCities.first { appCity ->
                        appCity.longitude == coreCity.x && appCity.latitude == coreCity.y
                    }
                }
                // --- End of real execution ---

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    buttonStart.isEnabled = true

                    val intent = Intent(this@MainActivity, MapActivity::class.java).apply {
                        putParcelableArrayListExtra("optimized_route", ArrayList(optimizedRoute))
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun getDummyCities(): List<City> {
        return listOf(
            City("Ljubljana", 46.0569, 14.5058),
            City("Maribor", 46.5547, 15.6467),
            City("Celje", 46.2389, 15.2674),
            City("Kranj", 46.2389, 14.3556),
            City("Koper", 45.5469, 13.7295)
        )
    }
}