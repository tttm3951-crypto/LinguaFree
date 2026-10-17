# Как участвовать в LinguaFree

Спасибо за помощь! Прочитайте сначала раздел «Жёсткие ограничения» в [CLAUDE.md](CLAUDE.md):
никакой рекламы, подписок, покупок, аналитики и интернета. PR с такими изменениями не принимаются.

## Ветки

- `main` — всегда собирается и проходит тесты.
- Работа ведётся в ветках `feature/<номер>-<кратко>` (этапы) или `fix/<кратко>`, `docs/<кратко>`.
- Изменения попадают в `main` через Pull Request после зелёного CI.

## Коммиты

Стиль [Conventional Commits](https://www.conventionalcommits.org/ru/):

```
feat(lesson): добавить упражнение «сопоставление пар»
fix(path): не разблокировать юнит без проверки
docs: описать формат контента
test(domain): покрыть расчёт серии
chore(ci): кэшировать Gradle
```

Тип — `feat`, `fix`, `docs`, `test`, `chore`, `refactor`, `perf`, `style`.

## Перед Pull Request

```bash
python3 scripts/content/validate_content.py
./gradlew lint test assembleDebug
bash scripts/ci/check_forbidden_deps.sh
```

## Контент

- Формат описан в [docs/content-format.md](docs/content-format.md). Новые юниты и уроки
  добавляются JSON-файлами, без изменения кода.
- Не более 3–5 новых слов на урок; не меньше 12 упражнений; задействуйте разные типы.
- Пишите тексты сами: не копируйте упражнения из учебников и других приложений.
- Картинки — собственные векторные иллюстрации (VectorDrawable), см. `scripts/icons/`.
- Тексты интерфейса — в `strings.xml`/Kotlin на русском. Чтобы добавить язык интерфейса
  (например, казахский `kk`), создайте `values-kk/strings.xml` и добавьте `kk`
  в `resourceConfigurations` и `locales_config.xml`.

## Код

- Логика без Android — в `:core:domain`, с тестами JUnit5.
- Экраны — Compose + ViewModel с `UiState` и событиями (однонаправленный поток данных).
- Лисёнок всегда на прозрачном фоне (`FoxMascot`), без подложек.
