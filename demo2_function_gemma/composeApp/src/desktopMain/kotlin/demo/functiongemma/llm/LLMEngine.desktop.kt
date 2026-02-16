package demo.functiongemma.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DesktopLLMEngine : LLMEngine {
    
    private var initialized = false
    
    override suspend fun initialize(): String? = withContext(Dispatchers.IO) {
        // Desktop requires building llama.cpp native library
        // Currently using stub responses
        initialized = true
        "Note: Desktop inference requires building llama.cpp native library. Using stub responses for now."
    }
    
    override suspend fun generateResponse(
        userMessage: String,
        tools: List<Tool>,
        onPartialResult: (String) -> Unit
    ): LLMResponse = withContext(Dispatchers.IO) {
        if (!initialized) {
            return@withContext LLMResponse.Error("LLM not initialized")
        }
        
        // Stub response for Desktop
        val response = generateStubResponse(userMessage, tools)
        onPartialResult(response)
        LLMResponse.Text(response)
    }
    
    private fun generateStubResponse(userMessage: String, tools: List<Tool>): String {
        val lowerMessage = userMessage.lowercase()
        
        return when {
            lowerMessage.contains("reminder") -> {
                "<start_function_call>call:set_reminder{title:<escape>Buy milk<escape>,time:<escape>tomorrow at 5pm<escape>}<end_function_call>"
            }
            lowerMessage.contains("navigate") || lowerMessage.contains("settings") -> {
                "<start_function_call>call:navigate_to_screen{screen:<escape>settings<escape>}<end_function_call>"
            }
            lowerMessage.contains("dark mode") || lowerMessage.contains("toggle") -> {
                "<start_function_call>call:toggle_setting{setting:<escape>dark_mode<escape>,enabled:<escape>true<escape>}<end_function_call>"
            }
            lowerMessage.contains("weather") -> {
                "<start_function_call>call:get_weather{location:<escape>Warsaw<escape>}<end_function_call>"
            }
            lowerMessage.contains("message") || lowerMessage.contains("send") -> {
                "<start_function_call>call:send_message{contact:<escape>John<escape>,message:<escape>Hello!<escape>}<end_function_call>"
            }
            lowerMessage.contains("music") || lowerMessage.contains("play") -> {
                "<start_function_call>call:play_music{song:<escape>Coldplay<escape>,artist:<escape>Unknown<escape>}<end_function_call>"
            }
            lowerMessage.contains("timer") -> {
                "<start_function_call>call:set_timer{duration:<escape>10 minutes<escape>}<end_function_call>"
            }
            lowerMessage.contains("call") -> {
                "<start_function_call>call:make_call{contact:<escape>Mom<escape>}<end_function_call>"
            }
            else -> "I understand you want to: $userMessage. Please describe which function you'd like to call."
        }
    }
    
    override fun isInitialized(): Boolean = initialized
}

actual fun createLLMEngine(): LLMEngine = DesktopLLMEngine()