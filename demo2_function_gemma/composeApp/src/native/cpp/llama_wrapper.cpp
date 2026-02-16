/**
 * LLaMA Wrapper for Kotlin/Native
 * ================================
 * 
 * This C wrapper exposes llama.cpp functionality to Kotlin via JNI.
 * It provides a simple API for loading models, tokenizing text, and generating tokens.
 * 
 * Model: FunctionGemma 270M IT (Q4_0 quantized, ~230MB)
 * Performance: ~0.3s TTFT, ~126 tok/s on mobile
 * 
 * Key Concepts:
 * - Context: Holds model state for inference
 * - Batch: Collection of tokens to process together
 * - Sampler: Controls how tokens are selected (temperature, top-k, top-p)
 * - Tokenization: Converting text to integer token IDs
 */

#include "llama_wrapper.h"
#include "llama.h"
#include "common.h"

#include <string>
#include <vector>
#include <cstring>

/**
 * Internal context structure.
 * 
 * This holds all state needed for inference:
 * - model: The loaded GGUF model weights
 * - ctx: Runtime state (KV cache, activations)
 * - sampler: Token selection strategy
 * - vocab: Tokenizer vocabulary
 * - tokens: Current conversation tokens
 */
struct LlamaContextInternal {
    llama_model* model = nullptr;           // Model weights loaded from GGUF file
    llama_context* ctx = nullptr;           // Runtime context with KV cache
    llama_sampler* sampler = nullptr;       // Token sampling strategy
    const llama_vocab* vocab = nullptr;     // Tokenizer vocabulary
    std::vector<llama_token> tokens;         // Current token sequence
    std::string system_prompt;               // System instruction
    std::string last_error;                  // Error message if any
    bool loaded = false;                     // Model loaded successfully
    int n_ctx = 2048;                        // Context window size (max tokens)
    int n_gpu_layers = 0;                    // GPU acceleration (0 = CPU only)
};

extern "C" {

/**
 * Create and initialize a new LLaMA context.
 * 
 * Steps:
 * 1. Initialize llama.cpp backend
 * 2. Load model from GGUF file
 * 3. Create inference context with KV cache
 * 4. Set up token sampler
 * 
 * @param model_path Path to the GGUF model file
 * @param n_ctx Context window size (max sequence length)
 * @param n_gpu_layers Number of layers to offload to GPU (0 = CPU only)
 * @return Opaque context handle, or nullptr on failure
 */
LlamaContext llama_create_context(const char* model_path, int n_ctx, int n_gpu_layers) {
    // Allocate internal state
    auto* internal = new LlamaContextInternal();
    internal->n_ctx = n_ctx > 0 ? n_ctx : 2048;
    internal->n_gpu_layers = n_gpu_layers;
    
    // Initialize llama.cpp backend (must call once)
    llama_backend_init();
    
    // Configure model loading
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = n_gpu_layers;  // GPU offloading for faster inference
    
    // Load model weights from GGUF file
    internal->model = llama_model_load_from_file(model_path, model_params);
    if (!internal->model) {
        internal->last_error = "Failed to load model from: " + std::string(model_path);
        return internal;
    }
    
    // Get tokenizer vocabulary from model
    internal->vocab = llama_model_get_vocab(internal->model);
    
    // Configure inference context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = internal->n_ctx;       // Max sequence length
    ctx_params.n_batch = internal->n_ctx;     // Max tokens per batch (for prompt processing)
    ctx_params.no_perf = true;                // Disable performance logging
    
    // Create runtime context (allocates KV cache)
    internal->ctx = llama_init_from_model(internal->model, ctx_params);
    if (!internal->ctx) {
        internal->last_error = "Failed to create context";
        llama_model_free(internal->model);
        internal->model = nullptr;
        return internal;
    }
    
    // Set up token sampler chain
    // This controls how the model selects the next token
    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    internal->sampler = llama_sampler_chain_init(sampler_params);
    
    // Add sampling stages (applied in order):
    llama_sampler_chain_add(internal->sampler, llama_sampler_init_penalties(-1, 1.3f, 0.1f, 0.0f)); // Repetition penalty (stronger)
    llama_sampler_chain_add(internal->sampler, llama_sampler_init_temp(0.9f));     // Temperature (slightly lower)
    llama_sampler_chain_add(internal->sampler, llama_sampler_init_top_k(40));       // Top-K sampling (lower for more focused)
    llama_sampler_chain_add(internal->sampler, llama_sampler_init_top_p(0.9f, 1)); // Top-P (nucleus) sampling
    llama_sampler_chain_add(internal->sampler, llama_sampler_init_dist(0));         // Random selection
    
    // SAMPLING EXPLAINED:
    // - Penalties: Penalize repeated tokens (1.3 = 30% penalty, 0.1 freq penalty)
    // - Temperature: Controls randomness. 0.9 = slightly focused
    // - Top-K: Only consider the K most likely tokens (40 here)
    // - Top-P: Keep tokens until cumulative probability reaches P (0.9 = 90%)
    // - Dist: Randomly select from the filtered distribution
    
    internal->loaded = true;
    return internal;
}

/**
 * Free all resources associated with a context.
 * 
 * Must be called when done to avoid memory leaks.
 * Order: sampler -> context -> model -> internal struct
 */
void llama_destroy_context(LlamaContext ctx) {
    if (ctx) {
        auto* internal = static_cast<LlamaContextInternal*>(ctx);
        if (internal->sampler) {
            llama_sampler_free(internal->sampler);
        }
        if (internal->ctx) {
            llama_free(internal->ctx);
        }
        if (internal->model) {
            llama_model_free(internal->model);
        }
        delete internal;
    }
}

/**
 * Check if model loaded successfully.
 */
bool llama_is_loaded(LlamaContext ctx) {
    if (!ctx) return false;
    auto* internal = static_cast<LlamaContextInternal*>(ctx);
    return internal->loaded;
}

/**
 * Set the system prompt (developer instruction).
 * 
 * In Gemma/FunctionGemma, this is the "developer" turn that
 * tells the model what tools it has and how to behave.
 * 
 * Clears existing tokens to start fresh.
 */
int llama_set_system_prompt(LlamaContext ctx, const char* prompt) {
    if (!ctx) return -1;
    auto* internal = static_cast<LlamaContextInternal*>(ctx);
    if (!internal->loaded) {
        internal->last_error = "Model not loaded";
        return -1;
    }
    
    internal->system_prompt = prompt ? prompt : "";
    internal->tokens.clear();  // Reset conversation
    
    return 0;
}

/**
 * Process the user prompt through the model.
 * 
 * This is the "prefill" phase - processing all input tokens at once.
 * 
 * Steps:
 * 1. Clear previous conversation tokens
 * 2. Tokenize system prompt (if any) and add to sequence
 * 3. Tokenize user prompt and add to sequence
 * 4. Create batch with all tokens
 * 5. Run inference (llama_decode) to fill KV cache
 * 
 * After this, the model is ready to generate output tokens.
 * 
 * @param ctx Context handle
 * @param prompt User's text input
 * @param max_tokens Maximum tokens to process (safety limit)
 * @return 0 on success, -1 on error
 */
int llama_process_user_prompt(LlamaContext ctx, const char* prompt, int max_tokens) {
    if (!ctx || !prompt) return -1;
    auto* internal = static_cast<LlamaContextInternal*>(ctx);
    if (!internal->loaded) {
        internal->last_error = "Model not loaded";
        return -1;
    }
    
    // Clear previous tokens (start new turn)
    internal->tokens.clear();
    
    // Tokenize and add system prompt if present
    if (!internal->system_prompt.empty()) {
        // common_tokenize parameters:
        // - vocab: tokenizer vocabulary
        // - text: string to tokenize
        // - add_bos: add beginning-of-sequence token
        // - add_eos: add end-of-sequence token (false for system prompt)
        std::vector<llama_token> sys_tokens = common_tokenize(
            internal->vocab, 
            internal->system_prompt, 
            true,   // Add BOS token (beginning of sequence)
            true    // Add special tokens for formatting
        );
        internal->tokens.insert(internal->tokens.end(), sys_tokens.begin(), sys_tokens.end());
    }
    
    // Tokenize user prompt
    std::vector<llama_token> user_tokens = common_tokenize(
        internal->vocab, 
        std::string(prompt), 
        false,  // Don't add BOS (already have it from system)
        true    // Add special tokens
    );
    
    if (user_tokens.empty()) {
        internal->last_error = "Failed to tokenize user prompt";
        return -1;
    }
    
    // Append user tokens to sequence
    internal->tokens.insert(internal->tokens.end(), user_tokens.begin(), user_tokens.end());
    
    if (internal->tokens.empty()) {
        internal->last_error = "No tokens to decode";
        return -1;
    }
    
    // Create batch for processing all tokens at once
    // This is more efficient than one-by-one for prompt processing
    size_t n_tokens = internal->tokens.size();
    llama_batch batch = llama_batch_init(n_tokens, 0, 1);
    
    // Fill batch with all tokens
    for (size_t i = 0; i < n_tokens; i++) {
        batch.token[i] = internal->tokens[i];      // Token ID
        batch.pos[i] = i;                           // Position in sequence
        batch.n_seq_id[i] = 1;                      // Number of sequences (always 1 for simple chat)
        batch.seq_id[i][0] = 0;                     // Sequence ID
        batch.logits[i] = (i == n_tokens - 1);      // Only compute logits for last token (optimization)
    }
    batch.n_tokens = n_tokens;
    
    // Run inference: process all tokens and fill KV cache
    // This is the "prefill" phase - processing input in parallel
    int result = llama_decode(internal->ctx, batch);
    llama_batch_free(batch);  // Clean up batch
    
    if (result != 0) {
        internal->last_error = "Failed to decode prompt (code: " + std::to_string(result) + ")";
        return -1;
    }
    
    return 0;
}

/**
 * Generate the next token.
 * 
 * This is the "decode" phase - generating one token at a time.
 * Each call generates a single token and updates the KV cache.
 * 
 * Steps:
 * 1. Sample next token from probability distribution
 * 2. Check for end-of-sequence (EOS) token
 * 3. Convert token ID back to text
 * 4. Add token to sequence
 * 5. Process token through model (update KV cache)
 * 
 * @param ctx Context handle
 * @return Token text string, or nullptr on EOS/error
 */
const char* llama_generate_next_token(LlamaContext ctx) {
    if (!ctx) return nullptr;
    auto* internal = static_cast<LlamaContextInternal*>(ctx);
    if (!internal->loaded) {
        return nullptr;
    }
    
    // Sample next token using the configured sampler chain
    // -1 means use the last position in KV cache
    llama_token new_token = llama_sampler_sample(internal->sampler, internal->ctx, -1);
    
    // Check for end-of-generation token
    // This signals the model is done generating
    if (llama_vocab_is_eog(internal->vocab, new_token)) {
        return nullptr;
    }
    
    // Convert token ID to text string
    // This handles special characters and byte-level tokens
    std::string token_str = common_token_to_piece(internal->vocab, new_token, true);
    
    // Add token to our tracking
    internal->tokens.push_back(new_token);
    
    // Process this new token through the model
    // This updates the KV cache for the next generation step
    size_t n_tokens = internal->tokens.size();
    llama_batch batch = llama_batch_init(1, 0, 1);
    
    batch.token[0] = new_token;          // The new token
    batch.pos[0] = n_tokens - 1;         // Position at end of sequence
    batch.n_seq_id[0] = 1;               // Single sequence
    batch.seq_id[0][0] = 0;              // Sequence ID 0
    batch.logits[0] = true;              // Need logits for next sampling
    batch.n_tokens = 1;
    
    // Run inference for this single token
    int result = llama_decode(internal->ctx, batch);
    llama_batch_free(batch);
    
    if (result != 0) {
        return nullptr;
    }
    
    // Return token string (using thread_local for safe return)
    static thread_local std::string result_str;
    result_str = token_str;
    return result_str.c_str();
}

/**
 * Reset the context for a new conversation.
 * 
 * Clears the KV cache and token history.
 * Call this before starting a new independent conversation.
 */
void llama_reset_context(LlamaContext ctx) {
    if (!ctx) return;
    auto* internal = static_cast<LlamaContextInternal*>(ctx);
    if (!internal->loaded) return;
    
    // Clear KV cache (model's working memory)
    llama_memory_t mem = llama_get_memory(internal->ctx);
    if (mem) {
        llama_memory_clear(mem, true);
    }
    
    // Clear our token tracking
    internal->tokens.clear();
    internal->system_prompt.clear();
}

/**
 * Get the last error message.
 * 
 * Returns empty string if no error occurred.
 */
const char* llama_get_error(LlamaContext ctx) {
    if (!ctx) return "Invalid context";
    auto* internal = static_cast<LlamaContextInternal*>(ctx);
    return internal->last_error.c_str();
}

}  // extern "C"