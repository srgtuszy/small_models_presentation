package org.example.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class VocabData(
    val stoi: Map<String, Int>,
    val itos: Map<String, String>,
    val vocab_size: Int,
    val block_size: Int,
    val n_embd: Int,
    val n_layer: Int,
    val n_head: Int
)

object ModelLoader {
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    fun loadFromAssets(vocabJson: String, weightsJson: String): Pair<TinyTransformer, CommandParser> {
        val vocabData = json.decodeFromString<VocabData>(vocabJson)
        
        val config = ModelConfig(
            vocabSize = vocabData.vocab_size,
            blockSize = vocabData.block_size,
            nEmbd = vocabData.n_embd,
            nLayer = vocabData.n_layer,
            nHead = vocabData.n_head
        )
        
        val model = TinyTransformer(config)
        
        val weightsMap = parseWeightsJson(weightsJson)
        model.loadWeights(weightsMap)
        
        val stoi = vocabData.stoi
        val itos = vocabData.itos.mapKeys { it.key.toInt() }
        
        val parser = CommandParser(model, stoi, itos, config.blockSize)
        
        return Pair(model, parser)
    }
    
    private fun parseWeightsJson(jsonString: String): Map<String, Any> {
        val element = json.parseToJsonElement(jsonString)
        return parseJsonObject(element.jsonObject)
    }
    
    private fun parseJsonObject(obj: JsonObject): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        for ((key, value) in obj) {
            result[key] = parseJsonValue(value)
        }
        return result
    }
    
    private fun parseJsonValue(element: JsonElement): Any {
        return when (element) {
            is JsonPrimitive -> {
                element.doubleOrNull ?: element.content
            }
            is JsonArray -> {
                element.map { parseJsonValue(it) }
            }
            is JsonObject -> {
                parseJsonObject(element)
            }
        }
    }
}