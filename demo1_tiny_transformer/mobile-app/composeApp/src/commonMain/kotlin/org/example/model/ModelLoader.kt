package org.example.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    private val json = Json { ignoreUnknownKeys = true }
    
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
        
        @Suppress("UNCHECKED_CAST")
        val weightsMap = json.decodeFromString<Map<String, Any>>(weightsJson)
        model.loadWeights(weightsMap)
        
        val stoi = vocabData.stoi
        val itos = vocabData.itos.mapKeys { it.key.toInt() }
        
        val parser = CommandParser(model, stoi, itos, config.blockSize)
        
        return Pair(model, parser)
    }
}