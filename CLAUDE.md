# CLAUDE.md — правила проекта LinguaFree

Этот файл читается в начале каждой сессии Claude Code. Правила ниже обязательны.

## Что это

LinguaFree — нативное Android-приложение для изучения английского русскоязычными
пользователями. Логика обучения, геймификация и UX повторяют модель Duolingo:
короткие уроки, путь из юнитов, XP, серии дней, сердца, лиги, мгновенная обратная связь.
Бренд собственный: название LinguaFree, маскот — лисёнок, своя палитра и иконки.
Логотип, сову, название и ассеты Duolingo не использовать.

- `app_name`: LinguaFree
- `applicationId`: `org.linguafree.app` — сохранён с версии 0.9, чтобы новая версия
  ставилась поверх и не теряла прогресс. Не менять без решения владельца.
- Тема Compose: `LinguaFreeTheme`, класс приложения: `LinguaFreeApp`.

## Жёсткие ограничения (на всех этапах, без исключений)

- **Никаких подписок**, премиум-тарифов, пробных периодов и платных функций. Всё бесплатно.
- **Никакой рекламы**: AdMob и любые рекламные SDK запрещены; нет баннеров,
  межстраничной рекламы и «посмотри видео за награду».
- **Никаких покупок за деньги**: Google Play Billing запрещён. Кристаллы
  зарабатываются только обучением (уроки, квесты, сундуки, достижения, лиги).
- **Никакой аналитики и трекеров** (Firebase, Crashlytics, AppsFlyer и т. п.).
- **Полностью офлайн**: нет разрешения `INTERNET`. Разрешены только
  `VIBRATE`, `POST_NOTIFICATIONS`, `RECORD_AUDIO`, `RECEIVE_BOOT_COMPLETED`.
- В интерфейсе нет пейволлов, замочков «Premium», кнопок «Попробовать бесплатно».
- Механики, которые в оригинале давят на оплату, сделаны честными: при 0 сердец —
  бесплатная практика восстанавливает все сердца; есть «Режим без сердец».
- CI (`scripts/ci/check_forbidden_deps.sh`, `scripts/ci/check_permissions.sh`) падает,
  если правило нарушено. Не ослаблять эти проверки.

## Стек

Kotlin 2.0, Jetpack Compose (Material 3), minSdk 26, targetSdk 35, Gradle Kotlin DSL,
version catalog `gradle/libs.versions.toml`. Clean Architecture + MVVM, UDF (UiState + события).
Hilt, Navigation Compose, Coroutines/Flow, Room, DataStore, WorkManager,
Android TextToSpeech + Media3, SpeechRecognizer, Lottie. Тесты: JUnit5 (+Turbine) для логики,
Compose UI Test для экранов.

## Модули

| Модуль | Назначение |
|---|---|
| `:app` | `LinguaFreeApp`, `MainActivity`, навигация, нижняя панель, уведомления |
| `:core:domain` | чистый Kotlin: модели, правила (ответы, SRS, серия, сердца, XP, лиги, квесты) |
| `:core:data` | Room, DataStore, загрузка и валидация контента, репозитории, TTS/речь/звук |
| `:core:ui` | `LinguaFreeTheme`, компоненты, лисёнок, иконки, шрифт Nunito |
| `:feature:onboarding` | онбординг |
| `:feature:path` | путь обучения (главный экран) |
| `:feature:lesson` | урок и 9 типов упражнений |
| `:feature:practice` | практика, словарь, истории, грамматика |
| `:feature:profile` | профиль, статистика, настройки, экспорт/импорт, «О приложении» |
| `:feature:leagues` | лиги |
| `:feature:shop` | магазин за кристаллы |

Логика без Android — в `:core:domain` (её можно проверять быстро и тестировать JUnit5).

## Команды

```bash
python3 scripts/build_offline_library.py   # офлайн-справочник (нужен до сборки :core:data)
python3 scripts/content/validate_content.py # проверка контента
./gradlew lint test assembleDebug           # то же, что в CI
./gradlew assembleRelease                   # R8; подпись — только из секретов CI
bash scripts/ci/check_forbidden_deps.sh     # нет рекламы/биллинга/аналитики
```

## Правила работы с репозиторием

- Основная ветка `main`. Каждый этап — отдельная ветка `feature/NN-name`, после
  зелёного CI — PR в `main` с описанием.
- Коммиты — Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `chore:`, `refactor:`.
- Ключи подписи и секреты не коммитить никогда (`.gitignore` это закрывает).
- Лицензию (`LICENSE`) не добавлять без решения владельца.
- Существующий контент (уроки, истории, словарь, грамматика) не удалять — переносить.
- Лисёнок всегда на прозрачном фоне, без подложек. Иконка — лисёнок на зелёном.
- При ошибке распознавания речи показывать подсказку про офлайн-пакет
  (`SpeechHelp.OFFLINE_PACK_HINT`).
