#include "llama_wrapper.h"
#include "llama.h"
#include "common.h"

#include <string>
#include <vector>
#include <cstring>

struct LlamaContextInternal {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    llama_sampler* sampler = nullptr;
    const llama_vocab* vocab = nullptr;
    std::vector<llama_token> tokens;
    std::string system_prompt;
    std::string last_error;
    bool loaded = false;
    int n_ctx = 2048;
    int n_gpu_layers = 0;
};

extern "C" {

LlamaContext llama_create_context(const char* model_path, int n_ctx, int n_gpu_layers) {
    auto* internal = new LlamaContextInternal();
    internal->n_ctx = n_ctx > 0 ? n_ctx : 2048;
    internal->n_gpu_layers = n_gpu_layers;
    
    llama_backend_init();
    
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = n_gpu_layers;
    
    internal->model = llama_model_load_from_file(model_path, model_params);
    if (!internal->model) {
        internal->last_error = "Failed to load model from: " + std::string(model_path);
        return internal;
    }
    
    internal->vocab = llama_model_get_vocab(internal->model);
    
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = internal->n_ctx;
    ctx_params.n_batch = 512;
    ctx_params.no_perf = true;
    
    internal->ctx = llama_init_from_model(internal->model, ctx_params);
    if (!internal->ctx) {
        internal->last_error = "Failed to create context";
        llama_model_free(internal->model);
        internal->model = nullptr;
        return internal;
    }
    
    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    internal->sampler = llama_sampler_chain_init(sampler_params);
    llama_sampler_chain_add(internal->sampler, llama_sampler_init_greedy());
    
    internal->loaded = true;
    return internal;
}

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

bool llama_is_loaded(LlamaContext ctx) {
    if (!ctx) return false;
    auto* internal = static_cast<LlamaContextInternal*>(ctx);
    return internal->loaded;
}

int llama_set_system_prompt(LlamaContext ctx, const char* prompt) {
    if (!ctx) return -1;
    auto* internal = static_cast<LlamaContextInternal*>(ctx);
    if (!internal->loaded) {
        internal->last_error = "Model not loaded";
        return -1;
    }
    
    internal->system_prompt = prompt ? prompt : "";
    internal->tokens.clear();
    
    if (!internal->system_prompt.empty()) {
        std::vector<llama_token> sys_tokens = common_tokenize(
            internal->vocab, 
            internal->system_prompt, 
            true, 
            true
        );
        internal->tokens.insert(internal->tokens.end(), sys_tokens.begin(), sys_tokens.end());
    }
    
    return 0;
}

int llama_process_user_prompt(LlamaContext ctx, const char* prompt, int max_tokens) {
    if (!ctx || !prompt) return -1;
    auto* internal = static_cast<LlamaContextInternal*>(ctx);
    if (!internal->loaded) {
        internal->last_error = "Model not loaded";
        return -1;
    }
    
    std::vector<llama_token> user_tokens = common_tokenize(
        internal->vocab, 
        std::string(prompt), 
        false, 
        true
    );
    
    internal->tokens.insert(internal->tokens.end(), user_tokens.begin(), user_tokens.end());
    
    llama_batch batch = llama_batch_get_one(internal->tokens.data(), internal->tokens.size());
    
    if (llama_decode(internal->ctx, batch) != 0) {
        internal->last_error = "Failed to decode prompt";
        return -1;
    }
    
    return 0;
}

const char* llama_generate_next_token(LlamaContext ctx) {
    if (!ctx) return nullptr;
    auto* internal = static_cast<LlamaContextInternal*>(ctx);
    if (!internal->loaded) {
        return nullptr;
    }
    
    llama_token new_token = llama_sampler_sample(internal->sampler, internal->ctx, -1);
    
    if (llama_vocab_is_eog(internal->vocab, new_token)) {
        return nullptr;
    }
    
    std::string token_str = common_token_to_piece(internal->vocab, new_token, true);
    
    llama_batch batch = llama_batch_get_one(&new_token, 1);
    if (llama_decode(internal->ctx, batch) != 0) {
        return nullptr;
    }
    
    static thread_local std::string result;
    result = token_str;
    return result.c_str();
}

void llama_reset_context(LlamaContext ctx) {
    if (!ctx) return;
    auto* internal = static_cast<LlamaContextInternal*>(ctx);
    if (!internal->loaded) return;
    
    llama_memory_t mem = llama_get_memory(internal->ctx);
    if (mem) {
        llama_memory_clear(mem, true);
    }
    internal->tokens.clear();
    internal->system_prompt.clear();
}

const char* llama_get_error(LlamaContext ctx) {
    if (!ctx) return "Invalid context";
    auto* internal = static_cast<LlamaContextInternal*>(ctx);
    return internal->last_error.c_str();
}

}