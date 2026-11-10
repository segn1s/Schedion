# Schedion

Schedion — Android-приложение для просмотра расписания вуза. Проект находится на начальном этапе разработки и содержит базовую структуру приложения с одним стартовым экраном на Jetpack Compose.

## Архитектура и стек технологий

- **Архитектура:** MVVM (Model-View-ViewModel)
- **Принцип SingleActivity:** единственной Activity приложения является `MainActivity`; вся навигация между экранами реализуется декларативно через Jetpack Compose и собственный `NavGraph`.
- **Язык разработки:** Kotlin 2.2.10
- **UI Framework:** Jetpack Compose & Material 3 (Compose BOM `2026.02.01`)
- **Внедрение зависимостей (DI):** Hilt
- **Сетевой слой:** Retrofit + OkHttp
- **Локальная база данных (офлайн-кэш):** Room
- **Навигация:** Собственный `NavGraph` на Jetpack Compose
- **Сборка:** Gradle 9.5.0, Android Gradle Plugin (AGP) 9.3.3
- **Управление зависимостями:** Gradle Version Catalog (`gradle/libs.versions.toml`)
- **Параметры SDK:**
  - `compileSdk`: 37 (Android 17)
  - `targetSdk`: 37 (Android 17)
  - `minSdk`: 29 (Android 10)
- **Java:** Source/Target Compatibility Java 11, среда сборки — JDK 17
- **Модульная структура:** Одномодульный проект (`:app`, namespace `net.schedion.app`)

## Серверное API

- **Базовый URL:** `https://schedion.ru/api/public/v1`
- **Документация OpenAPI:** [https://schedion.ru/openapi/v1.json](https://schedion.ru/openapi/v1.json)
- **Авторизация:** на данном этапе доступ публичный, авторизация студентов не требуется.
- **Основные эндпоинты:**
  - `GET /schedule` — расписание занятий на неделю (фильтрация по группам, преподавателям, курсам)
  - `GET /weeks` — список учебных недель семестра и определение чётности (цвета недели)
  - `GET /pairs` — расписание звонков (время пар)
  - `GET /home` — сводные данные главной страницы (текущая неделя, ближайшие события)
  - `GET /news` — новостная лента
  - `GET /documents` — публичные документы и графики учебного процесса

## Структура проекта

```text
Schedion/
├── .github/                      # Конфигурация GitHub (CI, CODEOWNERS, шаблоны)
│   ├── ISSUE_TEMPLATE/           # Шаблоны задач и баг-репортов
│   ├── workflows/ci.yml          # GitHub Actions CI (линтер, тесты, сборка debug APK)
│   ├── CODEOWNERS                # Назначение владельца кода (@segn1s)
│   └── pull_request_template.md  # Шаблон Pull Request
├── app/                          # Основной Android-модуль
│   └── src/
│       ├── main/                 # Исходный код (MainActivity, тема Material3)
│       ├── test/                 # Локальные Unit-тесты (JUnit4)
│       └── androidTest/          # Инструментальные UI-тесты (Compose, Espresso)
├── gradle/
│   ├── wrapper/                  # Gradle Wrapper 9.5.0
│   └── libs.versions.toml        # Version Catalog
├── AGENTS.md                     # Правила работы для команды и ИИ-ассистентов
└── README.md                     # Документация проекта
```

## Требования к окружению

- **JDK:** 17 или новее
- **Android Studio:** Ladybug / Meerkat или актуальная версия с поддержкой AGP 9.3+ и Gradle 9.5
- **Android SDK:** установленная платформа Android 17 (API 37) и Android SDK Build-Tools

## Сборка и запуск

Все команды выполняются из корня проекта. Для Windows используйте `.\gradlew.bat`, для Linux/macOS — `./gradlew`.

### 1. Запуск unit-тестов
```bash
# Windows
.\gradlew.bat testDebugUnitTest

# Linux / macOS
./gradlew testDebugUnitTest
```

### 2. Запуск статического анализа (Android Lint)
```bash
# Windows
.\gradlew.bat lintDebug

# Linux / macOS
./gradlew lintDebug
```

### 3. Сборка Debug APK
```bash
# Windows
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```
Собранный APK будет находиться по пути:
`app/build/outputs/apk/debug/app-debug.apk`

### 4. Установка на подключенное устройство или эмулятор
```bash
# Windows
.\gradlew.bat installDebug

# Linux / macOS
./gradlew installDebug
```

## Правила участия в разработке

Перед началом работы обязательно ознакомьтесь с [AGENTS.md](AGENTS.md).
Основные принципы:
- Архитектура: **MVVM**
- Прямой коммит в ветку `main` запрещён; все изменения вносятся через Pull Request.
- PR требует обязательного одобрения руководителя проекта (@segn1s, Code Owner) и успешного прохождения проверок в GitHub Actions.
- При создании PR заполняйте все разделы шаблона PR.
