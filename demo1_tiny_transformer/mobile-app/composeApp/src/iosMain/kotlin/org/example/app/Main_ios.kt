package org.example.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.model.CommandParser
import org.example.model.ModelLoader
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes

@OptIn(ExperimentalForeignApi::class)
fun MainViewController() = ComposeUIViewController {
    var parser by remember { mutableStateOf<CommandParser?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var parsing by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        try {
            parser = withContext(Dispatchers.Default) {
                loadParserSync()
            }
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

@OptIn(ExperimentalForeignApi::class)
private fun loadParserSync(): CommandParser? {
    return try {
        val bundle = NSBundle.mainBundle
        val vocabUrl = bundle.URLForResource("vocab", withExtension = "json")
        val weightsUrl = bundle.URLForResource("weights", withExtension = "json")
        
        if (vocabUrl != null && weightsUrl != null) {
            val vocabData = NSData.dataWithContentsOfURL(vocabUrl)!!
            val weightsData = NSData.dataWithContentsOfURL(weightsUrl)!!
            
            val vocabBytes = vocabData.bytes!!.readBytes(vocabData.length.toInt())
            val vocabJson = vocabBytes.decodeToString()
            
            val weightsBytes = weightsData.bytes!!.readBytes(weightsData.length.toInt())
            val weightsJson = weightsBytes.decodeToString()
            
            val (_, parser) = ModelLoader.loadFromAssets(vocabJson, weightsJson)
            parser
        } else {
            println("Model files not found in bundle")
            null
        }
    } catch (e: Exception) {
        println("Failed to load model: ${e.message}")
        null
    }
}