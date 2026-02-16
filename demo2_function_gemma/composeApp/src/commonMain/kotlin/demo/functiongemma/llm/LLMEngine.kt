package demo.functiongemma.llm

data class Tool(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)

data class FunctionCallResult(
    val name: String,
    val arguments: Map<String, String>
)

sealed class LLMResponse {
    data class Text(val content: String) : LLMResponse()
    data class FunctionCall(val result: FunctionCallResult) : LLMResponse()
    data class Error(val message: String) : LLMResponse()
}

interface LLMEngine {
    suspend fun initialize(): String?
    suspend fun generateResponse(
        userMessage: String,
        tools: List<Tool>,
        onPartialResult: (String) -> Unit = {}
    ): LLMResponse
    fun isInitialized(): Boolean
}

expect fun createLLMEngine(): LLMEngine