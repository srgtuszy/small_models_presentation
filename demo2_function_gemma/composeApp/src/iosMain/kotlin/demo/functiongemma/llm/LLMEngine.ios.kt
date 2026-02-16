package demo.functiongemma.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle

class iOSLLMEngine : LLMEngine {
    
    private var initialized = false
    
    override suspend fun initialize(): String? = withContext(Dispatchers.Default) {
        // iOS requires building llama.cpp as an xcframework
        // Run ./build-ios-framework.sh first
        initialized = true
        "Note: iOS inference requires building llama.cpp xcframework. Using stub responses for now."
    }
    
    override suspend fun generateResponse(
        userMessage: String,
        tools: List<Tool>,
        onPartialResult: (String) -> Unit
    ): LLMResponse = withContext(Dispatchers.Default) {
        if (!initialized) {
            return@withContext LLMResponse.Error("LLM not initialized")
        }
        
        // Stub response for iOS
        val response = generateStubResponse(userMessage, tools)
        onPartialResult(response)
        LLMResponse.Text(response)
    }
    
    private fun generateStubResponse(userMessage: String, tools: List<Tool>): String {
        val lowerMessage = userMessage.lowercase()
        
        return when {
            lowerMessage.contains("reminder") -> {
                "{\"name\": \"set_reminder\", \"arguments\": {\"title\": \"Buy milk\", \"time\": \"tomorrow at 5pm\"}}"
            }
            lowerMessage.contains("navigate") || lowerMessage.contains("settings") -> {
                "{\"name\": \"navigate_to_screen\", \"arguments\": {\"screen\": \"settings\"}}"
            }
            lowerMessage.contains("dark mode") || lowerMessage.contains("toggle") -> {
                "{\"name\": \"toggle_setting\", \"arguments\": {\"setting\": \"dark_mode\", \"enabled\": \"true\"}}"
            }
            lowerMessage.contains("weather") -> {
                "{\"name\": \"get_weather\", \"arguments\": {\"location\": \"Warsaw\"}}"
            }
            lowerMessage.contains("message") || lowerMessage.contains("send") -> {
                "{\"name\": \"send_message\", \"arguments\": {\"contact\": \"John\", \"message\": \"Hello!\"}}"
            }
            lowerMessage.contains("music") || lowerMessage.contains("play") -> {
                "{\"name\": \"play_music\", \"arguments\": {\"song\": \"Coldplay\", \"artist\": \"Unknown\"}}"
            }
            lowerMessage.contains("timer") -> {
                "{\"name\": \"set_timer\", \"arguments\": {\"duration\": \"10 minutes\"}}"
            }
            lowerMessage.contains("call") -> {
                "{\"name\": \"make_call\", \"arguments\": {\"contact\": \"Mom\"}}"
            }
            else -> "I understand you want to: $userMessage. Please describe which function you'd like to call."
        }
    }
    
    override fun isInitialized(): Boolean = initialized
}

actual fun createLLMEngine(): LLMEngine = iOSLLMEngine()