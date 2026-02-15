package org.example.app

import android.content.Context
import androidx.compose.runtime.*
import org.example.model.CommandParser
import org.example.model.ModelLoader

@Composable
fun AndroidApp(context: Context) {
    var parser by remember { mutableStateOf<CommandParser?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        try {
            val vocabJson = context.assets.open("vocab.json").bufferedReader().use { it.readText() }
            val weightsJson = context.assets.open("weights.json").bufferedReader().use { it.readText() }
            
            val (_, cmdParser) = ModelLoader.loadFromAssets(vocabJson, weightsJson)
            parser = cmdParser
        } catch (e: Exception) {
            error = "Failed to load model: ${e.message}"
        }
    }
    
    if (error != null) {
        App(null)
    } else {
        App(parser)
    }
}