#!/bin/bash
set -e

echo "Checking for running iOS simulator..."

if xcrun simctl list devices 2>/dev/null | grep -q "Booted"; then
    echo "✓ iOS simulator already running"
    exit 0
fi

SIMULATOR=$(xcrun simctl list devices available 2>/dev/null | \
    grep -E "iPhone (1[5-9])" | \
    grep -v "Watch" | \
    head -1 | \
    sed 's/.*(\(.*\)).*/\1/')

if [ -z "$SIMULATOR" ]; then
    SIMULATOR=$(xcrun simctl list devices available 2>/dev/null | \
        grep "iPhone" | \
        head -1 | \
        sed 's/.*(\(.*\)).*/\1/')
fi

if [ -z "$SIMULATOR" ]; then
    echo "ERROR: No iOS simulator available"
    exit 1
fi

echo "Booting simulator: $SIMULATOR"
xcrun simctl boot "$SIMULATOR" 2>/dev/null || true
open -a Simulator

sleep 3

echo "✓ iOS simulator booted successfully"