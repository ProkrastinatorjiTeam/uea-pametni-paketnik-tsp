package com.prokrastinatorji.pametnipaketnik

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewCities: RecyclerView
    private lateinit var cityAdapter: CityAdapter
    private val cities = mutableListOf<City>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        recyclerViewCities = findViewById(R.id.recyclerViewCities)
        val buttonStart = findViewById<Button>(R.id.buttonStart)
        val editTextPopulationSize = findViewById<TextInputEditText>(R.id.editTextPopulationSize)
        val editTextCrossover = findViewById<TextInputEditText>(R.id.editTextCrossover)
        val editTextMutation = findViewById<TextInputEditText>(R.id.editTextMutation)


        // Setup RecyclerView
        recyclerViewCities.layoutManager = LinearLayoutManager(this)
        // TODO: Load real city data
        cities.addAll(getDummyCities())
        cityAdapter = CityAdapter(cities)
        recyclerViewCities.adapter = cityAdapter

        // Set button click listener
        buttonStart.setOnClickListener {
            val selectedCities = cities.filter { it.isSelected }
            val populationSize = editTextPopulationSize.text.toString().toIntOrNull() ?: 100
            val crossoverProbability = editTextCrossover.text.toString().toDoubleOrNull() ?: 0.8
            val mutationProbability = editTextMutation.text.toString().toDoubleOrNull() ?: 0.1

            val message = "Zagnan algoritem z ${selectedCities.size} mesti.\n" +
                          "Populacija: $populationSize, Križanje: $crossoverProbability, Mutacija: $mutationProbability"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()

            // TODO: Start the genetic algorithm
        }
    }

    // Replace with real data loading
    private fun getDummyCities(): List<City> {
        return listOf(
            City("Ljubljana"),
            City("Maribor"),
            City("Celje"),
            City("Kranj"),
            City("Koper"),
            City("Novo mesto"),
            City("Velenje"),
            City("Murska Sobota")
        )
    }
}