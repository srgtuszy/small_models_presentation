# Small Models Demo - Running Guide

## Quick Start with VS Code Tasks

Press `Cmd+Shift+P` → "Tasks: Run Task" → Select a task:

### Demo 1: Tiny Transformer (Python)
- **▶ Demo 1: Train Tiny Transformer** - Run the training script (2-3 min)

### Demo 2: Function Gemma (Kotlin Multiplatform)
- **▶ Demo 2: Desktop App** - Launch on macOS/Windows/Linux
- **▶ Demo 2: Android** - Install on connected device/emulator

### Demo 3: Visual LLM (Kotlin Multiplatform)
- **▶ Demo 3: Desktop App** - Launch on macOS/Windows/Linux
- **▶ Demo 3: Android** - Install on connected device/emulator

### Android Setup
1. **📱 Android: List Available Emulators** - See all emulators
2. **📱 Android: Boot First Emulator** - Start emulator in background
3. **📱 Android: List Connected Devices** - Verify device is ready
4. Run the install task

### iOS Setup
1. **🍎 iOS: List Simulators** - See available simulators
2. **🍎 iOS: Boot iPhone 15 Simulator** - Start simulator
3. **🍎 iOS: Open Demo X in Xcode** - Open in Xcode, then Run from Xcode

### Slides
- **📽 Slides: Start Presentation Server** - Start at http://localhost:8000

---

## Manual Commands

### Demo 1: Python
```bash
cd demo1_tiny_transformer
python3 tiny_transformer_train.py
```

### Demo 2 & 3: Desktop
```bash
cd demo2_function_gemma  # or demo3_visual_llm
./gradlew run
```

### Demo 2 & 3: Android
```bash
# Boot emulator (optional)
emulator -list-avds                    # List available
emulator -avd <name> &                 # Boot specific one

# Build and install
cd demo2_function_gemma
./gradlew :composeApp:installDebug
```

### Demo 2 & 3: iOS
```bash
# Boot simulator
xcrun simctl list devices              # List available
xcrun simctl boot "iPhone 15"          # Boot specific one
open -a Simulator

# Open in Xcode
cd demo2_function_gemma/iosApp
open iosApp.xcworkspace  # or .xcodeproj
# Then press Cmd+R in Xcode
```

---

## Prerequisites

### For Python (Demo 1)
- Python 3.8+
- PyTorch: `pip install torch`

### For Android (Demo 2 & 3)
- Android SDK (~/Library/Android/sdk on macOS)
- Android emulator or physical device with USB debugging

### For iOS (Demo 2 & 3)
- Xcode 15+
- CocoaPods: `sudo gem install cocoapods`
- Run `pod install` in iosApp/ directory before opening Xcode

### For Desktop (Demo 2 & 3)
- JDK 17+
- No additional setup needed