package org.example.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.model.CommandParser
import org.example.model.ModelLoader

fun main() = application {
    val parser = loadParser()
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Command Parser"
    ) {
        App(parser)
    }
}

private fun loadParser(): CommandParser? {
    return try {
        val vocabJson = object {}.javaClass.getResourceAsStream("/vocab.json")?.bufferedReader()?.readText()
        val weightsJson = object {}.javaClass.getResourceAsStream("/weights.json")?.bufferedReader()?.readText()
        
        if (vocabJson != null && weightsJson != null) {
            val (_, parser) = ModelLoader.loadFromAssets(vocabJson, weightsJson)
            parser
        } else {
            println("Model files not found in resources")
            null
        }
    } catch (e: Exception) {
        println("Failed to load model: ${e.message}")
        null
    }
}