#!/usr/bin/env bash
set -u
adb shell wm size 1080x1920
adb shell wm density 480
adb shell settings put system font_scale 1.3
adb shell settings put secure show_ime_with_hard_keyboard 1
adb shell cmd connectivity airplane-mode enable
gradle --no-daemon connectedDebugAndroidTest
test_result=$?
mkdir -p device-artifacts
adb pull /sdcard/Download/LinguaFree-QA device-artifacts/ || true
exit "$test_result"
