# Demo 2: Function Gemma - On-Device Function Calling with llama.cpp

**Model:** Google FunctionGemma 270M IT  
**Model Size (Q4_0):** ~230MB  
**RAM Usage:** ~500MB  
**Purpose:** Translate natural language into executable function calls

## What This Demo Shows

This Jetpack Compose Multiplatform app demonstrates:
- **Real on-device LLM inference** using llama.cpp via Llamatik
- Natural language to function call translation with FunctionGemma
- Available functions sidebar with parameter hints
- Real-time function call visualization
- Cross-platform support (Android, iOS, Desktop)

## Architecture

```
┌─────────────────────────────────────────────┐
│                    App.kt                    │
│  (Compose UI - Chat interface, functions)   │
└────────────────────┬────────────────────────┘
                      │
┌────────────────────▼────────────────────────┐
│                 LLMEngine                    │
│         (Common interface - expect/actual)   │
└────────────────────┬────────────────────────┘
                      │
     ┌────────────────┼────────────────┐
     │                │                │
┌───▼───┐      ┌─────▼─────┐    ┌─────▼─────┐
│Android│      │  Desktop  │    │    iOS    │
│       │      │           │    │           │
│llama. │      │  llama.   │    │  llama.   │
│cpp    │      │  cpp      │    │  cpp      │
│(Llamatik)│   │(Llamatik) │    │(Llamatik) │
└───────┘      └───────────┘    └───────────┘
```

## Prerequisites

### All Platforms
- JDK 17
- ~500MB free storage for model

### Android
- Android device or emulator (API 26+)
- Android SDK 34

### iOS
- Xcode 15+
- iOS 16.6+ device or simulator

## Setup

### Step 1: Download Model

Run the model download script:

```bash
cd demo2_function_gemma
./download_model.sh
```

Or manually download from [HuggingFace](https://huggingface.co/bartowski/google_functiongemma-270m-it-GGUF):
- Download `google_functiongemma-270m-it-Q4_0.gguf`
- Place in `composeApp/src/androidMain/assets/`

### Step 2: Build and Run

#### Android

From VSCode:
1. Press `F5` or click "Run and Debug"
2. Select "▶ Demo 2: Android Debug"

Or from command line:
```bash
cd demo2_function_gemma
./gradlew :composeApp:installDebug
```

#### iOS

From VSCode:
1. Press `F5` or click "Run and Debug"  
2. Select "▶ Demo 2: iOS Debug"

Or from command line:
```bash
cd demo2_function_gemma
./gradlew :composeApp:assembleSharedXCFramework
cd iosApp
xcodegen generate
xcodebuild -project iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' build
```

#### Desktop

From VSCode:
1. Press `F5` or click "Run and Debug"
2. Select "▶ Demo 2: Desktop Debug"

Or from command line:
```bash
cd demo2_function_gemma
./gradlew run
```

## Debugging

### VSCode Debugging

This project is configured for debugging directly from VSCode:

**Android Debugging:**
1. Connect Android device or start emulator
2. Select "▶ Demo 2: Android Debug" from debug menu
3. Set breakpoints in Kotlin code
4. App launches with debugger attached

**iOS Debugging:**
1. Ensure iOS simulator is running
2. Select "▶ Demo 2: iOS Debug"
3. Set breakpoints in Kotlin or Swift code
4. App launches with LLDB debugger attached

## Technical Details

| Spec | Value |
|------|-------|
| Kotlin Version | 1.9.22 |
| Compose Version | 1.6.0 |
| Target Platforms | Android, iOS, Desktop |
| Min Android SDK | 26 |
| Target Android SDK | 34 |
| Min iOS Version | 16.6 |
| Inference Engine | llama.cpp (via Llamatik) |
| Model | FunctionGemma 270M IT (Q4_0) |

## Supported Functions

| Function | Parameters | Description |
|----------|------------|-------------|
| `set_reminder` | title, time | Set a reminder |
| `navigate_to_screen` | screen | Navigate to a screen |
| `toggle_setting` | setting, enabled | Toggle a setting |
| `send_message` | contact, message | Send a message |
| `get_weather` | location | Get weather info |
| `play_music` | song, artist | Play music |
| `set_timer` | duration | Set a timer |
| `make_call` | contact | Make a phone call |

## Example Commands

Try these in the chat:
- "Set a reminder to buy milk tomorrow at 5pm"
- "Navigate to settings"
- "Turn on dark mode"
- "What's the weather in Warsaw?"
- "Send a message to John saying hello"
- "Play some music by Coldplay"

## FunctionGemma Prompt Format

FunctionGemma uses a special token format:

```
<bos><start_of_turn>developer
{system_prompt with function definitions}
<end_of_turn>
<start_of_turn>user
{user message}
<end_of_turn>
<start_of_turn>model
```

## Troubleshooting

### Model won't load
- Ensure `google_functiongemma-270m-it-Q4_0.gguf` is in the assets directory
- Check file size (~230MB)
- Verify device has enough RAM (~500MB free)

### Build errors
- Ensure you have JDK 17 installed
- Run `./gradlew clean` and rebuild
- Check that Llamatik dependency is resolved

### iOS linking issues
- Make sure iOS deployment target is 16.6+
- Clean build folder in Xcode
- See [Llamatik troubleshooting](https://docs.llamatik.com/troubleshooting/ios-linking/)

## Model Sources

- **FunctionGemma GGUF**: [bartowski/google_functiongemma-270m-it-GGUF](https://huggingface.co/bartowski/google_functiongemma-270m-it-GGUF)
- **Original Model**: [google/functiongemma-270m-it](https://huggingface.co/google/functiongemma-270m-it)
- **Llamatik**: [GitHub Repository](https://github.com/ferranpons/Llamatik)

## License

This demo uses the Gemma model which is subject to Google's terms of service.