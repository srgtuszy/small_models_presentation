package org.example.app

import androidx.compose.ui.window.ComposeUIViewController
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
    val parser = loadParser()
    App(parser)
}

@OptIn(ExperimentalForeignApi::class)
private fun loadParser(): CommandParser? {
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