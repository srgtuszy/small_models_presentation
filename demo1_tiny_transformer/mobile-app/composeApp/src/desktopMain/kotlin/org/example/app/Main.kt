package org.example.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.model.CommandParser
import org.example.model.ModelLoader

fun main() = application {
    var parser by remember { mutableStateOf<CommandParser?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var parsing by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        try {
            parser = withContext(Dispatchers.IO) {
                loadParserSync()
            }
        } catch (e: Exception) {
            error = "Failed to load model: ${e.message}"
            e.printStackTrace()
        }
        loading = false
    }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Command Parser"
    ) {
        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading model...")
                    }
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> App(
                parser = parser,
                parsing = parsing,
                onParse = { input, callback ->
                    scope.launch {
                        parsing = true
                        val result = withContext(Dispatchers.Default) {
                            parser?.parse(input)
                        }
                        parsing = false
                        callback(result)
                    }
                }
            )
        }
    }
}

private fun loadParserSync(): CommandParser? {
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