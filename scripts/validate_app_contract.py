#!/usr/bin/env python3
"""Regression guard for LinguaFree offline content and existing user progress."""
from pathlib import Path
import json

root=Path(__file__).resolve().parents[1]
src=(root/"app/src/main/java/org/linguafree/app/MainActivity.kt").read_text(encoding="utf-8")
manifest=(root/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
build=(root/"app/build.gradle.kts").read_text(encoding="utf-8")
fox=(root/"app/src/main/java/org/linguafree/app/AnimatedFox.kt").read_text(encoding="utf-8")
reminders=(root/"app/src/main/java/org/linguafree/app/ReminderReceiver.kt").read_text(encoding="utf-8")
assets=root/"app/src/main/assets"
checks={
    "stable app identifier": 'applicationId = "org.linguafree.app"' in build,
    "stable progress store": 'getSharedPreferences("linguafree_progress"' in src,
    "progress export": "LinguaFreeProgress" in src and "exportProgress.launch" in src,
    "progress restore": "importProgress.launch" in src,
    "local lessons": 'assets.open("lessons.json")' in src,
    "local stories": 'assets.open("stories.json")' in src,
    "local dictionary": 'assets.open("dictionary.json")' in src,
    "local word flashcards": 'screen=="wordPractice"' in src and '"cardDue"' in src,
    "streaks and XP": '"dailyStreak"' in src and '"xp"' in src,
    "haptic and sound settings": '"effectsEnabled"' in src and "VibrationEffect" in src,
    "reminders": "ReminderReceiver.schedule" in src and "BOOT_COMPLETED" in manifest and "AlarmManager" in reminders,
    "offline speech preference": "EXTRA_PREFER_OFFLINE" in src,
    "fox animation": "rememberInfiniteTransition" in fox and 'mood=="celebrate"' in fox,
    "daily goals": '"goalDate"' in src and '"dailyGoal"' in src,
    "teacher dashboard": 'screen=="teacher"' in src and '"linguafree_teacher"' in src,
    "teacher report import": 'importTeacherReport.launch' in src and '"LinguaFreeStudyReport"' in src,
    "teacher report deletion": 'Данные ученика' in src and 'teacherReports.filterNot' in src,
    "voluntary progress report": '"LinguaFreeStudyReport"' in src and 'shareStudyReport.launch' in src,
    "report includes activity": '"studyDaysLast7"' in src and '"studyDaysLast30"' in src,
    "learner name stored locally": '"learnerName"' in src and 'saveLearnerName' in src,
    "learner name shown in exercises": 'твоё задание' in src and '$displayName' in src,
    "learner name editable": 'label={Text("Имя ученика")}' in src,
    "goal units consistent": 'занятий в день' in src and 'минут в день' not in src,
    "intro respects effects setting": 'if(getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE).getBoolean("effectsEnabled",true))playWelcomeMelody()' in src,

    "no network permission": 'android.permission.INTERNET' not in manifest,
    "content files bundled": all((assets/x).is_file() for x in ["lessons.json","stories.json","dictionary.json"]),
}
for name,ok in checks.items():
    print(("PASS" if ok else "FAIL")+": "+name)
if not all(checks.values()):
    raise SystemExit("App contract regression detected")
dictionary=json.loads((assets/"dictionary.json").read_text(encoding="utf-8"))
assert len(dictionary)>=600
print("Offline regression checks passed. Device testing and signing validation remain separate.")
