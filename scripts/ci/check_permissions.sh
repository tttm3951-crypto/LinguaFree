#!/usr/bin/env bash
# Fails if the APK requests any permission outside the allowed offline set.
set -euo pipefail
APK="$1"
AAPT2=$(ls -d "$ANDROID_HOME"/build-tools/*/ | sort -V | tail -1)aapt2
ALLOWED='android.permission.VIBRATE|android.permission.POST_NOTIFICATIONS|android.permission.RECORD_AUDIO|android.permission.RECEIVE_BOOT_COMPLETED|.*\.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
perms=$("$AAPT2" dump permissions "$APK" | grep -E '^(uses-permission|permission)' | sed -n "s/.*name='\([^']*\)'.*/\1/p" | sort -u)
echo "Permissions in $APK:"; echo "$perms"
bad=$(echo "$perms" | grep -vE "^($ALLOWED)$" || true)
if [ -n "$bad" ]; then
  echo "::error title=Unexpected permission::Only VIBRATE, POST_NOTIFICATIONS, RECORD_AUDIO, RECEIVE_BOOT_COMPLETED are allowed. Found:%0A${bad//$'\n'/%0A}"
  exit 1
fi
echo "OK: only allowed permissions."
