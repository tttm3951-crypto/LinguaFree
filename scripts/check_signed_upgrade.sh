#!/usr/bin/env bash
# In-place upgrade check on an emulator: the published, signed 0.9 release is installed and
# started, then this signed release is installed over it (same key, higher versionCode) and
# must start without crashing. Progress import itself is covered by LegacyMigrationTest.
set -euo pipefail
NEW_APK="$1"
PKG=org.linguafree.app
mkdir -p device-artifacts
{
  echo "== Installing 0.9 (upgrade-base.apk)"
  adb install upgrade-base.apk
  adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
  sleep 8
  adb shell am force-stop "$PKG"
  echo "== Upgrading in place to $NEW_APK"
  adb logcat -c
  adb install -r "$NEW_APK"
  adb shell dumpsys package "$PKG" | grep -E "versionName|versionCode" | head -2
  adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
  sleep 20
  if ! adb shell pidof "$PKG" >/dev/null; then echo "FAILED: app is not running after upgrade"; exit 1; fi
  adb logcat -d > device-artifacts/logcat.txt
  if grep -A20 "FATAL EXCEPTION" device-artifacts/logcat.txt | grep -q "$PKG"; then echo "FAILED: crash after upgrade"; exit 1; fi
  echo "OK: upgrade installed over the signed 0.9 release and the app runs"
} 2>&1 | tee upgrade-result.txt
grep -q '^OK: upgrade' upgrade-result.txt
