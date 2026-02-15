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

- **Android**: Android Studio with Android SDK
- **iOS**: Xcode 15+ with Xcode command line tools

## Running on Android Simulator

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to `mobile-app/` and open
4. Wait for Gradle sync to complete
5. Create/select an Android emulator (API 26+)
6. Click Run ▶️ or:
   ```bash
   cd mobile-app
   ./gradlew installDebug
   ```

## Running on iOS Simulator

1. Build the Kotlin framework:
   ```bash
   cd mobile-app
   ./gradlew linkDebugFrameworkIosSimulatorArm64
   ```

2. Open Xcode:
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

3. Select an iOS Simulator target
4. Click Run ▶️

## Running on Desktop

```bash
cd mobile-app
./gradlew desktopRun
```

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