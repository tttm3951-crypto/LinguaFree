#!/usr/bin/env bash
set -u
adb shell wm size 1080x1920
adb shell wm density 480
adb shell settings put system font_scale 1.3
gradle --no-daemon connectedDebugAndroidTest
test_result=$?
mkdir -p device-artifacts
adb exec-out run-as org.linguafree.app tar -C files -cf - screenshots > device-artifacts/screenshots.tar || true
exit "$test_result"
