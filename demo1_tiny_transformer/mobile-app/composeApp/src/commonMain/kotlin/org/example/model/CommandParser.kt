package org.example.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Command(
    val action: String,
    val message: String? = null,
    val target: String? = null,
    val setting: String? = null
)

class CommandParser(
    private val model: TinyTransformer,
    private val stoi: Map<String, Int>,
    private val itos: Map<Int, String>,
    private val blockSize: Int
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    fun parse(input: String): Command? {
        val prompt = "INPUT: $input OUTPUT: "
        val inputIds = encode(prompt)
        
        val outputIds = model.generate(inputIds, maxNewTokens = 60)
        val outputText = decode(outputIds)
        
        val outputPart = outputText.substringAfter("OUTPUT: ", "").trim()
        val jsonStr = outputPart.substringBefore("").trim()
        
        return try {
            if (jsonStr.isNotEmpty()) {
                json.decodeFromString<Command>(jsonStr)
            } else null
        } catch (e: Exception) {
            val fixedJson = tryFixJson(outputPart)
            if (fixedJson.isNotEmpty()) {
                try {
                    json.decodeFromString<Command>(fixedJson)
                } catch (e2: Exception) {
                    null
                }
            } else null
        }
    }
    
    private fun encode(text: String): IntArray {
        return IntArray(text.length) { i ->
            stoi[text[i].toString()] ?: 0
        }
    }
    
    private fun decode(ids: IntArray): String {
        return ids.joinToString("") { itos[it] ?: "" }
    }
    
    private fun tryFixJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) {
            text.substring(start, end + 1)
        } else ""
    }
}