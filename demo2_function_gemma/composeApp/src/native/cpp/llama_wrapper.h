#ifndef LLAMA_WRAPPER_H
#define LLAMA_WRAPPER_H

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef void* LlamaContext;

typedef bool (*TokenCallback)(const char* token, void* user_data);

LlamaContext llama_create_context(const char* model_path, int n_ctx, int n_gpu_layers);
void llama_destroy_context(LlamaContext ctx);

bool llama_is_loaded(LlamaContext ctx);

int llama_set_system_prompt(LlamaContext ctx, const char* prompt);
int llama_process_user_prompt(LlamaContext ctx, const char* prompt, int max_tokens);

const char* llama_generate_next_token(LlamaContext ctx);

void llama_reset_context(LlamaContext ctx);

const char* llama_get_error(LlamaContext ctx);

#ifdef __cplusplus
}
#endif

#endif