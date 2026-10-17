#!/usr/bin/env bash
# Fails if any advertising, billing, analytics or tracking library is declared or resolved.
# Usage: check_forbidden_deps.sh            -> static check of version catalog and build scripts
#        check_forbidden_deps.sh deps.txt   -> check a resolved `gradlew dependencies` report
set -euo pipefail
PATTERN='admob|billing|firebase|crashlytics|analytics|appsflyer|adjust-android|play-services-ads|(^|[^a-z])ads([^a-z]|$)'
cd "$(dirname "$0")/../.."
if [ $# -ge 1 ]; then
  # Only dependency coordinates (group:artifact) are checked in a resolved report.
  hits=$(grep -oiE '[a-z0-9_.-]+:[a-z0-9_.-]+' "$1" | sort -u | grep -iE "$PATTERN" || true)
  where="resolved dependencies ($1)"
else
  files=$(find . -path ./legacy -prune -o -path ./build -prune -o \( -name 'libs.versions.toml' -o -name '*.gradle.kts' -o -name '*.gradle' \) -print)
  hits=$(grep -inE "$PATTERN" $files | grep -v '^\s*//' || true)
  where="build scripts and version catalog"
fi
if [ -n "$hits" ]; then
  echo "::error title=Forbidden dependency::LinguaFree must not contain ads, billing, analytics or trackers. Found in $where:%0A${hits//$'\n'/%0A}"
  echo "$hits"
  exit 1
fi
echo "OK: no ads, billing, analytics or tracking libraries in $where."
