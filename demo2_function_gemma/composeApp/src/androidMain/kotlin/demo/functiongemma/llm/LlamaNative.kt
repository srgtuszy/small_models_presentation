package demo.functiongemma.llm

class LlamaNative {
    companion object {
        init {
            System.loadLibrary("llama_wrapper")
        }
    }
    
    private var contextPtr: Long = 0
    
    fun createContext(modelPath: String, nCtx: Int = 2048, nGpuLayers: Int = 0): Boolean {
        contextPtr = nativeCreateContext(modelPath, nCtx, nGpuLayers)
        return isLoaded()
    }
    
    fun destroyContext() {
        if (contextPtr != 0L) {
            nativeDestroyContext(contextPtr)
            contextPtr = 0
        }
    }
    
    fun isLoaded(): Boolean = contextPtr != 0L && nativeIsLoaded(contextPtr)
    
    fun setSystemPrompt(prompt: String): Int {
        if (contextPtr == 0L) return -1
        return nativeSetSystemPrompt(contextPtr, prompt)
    }
    
    fun processUserPrompt(prompt: String, maxTokens: Int = 512): String {
        if (contextPtr == 0L) return "Error: Context not created"
        return nativeProcessUserPrompt(contextPtr, prompt, maxTokens)
    }
    
    fun generateNextToken(): String? {
        if (contextPtr == 0L) return null
        return nativeGenerateNextToken(contextPtr)
    }
    
    fun resetContext() {
        if (contextPtr != 0L) {
            nativeResetContext(contextPtr)
        }
    }
    
    fun getError(): String {
        if (contextPtr == 0L) return "Context not created"
        return nativeGetError(contextPtr)
    }
    
    private external fun nativeCreateContext(modelPath: String, nCtx: Int, nGpuLayers: Int): Long
    private external fun nativeDestroyContext(contextPtr: Long)
    private external fun nativeIsLoaded(contextPtr: Long): Boolean
    private external fun nativeSetSystemPrompt(contextPtr: Long, prompt: String): Int
    private external fun nativeProcessUserPrompt(contextPtr: Long, prompt: String, maxTokens: Int): String
    private external fun nativeGenerateNextToken(contextPtr: Long): String?
    private external fun nativeResetContext(contextPtr: Long)
    private external fun nativeGetError(contextPtr: Long): String
}