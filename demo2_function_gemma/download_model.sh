#!/bin/bash

# Download FunctionGemma 270M IT model in GGUF format for llama.cpp inference
# Model source: https://huggingface.co/bartowski/google_functiongemma-270m-it-GGUF

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=================================================="
echo "  FunctionGemma GGUF Model Download"
echo "=================================================="
echo ""
echo "This script downloads FunctionGemma 270M IT model"
echo "in GGUF format optimized for llama.cpp inference."
echo ""

# Check for required tools
if ! command -v curl &> /dev/null && ! command -v wget &> /dev/null; then
    echo "Error: curl or wget is required"
    exit 1
fi

# Create directories
ANDROID_ASSETS_DIR="$SCRIPT_DIR/composeApp/src/androidMain/assets"
DESKTOP_RESOURCES_DIR="$SCRIPT_DIR/composeApp/src/desktopMain/resources"

mkdir -p "$ANDROID_ASSETS_DIR"
mkdir -p "$DESKTOP_RESOURCES_DIR"

# Using Q4_0 quantization - good balance of size and quality
MODEL_FILE="google_functiongemma-270m-it-Q4_0.gguf"
MODEL_URL="https://huggingface.co/bartowski/google_functiongemma-270m-it-GGUF/resolve/main/$MODEL_FILE"

echo "Downloading FunctionGemma 270M IT (Q4_0 quantization)..."
echo "Source: https://huggingface.co/bartowski/google_functiongemma-270m-it-GGUF"
echo ""

# Download for Android
if [ ! -f "$ANDROID_ASSETS_DIR/$MODEL_FILE" ]; then
    echo "Downloading to Android assets (~241MB)..."
    if command -v curl &> /dev/null; then
        curl -L -o "$ANDROID_ASSETS_DIR/$MODEL_FILE" "$MODEL_URL"
    else
        wget -O "$ANDROID_ASSETS_DIR/$MODEL_FILE" "$MODEL_URL"
    fi
else
    echo "Model already exists in Android assets, skipping..."
fi

# Download for Desktop
if [ ! -f "$DESKTOP_RESOURCES_DIR/$MODEL_FILE" ]; then
    echo "Downloading to Desktop resources (~241MB)..."
    if command -v curl &> /dev/null; then
        curl -L -o "$DESKTOP_RESOURCES_DIR/$MODEL_FILE" "$MODEL_URL"
    else
        wget -O "$DESKTOP_RESOURCES_DIR/$MODEL_FILE" "$MODEL_URL"
    fi
else
    echo "Model already exists in Desktop resources, skipping..."
fi

echo ""
echo "=================================================="
echo "  Download Complete!"
echo "=================================================="
echo ""
echo "Model files:"
ls -lh "$ANDROID_ASSETS_DIR/$MODEL_FILE" 2>/dev/null || true
echo ""
echo "Next steps:"
echo "1. Build the app: ./gradlew :composeApp:assembleDebug"
echo "2. Run on device/emulator from VSCode (F5)"
echo ""
echo "Model details:"
echo "  - Format: GGUF (llama.cpp)"
echo "  - Quantization: Q4_0"
echo "  - Size: ~241MB"
echo "  - Type: FunctionGemma 270M IT"
echo "  - Optimized for: Function calling tasks"
echo ""