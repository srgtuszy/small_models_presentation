#!/bin/bash
set -e

echo "Checking for running Android emulator..."

if adb devices 2>/dev/null | grep -q "emulator.*device"; then
    echo "✓ Android emulator already running"
    exit 0
fi

EMULATOR=$(emulator -list-avds 2>/dev/null | head -1)
if [ -z "$EMULATOR" ]; then
    echo "ERROR: No Android emulator available."
    echo "Create one in Android Studio: Tools > Device Manager > Create Device"
    exit 1
fi

echo "Booting emulator: $EMULATOR"
nohup emulator -avd "$EMULATOR" -no-snapshot-load -no-audio -no-boot-anim -accel on > /dev/null 2>&1 &

echo "Waiting for emulator to boot..."
adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done' 2>/dev/null

echo "✓ Android emulator booted successfully"