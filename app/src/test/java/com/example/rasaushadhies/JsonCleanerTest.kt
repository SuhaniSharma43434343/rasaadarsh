package com.example.rasaushadhies

import org.junit.Test
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File

class JsonCleanerTest {
    @Test
    fun cleanJson() {
        val file = File("src/main/assets/medicines.json")
        val content = file.readText(Charsets.UTF_8)
        
        val gson = GsonBuilder().setLenient().setPrettyPrinting().create()
        val jsonArray = gson.fromJson(content, JsonArray::class.java)
        
        for (i in 0 until jsonArray.size()) {
            val item = jsonArray.get(i).asJsonObject
            if (!item.has("id")) continue
            val id = item.get("id").asInt
            
            if (id in 56..60) {
                if (item.has("ingredients")) {
                    val rawIngredients = item.get("ingredients").asString
                    val lines = rawIngredients.split("\\n", "\n")
                    
                    val newList = JsonArray()
                    var isBhavanaSection = false
                    
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.contains("Ingredient (Sanskrit)", true)) continue
                        
                        if (trimmed.contains("Bhavana Dravya", true) || trimmed.equals("Bhavana", true)) {
                            isBhavanaSection = true
                            continue
                        }
                        
                        val parts = trimmed.split("\t")
                        val obj = JsonObject()
                        
                        if (parts.size >= 3) {
                            obj.addProperty("sanskritName", parts[0].trim())
                            obj.addProperty("englishName", parts[1].trim())
                            obj.addProperty("quantity", parts[2].trim())
                            newList.add(obj)
                        } else if (parts.size == 2) {
                            obj.addProperty("sanskritName", parts[0].trim())
                            obj.addProperty("englishName", parts[1].trim())
                            if (isBhavanaSection) obj.addProperty("quantity", "Bhavana Dravya")
                            else if (trimmed.contains("as required", true)) obj.addProperty("quantity", "As required")
                            else obj.addProperty("quantity", "")
                            newList.add(obj)
                        } else if (parts.size == 1) {
                            obj.addProperty("sanskritName", parts[0].trim())
                            obj.addProperty("englishName", "")
                            if (isBhavanaSection) obj.addProperty("quantity", "Bhavana Dravya")
                            else if (trimmed.contains("as required", true)) obj.addProperty("quantity", "As required")
                            else obj.addProperty("quantity", "")
                            newList.add(obj)
                        }
                    }
                    
                    if (newList.size() > 0) {
                        // Overwrite ingredientsListJson as string
                        item.addProperty("ingredientsListJson", gson.toJson(newList))
                    }
                }
            }
        }
        
        file.writeText(gson.toJson(jsonArray), Charsets.UTF_8)
        println("Successfully updated ingredientsListJson for 56-60 in medicines.json")
    }
}
