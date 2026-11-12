# LIFE RPG — Gamified Habit and Productivity Tracker for Android

LIFE RPG is a native Android app (Java + XML) that turns everyday goals into a role-playing game. Users create quests, run focus sessions and log gym workouts to earn XP, level up, raise five character attributes, keep streaks and unlock achievements. It works offline, with an optional OpenStreetMap-powered Gym Finder.


---

## Problem statement

Habit and productivity apps are easy to install and easy to abandon: completing a task gives little sense of progress. Game mechanics such as experience points, levels and streaks can make progress visible and motivating, but they need fair rules (no free XP, no double rewards) to stay meaningful.

## Objectives

- Model real-life activities (tasks, focused study, workouts) as RPG rewards with clear, deterministic rules.
- Make rewards tamper-resistant: every reward is granted once, inside a single database transaction.
- Work fully offline for core features, with network use limited to the optional Gym Finder.
- Keep the game logic in plain Java so it can be unit-tested without a device.

## Features

- **Onboarding and profile:** hero name and avatar, editable later in Settings.
- **Dashboard:** hero card (avatar, level, title, XP bar), streak and daily progress ring, today's quests, five attributes, recent achievements and shortcuts.
- **Quests:** create, edit, delete and complete quests across 6 categories (Study, Fitness, Personal Growth, Projects, Daily Life, Custom) and 4 difficulties (Easy, Medium, Hard, Epic), with estimated duration, suggested due date, late-completion penalty, daily-repeat quests, filters and per-quest history.
- **Progression:** XP, levels (level *n* requires `100(n−1) + 25(n−1)(n−2)` total XP), five attributes and streaks.
- **Focus mode:** 15/25/45/60-minute timer with start, pause, resume and reset. It is based on wall-clock time and persisted, so it survives backgrounding, rotation and process death.
- **Workouts:** choose a gym and workout type (Strength, Cardio, General), start and finish a session, and earn XP once per session.
- **Gym Finder:** search gyms near you, by area or by name using OpenStreetMap (Overpass API + Nominatim), with distance, directions, details and saved favourites. No API key needed; an optional Google map appears if a Maps key is configured.
- **Statistics:** totals, 7-day XP bar chart (custom `View`), category breakdown and history.
- **Achievements:** First Quest, First Focus Session, 10 Quests, 5-Day Streak, Level 5, First Workout.
- **Avatar studio:** 10 customization categories; avatars are drawn entirely in code from vector paths (no image assets).
- **Themes:** light, dark and system, with a custom nature-inspired palette and bundled fonts.

### Reward rules (as implemented)

| Source | XP | Attributes |
|---|---|---|
| Quest | Base XP (Easy 20, Medium 40, Hard 70, Epic 120) × duration multiplier (×0.75 to ×2.5), reduced if late | Category's primary attribute, plus half to a secondary |
| Focus | 15 min → 20, 25 → 35, 45 → 70, 60 → 100 | Intelligence (+ Discipline) |
| Workout | `(20 + min(minutes, 120)) × {Strength 1.2, Cardio 1.1, General 1.0}`, minimum 5 min | Strength / Vitality |

Late penalty: on time 100%; up to 24 h late 80%; 24–48 h 60%; 48–72 h 40%; over 72 h 25%.

## Technologies used

| Area | Technology |
|---|---|
| Language / UI | Java 8 (source level), Android XML layouts, ViewBinding, Material Components 1.11 |
| Architecture | Activities + Fragments, repository layer, pure-Java logic layer |
| Persistence | Room 2.6.1 (SQLite) with an explicit schema migration (v1 → v2); SharedPreferences |
| Location and maps | Google Play Services Fused Location; optional Google Maps SDK |
| Network data | OpenStreetMap Overpass API and Nominatim over `HttpURLConnection` |
| Build | Gradle 8.5, Android Gradle Plugin 8.2.2, minSdk 26, targetSdk 34 |
| Testing | JUnit 4 (48 unit tests for the game logic) |

## System architecture

```
ui/            Activities, Fragments, adapters, custom Views (chart, avatar)
   │
repository/    QuestRepository, ProfileRepository, SessionRepository, StatsRepository, GymRepository
   │           RewardEngine ── single Room transaction: log + XP + attributes + streak + achievements
   │
logic/         LevelSystem, XpCalculator, QuestXp, StreakCalculator, AchievementEngine, FocusTimerLogic
   │           (plain Java, no Android dependencies → unit-tested on the JVM)
data/          Room entities, DAOs, AppDatabase (schema v2)
service/       OsmGymService (Overpass + Nominatim), LocationHelper (Fused Location), MapKey
util/          Async executors, preferences, focus/workout session stores
```

**Reliability design**
- **One reward path:** `RewardEngine` writes the activity log, XP, attributes, streak and achievements in one Room transaction.
- **Duplicate protection:** `activity_log.refKey` has a UNIQUE index (e.g. `Q:<id>`, `Q:<id>:<date>` for daily quests, `F:<session>`, `W:<session>`), so the database itself rejects a second claim.
- **No destructive migrations:** `MIGRATION_1_2` adds a column instead of wiping user progress.
- **Polite network client:** identifying User-Agent, rate limiting, timeouts, mirror fallback, response-size cap, 5-minute cache and retryable error states.

## Project structure

```
life-rpg/
├── app/
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/liferpg/
│       │   │   ├── LifeRpgApp.java
│       │   │   ├── data/  logic/  model/  repository/  service/  ui/  util/
│       │   └── res/   layout/ drawable/ values/ values-night/ font/ menu/ raw/ ...
│       └── test/java/com/example/liferpg/   JUnit tests
├── gradle/wrapper/
├── licenses/FONTS-OFL.txt
├── local.properties.example
├── build.gradle, settings.gradle, gradle.properties
└── gradlew, gradlew.bat
```

## Installation

**Requirements:** Android Studio Hedgehog (2023.1.1) or newer (bundles JDK 17), and an emulator or phone running Android 8.0 (API 26) or later.

```bash
git clone https://github.com/vids-6/life-rpg.git
```

1. In Android Studio choose **File → Open** and select the project folder.
2. Let Gradle sync (downloads Gradle 8.5 and dependencies on first run).
3. *(Optional)* To show the Google map in Gym Finder, copy `local.properties.example` lines into `local.properties` and set `MAPS_API_KEY`. Restrict the key in Google Cloud to the Maps SDK for Android and this app's package + SHA-1. `local.properties` is git-ignored. Without a key, the map card is hidden and everything else works.

## How to run

- **Android Studio:** select a device and press **Run**.
- **Command line** (needs `sdk.dir` in `local.properties` and JDK 17):

```bash
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # run the unit tests
```

The game-logic tests (`model/` and `logic/`) have no Android dependencies, so they can also be run with plain `javac` + JUnit 4.


## Current limitations

- The focus timer has no foreground service or notification; if it ends while the app is closed, XP is credited the next time the Focus tab is opened.
- Workout duration is wall-clock time; there is no check that the user was really at the gym.
- Gym Finder relies on free public OpenStreetMap servers, which have rate limits and variable availability.
- Data is stored only on the device; there is no account, backup or sync.
- The package name is still `com.example.liferpg`, which must be changed before any Play Store release.

## Future improvements

- Foreground service and notification for the focus timer.
- Cloud backup/sync and optional accounts.
- Instrumented UI tests and Room migration tests.
- Reminders for due and daily quests.
- Social features such as shared challenges or parties.

## Acknowledgements

**Third-party resources**
- Fonts: Poppins and Marcellus, SIL Open Font License 1.1 (see `licenses/FONTS-OFL.txt`).
- Map and gym data © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright), ODbL; accessed through the Overpass API and Nominatim (shown in the app's gym details dialog).

