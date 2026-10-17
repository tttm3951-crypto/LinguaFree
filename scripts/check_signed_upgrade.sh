#!/usr/bin/env bash
set -euo pipefail
adb install -r upgrade-base.apk
adb shell am force-stop org.linguafree.app
cat > upgrade-progress.xml <<'XML'
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
<int name="xp" value="321" />
<int name="lesson" value="2" />
<string name="completed">0,1</string>
<string name="learnerName">Upgrade learner</string>
<string name="learnerId">linguafree-stable-learner</string>
<int name="foxAvatar" value="3" />
<boolean name="onboarded" value="true" />
<boolean name="showWelcome" value="false" />
<boolean name="effectsEnabled" value="false" />
<boolean name="hapticsEnabled" value="false" />
<int name="reminders" value="0" />
<set name="beginnerMastered"><string>start-01</string></set>
<string name="beginnerLevel">A1</string>
</map>
XML
adb push upgrade-progress.xml /data/local/tmp/upgrade-progress.xml
adb shell run-as org.linguafree.app mkdir -p shared_prefs
adb shell run-as org.linguafree.app cp /data/local/tmp/upgrade-progress.xml shared_prefs/linguafree_progress.xml
adb install -r "$1"
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class org.linguafree.app.UpgradePersistenceTest -e upgradeCheck true org.linguafree.app.test/androidx.test.runner.AndroidJUnitRunner | tee upgrade-result.txt
grep -q 'OK (1 test)' upgrade-result.txt
! grep -q 'FAILURES\|INSTRUMENTATION_FAILED\|Process crashed' upgrade-result.txt
