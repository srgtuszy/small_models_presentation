package demo.functiongemma.llm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AndroidLLMEngine(private val context: Context) : LLMEngine {
    
    private var llama: LlamaNative? = null
    private var initialized = false
    
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
            
            val systemPrompt = buildSystemPrompt(tools)
            llama!!.setSystemPrompt(systemPrompt)
            
            val prompt = buildFunctionGemmaPrompt(userMessage)
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
            }
            
            parseFunctionGemmaResponse(result.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            LLMResponse.Error("Generation failed: ${e.message}")
        }
    }
    
    private fun buildSystemPrompt(tools: List<Tool>): String {
        val sb = StringBuilder()
        sb.append("You are a model that can do function calling with the following functions.\n\n")
        sb.append("Available functions (in JSON Schema format):\n\n")
        
        for (tool in tools) {
            sb.append("{\n")
            sb.append("  \"type\": \"function\",\n")
            sb.append("  \"function\": {\n")
            sb.append("    \"name\": \"${tool.name}\",\n")
            sb.append("    \"description\": \"${tool.description}\",\n")
            sb.append("    \"parameters\": {\n")
            sb.append("      \"type\": \"object\",\n")
            sb.append("      \"properties\": {\n")
            
            val params = tool.parameters["properties"] as? Map<*, *> ?: tool.parameters
            val required = tool.parameters["required"] as? List<*> ?: emptyList<Any>()
            
            params.entries.forEachIndexed { index, entry ->
                val paramName = entry.key as String
                val paramDef = entry.value as? Map<*, *> ?: emptyMap<Any, Any>()
                val paramType = paramDef["type"] as? String ?: "string"
                val paramDesc = paramDef["description"] as? String ?: ""
                
                sb.append("        \"$paramName\": {\n")
                sb.append("          \"type\": \"$paramType\",\n")
                sb.append("          \"description\": \"$paramDesc\"\n")
                sb.append("        }${if (index < params.size - 1) "," else ""}\n")
            }
            
            sb.append("      },\n")
            sb.append("      \"required\": ${required.map { "\"$it\"" }}\n")
            sb.append("    }\n")
            sb.append("  }\n")
            sb.append("}\n\n")
        }
        
        sb.append("When you need to call a function, respond with JSON in this exact format:\n")
        sb.append("{\"name\": \"function_name\", \"arguments\": {\"param1\": \"value1\"}}\n")
        
        return sb.toString()
    }
    
    private fun buildFunctionGemmaPrompt(userMessage: String): String {
        val sb = StringBuilder()
        sb.append("<bos><start_of_turn>user\n")
        sb.append(userMessage)
        sb.append("<end_of_turn>\n")
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }
    
    private fun parseFunctionGemmaResponse(response: String): LLMResponse {
        val jsonRegex = """\{"name"\s*:\s*"(\w+)"\s*,\s*"arguments"\s*:\s*(\{[^}]*\})\}""".toRegex()
        
        val match = jsonRegex.find(response)
        
        if (match != null) {
            val functionName = match.groupValues[1]
            val argsStr = match.groupValues[2]
            
            val params = mutableMapOf<String, String>()
            val argRegex = """"(\w+)"\s*:\s*"([^"]*)"""".toRegex()
            
            argRegex.findAll(argsStr).forEach { argMatch ->
                params[argMatch.groupValues[1]] = argMatch.groupValues[2]
            }
            
            return LLMResponse.FunctionCall(
                FunctionCallResult(functionName, params)
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