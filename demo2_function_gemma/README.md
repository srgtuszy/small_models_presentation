# Demo 2: Function Gemma - On-Device Function Calling

**Model:** Google Gemma 3 270M IT (Function-tuned)  
**Model Size (INT4):** ~288MB  
**RAM Usage:** ~551MB  
**Purpose:** Translate natural language into executable function calls

## What This Demo Shows

This Jetpack Compose Multiplatform app demonstrates:
- Natural language to function call translation
- Available functions sidebar with parameter hints
- Real-time function call visualization
- Simulated on-device inference

## Running the Demo

### Desktop (JVM)
```bash
./gradlew :composeApp:run
```

### Android
1. Open project in Android Studio
2. Sync Gradle
3. Run on emulator or device

## Key Features

- **Function Sidebar**: Shows available functions and their parameters
- **Chat Interface**: Natural language input with simulated responses
- **Function Call Cards**: Visual representation of parsed function calls
- **Model Info Panel**: Shows model specifications

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

## Architecture

This is a UI demo. In production, integrate with:
- **MediaPipe LLM Inference API** - Google's official SDK
- **LiteRT** - TensorFlow Lite runtime
- **ONNX Runtime** - Cross-platform inference

## Technical Details

| Spec | Value |
|------|-------|
| Kotlin Version | 1.9.22 |
| Compose Version | 1.6.0 |
| Target Platforms | Android, iOS, Desktop |
| Min Android SDK | 24 |
| Min iOS Version | 14.0 |