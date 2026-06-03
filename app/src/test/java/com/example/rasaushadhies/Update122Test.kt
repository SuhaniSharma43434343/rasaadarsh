package com.example.rasaushadhies

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import org.junit.Test
import java.io.File

class Update122Test {
    @Test
    fun updateMedicine122() {
        val jsonFile = File("src/main/assets/medicines.json")
        val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        val jsonArray = gson.fromJson(jsonFile.readText(Charsets.UTF_8), JsonArray::class.java)

        for (i in 0 until jsonArray.size()) {
            val med = jsonArray.get(i).asJsonObject
            val id = med.get("id").asInt
            if (id == 122) {
                // Update ingredientsListJson
                val listStr = med.get("ingredientsListJson").asString
                val list = JsonParser.parseString(listStr).asJsonArray
                val newList = JsonArray()
                for (j in 0 until list.size()) {
                    val item = list.get(j).asJsonObject
                    if (!item.get("sanskritName").asString.contains("Madhu")) {
                        newList.add(item)
                    }
                }
                med.addProperty("ingredientsListJson", gson.toJson(newList))
                
                // Update ingredients string
                var ingRaw = med.get("ingredients").asString
                val newIngRaw = ingRaw.lines().filter { !it.contains("Madhu") }.joinToString("\n")
                med.addProperty("ingredients", newIngRaw)
            }
        }
        
        jsonFile.writeText(gson.toJson(jsonArray))
        println("SUCCESS")
    }
}
