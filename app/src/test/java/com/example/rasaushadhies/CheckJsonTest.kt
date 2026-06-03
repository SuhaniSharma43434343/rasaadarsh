package com.example.rasaushadhies

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import org.junit.Test
import java.io.File

class CheckJsonTest {
    @Test
    fun checkMedicinesJson() {
        val jsonFile = File("src/main/assets/medicines.json")
        val gson = GsonBuilder().create()
        val jsonArray = gson.fromJson(jsonFile.readText(Charsets.UTF_8), JsonArray::class.java)

        val sb = java.lang.StringBuilder()
        for (i in 0 until jsonArray.size()) {
            val med = jsonArray.get(i).asJsonObject
            val id = med.get("id").asInt
            if (id in 121..125) {
                sb.append("--- MEDICINE $id ---\n")
                sb.append("LIST: ").append(med.get("ingredientsListJson").asString).append("\n")
                sb.append("RAW: ").append(med.get("ingredients").asString).append("\n")
            }
        }
        File("test_out2.txt").writeText(sb.toString())
    }
}
