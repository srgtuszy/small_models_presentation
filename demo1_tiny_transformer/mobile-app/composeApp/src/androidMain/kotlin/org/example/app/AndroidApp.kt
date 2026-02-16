package org.example.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.model.CommandParser
import org.example.model.ModelLoader

@Composable
fun AndroidApp(context: Context) {
    var parser by remember { mutableStateOf<CommandParser?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var parsing by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        try {
            val (_, cmdParser) = withContext(Dispatchers.IO) {
                val vocabJson = context.assets.open("vocab.json").bufferedReader().use { it.readText() }
                val weightsJson = context.assets.open("weights.json").bufferedReader().use { it.readText() }
                ModelLoader.loadFromAssets(vocabJson, weightsJson)
            }
            parser = cmdParser
        } catch (e: Exception) {
            error = "Failed to load model: ${e.message}"
            e.printStackTrace()
        }
        loading = false
    }
    
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