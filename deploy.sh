#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "Building and deploying to device..."
./gradlew installDebug

echo "Launching Zenith Arranger..."
/home/sajins/development/android-sdk/platform-tools/adb shell am start -n com.zenitharranger/.MainActivity
