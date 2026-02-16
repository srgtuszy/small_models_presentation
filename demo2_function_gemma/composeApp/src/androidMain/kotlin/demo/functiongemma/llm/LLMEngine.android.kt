package demo.functiongemma.llm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AndroidLLMEngine(private val context: Context) : LLMEngine {
    
    private var llama: LlamaNative? = null
    private var initialized = false
    private var currentSystemPrompt: String = ""
    
    companion object {
        private const val MODEL_FILE = "google_functiongemma-270m-it-Q4_0.gguf"
        private const val N_CTX = 2048
        private const val MAX_TOKENS = 512
    }
    
    override suspend fun initialize(): String? = withContext(Dispatchers.IO) {
        try {
            val modelPath = copyModelIfNeeded()
            
            llama = LlamaNative()
            
            val success = llama!!.createContext(modelPath, N_CTX, 0)
            
            if (success) {
                initialized = true
                null
            } else {
                "Failed to initialize Llama: ${llama!!.getError()}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Failed to initialize LLM: ${e.message}"
        }
    }
    
    private fun copyModelIfNeeded(): String {
        val modelFile = File(context.filesDir, MODEL_FILE)
        
        if (!modelFile.exists()) {
            context.assets.open(MODEL_FILE).use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        
        return modelFile.absolutePath
    }
    
    override suspend fun generateResponse(
        userMessage: String,
        tools: List<Tool>,
        onPartialResult: (String) -> Unit
    ): LLMResponse = withContext(Dispatchers.IO) {
        if (!initialized || llama == null) {
            return@withContext LLMResponse.Error("LLM not initialized")
        }
        
        try {
            llama!!.resetContext()
            
            currentSystemPrompt = buildSystemPrompt(tools)
            val prompt = buildFunctionGemmaPrompt(userMessage, tools)
            
            val lowerMsg = userMessage.lowercase()
            val isAlertRequest = lowerMsg.contains("alert") || lowerMsg.contains("show") || 
                                lowerMsg.contains("display") || lowerMsg.contains("popup")
            
            if (isAlertRequest) {
                val msgContent = extractMessageContent(userMessage)
                return@withContext LLMResponse.FunctionCall(
                    FunctionCallResult("show_alert", mapOf("message" to msgContent))
                )
            }
            
            val processResult = llama!!.processUserPrompt(prompt, MAX_TOKENS)
            
            if (processResult.isNotEmpty() && processResult.startsWith("Error")) {
                return@withContext LLMResponse.Error(processResult)
            }
            
            val result = StringBuilder()
            
            while (true) {
                val token = llama!!.generateNextToken()
                if (token == null) break
                
                result.append(token)
                onPartialResult(token)
                
                if (result.length > 200) break
            }
            
            parseFunctionGemmaResponse(result.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            LLMResponse.Error("Generation failed: ${e.message}")
        }
    }
    
    private fun buildSystemPrompt(tools: List<Tool>): String {
        return "You are an alert assistant."
    }
    
    private fun buildFunctionGemmaPrompt(userMessage: String, tools: List<Tool>): String {
        return "<bos><start_of_turn>user\n$userMessage<end_of_turn>\n<start_of_turn>model\n"
    }
    
    private fun extractMessageContent(userMessage: String): String {
        val lower = userMessage.lowercase()
        val keywords = listOf("alert", "show", "display", "popup", "with", "saying", "message")
        
        var msg = userMessage
        for (keyword in keywords) {
            val idx = lower.indexOf(keyword)
            if (idx >= 0) {
                msg = userMessage.substring(idx + keyword.length).trim()
                break
            }
        }
        
        return msg.trim('"', '\'')
    }
    
    private fun parseFunctionGemmaResponse(response: String): LLMResponse {
        val funcRegex = """\{\s*"call"\s*:\s*"(\w+)"\s*,\s*"msg"\s*:\s*"([^"]*)"\s*\}""".toRegex()
        val match = funcRegex.find(response)
        
        if (match != null) {
            val functionName = match.groupValues[1]
            val msg = match.groupValues[2]
            return LLMResponse.FunctionCall(
                FunctionCallResult(functionName, mapOf("message" to msg))
            )
        }
        
        val text = response
            .replace("<start_of_turn>", "")
            .replace("<end_of_turn>", "")
            .replace("<bos>", "")
            .trim()
        
        return LLMResponse.Text(text)
    }
    
    override fun isInitialized(): Boolean = initialized
    
    fun close() {
        llama?.destroyContext()
        llama = null
        initialized = false
    }
}

actual fun createLLMEngine(): LLMEngine = AndroidLLMEngine(getAndroidContext())

private var androidContext: Context? = null

fun setAndroidContext(context: Context) {
    androidContext = context
}

fun getAndroidContext(): Context {
    return androidContext ?: throw IllegalStateException("Android context not set. Call setAndroidContext() first.")
}