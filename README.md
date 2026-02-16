# 🧠 Small Models Running in Your App

> **On-device AI without the cloud** — privacy-first, offline-capable, zero API costs.

This repository is the companion material for the talk *"Small Models Running in Your App"*. It contains a slide deck, two working demos, and everything you need to understand how small transformer models can power real features in mobile apps.

[![GitHub](https://img.shields.io/badge/GitHub-srgtuszy-181717?logo=github)](https://github.com/srgtuszy)
[![X](https://img.shields.io/badge/X-@srgtuszy-000000?logo=x)](https://x.com/srgtuszy)

---

## 💡 The Idea

Most mobile AI tasks — parsing commands, classifying text, calling functions — don't need trillion-parameter cloud models. A 270M-parameter model running *on the device* can handle them with **<300ms latency**, **full privacy**, and **zero per-request cost**.

This repo proves it with two progressively complex demos:

| | Demo 1 | Demo 2 |
|---|---|---|
| **Name** | Tiny Transformer | FunctionGemma |
| **Params** | ~20K | 270M (Q4) |
| **Size on disk** | ~80KB | ~230MB |
| **What it does** | Learns to parse commands → JSON | Production-grade function calling |
| **Runs on** | Any laptop (CPU) | Android, iOS, Desktop |
| **Training** | From scratch, 2-3 min | Fine-tuned with LoRA |

---

## 📂 Project Structure

```
small_models/
├── slides/                        # Reveal.js presentation
│   └── index.html                 # Full slide deck
├── demo1_tiny_transformer/        # Build a transformer from scratch
│   ├── train_simple.py            # Training script (~150 lines)
│   ├── generate_dataset.py        # Dataset generator
│   ├── dataset.txt                # Pre-generated training data
│   ├── model_output/              # Saved model weights & vocab
│   └── mobile-app/                # Compose Multiplatform app (Android + iOS)
├── demo2_function_gemma/          # On-device function calling
│   ├── composeApp/                # Shared Compose Multiplatform code
│   ├── iosApp/                    # iOS wrapper (XcodeGen)
│   ├── llama.cpp/                 # Inference engine (submodule)
│   └── download_model.sh          # Model download script
├── scripts/                       # Helper scripts
│   ├── ensure-android-emulator.sh
│   └── ensure-ios-simulator.sh
├── TALKING_POINTS.md              # Speaker notes & theory reference
└── README.md
```

---

## 🎤 Slides

The presentation is built with [Reveal.js](https://revealjs.com/) and covers:

1. **Why small models** — size vs. task matching
2. **How transformers work** — embeddings, attention, feed-forward (simplified)
3. **Function calling** — the killer mobile AI use case
4. **Two live demos** — from scratch to production
5. **Fine-tuning** — customize for your app with LoRA + Unsloth
6. **When to use on-device vs. cloud** — trade-offs & decision framework

### Run the slides

```bash
cd slides
npm start
# Opens at http://localhost:3000
```

> **Tip:** Press `S` to open speaker notes view.

---

## 🔬 Demo 1: Tiny Transformer (From Scratch)

**Goal:** Demystify transformers. Show that a working model is ~150 lines of Python.

A character-level transformer trained to parse natural language commands into structured JSON:

```
"show alert Hello"       → {"action": "alert", "message": "Hello"}
"navigate to settings"   → {"action": "navigate", "target": "settings"}
```

### Architecture

| Component | Value |
|---|---|
| Layers | 4 |
| Attention heads | 4 |
| Embedding dimension | 128 |
| Context window | 80 chars |
| Tokenization | Character-level |
| Weight tying | ✓ (input = output embeddings) |

### Run it

```bash
cd demo1_tiny_transformer
python3 train_simple.py
```

Training takes **2-3 minutes** on a laptop CPU. The script will:
1. Train for 10,000 iterations
2. Save model weights + vocabulary to `model_output/`
3. Run test inferences and print results
4. Export weights as JSON for mobile deployment

### Mobile App

The `mobile-app/` subdirectory contains a **Compose Multiplatform** app (Android + iOS) that loads the exported weights and runs inference entirely on-device — no PyTorch required.

---

## 📱 Demo 2: FunctionGemma (Production-Grade)

**Goal:** Show a real, shippable on-device function calling system.

A **Compose Multiplatform** chat app powered by Google's [FunctionGemma 270M](https://huggingface.co/google/functiongemma-270m-it) running through [llama.cpp](https://github.com/ggerganov/llama.cpp). The model translates natural language into structured function calls — entirely on-device.

### Supported Functions

| Function | Example input |
|---|---|
| `set_reminder` | *"Remind me to buy milk at 5pm"* |
| `navigate_to_screen` | *"Go to settings"* |
| `toggle_setting` | *"Turn on dark mode"* |
| `send_message` | *"Send John a message saying hello"* |
| `get_weather` | *"What's the weather in Warsaw?"* |
| `play_music` | *"Play something by Coldplay"* |
| `set_timer` | *"Set a timer for 10 minutes"* |
| `make_call` | *"Call mom"* |

### Setup

**1. Download the model**

```bash
cd demo2_function_gemma
./download_model.sh
```

Or manually download [`google_functiongemma-270m-it-Q4_0.gguf`](https://huggingface.co/bartowski/google_functiongemma-270m-it-GGUF) (~230MB) and place it in `composeApp/src/androidMain/assets/`.

**2. Run on your platform**

```bash
# Desktop
./gradlew run

# Android
./gradlew :composeApp:installDebug

# iOS
./gradlew :composeApp:assembleSharedXCFramework
cd iosApp && xcodegen generate
xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 16' build
```

### Technical Details

| Spec | Value |
|---|---|
| Inference engine | llama.cpp (via Llamatik) |
| Model | FunctionGemma 270M IT (Q4_0) |
| RAM usage | ~500MB |
| Platforms | Android (API 26+), iOS (16.6+), Desktop |
| Kotlin | 1.9.22 |
| Compose | 1.6.0 |

---

## 🛠 VS Code Integration

Press **F5** to launch any configuration:

| Configuration | What it does |
|---|---|
| ▶ Demo 1: Train Tiny Transformer | Runs Python training script |
| ▶ Demo 2: Desktop Debug | Launches desktop app with Kotlin debugger |
| ▶ Demo 2: Android Debug | Boots emulator → builds → installs → attaches debugger |
| ▶ Demo 2: iOS Debug | Boots simulator → builds XCFramework → builds app → attaches LLDB |
| 📽 Slides: Open Presentation | Starts dev server → opens browser |

### Required Extensions

| Extension | Purpose |
|---|---|
| `vadimcn.vscode-lldb` | Native/iOS debugging |
| `nisargjhaveri.android-debug` | Android debugging |
| `nisargjhaveri.ios-debug` | iOS simulator debugging |
| `fwcd.kotlin` | Kotlin language support |
| `vscjava.vscode-java-debug` | Java debugging |
| `redhat.java` | Java language support |

Install all at once:

```bash
code --install-extension vadimcn.vscode-lldb && \
code --install-extension nisargjhaveri.android-debug && \
code --install-extension nisargjhaveri.ios-debug && \
code --install-extension fwcd.kotlin && \
code --install-extension vscjava.vscode-java-debug && \
code --install-extension redhat.java
```

---

## ⚙️ Prerequisites

- **Python 3.8+** with PyTorch — for Demo 1
- **JDK 17+** — for Demo 2 (Kotlin/Gradle)
- **Android SDK 34** — for Android builds
- **Xcode 15+** & **XcodeGen** (`brew install xcodegen`) — for iOS builds

---

## 📚 Resources

| Resource | Link |
|---|---|
| FunctionGemma (original) | [google/functiongemma-270m-it](https://huggingface.co/google/functiongemma-270m-it) |
| FunctionGemma (GGUF) | [bartowski/google_functiongemma-270m-it-GGUF](https://huggingface.co/bartowski/google_functiongemma-270m-it-GGUF) |
| llama.cpp | [ggerganov/llama.cpp](https://github.com/ggerganov/llama.cpp) |
| Llamatik (KMP bindings) | [ferranpons/Llamatik](https://github.com/ferranpons/Llamatik) |
| Unsloth (fine-tuning) | [unslothai/unsloth](https://github.com/unslothai/unsloth) |
| NanoGPT | [karpathy/nanoGPT](https://github.com/karpathy/nanoGPT) |
| Reveal.js | [hakimel/reveal.js](https://github.com/hakimel/reveal.js) |

---

## 📄 License

Demo code is provided as-is for educational purposes. FunctionGemma is subject to [Google's Gemma Terms of Use](https://ai.google.dev/gemma/terms).

---

<p align="center">
  Built with ❤️ by <a href="https://github.com/srgtuszy">Michał Tuszyński</a>
</p>