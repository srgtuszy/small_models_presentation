# Demo 3: Visual LLM - Small Models for Image Understanding

**Purpose:** Demonstrate visual-language models with image understanding

## What This Demo Shows

This Jetpack Compose Multiplatform app demonstrates:
- Image captioning simulation
- Visual Question Answering (VQA)
- Model size vs quality trade-offs
- Processing pipeline visualization

## Running the Demo

### Desktop (JVM)
```bash
./gradlew :composeApp:run
```

### Android
1. Open project in Android Studio
2. Sync Gradle
3. Run on emulator or device

## Tabs

### 1. Image Analysis
- Select from sample images (emoji placeholders)
- Generate simulated captions
- Ask questions about the image (VQA)

### 2. Model Sizes
Compare model sizes and their capabilities:
- Nano (1M params) - Basic classification
- Tiny (10M params) - Simple captions
- Small (100M params) - Good captions
- Base (500M params) - Detailed descriptions
- Large (1B+ params) - Complex reasoning

### 3. Processing Flow
Interactive visualization of how VLMs work:
1. Image Input → Raw pixels
2. Vision Encoder → Visual features
3. Projection → Language space
4. Language Model → Generate text
5. Output → Final caption

### 4. Compare Models
Side-by-side comparison of how different model sizes handle the same image.

## Sample Images

The demo uses emoji placeholders to simulate images:
- 🐱 Cat on Sofa
- 🏙️ City Skyline
- 🌅 Beach Sunset
- 🌲 Forest Path
- ☕ Coffee Cup
- 🏔️ Mountain Peak
- 🍽️ Food Plate
- 📖 Old Book

## Key Concepts

### Vision-Language Models (VLMs)

VLMs combine:
1. **Vision Encoder** - Extracts features from images
2. **Projection Layer** - Maps visual features to language space
3. **Language Model** - Generates text descriptions

### On-Device Considerations

| Model Size | RAM Needed | Quality | Use Case |
|------------|------------|---------|----------|
| 10-50MB | 100-300MB | Basic | Object detection, classification |
| 100-300MB | 500MB-1GB | Good | Simple captioning, VQA |
| 500MB+ | 2GB+ | Great | Complex reasoning, detailed descriptions |

## Technical Details

| Spec | Value |
|------|-------|
| Kotlin Version | 1.9.22 |
| Compose Version | 1.6.0 |
| Target Platforms | Android, iOS, Desktop |
| Min Android SDK | 24 |
| Min iOS Version | 14.0 |