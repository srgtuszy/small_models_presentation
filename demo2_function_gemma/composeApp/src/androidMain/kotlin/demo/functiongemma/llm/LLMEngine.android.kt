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
        val sb = StringBuilder()
        sb.append("You are a model that can do function calling with the following functions")
        
        for (tool in tools) {
            sb.append(buildFunctionDeclaration(tool))
        }
        
        return sb.toString()
    }
    
    private fun buildFunctionDeclaration(tool: Tool): String {
        val sb = StringBuilder()
        sb.append("<start_function_declaration>declaration:")
        sb.append(tool.name)
        sb.append("{description:<escape>")
        sb.append(tool.description)
        sb.append("<escape>,parameters:{properties:{")
        
        val props = tool.parameters["properties"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val required = (tool.parameters["required"] as? List<*>) ?: emptyList<Any>()
        
        val propEntries = props.entries.toList()
        propEntries.forEachIndexed { index, entry ->
            val paramName = entry.key.toString()
            val paramDef = entry.value as? Map<*, *> ?: emptyMap<Any, Any>()
            
            if (index > 0) sb.append(",")
            
            sb.append(paramName)
            sb.append(":{description:<escape>")
            sb.append(paramDef["description"] ?: "")
            sb.append("<escape>,type:<escape>")
            sb.append(paramDef["type"] ?: "string")
            sb.append("<escape>")
            
            val enum = paramDef["enum"] as? List<*>
            if (enum != null) {
                sb.append(",enum:<escape>[")
                sb.append(enum.joinToString(",") { "\"$it\"" })
                sb.append("]<escape>")
            }
            sb.append("}")
        }
        
        sb.append("}},required:<escape>[")
        sb.append(required.joinToString(",") { "\"$it\"" })
        sb.append("]<escape>}}<end_function_declaration>")
        
        return sb.toString()
    }
    
    private fun buildFunctionGemmaPrompt(userMessage: String, tools: List<Tool>): String {
        val sb = StringBuilder()
        sb.append("<bos><start_of_turn>developer\n")
        sb.append(buildSystemPrompt(tools))
        sb.append("<end_of_turn>\n")
        sb.append("<start_of_turn>user\n")
        sb.append(userMessage)
        sb.append("<end_of_turn>\n")
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }
    
    private fun parseFunctionGemmaResponse(response: String): LLMResponse {
        if (response.isBlank()) {
            return LLMResponse.Text("")
        }
        
        // Try multiple formats the model might output
        val patterns = listOf(
            """call:(\w+)\{([^}]*)\}""",
            """<start_function_call>call:(\w+)\{([^}]*)\}<end_function_call>""",
            """(\w+)\s*\(\s*([^)]*)\s*\)"""
        )
        
        for (pattern in patterns) {
            val match = pattern.toRegex().find(response)
            if (match != null) {
                val functionName = match.groupValues[1]
                val paramsStr = match.groupValues.getOrNull(2) ?: ""
                val params = mutableMapOf<String, String>()
                
                // Parse parameters in different formats
                val paramPatterns = listOf(
                    """(\w+):<escape>([^<]*)<escape>""",
                    """(\w+):‹([^›]*)›""",
                    """(\w+):["']([^"']*)["']""",
                    """"(\w+)"\s*:\s*"([^"]*)""""
                )
                
                for (paramPattern in paramPatterns) {
                    paramPattern.toRegex().findAll(paramsStr).forEach { paramMatch ->
                        params[paramMatch.groupValues[1]] = paramMatch.groupValues[2]
                    }
                    if (params.isNotEmpty()) break
                }
                
                if (functionName.isNotBlank()) {
                    return LLMResponse.FunctionCall(FunctionCallResult(functionName, params))
                }
            }
        }
        
        val text = response
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("‹[^›]*›"), "")
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