package com.prokrastinatorji.pametnipaketnik

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class City(val name: String, var isSelected: Boolean = true)

class CityAdapter(private val cities: List<City>) : RecyclerView.Adapter<CityAdapter.CityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_city, parent, false)
        return CityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        val city = cities[position]
        holder.cityName.text = city.name
        holder.cityCheckBox.isChecked = city.isSelected
        holder.cityCheckBox.setOnCheckedChangeListener { _, isChecked ->
            city.isSelected = isChecked
        }
    }

    override fun getItemCount() = cities.size

    class CityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cityName: TextView = itemView.findViewById(R.id.cityName)
        val cityCheckBox: CheckBox = itemView.findViewById(R.id.cityCheckBox)
    }
}