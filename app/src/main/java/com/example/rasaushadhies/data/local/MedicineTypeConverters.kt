package com.example.rasaushadhies.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MedicineTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromIngredientList(value: List<Ingredient>?): String {
        return gson.toJson(value ?: emptyList<Ingredient>())
    }

    @TypeConverter
    fun toIngredientList(value: String): List<Ingredient> {
        val listType = object : TypeToken<List<Ingredient>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromClinicalProperties(value: ClinicalProperties?): String {
        return gson.toJson(value ?: ClinicalProperties())
    }

    @TypeConverter
    fun toClinicalProperties(value: String): ClinicalProperties {
        return gson.fromJson(value, ClinicalProperties::class.java) ?: ClinicalProperties()
    }
}
