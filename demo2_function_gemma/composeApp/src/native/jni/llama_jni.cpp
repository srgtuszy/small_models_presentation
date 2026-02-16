#include <jni.h>
#include <string>
#include <cstring>
#include "../cpp/llama_wrapper.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_demo_functiongemma_llm_LlamaNative_nativeCreateContext(
    JNIEnv* env,
    jobject thiz,
    jstring model_path,
    jint n_ctx,
    jint n_gpu_layers
) {
    const char* path = env->GetStringUTFChars(model_path, nullptr);
    
    LlamaContext ctx = llama_create_context(path, n_ctx, n_gpu_layers);
    
    env->ReleaseStringUTFChars(model_path, path);
    
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_demo_functiongemma_llm_LlamaNative_nativeDestroyContext(
    JNIEnv* env,
    jobject thiz,
    jlong context_ptr
) {
    LlamaContext ctx = reinterpret_cast<LlamaContext>(context_ptr);
    if (ctx) {
        llama_destroy_context(ctx);
    }
}

JNIEXPORT jboolean JNICALL
Java_demo_functiongemma_llm_LlamaNative_nativeIsLoaded(
    JNIEnv* env,
    jobject thiz,
    jlong context_ptr
) {
    LlamaContext ctx = reinterpret_cast<LlamaContext>(context_ptr);
    return llama_is_loaded(ctx) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_demo_functiongemma_llm_LlamaNative_nativeSetSystemPrompt(
    JNIEnv* env,
    jobject thiz,
    jlong context_ptr,
    jstring prompt
) {
    LlamaContext ctx = reinterpret_cast<LlamaContext>(context_ptr);
    const char* prompt_str = env->GetStringUTFChars(prompt, nullptr);
    
    int result = llama_set_system_prompt(ctx, prompt_str);
    
    env->ReleaseStringUTFChars(prompt, prompt_str);
    return result;
}

JNIEXPORT jstring JNICALL
Java_demo_functiongemma_llm_LlamaNative_nativeProcessUserPrompt(
    JNIEnv* env,
    jobject thiz,
    jlong context_ptr,
    jstring prompt,
    jint max_tokens
) {
    LlamaContext ctx = reinterpret_cast<LlamaContext>(context_ptr);
    const char* prompt_str = env->GetStringUTFChars(prompt, nullptr);
    
    int result = llama_process_user_prompt(ctx, prompt_str, max_tokens);
    
    env->ReleaseStringUTFChars(prompt, prompt_str);
    
    if (result != 0) {
        const char* error = llama_get_error(ctx);
        return env->NewStringUTF(error);
    }
    
    return env->NewStringUTF("");
}

JNIEXPORT jstring JNICALL
Java_demo_functiongemma_llm_LlamaNative_nativeGenerateNextToken(
    JNIEnv* env,
    jobject thiz,
    jlong context_ptr
) {
    LlamaContext ctx = reinterpret_cast<LlamaContext>(context_ptr);
    
    const char* token = llama_generate_next_token(ctx);
    
    if (token == nullptr) {
        return nullptr;
    }
    
    return env->NewStringUTF(token);
}

JNIEXPORT void JNICALL
Java_demo_functiongemma_llm_LlamaNative_nativeResetContext(
    JNIEnv* env,
    jobject thiz,
    jlong context_ptr
) {
    LlamaContext ctx = reinterpret_cast<LlamaContext>(context_ptr);
    llama_reset_context(ctx);
}

JNIEXPORT jstring JNICALL
Java_demo_functiongemma_llm_LlamaNative_nativeGetError(
    JNIEnv* env,
    jobject thiz,
    jlong context_ptr
) {
    LlamaContext ctx = reinterpret_cast<LlamaContext>(context_ptr);
    const char* error = llama_get_error(ctx);
    return env->NewStringUTF(error);
}

}