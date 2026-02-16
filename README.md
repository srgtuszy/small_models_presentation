# Small Models Demo - Running Guide

## One-Click Debug in VS Code

Press `F5` or go to **Run and Debug** panel → Select configuration → Click run:

| Configuration | What Happens |
|---------------|--------------|
| ▶ Demo 1: Train Tiny Transformer | Run Python training script |
| ▶ Demo 2: Desktop Debug | Launch app with Kotlin debugger |
| ▶ Demo 2: Android Debug | Boot emulator → build → install → launch → attach debugger |
| ▶ Demo 2: iOS Debug | Boot simulator → build XCFramework → build app → launch → attach debugger |
| 📽 Slides: Open Presentation | Start server → open browser |

---

## Prerequisites

### Required Software
- **JDK 17+** - For Kotlin/Gradle
- **Xcode 15+** - For iOS development
- **XcodeGen** - `brew install xcodegen`
- **Android SDK** - With emulator created
- **Python 3.8+** - For Demo 1

### Required VS Code Extensions
VS Code will prompt to install these when opening the workspace:

| Extension | Purpose |
|-----------|---------|
| `vadimcn.vscode-lldb` | Core debugger (iOS + Native) |
| `nisargjhaveri.android-debug` | Android debugging |
| `nisargjhaveri.ios-debug` | iOS simulator debugging |
| `fwcd.kotlin` | Kotlin language + debug adapter |
| `vscjava.vscode-java-debug` | Java debugging support |
| `redhat.java` | Java language support |

Install all at once:
```bash
code --install-extension vadimcn.vscode-lldb
code --install-extension nisargjhaveri.android-debug
code --install-extension nisargjhaveri.ios-debug
code --install-extension fwcd.kotlin
code --install-extension vscjava.vscode-java-debug
code --install-extension redhat.java
```

---

## First-Time Setup

### 1. Create Android Emulator (one-time)
```bash
# List available device definitions
avdmanager list device

# Create emulator (example: Pixel 7, API 34)
echo "no" | avdmanager create avd \
  -n "Pixel_7_API_34" \
  -k "system-images;android-34;google_apis;arm64-v8a" \
  -d "pixel_7"
```

Or use Android Studio: **Tools → Device Manager → Create Device**

### 2. iOS Setup (automatic)
iOS simulator will be created automatically if needed.

---

## Project Structure

```
small_models/
├── .vscode/
│   ├── extensions.json    # Extension recommendations
│   ├── launch.json        # Debug configurations (F5)
│   └── tasks.json         # Build tasks
├── scripts/
│   ├── ensure-android-emulator.sh
│   └── ensure-ios-simulator.sh
├── demo1_tiny_transformer/
│   └── tiny_transformer_train.py
├── demo2_function_gemma/
│   ├── composeApp/
│   │   ├── build.gradle.kts
│   │   └── src/
│   └── iosApp/
│       ├── project.yml        # XcodeGen spec
│       ├── iosApp.xcodeproj/  # Generated
│       └── iosApp/
│           ├── iOSApp.swift
│           ├── ContentView.swift
│           └── Info.plist
├── slides/
│   └── index.html
├── TALKING_POINTS.md
└── README.md
```

---

## Manual Commands (if needed)

### Demo 1: Python
```bash
cd demo1_tiny_transformer
python3 tiny_transformer_train.py
```

### Demo 2: Desktop
```bash
cd demo2_function_gemma
./gradlew run
```

### Demo 2: Android
```bash
cd demo2_function_gemma
./gradlew :composeApp:installDebug
```

### Demo 2: iOS
```bash
cd demo2_function_gemma

# Generate Xcode project
cd iosApp && xcodegen generate && cd ..

# Build XCFramework
./gradlew :composeApp:assembleSharedXCFramework

# Build and run in simulator
cd iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  build
```

---

## Troubleshooting

### "No Android emulator available"
Create one via Android Studio or command line (see First-Time Setup).

### "xcodegen not found"
```bash
brew install xcodegen
```

### iOS build fails with "Shared.xcframework not found"
```bash
cd demo2_function_gemma
./gradlew :composeApp:assembleSharedXCFramework
```

### Android debugger won't attach
1. Ensure device/emulator has USB debugging enabled
2. Check `adb devices` shows your device
3. Try: `adb kill-server && adb start-server`

### iOS debugger won't attach
1. Ensure simulator is booted: `open -a Simulator`
2. Check LLDB can connect: `lldb` then `platform select ios-simulator`