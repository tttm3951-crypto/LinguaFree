# Offline support and update safety

## Built into APK (no network needed)
- All 300 lessons in assets/lessons.json; 70 original stories in assets/stories.json.
- Searchable vocabulary is assembled locally from lessons and stories.
- XP, gems, completed lessons, current lesson, streak, study days, skill counters, repeat schedule, story rewards and preferences are stored in SharedPreferences named `linguafree_progress`.
- Sounds are generated locally with AudioTrack. Fox animation uses Compose Canvas. Haptics use the phone vibrator.
- Notifications are scheduled locally with AlarmManager, re-scheduled after reboot.

## Requires Android components installed on the device
- English text-to-speech requires an installed English offline TTS voice. The app prefers a non-network voice.
- English speech recognition requests `RecognizerIntent.EXTRA_PREFER_OFFLINE=true`, but Android speech providers may ignore the preference or have no offline English model. Do not promise guaranteed offline recognition.

## Updating without losing existing progress
1. Install the new APK **over** the old one, never uninstall the old app first.
2. Keep applicationId `org.linguafree.app`, signing certificate, and preference file `linguafree_progress` unchanged. **CRITICAL: Current GitHub Actions builds a debug APK on a fresh runner, where the default Android debug signing key may change between runs. Different certificates prevent installing an update over the old APK.** Set up a persistent, privately stored signing key in GitHub Actions secrets before relying on seamless updates. Never commit a private signing key into the repository.
3. Keep versionCode monotonically increasing (2 for this release).
4. Do not use Clear Storage/Clear Data. Uninstalling can delete local progress; AndroidManifest currently has allowBackup=false, so cloud restore is not provided.
5. All progress fields remain in the same preferences; lessons are still indexed 0–299.
6. Note: some lesson questions were rewritten while keeping lesson indices. Previously saved per-question review keys are index-based; their association with exact words may have shifted. Completed lesson history and overall XP remain intact, but prior word-level review history cannot be perfectly remapped without an older content-to-key migration.

## Offline QA checklist
- Disable Wi-Fi and mobile data; restart the app.
- Open all screens, complete a lesson, open stories, search vocabulary, use speed game, verify XP and completion.
- Close and reopen; verify progress, study days, settings, streak, sounds, haptics and achievements.
- Confirm notifications after restart and device reboot; Android notification permission must be granted.
- Test English speech with downloaded offline TTS and speech-recognition packages. Check fallback messages when packs are absent.
- Update over previous installed APK with the same signing key; compare progress before and after.
