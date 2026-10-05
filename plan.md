# Implementation Plan — Period Tracking App ("Bloom")

Status: **in progress.** Phases 0–8 merged to `main` (Phases 5–8 delivered logging, calendar,
insights and the notification policy; **104 unit + 4 instrumentation tests** green).
Phases 9–12 not started.
Last reviewed: 2026-10-05
Target repo: `C:\Users\patra\Desktop\CompleteProjects\womenApp`

**Working agreement (user directive):** one branch per phase → commit with a **4–15 word** message →
push → open PR with `gh` → merge into `main` → delete the branch. Never push directly to `main`.

**Verified green:**
- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` → BUILD SUCCESSFUL, **104/104 unit tests**
  (CycleCalculator 31, PhaseResolver 11, CalendarModel 17, ChartSeries 13, HomeSummary 8,
  PeriodEntryRules 7, ReminderDecision 9, InsightsAnalysis 5, ReminderRescheduleReceiver 3).
- `.\gradlew.bat :app:connectedDebugAndroidTest` on emulator `Pixel_9` → **4/4 instrumentation tests**
  (plaintext-canary, fresh reopen, schema create, live-schema-vs-JSON).

---

## 1. TL;DR

This started as an **untouched Android Studio "Empty Activity" template** — a greenfield build, not a
refactor. Everything in `README.md` had to be written from scratch.

Recommended sequencing, in priority order (phase numbers match §10):

1. ✅ **Foundation, infra, design system + app shell** (Phases 0–2). Done and merged.
2. ✅ **Cycle calculation engine + its unit tests** (Phase 3 ⭐). Done and merged — 42 tests, every
   §8.4 edge case covered. This is the product: pure Kotlin, no UI dependencies, and where all real
   correctness risk lived.
3. ✅ **Encrypted local persistence** (Phase 4). Done — SQLCipher + Keystore + backup excludes,
   schema export, and 4 on-device instrumentation tests (including a real `PassphraseProvider` bug
   the reopen test caught).
4. ✅ **Logging flow** (Phase 5). Done — onboarding, Today card, period/symptom sheets, settings,
   reminder scheduling. The minimum viable loop: user logs a period and sees it on the home card.
5. ✅ **Calendar + insights** (Phases 6–7). The rest of the loop: logged periods on a calendar, charts.
6. ✅ **Content, projections & notification policy** (Phase 8). Curated content library, the pure
   reminder decision (early-signal nudges, boot/timezone rescheduling), the six-month projection.
7. Everything else in the README is layered on top of that loop.

Estimated solo build: **6–8 weeks** to a shippable v1 covering all README features except real
payments and live chat.

---

## 2. Current State — What Actually Exists

The template is gone. This is now a real codebase: **54 Kotlin source files (48 main, 4 unit test,
2 instrumentation), 57 passing unit tests + 4 passing on-device tests, a Fraunces/Karla type system,
and one merged PR per completed phase.**

### Done (Phases 0–5)

| Area | Files |
|---|---|
| App class / DI | `BloomApplication.kt`, `AppContainer.kt` (hand-rolled, no Hilt), `ui/AppContainer.kt` (`LocalAppContainer`) |
| Time | `core/time/CycleDate.kt` — `typealias CycleDate = LocalDate`, `CycleClock` interface, `SystemCycleClock` |
| Domain models | `domain/model/Models.kt`, `domain/model/SymptomCatalog.kt` |
| Cycle engine | `domain/cycle/CycleCalculator.kt`, `domain/cycle/PhaseResolver.kt` |
| Domain logic | `domain/home/HomeSummary.kt` (Today card), `domain/validation/PeriodEntryRules.kt` |
| Repositories | `domain/repository/Repositories.kt` + `data/repo/{Cycle,Settings,Entitlement,Chat,Content}RepositoryImpl.kt` |
| Content | `domain/content/ContentLibrary.kt` — ~15 curated, cited items |
| Persistence | `data/local/{Entities,Daos,Converters,AppDatabase,DatabaseFactory}.kt`, `data/crypto/PassphraseProvider.kt` |
| Notifications | `notifications/{BloomNotifications,ReminderWorker,ReminderScheduler,ReminderRescheduleReceiver}.kt` + `res/drawable/ic_stat_bloom.xml` |
| Theme | `ui/theme/{Color,Type,Shape,Motion,Spacing,Theme}.kt` + 7 `res/font/*.ttf` |
| Shell | `ui/BloomApp.kt`, `ui/navigation/BloomDestination.kt`, `ui/phase/PhaseVisuals.kt` |
| Components | `ui/components/{Components,Placeholder,DateField}.kt`, `ui/format/DateText.kt` |
| Screens | `ui/screens/{Home,Settings,Onboarding,PeriodLogSheet,SymptomLogSheet,CalendarScreen,InsightsScreen}.kt` real; `ChatScreen.kt` placeholder (Phase 11) |
| Tests | `src/test/.../{CycleCalculatorTest(31),PhaseResolverTest(11),CalendarModelTest(17),ChartSeriesTest(13),HomeSummaryTest(8),PeriodEntryRulesTest(7),ReminderDecisionTest(9),InsightsAnalysisTest(5),ReminderRescheduleReceiverTest(3)}.kt` = 104 |
| Instrumentation | `src/androidTest/.../{DatabasePersistenceTest,MigrationSchemaTest}.kt` (4) |
| Schema baseline | `app/schemas/com.bloomcycle.app.data.local.AppDatabase/1.json` |

### Removed from the template

`com/example/myapplication/**` sources, the 3 placeholder nav icons, `ExampleUnitTest.kt`,
`ExampleInstrumentedTest.kt` (hardcoded old package at line 22), the purple Material palette,
`dynamicColor = true`.

### Git state

One branch → PR → merge per phase, never a direct push to `main`. Pattern:

| Phase | Branch | PR |
|---|---|---|
| 2 design system | `phase-2-design-system` | #1 ✅ |
| 2 app shell | `phase-2-app-shell` | #2 ✅ |
| 3 cycle engine | `phase-3-cycle-engine` | #3 ✅ |
| 4 persistence | `phase-4-persistence` | #4 ✅ |
| 5 logging flow | `phase-5-logging-flow` | #5 ✅ |

`origin` = `https://github.com/Abhinavpatra/app-test.git`, `gh` v2.69.0 authenticated as
`Abhinavpatra`. Run `git log --oneline -10` for the live list.

### Known gaps (tracked per phase in §10)

- **Chat is still a placeholder** — Home, Settings, Onboarding, both log sheets, Calendar (Phase 6)
  and Insights (Phase 7) are real; Chat lands in Phase 11.
- **Reminder *policy* is complete, on-device firing is not** — settings changes, every log action and
  `BOOT_COMPLETED`/timezone/clock broadcasts all reschedule through `AppContainer.resyncReminder()`,
  but the acceptance's manual firing test still needs the emulator (Phase 12).
- **Firebase still blocked** — no `google-services.json`; `BuildConfig.FIREBASE_CHAT = false` (Q10).

### Fixed this phase (Phase 5)

- Onboarding, `HomeScreen`, period/symptom sheets and a real `SettingsScreen` replaced the
  placeholders; `LocalAppContainer` composition local is in (`ui/AppContainer.kt`).
- `BloomApp` reacts to `remindersEnabled` / hour / minute; from Phase 8 the log actions and the
  boot/timezone receiver go through `AppContainer.resyncReminder()` instead of calling it directly.
- Pure domain additions with tests: `HomeSummarizer` (Today card copy) and `PeriodEntryRules`
  (log-sheet validation, including overlap and self-edit cases).

---

## 3. Decisions Locked In

Confirmed with the user:

| Decision | Choice |
|---|---|
| Chat backend | Firebase — Anonymous Auth + Firestore |
| Premium gating | `EntitlementRepository` interface + local stub + paywall UI (no billing SDK yet) |
| Package name | ✅ Renamed from `com.example.myapplication` → `com.bloomcycle.app` |
| Plan style | Phased roadmap with tasks, acceptance criteria, and risks |

### Package / app name — ✅ resolved

| Item | Final value |
|---|---|
| Package / namespace / applicationId | `com.bloomcycle.app` |
| App display name | **Bloom** (`rootProject.name = "Bloom"`, `app_name = Bloom`) |
| Gradle path | `app/src/main/java/com/bloomcycle/app/` |

---

## 4. Target Architecture

Single Gradle module `:app` with strict **internal** package boundaries. Splitting into
`:core`/`:feature` Gradle modules is deliberately deferred — see Risk R7.

Actual layout (dashed = planned, not yet written):

```
com.bloomcycle.app/
├── BloomApplication.kt            ✅ application class, owns AppContainer
├── AppContainer.kt                ✅ hand-rolled DI (no Hilt — see §5.2)
├── MainActivity.kt                ✅ BloomTheme { BloomApp() }
│
├── core/time/                     ✅ CycleDate, CycleClock, SystemCycleClock
│
├── domain/                        PURE KOTLIN — no Android imports, JVM-testable
│   ├── model/                     ✅ Models.kt, SymptomCatalog.kt
│   ├── cycle/                     ✅ CycleCalculator.kt, PhaseResolver.kt
│   │                                ⏳ PredictionEngine/Fertility folded into CycleCalculator
│   ├── content/                   ✅ ContentLibrary.kt (pure Kotlin, not Room-seeded)
│   ├── calendar/                  ✅ CalendarModel.kt (month matrix, day marks)
│   ├── insights/                  ✅ ChartSeries.kt, InsightsAnalysis.kt
│   ├── notifications/             ✅ ReminderDecision.kt (the whole notification policy)
│   ├── wellness/                  ⏳ PainReliefContent, LiteratureReference, WorkoutAdvisor
│   ├── insight/                   ⏳ PersonalityProfile, InclinationProfile
│   └── repository/                ✅ Repositories.kt (all interfaces)
│
├── data/
│   ├── crypto/                    ✅ PassphraseProvider.kt
│   ├── local/                     ✅ Entities, Daos, Converters, AppDatabase, DatabaseFactory
│   ├── repo/                      ✅ Cycle, Settings, Entitlement, Chat, Content impls
│   ├── settings/                  ⏳ DataStore-backed SettingsRepositoryImpl lives in data/repo today
│   └── remote/                    ⏳ FirebaseAuthDataSource, FirestoreChatDataSource, DTOs
│
├── notifications/                 ✅ BloomNotifications, ReminderWorker, ReminderScheduler,
│                                   ✅ ReminderRescheduleReceiver (boot / timezone / clock)
│
├── ui/
│   ├── theme/                     ✅ Color, Type, Shape, Motion, Spacing, Theme   (= planned core/design/)
│   ├── navigation/                ✅ BloomDestination.kt
│   ├── phase/                     ✅ PhaseVisuals.kt
│   ├── components/                ✅ Components.kt, Placeholder.kt, DateField.kt
│   ├── format/                     ✅ DateText.kt (date copy + picker conversion)
│   ├── screens/                   ✅ Home, Settings, Onboarding, PeriodLogSheet, SymptomLogSheet
│   │                               ✅ Calendar, Insights   ⏳ Chat (placeholder)
│   ├── AppContainer.kt             ✅ LocalAppContainer + rememberAppContainer()
│   └── BloomApp.kt                ✅ NavHost, onboarding gate, reminder scheduling
│
└── feature/                       ⏳ — real calendar ✅ (Phase 6), charts ✅ (Phase 7),
                                         paywall detail: all planned, none written
```

**Layering rule:** `ui/` → `domain/` interfaces ← `data/` implementations. No `data/` type
ever appears in a composable. No `ui/` code touches Room or Firebase directly.

### Deviations from the original plan (locked in)

| Planned | Actual | Why |
|---|---|---|
| `core/design/` | `ui/theme/` + `ui/components/` + `ui/phase/` | Theme is Compose UI, not core; keeps `domain/` Android-free |
| `feature/<name>/` packages | `ui/screens/` + `ui/components/` | One module, flat screen package reads better; revisit at R7 split |
| Navigation 3 (`androidx.navigation3` alpha) | **Navigation 2** `navigation-compose` 2.10.2 | Shipping on a stable, well-documented API beats an alpha; typed routes still available |
| Room-seeded `content_items` table + `res/raw/content_seed.json` | Pure Kotlin `ContentLibrary` | No runtime seeding, no migration surface, fully unit-testable; citations still stored |
| Hilt | Hand-rolled `AppContainer` | §5.2 version trap |
| Separate `PredictionEngine.kt` / `FertilityCalculator.kt` | Folded into `CycleCalculator.kt` | One cohesive cycle-maths file with one test suite |
| Local chat in Room (transitional) | `chat_messages` entity exists | Dropped when Firestore lands (§7.4: chat must never sit in the encrypted DB) |

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

**What we actually pinned** (see §5.3): Room **3.0.3**, SQLCipher **4.19.1**, WorkManager **2.12.0**,
Kotlin **2.2.10** (kept from the template — deliberately not bumped while Hilt is out of the picture,
though the EOL note stands and a Kotlin upgrade should be its own deliberate change), KSP **2.3.10**.
Hilt **not adopted**.

### 5.2 ⚠️ The Kotlin / Hilt version trap

This is the single most likely thing to burn a day:

- Staying on Kotlin 2.2.x → Hilt 2.59.2 works, but you are on an **end-of-life compiler**.
- Moving to Kotlin 2.4.x → Hilt 2.59.2 **fails to compile** with
  `Provided Metadata instance has version 2.4.0, while maximum supported version is 2.3.0`.

**Recommendation: skip Hilt entirely for v1.** Build a hand-rolled `AppContainer` — as actually
implemented in `AppContainer.kt`:

```kotlin
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val cycleClock: CycleClock = SystemCycleClock()

    val passphraseProvider: PassphraseProvider by lazy { PassphraseProvider(appContext) }
    val database by lazy { DatabaseFactory(appContext, passphraseProvider).create() }

    val settings by lazy { SettingsRepositoryImpl(appContext) }
    val entitlements by lazy { EntitlementRepositoryImpl(appContext) }
    val cycleRepository by lazy { CycleRepositoryImpl(database.periodEventDao(), database.symptomLogDao(), cycleClock) }
    val chatRepository by lazy { ChatRepositoryImpl(database.chatMessageDao(), cycleClock) }
    val contentRepository by lazy { ContentRepositoryImpl() }
    val reminderScheduler by lazy { ReminderScheduler(appContext) }
}
```

Reached today via `BloomApplication.container` (a plain property). **Planned upgrade:** a
`LocalAppContainer` composition local, added with the Phase 5 real screens so previews and tests can
substitute a container.

Accessed via a single `LocalAppContainer` composition local. For one module with ~8 dependencies
this is ~40 lines, has zero annotation processing, and eliminates the whole version-matrix problem.
**Revisit Hilt only if you split into Gradle modules** (where manual wiring starts to hurt).

### 5.3 Version set — ✅ pinned and building

Actual `gradle/libs.versions.toml` as of 2026-10-02, verified by a green
`:app:assembleDebug :app:testDebugUnitTest`:

```toml
[versions]
agp             = "9.4.1"
kotlin          = "2.2.10"    # kept from template; §5.1's EOL note still applies
ksp             = "2.3.10"    # KSP2 — do not set the "ksp.useKSP2" flag
room            = "3.0.3"     # androidx.room3 — see the annotation renames below
sqlcipher       = "4.19.1"    # newer than the planned 4.18.0
workManager     = "2.12.0"
datastore       = "1.2.1"
desugar         = "2.1.5"
coroutines      = "1.10.2"
kotlinxSerialization = "1.8.1"  # forced — see §5.4, Room's migration helper needs it
turbine         = "1.2.1"
junit           = "4.13.2"
navigationCompose = "2.10.2"   # Navigation 2, not Navigation 3 — see §4 deviations
composeBom      = "2025.12.00"
```

Not pinned / not adopted: **Hilt** (§5.2), **Firebase BOM + google-services plugin** (blocked on
`google-services.json`; gated by `BuildConfig.FIREBASE_CHAT = false`), **Navigation 3**.
Added this phase: **room3-testing** (`androidTestImplementation`) + `kotlinx-serialization-json`/`-core`
pinned to `1.8.1` — see §5.4.

Enabled in `app/build.gradle.kts`:

```kotlin
buildFeatures { compose = true; buildConfig = true }
compileOptions { isCoreLibraryDesugaringEnabled = true }   // java.time on minSdk 24
buildConfigField("boolean", "FIREBASE_CHAT", "false")
```

Library list:

| Purpose | Library | Status |
|---|---|---|
| Database | `androidx.room3:room3-runtime`, `ksp(room3-compiler)` | ✅ |
| Encryption | `net.zetetic:sqlcipher-android` | ✅ |
| Async / scheduling | `androidx.work:work-runtime-ktx` | ✅ |
| Settings | `androidx.datastore:datastore-preferences` | ✅ |
| Navigation | `androidx.navigation:navigation-compose` (Nav 2) | ✅ |
| Chat | Firebase BOM → `firebase-auth`, `firebase-firestore` | ⏳ blocked on `google-services.json` |
| Firebase config | `com.google.gms.google-services` plugin | ⏳ verify with AGP 9.4.1 at Phase 11 |
| Tests | `app.cash.turbine`, `kotlinx-coroutines-test` | ✅ · `room3-testing` ✅ (Phase 4) |

### 5.4 ⚠️ Room 3 gotchas (hit and already paid for)

- **Package is `androidx.room3`, not `androidx.room`.** Artifact IDs are `room3-runtime` / `room3-compiler`.
- **Annotations were renamed:** `@TypeConverter` → **`@ColumnTypeConverter`**, `@TypeConverters` →
  **`@ColumnTypeConverters`**. Missing this produces the notoriously opaque
  `[MissingType]: Element 'com.bloomcycle.app.data.local.AppDatabase' references a type that is not present`.
- **KSP only.** No KAPT, no Java codegen.
- Schema export is **on** — root `build.gradle.kts` applies `alias(libs.plugins.room3) apply false`,
  `app/build.gradle.kts` applies the plugin and sets `room3 { schemaDirectory("$projectDir/schemas") }`
  (the extension is `room3`, **not** `room`), and `AppDatabase.exportSchema = true`. The plugin
  auto-copies schemas into androidTest assets via `copyRoomSchemasToAndroidTestAssetsDebugAndroidTest`
  — no manual `sourceSets` block needed.
- **`MigrationTestHelper` is a `TestWatcher` now.** Room 3 ships it in `androidx.room3.testing` with
  constructor `(instrumentation, file, driver, databaseClass, databaseFactory = default, autoMigrationSpecs = emptyList())`.
  `createDatabase(version)` and `runMigrationsAndValidate(version, migrations)` are **suspend** (wrap
  in `runBlocking`), there is **no `validateDroppedTables`** parameter, and the connection is a
  `SQLiteConnection` (`prepare(sql)` → `SQLiteStatement` with `step()`/`getInt(0)`/`close()`).
  Android test methods must return **`void`**: `fun x() = runBlocking { ... }` only compiles if the
  block's last expression is `Unit`, otherwise use a block body with `runBlocking` inside.
- **kotlinx-serialization must be ≥ 1.8.1 or the migration helper explodes.** `room3-migration`'s
  generated `FieldBundle$$serializer` relies on `GeneratedSerializer.typeParametersSerializers()`
  being a **`default`** method (it became one in 1.8.1); in 1.7.3 it is `abstract`, so the helper
  throws `AbstractMethodError` the moment it deserializes the schema JSON. AGP pins the graph at
  1.7.3, so `app/build.gradle.kts` declares a `strictly` constraint at 1.8.1 for
  `kotlinx-serialization-core` / `-json` (§5.3).

### 5.5 ⚠️ Kotlin gotcha — JVM erasure clashes

Two functions `List<Cycle>` and `List<Int>` erase to the same JVM signature and fail with
`Platform declaration clash: ... have the same JVM signature (foo(Ljava/util/List;)Z)`. This has
already cost two rounds: `median` → `medianOf(List<Double>)`, `regularityDays` → `regularityOf`, and
`outsideTypicalRange(cycles)` → private `outsideTypical(lengths)`. **Name the list-of-primitives
helper differently instead of overloading it.** The same trap makes `emptyList()` ambiguous when
both overloads exist — give it an explicit type argument.

**No chart library.** All charts are hand-drawn on Compose `Canvas` (Phase 6). Avoids a dependency
that fights Compose's layout and keeps the chart set exactly tailored to cycle data.

**Note on Firebase + AGP 9 / Gradle 9.6:** the `google-services` plugin historically lags new AGP
major versions. Verify the plugin works with AGP 9.4.1 **in Phase 1, not Phase 10**. If it fails,
either bump the plugin or bypass it with a manual `FirebaseOptions` initialization.

---

## 6. Data Model & Schema

### 6.1 Room entities — ✅ implemented (with changes)

As written in `data/local/Entities.kt` (version 1, `bloom.db`):

```kotlin
@Entity(tableName = "period_events", indices = [Index(["startDate"], unique = true)])
data class PeriodEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: CycleDate,          // ISO-8601 LocalDate string — see 6.2
    val endDate: CycleDate?,           // null = ongoing
    val flow: FlowLevel?,              // LIGHT / MEDIUM / HEAVY / SPOTTING
    val isSpottingOnly: Boolean,       // distinguishes spotting from a real period
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(tableName = "symptom_logs", indices = [Index(["date"])])
data class SymptomLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: CycleDate,
    val symptomId: String,             // key into curated SymptomCatalog
    val severity: Int,                 // 1..5
    // NOTE: no `cycleDay` column — derive it at query/chart time from the cycle it falls in.
    //       Storing it denormalised was in the original plan; dropping it avoids stale
    //       values when a period gets edited after symptoms were logged.
)

@Entity(tableName = "chat_messages")   // TRANSITIONAL — dropped when Firestore lands (§7.4)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val text: String, val authorName: String,
    val phaseBucket: String, val isOwn: Boolean,
    val createdAt: Instant, val scope: String,
)
```

Supporting:
- `SymptomCatalog` — static Kotlin object, not a DB table. ✅
- `UserSettings` — a **domain model** (`Models.kt`) persisted via DataStore, not a Room entity. ✅
  Fields: `onboardingComplete`, `remindersEnabled`, `reminderHour/Minute`, `dailyNoteEnabled`,
  `predictionsMuted`, `cycleContext`, `chatDisplayName`, `notificationPermissionAsked`.
  `cycleContext` (`CycleContext`, §8.4) is stored by **enum name** with a fallback to `NONE` on an
  unknown value — a corrupt preference must never make the settings screen unopenable.
- **`content_items` table: dropped.** Content lives in `domain/content/ContentLibrary.kt` as pure
  Kotlin with stored citations. No seeding, no `res/raw/content_seed.json`, no marker-in-settings.
- Firestore `chats/{roomId}/messages/{messageId}` — **remote, not Room**. Chat history is cached
  in-memory only; it must never land in the encrypted DB, since it is the one part of the app that
  leaves the device unencrypted.

### 6.2 ⚠️ Do not store dates as `Long` epoch millis

Storing instants for calendar dates creates off-by-one-day bugs across timezones. Store
`LocalDate` as ISO-8601 `String` (`"2026-10-02"`) via a `TypeConverter`. Store true instants
(`createdAt`) as `Long` epoch millis — those are genuinely instants. This distinction must be
respected everywhere, including Room queries and notification scheduling.

### 6.3 Migrations

✅ **Done in Phase 4.** `AppDatabase` is `version = 1, exportSchema = true`, `app/build.gradle.kts`
applies the `androidx.room3` plugin and sets `room3 { schemaDirectory("$projectDir/schemas") }`, and
the baseline is committed:

```
app/schemas/com.bloomcycle.app.data.local.AppDatabase/1.json   (identityHash aa41f0e0…4755e)
```

`MigrationSchemaTest` validates the exported JSON against the live schema on device, so any future
entity change must bump `version`, add a `Migration`, and extend that test before it ships. See §5.4
for the Room 3 helper API.

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
  user input (a forgotten passphrase = total data loss, with no recovery). ✅ `PassphraseProvider.getOrCreate()`
- ~~Zero the passphrase `ByteArray` after the driver opens the DB.~~ **Do not zero it.**
  `SQLCipherDriver` retains the array and re-supplies it on every connection open/reopen; clearing it
  leaves the driver holding zeros and the database stops opening. The array is held in-process only
  and never logged — see the comment in `DatabaseFactory.create()`.

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

### 7.3 ⚠️ Backup rules — data-loss trap ✅ fixed

Both files are now written with full exclusions (`database`, `sharedpref`, `file`, `root`) and an
in-file comment explaining *why* (a restored ciphertext with no Keystore key is an unopenable
database). `backup_rules.xml` for pre-API-31, `data_extraction_rules.xml` for 31+.

Original hazard, for the record: with encrypted data, Android Auto Backup will happily copy
`bloom.db` to Google Drive and restore it onto a device where the Keystore key does not exist —
producing an unopenable database and total data loss.

Alternative that was considered and rejected in favour of explicit excludes: setting
`android:allowBackup="false"`. Either is acceptable; what is **not** acceptable is leaving the
stubs empty.

### 7.4 What is *not* encrypted

Firebase chat messages leave the device in plaintext to the server. This must be designed around:

- Send **only** a coarse `phaseBucket` (e.g. `"luteal-day-24"`) and a `cycleLengthBand`
  (`"28-30"`) — never exact dates, never a birth date, never a real name.
- Chat display names are user-chosen pseudonyms, generated locally.
- Firestore rules must enforce that a client can only write its own message document.

---

## 8. Cycle Engine Specification (Phase 3 — the core) ⭐

Pure Kotlin in `domain/cycle`. No Android imports. This is where the product's value lives.

**Status: complete. 42 tests green (31 + 11), every §8.4 edge case covered — see the per-item
status below. `PredictionEngine` and `FertilityCalculator` were folded into `CycleCalculator.kt`;
`domain/wellness/` and `domain/insight/` are still unwritten.**

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

Status against `CycleCalculatorTest` (22) + `PhaseResolverTest` (11):

- ✅ **0 logged cycles** → documented defaults (28-day cycle, 5-day period, 14-day luteal), and
  `predict()` returns **null** rather than fabricating a prediction.
  `no logged cycles falls back to documented defaults`, `defaults are used until at least one real
  length exists`, `prediction is null rather than fabricated when there is no history`.
- ✅ **1 logged cycle** → predictions shown, confidence pinned to LOW.
  `a single logged cycle produces low confidence`.
- ✅ **Spotting logged as a period** → excluded from cycle-length statistics and never starts a cycle.
  `spotting alone does not start a cycle`.
- ⚠️→✅ **Very long / very short cycles** (>35 or <21 days) → **done.** Statistics compute them
  (plausible band is 15–60 days, not 21–35); `outsideTypicalRange()` detects an average outside
  21–35; `regularityLabel()` and `PhaseResolver.describeReadiness()` then append an honest note —
  `"Very consistent — around 40 days, which is longer than typical"`. Numbers and confidence are
  deliberately **unchanged**: only the wording softens. The "suppress you're pregnant? prompts" half
  remains vacuously satisfied — no such prompt exists.
  `a consistently long cycle is still computed but says so in the label`,
  `a consistently short cycle is flagged as shorter than typical`,
  `a typical cycle is computed and never flagged`.
- ✅ **Overlapping or duplicate period events** → merged on write; `startDate` is also a **unique
  index**, so a zero-length cycle is structurally impossible.
  `two events logged on the same day are merged`, `duplicate starting points never produce a zero
  length cycle`.
- ✅ **Date gaps** (user skipped 3 months) → do not fabricate cycles; excluded from the mean.
  `GAP_THRESHOLD_DAYS = 60`; `a three month logging gap is not treated as a cycle length`,
  `a long logging gap does not pretend a phase`.
- ✅ **Hormonal contraception / perimenopause / postpartum flags** → **done.**
  `CycleContext` on `UserSettings` (`NONE`, `HORMONAL_CONTRACEPTION`, `PERIMENOPAUSE`,
  `POSTPARTUM`), persisted by enum name in DataStore with a safe fallback to `NONE`.
  `predict(cycles, today, context)`:
  - `HORMONAL_CONTRACEPTION` → returns **null** (ovulation is suppressed, there is no cycle to
    read — silence beats a confident fiction). `ReminderWorker` therefore sends no period reminder.
  - `PERIMENOPAUSE` / `POSTPARTUM` → still predicts, but `prediction.isCaveated == true` and
    confidence is forced to **LOW** regardless of how tidy the history looks.
  - `ReminderWorker` additionally refuses to send the "your period may be close" notification for
    any caveated prediction; it keeps only the date-neutral check-in.
  `hormonal contraception suppresses the prediction instead of inventing one`,
  `perimenopause predictions are caveated and pinned to low confidence`,
  `postpartum predictions are caveated too`,
  `a default context prediction is not caveated`.

Also covered beyond the spec: implausible lengths filtered from stats, outlier rejection via
`robustMean`, clamped luteal so ovulation cannot precede menstruation, late periods reported as
late rather than projected away, open/ongoing cycles, and phase resolution across boundaries.

### 8.4a Acceptance criteria — ✅ met

| Required | Test |
|---|---|
| Defaults with 0 cycles | `no logged cycles falls back to documented defaults` |
| LOW confidence with 1 cycle | `a single logged cycle produces low confidence` |
| 28-day cycles predicted correctly | `six regular 28 day cycles predict exactly 28 days ahead` |
| Stddev widening the prediction range | `an irregular history widens the predicted range` |
| Spotting excluded from averages | `spotting alone does not start a cycle` |
| Phase resolution: day 1, last bleeding day, ovulation day, final cycle day | 4 tests in `PhaseResolverTest` |

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

| # | README requirement | Phase | Premium? | Status |
|---|---|---|---|---|
| 1 | Tracks periods | 5 | No | ✅ logging flow, home card, undo delete |
| 2 | Charts explaining | 7 | Partly | ✅ canvas charts + written analysis (written reading = Premium) |
| 3 | Global/local chat, phase-matched women | 11 | No | ⬜ local stub only |
| 4 | Stored in DB, encrypted with local key | 4 | No | ✅ SQLCipher + Keystore, verified on device (4/4 instrumentation tests) |
| 5 | Calendar based | 6 | No | ✅ month grid, day sheet, legend, year strip |
| 6 | Tips for cramps/pain | 8 | No | ✅ `ContentLibrary` |
| 7 | What literature says about period duration | 8 | No | ✅ `ContentLibrary` (some URLs missing) |
| 8 | **Premium:** Analysis | 9 | Yes | ⬜ gate ⬜, analysis ⬜ |
| 9 | **Premium:** Best fertility window | 10 | Yes | ⬜ calculator ✅, presentation ⬜ |
| 10 | **Premium:** Year to conceive — astrology / sports / academic inclinations | 10 | Yes | ⬜ |
| 11 | "What their cycle says about them" | 10 | Yes | ⬜ |
| 12 | Long-term prediction + early warning signs | 8 | No (long-term = Yes) | ✅ engine (`projectNext`, `CycleContext`), six-month card gated on `LONG_HORIZON`, early-signal nudge in the PMS window |
| 13 | Notifications during / before, time-of-month aware | 8 | No | ✅ `ReminderDecision` policy, channels, boot/timezone rescheduling |
| 14 | **Premium:** Gym / run intensity by cycle phase | 9 | Yes | ⬜ |
| 15 | Soft inclusive language, smooth adaptive UI | 2, 12 | No | ✅ design system, ⬜ copy review |
| 16 | 1-slide chat entry point | 11 | No | ⬜ placeholder screen |

---

## 10. Phased Roadmap

Each phase lists concrete files and an acceptance test. Do not start a phase until the previous
phase's acceptance test passes.

---

### Phase 0 — Foundation & Rename ✅ (1 open checkbox)
**Effort:** ~0.5 day · **Status: done, merged to `main`**

- [x] Choose real package ID and app display name → `com.bloomcycle.app` / "Bloom".
- [x] Move `com/example/myapplication/**` → new package path; update `namespace` and `applicationId`
      in `app/build.gradle.kts`.
- [x] Update `settings.gradle.kts` → `rootProject.name = "Bloom"`.
- [x] Update `app/src/main/res/values/strings.xml` → `app_name = Bloom`.
- [x] Fix `ExampleInstrumentedTest.kt:22` — resolved by **deleting** the file (it hardcoded the old
      package and would have failed; there was nothing worth keeping).
- [x] Delete `ExampleUnitTest.kt` / `ExampleInstrumentedTest.kt`.
- [x] Delete template `ic_home`/`ic_favorite`/`ic_account_box` drawables.
- [x] Replace default purple `Color.kt` with a real palette — done in Phase 2 as `ui/theme/Color.kt`
      (DriedRose/Sage/Terracotta, light + `values-night`). `dynamicColor = false`, see R6.
- [x] **Add `.gitignore` entries for `google-services.json`, `*.jks`, `keystore.properties`.** ✅ Phase 4.
- [x] **Confirm `.idea/` is fully ignored.** ✅ Phase 4 — replaced the 6 partial rules with a single
      `/.idea/` and ran `git rm -r --cached .idea` (8 files untracked, still on disk). The rewrite also
      adds `build/`, `.gradle/`, `*.iml`, `*.keystore`, `.DS_Store`.
- [ ] **Verify the Firebase `google-services` plugin works with AGP 9.4.1 now**, not in Phase 11.
      ⏳ Still open — cannot be verified without a `google-services.json` (Q-blocked). Keep it on
      the Phase 11 checklist and treat R4 as live.

**Acceptance:** `./gradlew assembleDebug` succeeds; app launches; package name correct; app name
shows in the launcher. ✅ Met.

---

### Phase 1 — Project Infrastructure ✅ (all checkboxes done)
**Effort:** ~1 day · **Status: done**

- [x] Add all dependencies from §5.3 to `libs.versions.toml`.
- [x] Enable core library desugaring; `java.time.LocalDate` available on minSdk 24.
- [x] Apply KSP (2.3.10, KSP2).
- [x] **Enable schema export** — `room3 { schemaDirectory(...) }` + `exportSchema = true`. ✅ Phase 4
      (§6.3); the plugin is `androidx.room3`, not `androidx.room`.
- [x] Create `BloomApplication` + `AppContainer`.
      ✅ `LocalAppContainer` composition local added in Phase 5 (`ui/AppContainer.kt`), reached with
      `rememberAppContainer()`; production falls back to `BloomApplication.container`, so tests and
      previews can inject a stub.
- [x] Create `core/time/`:
  - `typealias CycleDate = LocalDate` ✅
  - `interface CycleClock { fun today(): CycleDate; fun now(): Instant }` ✅ + `SystemCycleClock`,
    injected into `CycleRepositoryImpl`, `ChatRepositoryImpl`, `ReminderWorker`.
- [x] Enable `buildConfig`; `FIREBASE_CHAT = false` (no API secrets hardcoded).
- [x] Add `Turbine` + `kotlinx-coroutines-test` to `testImplementation`.
      ⚠️ Declared but **not yet used by any test** — all 57 tests are synchronous JUnit. Reach for
      them when `SettingsRepository` (Flow) gets its first test.

**Acceptance:** App builds with all deps resolved; a unit test can inject a fixed `CycleClock` and
assert on a date 40 days later. ⚠️ **Partly met** — build is green and `CycleClock` is injectable,
but no test actually exercises it yet.

---

### Phase 2 — Design System & App Shell ✅
**Effort:** ~1.5 days · **Status: done — PR #1 (`phase-2-design-system`) + PR #2 (`phase-2-app-shell`) merged**

- [x] Theme: `ui/theme/{Color,Type,Shape,Motion,Spacing,Theme}.kt`
      (was `core/design/` — see §4 deviation). Botanical-editorial on warm paper:
      DriedRose primary / Sage secondary / Terracotta tertiary, phase colours are semantic
      (Oxblood, Moss, Amber, Mauve + `*Night` dark variants), `dynamicColor = false`.
- [x] Type: **Fraunces** (display + text) and **Karla**, instanced as static TTFs from Google Fonts
      variable fonts with fontTools; 7 files in `res/font/` (~435 KB).
- [x] `PhaseVisuals` → `ui/phase/PhaseVisuals.kt` — one mapping from each of the 4 phases to
      colour/glyph/label/blurb, used everywhere. Colour never carries meaning alone (glyph + word).
- [x] Components: `SoftCard`, `PhaseChip`, `PrimaryButton`, `SectionHeader`, `EmptyState` in
      `ui/components/Components.kt`.
- [x] `ui/navigation/BloomDestination.kt` + real nav (Navigation **2** typed routes — see §4
      deviation; Navigation 3 was an alpha).
- [x] Replace the template's enum hack. `ui/BloomApp.kt` uses `NavigationSuiteScaffold` +
      `NavHost` with cross-fade transitions. Destinations: Home, Calendar, Insights, Chat, Settings.
- [x] Tablet/foldable adaptation via `material3-adaptive-navigation-suite`
      (`NavigationSuiteScaffold` measures content into the remaining space; its `content` lambda
      takes **no `PaddingValues`** — a real API trap).
- [ ] **Accessibility pass:** 48dp touch targets, content descriptions on Canvas drawings,
      `semantics` for phase state, no colour-only signalling. ⚠️ Partial — `PhaseVisuals` already
      pairs colour with glyph+word, but there is no dedicated audit and no TalkBack run yet.
      Move the audit to Phase 12 where the plan already budgets for it.

**Acceptance:** All 5 destinations navigate; rotates without losing state; renders sensibly at
phone, foldable, and tablet widths; TalkBack reads every control. ⚠️ **Partly met** — navigation,
rotation and adaptive layout are in; TalkBack has not been run.

---

### Phase 3 — Domain & Cycle Engine ⭐ ✅ done
**Effort:** ~3–5 days · **Highest priority — do not defer** · **Status: complete, 42/42 tests green**

- [x] `domain/model/` — `Cycle`, `PeriodEvent` (was `PeriodLog`), `FlowLevel`, `SymptomLog`,
      `PhaseType`, `CyclePrediction`, `PhaseState`, `PredictionConfidence`, `CycleContext`,
      `UserSettings`, `SymptomCatalog`.
- [x] `domain/cycle/CycleCalculator.kt` — everything in §8.3 (`buildCycles`, `lengthsForStats`,
      `robustMean`, `median`, `regularityOf`/`regularityLabel`, `outsideTypicalRange`/
      `typicalityNote`, `averageCycleLength`, `averagePeriodDuration`, `lutealPhaseLength`,
      `confidence`, `predict`, plus fertile window and ovulation).
- [x] `domain/cycle/PredictionEngine.kt` — **folded into `CycleCalculator.kt`** (§4 deviation).
      Confidence rubric §8.5 implemented: `<3` LOW, `3–5` + stddev ≤3 MEDIUM, `≥6` + stddev ≤3 HIGH,
      stddev >7 downgrades one level.
- [x] `domain/cycle/FertilityCalculator.kt` — **folded into `CycleCalculator.kt`** (§4 deviation).
- [x] `domain/cycle/PhaseResolver.kt` — current phase for any date, with gap guard, ovulation
      anchored to the *current* cycle, and a `describeReadiness()` read that carries the same
      typicality note as the regularity label.
- [x] Unit tests covering **every** §8.4 edge case — 42 tests (31 + 11), 0 failing.

**All §8.4 gaps closed.** See the per-item status above.

**Acceptance:** Test suite demonstrates: defaults with 0 cycles; LOW confidence with 1 cycle;
28-day cycles predicted correctly; stddev widening the prediction range; spotting excluded from
averages; phase resolution correct on day 1, last bleeding day, ovulation day, and final cycle day.
✅ **All six met** — see §8.4a.

---

### Phase 3 follow-up — spec gaps & hygiene ✅ done (branch `phase-3-cycle-engine`)
**Effort:** ~0.5 day · Was: do this before starting Phase 5

- [x] Add `CycleContext` enum (`NONE`, `HORMONAL_CONTRACEPTION`, `PERIMENOPAUSE`, `POSTPARTUM`) to
      `UserSettings` with a DataStore-backed setter on `SettingsRepository`
      (stored by enum name; unknown value falls back to `NONE` rather than throwing).
- [x] `CycleCalculator.predict(cycles, context)` returns `null` for `HORMONAL_CONTRACEPTION` and
      forces `isCaveated = true` / `LOW` confidence for the other non-default contexts.
- [x] `ReminderWorker` passes `settings.cycleContext`, and never sends the date-claiming
      period reminder on a caveated prediction.
- [x] Add `outsideTypicalRange()` + `typicalityNote()` and push the softened wording through
      `regularityLabel()` and `PhaseResolver.describeReadiness()`.
- [x] Tests: 9 added covering contraception suppression, both caveats, long/short/typical ranges,
      empty history, and label↔readiness agreement.
- [x] Build + tests verified green, then committed → pushed → PR → merged.

---

### Phase 4 — Encrypted Persistence ✅ done
**Effort:** ~3–4 days · **Status: merged — schema export on, 4/4 instrumentation tests green on `Pixel_9`**

- [x] `data/crypto/PassphraseProvider.kt` — Keystore AES-256-GCM wrap/unwrap of a 32-byte
      `SecureRandom` passphrase, Base64 envelope (IV ‖ ciphertext) in ordinary SharedPreferences.
      `setUserAuthenticationRequired` deliberately **not** set (§7.1).
- [x] **Fix `PassphraseProvider.unwrap()`** — caught by the reopen test, a real production bug.
      It derived the IV length as `size - GCM_TAG_BITS / 8`, but `Cipher.doFinal()` output *already
      includes* the 16-byte tag, so a 60-byte envelope yielded a 44-byte IV and Keystore threw
      `InvalidAlgorithmParameterException: Unsupported IV length … Only 12 bytes long IV supported`.
      First launch worked (nothing to unwrap); **every reopen failed**, i.e. the app could not open
      its own database a second time. Now `GCM_IV_BYTES = 12` with a size guard, envelope layout
      unchanged.
- [x] `data/local/DatabaseFactory.kt` — `System.loadLibrary("sqlcipher")`, `SQLCipherDriver(passphrase, null, null)`,
      `Room.databaseBuilder(...).setDriver(driver)`. Passphrase **not** zeroed (§7.1).
- [x] Room entities, DAOs, `TypeConverter`s (`CycleDate` ↔ ISO string, `Instant` ↔ epoch),
      `AppDatabase` (`bloom.db`, version 1, `exportSchema = true`).
- [ ] ~~Seed `ContentItemEntity` from `res/raw/content_seed.json` on first run~~
      **Dropped by design** — content is `domain/content/ContentLibrary.kt`, pure Kotlin (§4 deviation).
- [x] Repository interfaces in `domain/repository/`, impls in `data/repo/`
      (`Cycle`, `Settings`, `Entitlement`, `Chat`, `Content`).
- [x] `SettingsRepository` over DataStore (not Room — preferences shouldn't require DB unlock).
- [x] **Fix `backup_rules.xml` and `data_extraction_rules.xml`** (§7.3). ✅ Done, both files.
- [x] **Enable Room schema export** (§6.3) — `room3 { schemaDirectory(...) }`, `exportSchema = true`,
      `app/schemas/…/1.json` committed. Root `build.gradle.kts` applies the plugin `apply false`,
      `app/build.gradle.kts` applies it (§5.4).
- [x] **Migration/schema tests** (`MigrationSchemaTest`, 2 tests) — `room3-testing` added to
      `androidTestImplementation`; creates DB from the exported JSON and validates the live schema
      against it (§5.4 for the Room 3 helper API).
- [x] **Persistence instrumentation tests** (`DatabasePersistenceTest`, 2 tests) — write + close +
      reopen through a **fresh factory/driver/Room instance**, and a plaintext-canary scan over
      `bloom.db` **plus its `-wal`/`-shm`** (WAL sidecars included, since Room runs in WAL mode).
- ⚠️ **Two deviations from the original acceptance criteria:** the canary is asserted in-process by
      reading the files and searching for the bytes, rather than `adb shell … | xxd`; and "reopen in
      a new process" is a full teardown + fresh-open in the same instrumentation process (a real
      second process would need a separate test run). Both still prove the same property.
- ✅ **Device available:** AVD `Pixel_9` (API 16, x86_64) on `emulator-5554` — Q9 resolved.

**Acceptance:** no readable period data on disk (checked in-process), reopen returns correct data,
4/4 instrumentation tests pass. ✅ Met — `.\gradlew.bat :app:connectedDebugAndroidTest` → BUILD
SUCCESSFUL, `tests="4" failures="0"`.

---

### Phase 5 — Logging Flow ✅ merged
**Effort:** ~2 days

> Landed this phase: six-step onboarding, real `HomeScreen` with the Today card, period and symptom
> sheets with snackbar undo, a real `SettingsScreen`, `LocalAppContainer`, and `BloomApp` as the one
> caller of `ReminderScheduler` (settings and onboarding only write flags). Screens hold their own
> settings/Room state with `LaunchedEffect` + `collectAsStateWithLifecycle` — no ViewModels, which
> would be scaffolding without an extra job to do yet.

- [x] Onboarding: welcome, birth date, current cycle start, "typical cycle length" with soft
      defaults, notification permission request.
- [x] `feature/home/` (in practice: flesh out `ui/screens/HomeScreen.kt`) — today's phase card,
      cycle day, days to next predicted period, via a new pure `HomeSummarizer`
      (`domain/home/HomeSummary.kt`).
- [x] Period logging: start / end / flow level / edit / delete, with validation and undo.
      `PeriodEntryRules` holds the validation; snackbar undo restores a deleted event.
- [x] Symptom logging: multi-select from catalog + severity (`SymptomLogSheet`).
- [x] Optimistic UI updates; Snackbar undo for destructive actions.
- [x] Introduce `LocalAppContainer` (Phase 1 debt) so screens and previews can take a container
      (`ui/AppContainer.kt`).
- [x] **Surface `CycleContext` in the UI:** onboarding step + settings picker (none / hormonal
      contraception / perimenopause / postpartum), and — when the context is caveated — copy on
      the home card that says the dates are a rough guide. `predict()` already returns null or
      `isCaveated = true`; nothing must present those dates as settled.
- [x] Wire `ReminderScheduler`: `BloomApp` reacts to `remindersEnabled` / `reminderHour` /
      `reminderMinute` and calls `ensureScheduled` or `cancel` — the only caller, so onboarding and
      Settings only write flags.
- [x] Unit tests for the two new pure pieces: `HomeSummaryTest` (8) + `PeriodEntryRulesTest` (7).

**Acceptance:** A user can log a period from cold start in under 10 seconds; the home card updates
immediately; all logged data survives process death.

✅ **Done, merged as #5.** 57/57 unit tests + 4/4 instrumentation tests green; `assembleDebug` builds.

---

### Phase 6 - Calendar ✅ merged (branch `phase-6-calendar`, PR #7)
**Effort:** ~3–4 days

> Pre-work already on `main`: `ui/screens/CalendarScreen.kt` placeholder; `PhaseVisuals` gives each
> phase its colour + glyph; the design system has `Hairline`, `Gap`, `SoftCard` to build on.

- [x] Month grid: period days, predicted days, fertile window, today — **visually distinguishable
      without relying on colour alone** (pattern/dot density + labels).
- [x] Swipe month-to-month, tap day → day detail sheet.
- [x] Log directly from a calendar day.
- [x] "Why am I seeing this?" legend — important, since predictions and actuals look similar.
- [x] Logged-outside-expected-range indicator (gentle, non-alarming).
- [x] Year overview strip.

**Acceptance:** A full year renders correctly including month boundaries and leap years; every day
is tappable; predictions are visually distinct from logged days; works at all supported font scales.

**Delivered:** `domain/calendar/CalendarModel.kt` (pure: month matrix with locale week start, day
classification, phase resolution) + `ui/screens/CalendarScreen.kt` (HorizontalPager, day sheet,
legend, year strip) + `PeriodLogSheet(initialStartDate)` so a calendar day seeds the log entry.
Shape-first marks, never colour-only; every mark also spoken for TalkBack.
Tests: `CalendarModelTest` (17) — leap years, 365-day year walk, window edges, out-of-range flag.

---

### Phase 7 - Insights & Charts ✅ merged (branch `phase-7-insights`, PR #8)
**Effort:** ~2–3 days

> Pre-work already on `main`: `ui/screens/InsightsScreen.kt` placeholder; `CycleCalculator` exposes
> `lengthsForStats`, `regularityDays`, `regularityLabel` — the raw inputs for the line/bar charts.

- [x] Hand-drawn Compose `Canvas` charts — **no third-party chart library**:
  - cycle length history (line, with predicted band)
  - period duration history (bar)
  - phase timeline / wheel
  - symptom frequency by cycle day (heatmap) ← answers README line 2 directly
  - consistency score
- [x] Each chart gets a text/summary alternative for accessibility.
- [x] `InsightsAnalysis` domain service produces the plain-language interpretation.
- [x] Gate advanced analytics behind Premium (README line 7); free tier keeps at least one chart.

**Acceptance:** All charts render with 0, 1, and 24 cycles without crashing or dividing by zero;
every chart has an accessible non-visual equivalent.

**Delivered:** `domain/insights/ChartSeries.kt` (series defined for 0/1/24 cycles, ranges always
plottable) + `InsightsAnalysis.kt` (`InsightsReport.summary` is the text alternative) +
`ui/charts/Charts.kt` (line+band, bars+average, phase ring, heatmap; every canvas
`clearAndSetSemantics {}`) + `ui/components/PremiumGate.kt` (`PremiumTeaser`, `PremiumBadge`).
Free: lengths, durations, symptom heatmap, consistency. Premium (`ANALYSIS`): the ring and the
written reading. Tests: `ChartSeriesTest` (13) + `InsightsAnalysisTest` (5).


---

### Phase 8 — Content, Predictions & Notifications ✅ merged (branch `phase-8-remainder`, PR #9)
**Effort:** ~3–4 days

- [x] Curate content for README lines 6 & 13 — **research, not coding** → `domain/content/ContentLibrary.kt`,
      ~15 items across `ContentCategory.{PAIN_RELIEF, CYCLE_FACTS, MOVEMENT, NUTRITION, MIND}`.
      Sourced from public health bodies (NHS, NICE NG92) where confident;
      `sourceUrl = null` where not — no unsourced claims.
- [x] Store citations (`sourceTitle`/`sourceUrl` on `ContentItem`) and render them.
      ⚠️ Still need a **Phase 12 copy review** of the actual wording and the missing URLs.
- [x] `notifications/BloomNotifications.kt` — separate channels so users can mute tips but keep
      period reminders; created in `BloomApplication.onCreate`.
- [x] `ReminderWorker` + `ReminderScheduler`:
  - ✅ `PeriodicWorkRequestBuilder<ReminderWorker>(1, DAYS)` under unique work name
        `bloom.daily-reminder` — reads `container.cycleClock.today()`.
  - ✅ **Not** `AlarmManager.setExactAndAllowWhileIdle`, so no `SCHEDULE_EXACT_ALARM` permission.
  - ✅ Rescheduled on settings change (Phase 5): `BloomApp` calls `ensureScheduled` /
        `cancel` whenever `remindersEnabled`, `reminderHour` or `reminderMinute` changes, and
        onboarding writes the same flags.
- [x] Runtime `POST_NOTIFICATIONS` permission on Android 13+, requested with context, never blocking.
      ✅ Requested from onboarding and from the Settings toggle (Phase 5); the answer is recorded in
      `UserSettings.notificationPermissionAsked`, so it is asked at most once.
- [x] **Reschedule on every log** — `HomeScreen` (save, update, delete, undo-restore) and
      `CalendarScreen` (save) call `AppContainer.resyncReminder()`, so the next run follows the
      prediction the log just moved.
- [x] **Boot / timezone / clock receiver** — `notifications/ReminderRescheduleReceiver.kt`
      handles `BOOT_COMPLETED`, `TIMEZONE_CHANGED` and `TIME_SET`, re-asserting the schedule from
      the *current* clock; `RECEIVE_BOOT_COMPLETED` + `<receiver>` declared in the manifest.
- [x] **Early-signal nudges** — `domain/notifications/ReminderDecision.kt` is the whole policy as
      pure Kotlin: caveated contexts and `predictionsMuted` say nothing; days 0–2 send the period
      reminder; days 3–6 inside the PMS window send the early-signal note, and only when
      `dailyNoteEnabled` (it posts on the notes channel, so the toggle is the honest switch).
      `ReminderWorker` now dispatches its verdict instead of deciding inline; the old generic
      check-in copy is gone.
- [x] **Long-term (6-month) projection → Premium** — `InsightsScreen` gains a "Six months ahead"
      card: `PredictionList(projectNext(months = 6))` when `LONG_HORIZON` is unlocked,
      `PremiumTeaser` when not (`PremiumGate.kt` from Phase 7).
- [x] Tests: `ReminderDecisionTest` (9) + `ReminderRescheduleReceiverTest` (3) → **104/104 green**.

**Acceptance:** A reminder fires within a minute of its scheduled time in a manual test; disabling
notifications actually silences it; changing the device timezone reschedules correctly.
⚠️ **Policy unit-tested, firing still manual** — `ReminderDecision` covers every branch that decides
*whether* to speak; the "does it actually fire" test on the emulator (`Pixel_9`, Q9) is deferred to
Phase 12, with the phases that need a device.

---

### Phase 9 — Entitlements & Paywall ⚠️ ~45% done
**Effort:** ~2 days

- [x] `domain/repository/EntitlementRepository.kt` — interface + `PremiumFeature` enum +
      `EntitlementState` (with `isDebugUnlocked`) already written:
  ```kotlin
  enum class PremiumFeature { ANALYSIS, FERTILITY_WINDOW, LONG_HORIZON, WORKOUT_PLAN, PERSONALITY_READING }
  interface EntitlementRepository {
      val state: Flow<EntitlementState>
      fun isUnlocked(feature: PremiumFeature): Boolean
      suspend fun unlock(feature: PremiumFeature)   // stub
  }
  ```
- [x] `LocalEntitlementRepository` → **renamed `data/repo/EntitlementRepositoryImpl.kt`** with DataStore
      persistence (`unlocked_features`, `debug_unlock_all`) — ✅ written, and `InsightsScreen` already
      reads it. Still open: the **debug-only unlock toggle in a developer settings screen**.
- [x] `ui/components/PremiumGate.kt` — `PremiumTeaser(feature, onUnlock)` tasteful teaser card,
      written in Phase 7 and used on Insights for `ANALYSIS` and (Phase 8) `LONG_HORIZON`.
      Still open: a generic `PremiumGate(feature) { ... }` wrapper so every other screen gets one
      call site instead of hand-rolling the lock.
- [ ] `ui/screens` paywall — soft, non-pressuring copy. No dark patterns, no fake countdowns, no
      fake "47 people viewing". Play Store policy risk (R2). Not written.
- [ ] Add the billing seam: a `BillingEntitlementRepository` stub with the same interface, and a
      TODO for Play Billing or RevenueCat. Not written.

**Acceptance:** Every Premium feature is reachable for testing via the debug toggle; the paywall
never blocks a free-tier task; no feature is silently unavailable.

---

### Phase 10 — Premium Analyses ⬜ not started
**Effort:** ~3–4 days · Blocked on Phase 9's `PremiumGate`.

- [ ] **Fertility window** — from `CycleCalculator`'s fertile-window output, presented as a range with an explicit
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

### Phase 11 — Firebase Chat ⬜ not started (local stub in place)
**Effort:** ~5–7 days

> Pre-work already on `main`: `domain/repository/Repositories.kt` declares `ChatRepository`;
> `data/repo/ChatRepositoryImpl.kt` is a **local Room-backed stub** over a `chat_messages` entity;
> `ui/screens/ChatScreen.kt` is a placeholder; `BuildConfig.FIREBASE_CHAT = false` gates the real
> backend; `ChatMessage` already carries a coarse `phaseBucket` (§7.4 shape is respected).
> **The `chat_messages` table must be dropped the day Firestore lands** — chat must never sit in the
> encrypted DB (§7.4). Note also that `seedChatFor()` was removed: the app shows an honest empty
> room rather than seeded fake messages.

- [ ] Firebase project setup; `google-services.json` gitignored; per-build-type config.
- [ ] Anonymous Auth — sign in silently on first launch; no email, no phone, no PII.
- [ ] `ChatRepository` interface in `domain/` ✅ + Firestore implementation in `data/remote/`.
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

### Phase 12 — Polish, Testing & Release ⬜ not started
**Effort:** ~4–6 days · **Also carries the Phase 2 accessibility audit and the Phase 8 content copy review.**

- [ ] Full unit + instrumentation test pass; CycleCalculator coverage ≥ 90%.
      (Unit side: 33 tests exist. Instrumentation side: 0 tests exist.)
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
| **R6** | **Dynamic color fights the brand.** Material You extraction produces arbitrary colors and can wreck phase-specific color coding, which carries real meaning here. ✅ **Mitigated:** the template had `dynamicColor = true`; it is now `false` in `ui/theme/Theme.kt`. | Medium | Keep `dynamicColor = false`. Offer it as an opt-in accessibility setting later — but only if it does not silently override phase colours. |
| **R7** | **Single-module growth.** ~9 feature packages + domain + data can get unwieldy. | Medium | Deliberate for v1 — Gradle module splits cost real time with one developer. Enforce boundaries with package conventions and ArchUnit-style tests. Split only when build times actually hurt. |
| **R8** | **Chat + encryption mismatch.** Encrypted local DB + unencrypted server chat is a contradiction users may not understand. | Medium | Onboarding must state plainly what is on-device vs. on-server. Never store chat in the encrypted DB. |
| **R9** | **Prediction overreach.** Confident predictions on irregular cycles are medically misleading. | Medium | Confidence rubric (§8.5), suppression flags for contraception/perimenopause/postpartum, hedged language everywhere. |
| **R10** | **Scope.** This README is a 6–8 week build. Premature monetization, chat, and astrology all compete for the same time. | Medium | Phases are ordered so a useful app exists after Phase 6. Everything after is additive and independently shippable. |

---

## 12. Open Questions — Need Your Input

| # | Question | Blocks | Status / default if unanswered |
|---|---|---|---|
| Q1 | Real package ID + app display name? | Phase 0 | ✅ **Resolved:** `com.bloomcycle.app` / "Bloom" |
| Q2 | Where does the content research (README lines 6, 13) come from — will you source it, or should I compile a starting set from public health bodies (ACOG/NHS/etc.)? | Phase 8 | ⚠️ **Default taken:** compiled a cited starter set (NHS, NICE NG92) in `ContentLibrary.kt`. Some items still have `sourceUrl = null` — review in Phase 12. |
| Q3 | Is the chat global-only, or do you want private/local rooms between people who know each other? Changes the Firestore schema. | Phase 11 | Default if unanswered: phase-matched global rooms only |
| Q4 | Reporting/moderation: is there anyone who can actually review reported messages, or does it need to be automated-only for v1? | Phase 11 | Default if unanswered: automated-only, with reports stored for later review |
| Q5 | Is "Premium" ever going to be subscription, one-time unlock, or both? | Phase 9 | Default if unanswered: one-time unlock per feature |
| Q6 | Do you need Health Connect integration (syncing to Google Fit / Apple Health) at any point? Significant additional work and policy surface. | Post-v1 | Default if unanswered: out of scope |
| Q7 | Tablet/foldable support required for v1, or phone-only? | Phase 2 | ⚠️ **Default taken:** phone-first, adaptive `NavigationSuiteScaffold` already handles tablet/foldable widths. Confirm before Phase 12. |
| Q8 | Localization — is this English-only at launch? | Phase 12 | Default if unanswered: English-only, strings already externalized |
| **Q9** | **Do you have an Android device or emulator available?** Phase 4's migration/reopen tests, Phase 8's reminder test, and Phase 12's TalkBack audit all need one. | Phase 4, 8, 12 | ✅ **Resolved:** AVD `Pixel_9` (API 16) on `emulator-5554`; Phase 4's 4 tests ran green on it. Phase 8's policy is unit-tested; its manual firing test is deferred to Phase 12 alongside the TalkBack audit. |
| **Q10** | **Provide `google-services.json` when ready for real chat.** Until then `BuildConfig.FIREBASE_CHAT = false` and the local Room stub stands. | Phase 11 | Firebase work is deferred, not abandoned |

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

## 14. Where We Are & Next Actions

**Position:** Phases 0–8 merged to `main`. Build green, **104/104 unit tests + 4/4 instrumentation
tests**, schema baseline committed, installable APK at
`app\build\outputs\apk\debug\app-debug.apk` (see `README.md`).

**Next, in order:**

1. **Phase 9 — entitlements & paywall:** billing-backed unlock path (the UI gate itself exists:
   `PremiumGate.kt`, `PremiumTeaser`, per-feature `isUnlocked`/`unlock`).
2. Then 10 → 11 → 12, in that order.
3. Still open across phases: `google-services.json` (Q10 → Phase 11), Phase 0's Firebase/AGP 9
   verification, a review of `ContentLibrary.kt` citations (Q2 → Phase 12), and Phase 8's deferred
   on-device firing test (Q9).

**Branch/PR discipline:** one branch per phase, never push to `main`, commit messages **4–15 words**.
