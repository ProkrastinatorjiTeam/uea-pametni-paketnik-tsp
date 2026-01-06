package com.prokrastinatorji.pametnipaketnik

import android.content.Intent
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
        cities.addAll(getDummyCities())
        cityAdapter = CityAdapter(cities)
        recyclerViewCities.adapter = cityAdapter

        // Set button click listener
        buttonStart.setOnClickListener {
            val selectedCities = ArrayList(cities.filter { it.isSelected })
            if (selectedCities.isEmpty()) {
                Toast.makeText(this, "Izberite vsaj eno mesto.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val populationSize = editTextPopulationSize.text.toString().toIntOrNull() ?: 100
            val crossoverProbability = editTextCrossover.text.toString().toDoubleOrNull() ?: 0.8
            val mutationProbability = editTextMutation.text.toString().toDoubleOrNull() ?: 0.1

            // Start MapActivity
            val intent = Intent(this, MapActivity::class.java).apply {
                putParcelableArrayListExtra("selected_cities", selectedCities)
            }
            startActivity(intent)
        }
    }

    // Replace with real data loading
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