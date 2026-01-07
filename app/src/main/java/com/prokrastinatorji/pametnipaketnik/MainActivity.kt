package com.prokrastinatorji.pametnipaketnik

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prokrastinatorji.core.GA
import com.prokrastinatorji.core.TSP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewCities: RecyclerView
    private lateinit var cityAdapter: CityAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonStart: Button
    private lateinit var buttonSelectAll: Button

    private val allCities = mutableListOf<City>()
    private var distanceMatrix: Array<DoubleArray>? = null
    private var durationMatrix: Array<DoubleArray>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerViewCities = findViewById(R.id.recyclerViewCities)
        progressBar = findViewById(R.id.progressBar)
        buttonStart = findViewById(R.id.buttonStart)
        buttonSelectAll = findViewById(R.id.buttonSelectAll)
        val editTextPopulationSize = findViewById<TextInputEditText>(R.id.editTextPopulationSize)
        val editTextCrossover = findViewById<TextInputEditText>(R.id.editTextCrossover)
        val editTextMutation = findViewById<TextInputEditText>(R.id.editTextMutation)
        val radioTime = findViewById<RadioButton>(R.id.radioButtonTime)

        loadDataFromAssets()

        recyclerViewCities.layoutManager = LinearLayoutManager(this)
        cityAdapter = CityAdapter(allCities)
        recyclerViewCities.adapter = cityAdapter

        buttonSelectAll.setOnClickListener {
            val allSelected = allCities.all { it.isSelected }
            val newState = !allSelected
            allCities.forEach { it.isSelected = newState }
            cityAdapter.notifyDataSetChanged()
            buttonSelectAll.text = if (newState) "Počisti vse" else "Izberi vse"
        }

        buttonStart.setOnClickListener {
            val selectedCities = allCities.filter { it.isSelected }
            if (selectedCities.isEmpty()) {
                Toast.makeText(this, "Izberite vsaj eno mesto.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val populationSize = editTextPopulationSize.text.toString().toIntOrNull() ?: 100
            val crossoverProbability = editTextCrossover.text.toString().toDoubleOrNull() ?: 0.8
            val mutationProbability = editTextMutation.text.toString().toDoubleOrNull() ?: 0.1
            val isOptimizingTime = radioTime.isChecked

            setUiEnabled(false)

            lifecycleScope.launch(Dispatchers.IO) {

                val selectedIndices = selectedCities.map { allCities.indexOf(it) }
                val sourceMatrix = if (isOptimizingTime) durationMatrix else distanceMatrix
                val subMatrix = Array(selectedCities.size) { i ->
                    DoubleArray(selectedCities.size) { j ->
                        sourceMatrix?.get(selectedIndices[i])?.get(selectedIndices[j]) ?: 0.0
                    }
                }

                val tspProblem = TSP()
                tspProblem.maxEvaluations = 1000 * selectedCities.size
                tspProblem.numberOfCities = selectedCities.size
                tspProblem.distanceType = TSP.DistanceType.WEIGHTED
                tspProblem.weights = subMatrix
                
                selectedCities.forEachIndexed { index, _ ->
                    tspProblem.cities.add(TSP.City(id = index + 1))
                }
                if (tspProblem.cities.isNotEmpty()) {
                    tspProblem.start = tspProblem.cities[0]
                }

                val algorithm = GA(populationSize, crossoverProbability, mutationProbability)
                val resultTour = algorithm.execute(tspProblem)

               val optimizedRoute = resultTour.path.map { coreCity ->
                    selectedCities[coreCity.id - 1]
                }

                withContext(Dispatchers.Main) {
                    setUiEnabled(true)
                    val intent = Intent(this@MainActivity, MapActivity::class.java).apply {
                        putParcelableArrayListExtra("optimized_route", ArrayList(optimizedRoute))
                        putExtra("total_distance", resultTour.distance)
                        putExtra("is_time_optimization", isOptimizingTime)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun loadDataFromAssets() {
        val gson = Gson()
        try {
            assets.open("locations.json").use { inputStream ->
                val listType = object : TypeToken<List<City>>() {}.type
                val loadedCities: List<City> = gson.fromJson(InputStreamReader(inputStream), listType)
                allCities.clear()
                allCities.addAll(loadedCities)
                allCities.forEach { it.isSelected = false }
            }

            assets.open("distances.json").use { inputStream ->
                distanceMatrix = gson.fromJson(InputStreamReader(inputStream), Array<DoubleArray>::class.java)
            }

            assets.open("durations.json").use { inputStream ->
                durationMatrix = gson.fromJson(InputStreamReader(inputStream), Array<DoubleArray>::class.java)
            }

            Toast.makeText(this, "Podatki uspešno naloženi.", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Napaka pri nalaganju podatkov iz assets! Preverite Logcat.", Toast.LENGTH_LONG).show()
            allCities.clear()
            allCities.add(City("Napaka", "Napaka", "", 0.0, 0.0))
        }
    }

    private fun setUiEnabled(isEnabled: Boolean) {
        progressBar.visibility = if (isEnabled) View.GONE else View.VISIBLE
        buttonStart.isEnabled = isEnabled
        buttonSelectAll.isEnabled = isEnabled
    }
}