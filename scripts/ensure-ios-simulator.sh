#!/bin/bash
set -e

# Use iPhone 17 simulator (iOS 26.x) for compatibility with SDK 26.2
SIMULATOR_ID="24BF1FED-6B05-479D-88B1-3E48AB3291B7"

echo "Checking for running iOS simulator..."

if xcrun simctl list devices 2>/dev/null | grep -q "$SIMULATOR_ID.*Booted"; then
    echo "✓ iPhone 17 simulator already running"
    exit 0
fi

if xcrun simctl list devices 2>/dev/null | grep -q "Booted"; then
    echo "✓ iOS simulator already running"
    exit 0
fi

echo "Booting iPhone 17 simulator..."
xcrun simctl boot "$SIMULATOR_ID" 2>/dev/null || true
open -a Simulator

sleep 3

echo "✓ iOS simulator booted successfully"