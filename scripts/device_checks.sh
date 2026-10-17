#!/usr/bin/env bash
# Instrumented tests on an emulator: small screen, enlarged text, airplane mode (the app is offline).
set -u
adb shell wm size 1080x1920
adb shell wm density 480
adb shell settings put system font_scale 1.3
adb shell settings put secure show_ime_with_hard_keyboard 1
adb shell cmd connectivity airplane-mode enable || true
./gradlew --no-daemon --stacktrace connectedDebugAndroidTest
test_result=$?
mkdir -p device-artifacts
adb logcat -d > device-artifacts/logcat.txt || true
exit "$test_result"
