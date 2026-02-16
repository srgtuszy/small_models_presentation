# Command Parser Mobile App

A Kotlin Multiplatform app that uses a tiny transformer model to convert natural language commands to JSON.

## Project Structure

```
mobile-app/
├── composeApp/           # Kotlin Multiplatform shared code
│   ├── src/
│   │   ├── commonMain/   # Shared Kotlin code (model, UI)
│   │   ├── androidMain/  # Android-specific code
│   │   ├── desktopMain/  # Desktop JVM code  
│   │   └── iosMain/      # iOS-specific code
│   └── build.gradle.kts
├── iosApp/               # iOS Xcode project
│   └── iosApp.xcodeproj
└── gradle/
```

## Prerequisites

- **Android**: Android Studio with Android SDK (set `ANDROID_HOME`)
- **iOS**: Xcode 15+ 

## Running on iOS Simulator

**Important**: Do NOT run `assembleXCFramework` directly. Use these steps:

1. Build the Kotlin framework:
   ```bash
   cd mobile-app
   ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
   ```

2. Open and run in Xcode:
   ```bash
   open iosApp/iosApp.xcodeproj
   ```
   Then press ⌘R to run on iOS Simulator.

## Running on Android Simulator

```bash
cd mobile-app
export ANDROID_HOME=/path/to/android/sdk  # or add to local.properties
./gradlew installDebug
```

## Training the Model

```bash
cd ..  # back to demo1_tiny_transformer root
pip install torch
python tiny_transformer_train.py
```

The model will be trained and copied to all platform assets automatically.

## Training the Model

1. Train the transformer model:
   ```bash
   pip install torch
   python tiny_transformer_train.py
   ```

2. The script will:
   - Generate synthetic training data
   - Train a tiny transformer (~0.02M parameters)
   - Export model weights to `model_output/`
   - Copy to mobile app assets

## Supported Commands

The model converts natural language to JSON:

| Input | Output |
|-------|--------|
| "show alert with message Hello" | `{"action": "alert", "message": "Hello"}` |
| "navigate to settings" | `{"action": "navigate", "target": "settings"}` |
| "toggle dark_mode" | `{"action": "toggle", "setting": "dark_mode"}` |
| "go back" | `{"action": "back"}` |
| "refresh the page" | `{"action": "refresh"}` |
| "close the app" | `{"action": "close"}` |