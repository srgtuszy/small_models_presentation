cmake_minimum_required(VERSION 3.22.1)

project(llama_wrapper)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED true)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED true)

if(ANDROID)
    if(ANDROID_ABI STREQUAL "arm64-v8a")
        set(GGML_SYSTEM_ARCH "ARM")
        set(GGML_CPU_KLEIDIAI OFF)
        set(GGML_OPENMP ON)
    elseif(ANDROID_ABI STREQUAL "x86_64")
        set(GGML_SYSTEM_ARCH "x86")
        set(GGML_CPU_KLEIDIAI OFF)
        set(GGML_OPENMP OFF)
    endif()
    
    set(LLAMA_SRC ${CMAKE_CURRENT_LIST_DIR}/../../../../llama.cpp)
    add_subdirectory(${LLAMA_SRC} build-llama)
    
    add_library(llama_wrapper SHARED
        ${CMAKE_CURRENT_LIST_DIR}/../jni/llama_jni.cpp
        ${CMAKE_CURRENT_LIST_DIR}/llama_wrapper.cpp
    )
    
    target_compile_definitions(llama_wrapper PRIVATE
        GGML_SYSTEM_ARCH=${GGML_SYSTEM_ARCH}
        GGML_CPU_KLEIDIAI=$<BOOL:${GGML_CPU_KLEIDIAI}>
        GGML_OPENMP=$<BOOL:${GGML_OPENMP}>
    )
    
    target_include_directories(llama_wrapper PRIVATE
        ${LLAMA_SRC}
        ${LLAMA_SRC}/common
        ${LLAMA_SRC}/include
        ${LLAMA_SRC}/ggml/include
        ${LLAMA_SRC}/ggml/src
    )
    
    target_link_libraries(llama_wrapper
        llama
        common
        android
        log
    )
endif()