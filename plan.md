# Implementation Plan — Period Tracking App

Status: planning document. No code written yet.
Last reviewed: 2026-10-02
Target repo: `C:\Users\patra\Desktop\CompleteProjects\womenApp`

---

## 1. TL;DR

The repo is an **untouched Android Studio "Empty Activity" template**. There is no app code to
modify — this is a greenfield build, not a refactor. Everything in `README.md` must be written from
scratch.

Recommended sequencing, in priority order:

1. **Cycle calculation engine + its unit tests** (Phase 2). This is the product. It is pure Kotlin,
   has zero UI dependencies, and is where all real correctness risk lives. Build it before any screen.
2. **Encrypted local persistence** (Phase 3). README explicitly requires DB encryption with a local key.
3. **Calendar + logging** (Phases 4–5). The minimum viable loop: user logs a period, sees it on a calendar.
4. Everything else in the README is layered on top of that loop.

Estimated solo build: **6–8 weeks** to a shippable v1 covering all README features except real
payments and live chat.

---

## 2. Current State — What Actually Exists

Full inventory of non-boilerplate content:

| File | State |
|---|---|
| `README.md` | Product pitch only, 13 lines, no technical content |
| `build.gradle.kts` (root) | Template, 2 plugins |
| `settings.gradle.kts` | Template, `rootProject.name = "My Application"` |
| `gradle/libs.versions.toml` | Template versions only |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 9.6.0 |
| `app/build.gradle.kts` | Template. `minSdk 24`, `compileSdk/targetSdk 37` |
| `AndroidManifest.xml` | No permissions declared at all |
| `MainActivity.kt` | `MyApplicationApp()` + a "Hello Android!" greeting + 3-item `NavigationSuiteScaffold` |
| `ui/theme/{Color,Theme,Type}.kt` | Default purple Material 3 palette, `dynamicColor = true` |
| `res/drawable/ic_{home,favorite,account_box}.xml` | 3 placeholder nav icons |
| `res/xml/backup_rules.xml`, `data_extraction_rules.xml` | **Empty template stubs — no includes/excludes** |
| `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt` | Template tests; the instrumented one hardcodes `assertEquals("com.example.myapplication", ...)` at line 22 |
| `.idea/workspace.xml` | IDE state, ignore |

Toolchain pinned by the template:

| Component | Version |
|---|---|
| AGP | 9.4.1 |
| Gradle | 9.6.0 |
| Kotlin | 2.2.10 |
| Compose BOM | 2025.12.00 |
| compileSdk / targetSdk | 37 |
| minSdk | 24 |

**There is no database, no DI, no navigation framework, no network layer, and no tests worth keeping.**

---

## 3. Decisions Locked In

Confirmed with the user:

| Decision | Choice |
|---|---|
| Chat backend | Firebase — Anonymous Auth + Firestore |
| Premium gating | `EntitlementRepository` interface + local stub + paywall UI (no billing SDK yet) |
| Package name | Rename from `com.example.myapplication` to a real ID |
| Plan style | Phased roadmap with tasks, acceptance criteria, and risks |

### Package / app name

`plan.md` uses `com.<owner>.bloom` throughout as a placeholder. **Task 0 is to pick the real
values** and run one find/replace:

- Package: `com.<yourorg>.bloom`
- App display name: proposed **"Bloom"** (short, warm, fits the "soft language / inclusive" tone)
- Update `settings.gradle.kts` → `rootProject.name`

---

## 4. Target Architecture

Single Gradle module `:app` with strict **internal** package boundaries. Splitting into
`:core`/`:feature` Gradle modules is deliberately deferred — see Risk R7.

```
com.<owner>.bloom/
├── BloomApplication.kt            application class, owns AppContainer
├── MainActivity.kt
│
├── core/
│   ├── time/          CycleDate, CycleClock (injectable — testability), java.time helpers
│   ├── design/        Color, Type, Shape, Spacing, reusable composables, PhaseVisuals
│   └── util/
│
├── domain/            PURE KOTLIN — no Android imports, fully unit-testable on JVM
│   ├── model/         Cycle, PeriodLog, Flow, Symptom, LoggedSymptom, UserSettings
│   ├── cycle/         CycleCalculator, PredictionEngine, FertilityCalculator, PhaseResolver
│   ├── wellness/      PainReliefContent, LiteratureReference, WorkoutAdvisor
│   ├── insight/       PersonalityProfile, InclinationProfile
│   └── repository/    Interfaces only (CycleRepository, ChatRepository,
│                      EntitlementRepository, SettingsRepository, ContentRepository)
│
├── data/
│   ├── crypto/        KeystoreKeyManager, PassphraseProvider, DatabaseFactory
│   ├── local/         Room entities, DAOs, converters, AppDatabase
│   ├── settings/      DataStore Preferences
│   └── remote/        FirebaseAuthDataSource, FirestoreChatDataSource, DTOs
│
├── notifications/     NotificationChannels, CycleReminderWorker, WorkScheduler
│
├── feature/
│   ├── onboarding/    welcome, birth date, current-cycle setup, notification permission
│   ├── home/          today card, phase summary
│   ├── calendar/      month grid, day detail, log/edit period
│   ├── insights/      cycle charts, symptom patterns
│   ├── tips/          pain relief + literature content
│   ├── chat/          global/local, phase-matched
│   ├── premium/       paywall, entitlement gating composables
│   └── settings/      notifications, privacy, export, data reset
│
└── navigation/        NavGraph, AppDestination, type-safe routes
```

**Layering rule:** `feature/` → `domain/` interfaces ← `data/` implementations. No `data/` type
ever appears in a composable. No `feature/` code touches Room or Firebase directly.

### Navigation

The template's `enum class AppDestinations` + `mutableStateOf` is a toy pattern — it cannot survive
process death or deep links. Replace with **Navigation 3** (`androidx.navigation3`, currently
`1.0.0-alpha08` — pin the stable release when it ships, else Navigation 2 typed routes). Either way,
route arguments carry cycle dates as `String` ISO-8601, not `Long` epoch millis, to stay
timezone-correct.

---

## 5. Toolchain & Dependency Plan

### 5.1 Version findings (verified against upstream release notes)

| Library | Current | Notes |
|---|---|---|
| **Room** | **3.0.0 stable** (released 2026-07-01) | New package `androidx.room3`. Artifact IDs changed: `androidx.room3:room3-runtime`. **KSP only**, no KAPT, no Java codegen. **Coroutines-first** — every DAO function must be `suspend` or return `Flow`. Drops `SupportSQLite` APIs unless `room3-sqlite-wrapper` is added. |
| Room (2.x line) | 2.8.5 | Maintenance mode — bug fixes only, no new features. |
| **SQLCipher** | **`net.zetetic:sqlcipher-android:4.18.0`** (2026-08-19) | Supports **both Room 2 and Room 3**. Requires **minSdk 23+** (template's 24 is fine). Must call `System.loadLibrary("sqlcipher")` before any DB access. |
| WorkManager | 2.11.2 stable (2026-08-12) | 2.12.0-rc01 available. |
| Kotlin | 2.2.10 in template | **Kotlin 2.2 is EOL** (2025-12-16); final patch 2.2.21. Current is 2.4.10. |
| Hilt | 2.59.2 | **2.59.2 cannot read Kotlin 2.4 metadata** (its bundled `kotlin-metadata-jvm` maxes at 2.3.0). |

### 5.2 ⚠️ The Kotlin / Hilt version trap

This is the single most likely thing to burn a day:

- Staying on Kotlin 2.2.x → Hilt 2.59.2 works, but you are on an **end-of-life compiler**.
- Moving to Kotlin 2.4.x → Hilt 2.59.2 **fails to compile** with
  `Provided Metadata instance has version 2.4.0, while maximum supported version is 2.3.0`.

**Recommendation: skip Hilt entirely for v1.** Build a hand-rolled `AppContainer`:

```kotlin
class AppContainer(context: Context) {
    val passphraseProvider by lazy { PassphraseProvider(context) }
    val database by lazy { DatabaseFactory(context, passphraseProvider).create() }
    val cycleRepository: CycleRepository by lazy { CycleRepositoryImpl(database.cycleDao()) }
    val entitlementRepository: EntitlementRepository by lazy { LocalEntitlementRepository(context) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(context) }
}
```

Accessed via a single `LocalAppContainer` composition local. For one module with ~8 dependencies
this is ~40 lines, has zero annotation processing, and eliminates the whole version-matrix problem.
**Revisit Hilt only if you split into Gradle modules** (where manual wiring starts to hurt).

### 5.3 Recommended version set

Pin these in `gradle/libs.versions.toml`. Verify each at implementation time — this plan was
written 2026-10-02.

```toml
[versions]
kotlin            = "2.2.21"   # or 2.4.10 if/when Hilt is adopted — see 5.2
ksp               = "<match kotlin>"   # KSP versions track Kotlin: 2.2.x-2.0.z
room              = "3.0.0"
sqlcipher         = "4.18.0"
sqlite            = "2.6.0"     # version Room 3.0.0 aligns with; confirm
work              = "2.11.2"
datastore         = "<current>"
firebaseBom       = "<current>"
navigation3       = "<stable or latest alpha>"
turbine           = "<current>" # test
```

New dependencies to add:

```kotlin
// build config
buildFeatures { compose = true; buildConfig = true }
compileOptions {
    isCoreLibraryDesugaringEnabled = true   // java.time on minSdk 24
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:<current>")
}
```

Library list:

| Purpose | Library |
|---|---|
| Database | `androidx.room3:room3-runtime`, `ksp(room3-compiler)`, `androidx.sqlite:sqlite` |
| Encryption | `net.zetetic:sqlcipher-android` |
| Async / scheduling | `androidx.work:work-runtime-ktx` |
| Settings | `androidx.datastore:datastore-preferences` |
| Navigation | `androidx.navigation3:navigation3-runtime` |
| Chat | Firebase BOM → `firebase-auth`, `firebase-firestore` |
| Firebase config | `com.google.gms.google-services` plugin |
| Tests | `app.cash.turbine`, `androidx.room3:room3-testing`, `kotlinx-coroutines-test` |

**No chart library.** All charts are hand-drawn on Compose `Canvas` (Phase 6). Avoids a dependency
that fights Compose's layout and keeps the chart set exactly tailored to cycle data.

**Note on Firebase + AGP 9 / Gradle 9.6:** the `google-services` plugin historically lags new AGP
major versions. Verify the plugin works with AGP 9.4.1 **in Phase 1, not Phase 10**. If it fails,
either bump the plugin or bypass it with a manual `FirebaseOptions` initialization.

---

## 6. Data Model & Schema

### 6.1 Room entities

```kotlin
@Entity(tableName = "period_events")
data class PeriodEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: CycleDate,          // ISO-8601 LocalDate string — see 6.2
    val endDate: CycleDate?,           // null = ongoing
    val flow: FlowLevel?,              // LIGHT / MEDIUM / HEAVY / SPOTTING
    val isSpottingOnly: Boolean,       // distinguishes spotting from a real period
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Entity(tableName = "symptom_logs")
data class SymptomLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: CycleDate,
    val symptomId: String,             // FK into curated symptom catalog
    val severity: Int,                 // 1..5
    val cycleDay: Int,                 // denormalised for charting
)

@Entity(tableName = "content_items")  // seeded on first run from res/raw
data class ContentItemEntity(
    @PrimaryKey val id: String,
    val category: ContentCategory,     // PAIN_RELIEF / CYCLE_FACTS / NUTRITION
    val phaseTag: PhaseType?,          // null = always relevant
    val title: String,
    val body: String,
    val sourceTitle: String?,          // literature citations (README line 6)
    val sourceUrl: String?,
    val isPremium: Boolean
)
```

Supporting:
- `SymptomCatalog` — static Kotlin object (~40 symptoms), not a DB table.
- `UserSettingsEntity` — cycle start date, average cycle length override, notification prefs,
  last-used reminder time, chat display name.
- Firestore `chats/{roomId}/messages/{messageId}` — **remote, not Room**. Chat history is cached
  in-memory only; it must never land in the encrypted DB, since it is the one part of the app that
  leaves the device unencrypted.

### 6.2 ⚠️ Do not store dates as `Long` epoch millis

Storing instants for calendar dates creates off-by-one-day bugs across timezones. Store
`LocalDate` as ISO-8601 `String` (`"2026-10-02"`) via a `TypeConverter`. Store true instants
(`createdAt`) as `Long` epoch millis — those are genuinely instants. This distinction must be
respected everywhere, including Room queries and notification scheduling.

### 6.3 Migrations

Enable schema export from day one — retrofitting later is painful:

```kotlin
// app/build.gradle.kts
room { schemaDirectory("$projectDir/schemas") }
// commit app/schemas to version control
```

---

## 7. Encryption Design (README line 3)

README: *"stored in the DB but encrypted with a local key."*

### 7.1 Key hierarchy

```
Android Keystore (TEE-backed, non-exportable)
  └─ AES-256-GCM key "db_wrap_key"
       └─ encrypts/decrypts a random 32-byte SQLCipher passphrase
            └─ passphrase stored (wrapped) in EncryptedSharedPreferences
                 └─ passed to SQLCipherDriver to open the Room DB
```

- Generate the AES key in Keystore with `KeyGenParameterSpec`, `PURPOSE_ENCRYPT | PURPOSE_DECRYPT`,
  `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`.
- **Do not** set `setUserAuthenticationRequired(true)` — that would make the DB unreadable when the
  screen is locked, and background notification workers would break.
- Generate the 32-byte SQLCipher passphrase from `SecureRandom` on first launch; never derive it from
  user input (a forgotten passphrase = total data loss, with no recovery).
- Zero the passphrase `ByteArray` after the driver opens the DB.

### 7.2 Wiring SQLCipher to Room 3

`net.zetetic:sqlcipher-android` supports Room 3 via a **driver**, not the Room 2 factory:

```kotlin
System.loadLibrary("sqlcipher")           // must run before any DB access

val passphrase = passphraseProvider.get()   // ByteArray from Keystore
val file = context.getDatabasePath("bloom.db")
val driver = SQLCipherDriver(passphrase, null, null)

Room.databaseBuilder(context, AppDatabase::class.java, file.absolutePath)
    .setDriver(driver)
    .build()
```

The `System.loadLibrary` call must not race DB creation. Since `AppContainer` uses `by lazy`, the
load happens on first DB touch inside a single-threaded init — correct by construction. Do **not**
call `loadLibrary` from `Application.onCreate` *and* separately from the container; pick one.

*(Room 2 alternative, if the driver proves troublesome: `SupportOpenHelperFactory(passphrase)` +
`.openHelperFactory(factory)`. This pins you to Room 2.8.x. Try Room 3 + driver first.)*

### 7.3 ⚠️ Backup rules — data-loss trap

`backup_rules.xml` and `data_extraction_rules.xml` are **currently empty stubs**. With encrypted data,
Android Auto Backup will happily copy `bloom.db` to Google Drive and restore it onto a device where
the Keystore key does not exist — producing an unopenable database and total data loss.

Required, before shipping anything:

```xml
<!-- data_extraction_rules.xml -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="database" />
        <exclude domain="sharedpref" />
        <exclude domain="file" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="database" />
        <exclude domain="sharedpref" />
        <exclude domain="file" />
    </device-transfer>
</data-extraction-rules>
```

Same exclusions in `backup_rules.xml` for pre-API-31 devices. Alternatively set
`android:allowBackup="false"` — simpler and more honest for an encrypted health app. Pick one;
do not leave the stubs empty.

### 7.4 What is *not* encrypted

Firebase chat messages leave the device in plaintext to the server. This must be designed around:

- Send **only** a coarse `phaseBucket` (e.g. `"luteal-day-24"`) and a `cycleLengthBand`
  (`"28-30"`) — never exact dates, never a birth date, never a real name.
- Chat display names are user-chosen pseudonyms, generated locally.
- Firestore rules must enforce that a client can only write its own message document.

---

## 8. Cycle Engine Specification (Phase 2 — the core)

Pure Kotlin in `domain/cycle`. No Android imports. This is where the product's value lives.

### 8.1 Terminology

- **Cycle** = first day of a period → the day before the next period's first day.
- **Cycle length** = days between consecutive period starts.
- **Period duration** = days of actual bleeding (literature: median ~5, typical range 3–7;
  anything under 2 or over 8 is worth a gentle "is this typical for you?" prompt, not an alarm).

### 8.2 Phase model

| Phase | Bounds |
|---|---|
| Menstrual | Cycle day 1 → last day of bleeding |
| Follicular | Day after menstruation ends → ovulation |
| Ovulation | Computed day (see below), ±1 |
| Luteal | Day after ovulation → last cycle day |

### 8.3 Calculator responsibilities

```
CycleCalculator
  ├─ averageCycleLength(cycles)       : weighted mean of last N (default 6), outlier-rejected
  ├─ lutealPhaseLength(cycles)        : mean; falls back to 14 when fewer than 3 cycles
  ├─ ovulationDay(cycle)              : nextPeriodStart − lutealPhaseLength
  ├─ fertileWindow(cycle)              : [ovulation − 5, ovulation + 1]
  │      (sperm survive up to ~5 days; ovum ~24h)
  ├─ nextPeriodPrediction(cycles)     : start date + mean length, ± stddev range
  ├─ predictionConfidence(cycles)     : LOW / MEDIUM / HIGH from cycle count + stddev
  ├─ currentPhase(date, cycles)       : PhaseType + cycleDay + dayOfPhase
  └─ regularityAssessment(cycles)     : "consistent" / "varies a little" / "irregular"
```

### 8.4 Explicit edge cases to handle and test

- **0 logged cycles** → use documented defaults (28-day cycle, 5-day period, 14-day luteal).
- **1 logged cycle** → predictions shown, but confidence pinned to LOW with honest copy
  ("we're still learning your rhythm").
- **Spotting logged as a period** → excluded from cycle-length statistics.
- **Very long / very short cycles** (>35 or <21 days) → still computed, but suppress
  "you're pregnant?" prompts (out of scope) and soften confidence language.
- **Overlapping or duplicate period events** → merge on write.
- **Date gaps** (user skipped 3 months) → do not fabricate cycles; exclude gaps from the mean.
- **Hormonal contraception / perimenopause / postpartum flags** in settings → suppress or
  heavily caveat predictions. These are the main sources of genuinely misleading output.

### 8.5 Prediction approach (v1)

Keep it honest and simple: **weighted mean of recent cycle lengths + standard deviation**, with
luteal-phase-derived ovulation. Do not build ML. A 28-day average ± 3 days is genuinely useful;
a black box claiming 99% accuracy is not, and creates a Google Play health-claims problem (§11, R1).

Confidence rubric:
- `< 3` cycles → LOW
- `3–5` cycles AND stddev ≤ 3 days → MEDIUM
- `≥ 6` cycles AND stddev ≤ 3 days → HIGH
- stddev > 7 days → downgrade one level regardless of count.

---

## 9. README Feature → Implementation Map

| # | README requirement | Phase | Premium? |
|---|---|---|---|
| 1 | Tracks periods | 5 | No |
| 2 | Charts explaining | 7 | Partly |
| 3 | Global/local chat, phase-matched women | 11 | No |
| 4 | Stored in DB, encrypted with local key | 4 | No |
| 5 | Calendar based | 6 | No |
| 6 | Tips for cramps/pain | 8 | No |
| 7 | What literature says about period duration | 8 | No |
| 8 | **Premium:** Analysis | 9 | Yes |
| 9 | **Premium:** Best fertility window | 10 | Yes |
| 10 | **Premium:** Year to conceive — astrology / sports / academic inclinations | 10 | Yes |
| 11 | "What their cycle says about them" | 10 | Yes |
| 12 | Long-term prediction + early warning signs | 8 | No (long-term = Yes) |
| 13 | Notifications during / before, time-of-month aware | 8 | No |
| 14 | **Premium:** Gym / run intensity by cycle phase | 9 | Yes |
| 15 | Soft inclusive language, smooth adaptive UI | 2, 12 | No |
| 16 | 1-slide chat entry point | 11 | No |

---

## 10. Phased Roadmap

Each phase lists concrete files and an acceptance test. Do not start a phase until the previous
phase's acceptance test passes.

---

### Phase 0 — Foundation & Rename
**Effort:** ~0.5 day

- [ ] Choose real package ID and app display name.
- [ ] Move `com/example/myapplication/**` → new package path; update `namespace` and `applicationId`
      in `app/build.gradle.kts`.
- [ ] Update `settings.gradle.kts` → `rootProject.name`.
- [ ] Update `app/src/main/res/values/strings.xml` → `app_name`.
- [ ] Fix `ExampleInstrumentedTest.kt:22` — it hardcodes the old package name and **will fail**.
- [ ] Delete `ExampleUnitTest.kt` / `ExampleInstrumentedTest.kt`; replace with real tests later.
- [ ] Delete template `ic_home`/`ic_favorite`/`ic_account_box` drawables when real nav ships.
- [ ] Replace default purple `Color.kt` with a real palette (soft rose/plum/sage, both light and
      dark). Decide whether `dynamicColor` stays on — see R6.
- [ ] Add `.gitignore` entries for `google-services.json`, `*.jks`, `keystore.properties`.
- [ ] Confirm `.idea/` is ignored.
- [ ] **Verify the Firebase `google-services` plugin works with AGP 9.4.1 now**, not in Phase 11.

**Acceptance:** `./gradlew assembleDebug` succeeds; app launches; package name correct; app name
shows in the launcher.

---

### Phase 1 — Project Infrastructure
**Effort:** ~1 day

- [ ] Add all dependencies from §5.3 to `libs.versions.toml`.
- [ ] Enable core library desugaring; confirm `java.time.LocalDate` works on minSdk 24.
- [ ] Apply KSP; add Room Gradle plugin; enable schema export.
- [ ] Create `BloomApplication` + `AppContainer` + `LocalAppContainer` composition local.
- [ ] Create `core/time/`:
  - `typealias CycleDate = LocalDate`
  - `interface CycleClock { fun today(): CycleDate; fun now(): Instant }` — injectable so tests
    can pin "today" instead of depending on the wall clock.
- [ ] Enable `buildConfig`; put Firebase/API config in `BuildConfig`, not hardcoded.
- [ ] Add `Turbine` + `kotlinx-coroutines-test`.

**Acceptance:** App builds with all deps resolved; a unit test can inject a fixed `CycleClock` and
assert on a date 40 days later.

---

### Phase 2 — Design System & App Shell
**Effort:** ~1.5 days

- [ ] `core/design/`: `Color`, `Type`, `Shape`, `Spacing`, `PhaseVisuals`.
- [ ] `PhaseVisuals` — one place mapping each of the 4 phases to color/emoji/iconography, used by
      calendar, home card, charts, and chat. Prevents the app from drifting into five phase colors.
- [ ] Reusable components: `SoftCard`, `PhaseChip`, `PrimaryButton`, `SectionHeader`, `EmptyState`.
- [ ] `navigation/AppDestination.kt` + real nav (Navigation 3 or typed Navigation 2).
- [ ] Replace the template's `NavigationSuiteScaffold` enum hack; destinations: Home, Calendar,
      Insights, Chat, Settings.
- [ ] Tablet/foldable adaptation via `WindowSizeClass` (the template already ships
      `material3-adaptive-navigation-suite`).
- [ ] Accessibility pass: minimum 48dp touch targets, content descriptions on all custom Canvas
      drawings, `semantics` for phase state, no colour-only signalling.

**Acceptance:** All 5 destinations navigate; rotates without losing state; renders sensibly at
phone, foldable, and tablet widths; TalkBack reads every control.

---

### Phase 3 — Domain & Cycle Engine ⭐
**Effort:** ~3–5 days · **Highest priority — do not defer**

- [ ] `domain/model/` — `Cycle`, `PeriodLog`, `FlowLevel`, `Symptom`, `PhaseType`, `UserSettings`.
- [ ] `domain/cycle/CycleCalculator.kt` — everything in §8.3.
- [ ] `domain/cycle/PredictionEngine.kt` — next period prediction + range + confidence (§8.5).
- [ ] `domain/cycle/FertilityCalculator.kt` — fertile window.
- [ ] `domain/cycle/PhaseResolver.kt` — current phase for any date.
- [ ] Unit tests covering **every** edge case in §8.4. Target 90%+ on this package.

**Acceptance:** Test suite demonstrates: defaults with 0 cycles; LOW confidence with 1 cycle;
28-day cycles predicted correctly; stddev widening the prediction range; spotting excluded from
averages; phase resolution correct on day 1, last bleeding day, ovulation day, and final cycle day.

---

### Phase 4 — Encrypted Persistence
**Effort:** ~3–4 days

- [ ] `data/crypto/PassphraseProvider.kt` — Keystore AES-GCM wrap/unwrap, `SecureRandom` generation.
- [ ] `data/crypto/DatabaseFactory.kt` — `loadLibrary`, `SQLCipherDriver`, `setDriver`.
- [ ] Room entities, DAOs, `TypeConverter`s (`CycleDate` ↔ ISO string), `AppDatabase`.
- [ ] Seed `ContentItemEntity` from `res/raw/content_seed.json` on first run (marker in settings).
- [ ] Repository interfaces in `domain/repository/`, impls in `data/local/`.
- [ ] `SettingsRepository` over DataStore (not Room — preferences shouldn't require DB unlock).
- [ ] **Fix `backup_rules.xml` and `data_extraction_rules.xml`** (§7.3).
- [ ] Migration test using `MigrationTestHelper`.
- [ ] Instrumentation test: create DB, insert, close, **reopen in a new process**, assert data survives.

**Acceptance:** `adb shell run-as <pkg> cat databases/bloom.db | xxd | head` shows no readable
period data. Reopen after process death returns correct data. Uninstall/reinstall does not crash on
a stale encrypted DB.

---

### Phase 5 — Logging Flow
**Effort:** ~2 days

- [ ] Onboarding: welcome, birth date, current cycle start, "typical cycle length" with soft
      defaults, notification permission request.
- [ ] `feature/home/` — today's phase card, cycle day, days to next predicted period.
- [ ] Period logging: start / end / flow level / edit / delete, with validation and undo.
- [ ] Symptom logging: multi-select from catalog + severity.
- [ ] Optimistic UI updates; Snackbar undo for destructive actions.

**Acceptance:** A user can log a period from cold start in under 10 seconds; the home card updates
immediately; all logged data survives process death.

---

### Phase 6 — Calendar
**Effort:** ~3–4 days

- [ ] Month grid: period days, predicted days, fertile window, today — **visually distinguishable
      without relying on colour alone** (pattern/dot density + labels).
- [ ] Swipe month-to-month, tap day → day detail sheet.
- [ ] Log directly from a calendar day.
- [ ] "Why am I seeing this?" legend — important, since predictions and actuals look similar.
- [ ] Logged-outside-expected-range indicator (gentle, non-alarming).
- [ ] Year overview strip.

**Acceptance:** A full year renders correctly including month boundaries and leap years; every day
is tappable; predictions are visually distinct from logged days; works at all supported font scales.

---

### Phase 7 — Insights & Charts
**Effort:** ~2–3 days

- [ ] Hand-drawn Compose `Canvas` charts — **no third-party chart library**:
  - cycle length history (line, with predicted band)
  - period duration history (bar)
  - phase timeline / wheel
  - symptom frequency by cycle day (heatmap) ← answers README line 2 directly
  - consistency score
- [ ] Each chart gets a text/summary alternative for accessibility.
- [ ] `InsightsAnalysis` domain service produces the plain-language interpretation.
- [ ] Gate advanced analytics behind Premium (README line 7); free tier keeps at least one chart.

**Acceptance:** All charts render with 0, 1, and 24 cycles without crashing or dividing by zero;
every chart has an accessible non-visual equivalent.

---

### Phase 8 — Content, Predictions & Notifications
**Effort:** ~3–4 days

- [ ] Curate content for README lines 6 & 13 — **this is research, not coding**:
      - typical period duration with real citations
      - cramp/pain relief guidance (heat, movement, hydration, OTC medication notes, when to see
        a doctor)
      - phase-specific guidance
- [ ] Store citations (title + URL) and render them; a health app must not make unsourced claims.
- [ ] `notifications/NotificationChannels.kt` — separate channels so users can mute tips but keep
      period reminders.
- [ ] `CycleReminderWorker` + `WorkScheduler`:
  - reschedule on every log, settings change, boot, and timezone change
  - use `OneTimeWorkRequest.setInitialDelay` — **do not use `AlarmManager.setExactAndAllowWhileIdle`**,
      which requires `SCHEDULE_EXACT_ALARM` and is heavily restricted on Android 12+
  - exact-alarm permission is unnecessary for day-scale reminders
- [ ] Runtime `POST_NOTIFICATIONS` permission on Android 13+, requested with context, never blocking.
- [ ] Early-signs nudges during predicted PMS/luteal window.
- [ ] Long-term (6-month) projection → **Premium**.

**Acceptance:** A reminder fires within a minute of its scheduled time in a manual test; disabling
notifications actually silences it; changing the device timezone reschedules correctly.

---

### Phase 9 — Entitlements & Paywall
**Effort:** ~2 days

- [ ] `domain/repository/EntitlementRepository.kt`:
  ```kotlin
  enum class PremiumFeature { ANALYSIS, FERTILITY_WINDOW, PERSONALITY_READING, WORKOUT_PLAN, LONG_HORIZON }
  interface EntitlementRepository {
      val state: Flow<EntitlementState>
      fun isUnlocked(feature: PremiumFeature): Boolean
      suspend fun unlock(feature: PremiumFeature)   // stub
  }
  ```
- [ ] `LocalEntitlementRepository` — DataStore-backed, with a **debug-only** unlock toggle in a
      developer settings screen.
- [ ] `composable PremiumGate(feature) { ... }` — renders a tasteful teaser card + paywall CTA.
      Never a hard wall mid-task; always show the user something real first.
- [ ] `feature/premium/PaywallScreen` — soft, non-pressuring copy. No dark patterns, no fake
      countdowns, no fake "47 people viewing". Play Store policy risk (R2).
- [ ] Add the billing seam: a `BillingEntitlementRepository` stub with the same interface, and a
      TODO for Play Billing or RevenueCat.

**Acceptance:** Every Premium feature is reachable for testing via the debug toggle; the paywall
never blocks a free-tier task; no feature is silently unavailable.

---

### Phase 10 — Premium Analyses
**Effort:** ~3–4 days

- [ ] **Fertility window** — from `FertilityCalculator`, presented as a range with an explicit
      uncertainty statement. Must state that it is an estimate, not contraception advice, and not a
      pregnancy test. Add a "not a medical device" note.
- [ ] **Workout advisor** — per-phase intensity guidance (e.g. higher-intensity work earlier in
      the cycle, strength/yoga focus in the luteal phase). Frame as *gentle suggestions* with an
      explicit "listen to your body" note; never instruct anyone to train through pain.
- [ ] **Astrology / inclination profiles** (README lines 10 & 11) — deterministic, computed
      **entirely on-device** from birth date:
      - natal moon sign, biorhythm phase
      - "inclination" mapping (study focus, athletic focus) — kept clearly separated from the
        astrological output
- [ ] ⚠️ **Hard requirement:** every astrology screen carries a persistent, non-dismissible
      "for reflection and entertainment only — not medical guidance" label. See R1.
- [ ] Keep these behind `PremiumGate`.

**Acceptance:** All premium screens render with the debug unlock; astrology content is
unambiguously labelled; nothing in this phase sends birth data to any server.

---

### Phase 11 — Firebase Chat
**Effort:** ~5–7 days

- [ ] Firebase project setup; `google-services.json` gitignored; per-build-type config.
- [ ] Anonymous Auth — sign in silently on first launch; no email, no phone, no PII.
- [ ] `ChatRepository` interface in `domain/` + Firestore implementation in `data/remote/`.
- [ ] Phase bucketing (§7.4): `phaseBucket` + `cycleLengthBand` only. **Never exact dates or birth data.**
- [ ] Firestore schema:
  ```
  chats/{roomId}/messages/{messageId}
     text, pseudonymousName, phaseBucket, cycleLengthBand, createdAt
  ```
- [ ] Firestore security rules — a client may create only its own message, and only with a
  `phaseBucket` it is allowed to claim; no listing of other users' identifiers.
- [ ] Composite index on `(phaseBucket, createdAt)`.
- [ ] **App Check (Play Integrity)** — essential for a health app with user-generated content.
  Without it, the Firestore bill is trivially abusable.
- [ ] Chat UI: 1 entry point on Home (README line 2), global room + smaller phase rooms, soft
      content reporting + a "block" affordance.
- [ ] Rate limiting, message length caps, profanity/basic-abuse filter, and a blocklist check.
- [ ] `ChatModerationPolicy` and an in-app report flow. **Assume someone will post something
      harmful — there must be a report path before launch.**
- [ ] Graceful offline state: queue messages, retry, clear "can't reach the circle" messaging.
- [ ] Premium: respectful gating only (e.g. unlimited history). **Never gate the ability to post.**

**Acceptance:** Two devices on the same `phaseBucket` see each other's messages; security rules
block a client from writing another user's message; with airplane mode on, the app degrades
gracefully and never crashes; a reported message is hidden from other users.

---

### Phase 12 — Polish, Testing & Release
**Effort:** ~4–6 days

- [ ] Full unit + instrumentation test pass; CycleCalculator coverage ≥ 90%.
- [ ] Accessibility audit (TalkBack, large font, contrast, RTL — `supportsRtl` is already `true`).
- [ ] Tablet/foldable layouts.
- [ ] Copy review pass for the "soft, inclusive" tone (README line 15) — every string, including
      error states. Avoid clinical language; avoid shame about irregularity.
- [ ] Onboarding content explaining what is stored on-device vs. sent to the server.
- [ ] Data export (user-requested JSON/CSV) and "delete all my data".
- [ ] Play Store readiness:
  - **Health data declarations** in the Data safety form
  - Privacy policy — required, and must describe the chat component
  - Content rating
  - Target-audience declarations
  - Medical disclaimer in the listing description
- [ ] Release signing config, ProGuard/R8 rules (SQLCipher ships consumer rules as of 4.14.0).
- [ ] Play Console internal testing track.

**Acceptance:** An internal-test install on a physical device completes onboarding → log → predict →
notify → chat without a single crash.

---

## 11. Risks

| ID | Risk | Severity | Mitigation |
|---|---|---|---|
| **R1** | **Health-claims / medical-claims policy violation.** README lines 8 & 10 ("bullshit eastern philosophy") and line 9 (fertility windows) push straight at Google Play's health-claims rules. Pregnancy/fertility claims require specific declarations; misleading claims get the app removed. | High | Persistent "not a medical device" disclaimers on all prediction screens. Never claim ovulation-prediction accuracy percentages. Astrology content explicitly framed as entertainment. Verify Play Console health-data declarations before listing. |
| **R2** | **Chat safety & moderation.** Anonymous + health-adjacent + global = real abuse risk. | High | App Check, rate limits, report + block flows, moderation rules, and a plan for a human review path. Do not ship chat without the report flow. |
| **R3** | **Keystore key loss = total data loss.** Screen-lock change, factory reset, or a restore to a new device destroys the key. | High | Exclude DB from backups (§7.3). Warn users in onboarding that data is device-local. Never derive the passphrase from user input — you cannot recover a lost passphrase. Consider an optional user-set PIN as a *second* factor, not the sole key. |
| **R4** | **Toolchain version churn.** Kotlin 2.2 is EOL; Hilt cannot read Kotlin 2.4 metadata; Room 3 changed packages and artifact IDs; Firebase plugin may lag AGP 9. | High | Skip Hilt (§5.2). Verify Firebase + AGP 9 in Phase 0. Pin all versions in `libs.versions.toml`. Upgrade Kotlin deliberately, not opportunistically. |
| **R5** | **Timezone / date correctness.** Day-boundary bugs are the classic period-tracker defect, and they silently corrupt every prediction. | High | ISO `String` dates, not epoch millis (§6.2). Injectable `CycleClock`. Tests that cross month/year/leap boundaries. |
| **R6** | **Dynamic color fights the brand.** `dynamicColor = true` is currently on; Material You extraction will produce arbitrary colors and can wreck phase-specific color coding, which carries real meaning here. | Medium | Default `dynamicColor = false`. Offer it as an opt-in accessibility setting. |
| **R7** | **Single-module growth.** ~9 feature packages + domain + data can get unwieldy. | Medium | Deliberate for v1 — Gradle module splits cost real time with one developer. Enforce boundaries with package conventions and ArchUnit-style tests. Split only when build times actually hurt. |
| **R8** | **Chat + encryption mismatch.** Encrypted local DB + unencrypted server chat is a contradiction users may not understand. | Medium | Onboarding must state plainly what is on-device vs. on-server. Never store chat in the encrypted DB. |
| **R9** | **Prediction overreach.** Confident predictions on irregular cycles are medically misleading. | Medium | Confidence rubric (§8.5), suppression flags for contraception/perimenopause/postpartum, hedged language everywhere. |
| **R10** | **Scope.** This README is a 6–8 week build. Premature monetization, chat, and astrology all compete for the same time. | Medium | Phases are ordered so a useful app exists after Phase 6. Everything after is additive and independently shippable. |

---

## 12. Open Questions — Need Your Input

| # | Question | Blocks | Default if unanswered |
|---|---|---|---|
| Q1 | Real package ID + app display name? | Phase 0 | `com.<owner>.bloom` / "Bloom" |
| Q2 | Where does the content research (README lines 6, 13) come from — will you source it, or should I compile a starting set from public health bodies (ACOG/NHS/etc.)? | Phase 8 | Compile a cited starter set |
| Q3 | Is the chat global-only, or do you want private/local rooms between people who know each other? Changes the Firestore schema. | Phase 11 | Phase-matched global rooms only |
| Q4 | Reporting/moderation: is there anyone who can actually review reported messages, or does it need to be automated-only for v1? | Phase 11 | Automated-only, with reports stored for later review |
| Q5 | Is "Premium" ever going to be subscription, one-time unlock, or both? | Phase 9 | One-time unlock per feature |
| Q6 | Do you need Health Connect integration (syncing to Google Fit / Apple Health) at any point? Significant additional work and policy surface. | Post-v1 | Out of scope |
| Q7 | Tablet/foldable support required for v1, or phone-only? | Phase 2 | Phone-first, layouts that don't break on tablet |
| Q8 | Localization — is this English-only at launch? | Phase 12 | English-only, strings already externalized |

---

## 13. Explicitly Out of Scope for v1

State these now so they do not creep in:

- Real payment processing (billing SDK, subscriptions, Play Console purchase config)
- Push notifications for chat (Firestore real-time listeners cover v1)
- Health Connect / Apple Health integration
- Wear OS / widgets / home-screen widgets (Calendar provider integration is a natural v1.1 win)
- Cloud sync of cycle data across devices (fundamentally conflicts with local-key encryption — if
  added later, it needs a user-held key and E2E encryption, a separate project)
- iOS version
- Sex-selection / gender prediction (ethically fraught, and a policy risk)
- Medication reminders with dose tracking
- Community expert moderation tooling

---

## 14. First Actions

1. Answer Q1 (package + app name) — everything else is blocked on it.
2. Phase 0: rename, clean template, verify build.
3. Phase 1: dependencies, `AppContainer`, injectable `CycleClock`.
4. **Phase 3: the cycle engine and its tests.** This is the highest-leverage work in the entire
   project and it is pure Kotlin — it can be written, tested, and trusted before any UI exists.
   Getting it right is what makes the rest of the app worth building.
