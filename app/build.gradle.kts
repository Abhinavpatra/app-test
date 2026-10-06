plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
    alias(libs.plugins.jacoco)
}

import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.util.Properties

// The google-services plugin is on the classpath via the root `apply false` declaration
// but is only applied when the config file exists. Fresh clones without
// google-services.json (gitignored, plan.md §5.3) must still build: chat then falls
// back to the in-memory repository and FIREBASE_CHAT stays false.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Mirrors the plugin condition above: true exactly when the google-services plugin runs.
val hasFirebaseConfig = file("google-services.json").exists()

// Exports the schema JSON next to the code that owns it. Without this there is no
// baseline to migrate from, so any future entity change is an untested destructive
// migration (plan.md §6.3).
room3 {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.bloomcycle.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.bloomcycle.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // True exactly when the google-services plugin is applied above (config file
        // present). The Firestore chat implementation is chosen behind this flag in
        // AppContainer; without it the in-memory repository stands in (plan.md Phase 11).
        buildConfigField("boolean", "FIREBASE_CHAT", hasFirebaseConfig.toString())
        // Selects the billing-backed entitlement repository. v1 is false: no billing SDK
        // ships until the store release (plan.md Phase 9), so the DataStore-backed
        // implementation stands in behind the same interface.
        buildConfigField("boolean", "BILLING", "false")
    }

    // Release signing reads `keystore.properties` at the repo root (see the committed
    // `.example`). Absent file = unsigned release build that still compiles — including
    // through R8 — so CI and fresh clones always verify the release pipeline.
    signingConfigs {
        val keystorePropsFile = rootProject.file("keystore.properties")
        if (keystorePropsFile.exists()) {
            val keystoreProps = Properties()
                .apply { load(keystorePropsFile.inputStream()) }
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // room3-migration's generated serializers rely on typeParametersSerializers() being a
    // default method, which it only became in kotlinx-serialization 1.8. Something in the
    // graph pulls 1.7.3 in and pins it, and at 1.7.3 MigrationTestHelper dies with
    // AbstractMethodError the moment it deserializes the schema JSON.
    constraints {
        listOf("implementation", "testImplementation", "androidTestImplementation").forEach { cfg ->
            add(cfg, "org.jetbrains.kotlinx:kotlinx-serialization-core") {
                version { strictly(libs.versions.kotlinxSerialization.get()) }
            }
            add(cfg, "org.jetbrains.kotlinx:kotlinx-serialization-json") {
                version { strictly(libs.versions.kotlinxSerialization.get()) }
            }
        }
    }

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlcipher)

    // Firebase — Phase 11b. Versions ride the BoM; only the BoM is pinned
    // (gradle/libs.versions.toml). firebase-auth/firestore are enough for chat.
    // Both App Check providers are plain `implementation` (not split by variant)
    // because BloomApplication references both factories and `BuildConfig.DEBUG`
    // picks which one is installed at runtime.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.appcheck.playintegrity)

    ksp(libs.androidx.room3.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room3.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Phase 12 coverage gate: CycleCalculator line coverage must stay >= 90%.
// The report/verification tasks below run on the debug unit-test execution data;
// `./gradlew :app:jacocoCycleCalculatorCheck` fails the build when the gate breaks.
jacoco {
    toolVersion = libs.versions.jacoco.get()
}
tasks.withType<Test> {
    extensions.configure<JacocoTaskExtension> { isEnabled = true }
}

// Class output of the debug Kotlin compile. The path is owned by the Kotlin Gradle
// plugin's AGP integration; if a toolchain upgrade moves it, the report comes back
// empty and the check passes vacuously — so `jacocoCycleCalculatorReport` also
// asserts non-empty coverage (see the `doLast` below).
val cycleCalculatorClasses = fileTree(
    layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"),
) {
    include("com/bloomcycle/app/domain/cycle/CycleCalculator*.class")
}

tasks.register<JacocoReport>("jacocoCycleCalculatorReport") {
    group = "verification"
    description = "Line coverage report for CycleCalculator (Phase 12 gate)."
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(cycleCalculatorClasses)
    sourceDirectories.setFrom(layout.projectDirectory.dir("src/main/java"))
    executionData.setFrom(files(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    doLast {
        val covered = classDirectories.files.sumOf { dir ->
            dir.walkTopDown().count { it.extension == "class" }
        }
        require(covered > 0) {
            "No CycleCalculator classes found — the intermediates path above is stale."
        }
    }
}

tasks.register<JacocoCoverageVerification>("jacocoCycleCalculatorCheck") {
    group = "verification"
    description = "Fails when CycleCalculator line coverage drops below 90%."
    dependsOn("jacocoCycleCalculatorReport")
    classDirectories.setFrom(cycleCalculatorClasses)
    sourceDirectories.setFrom(layout.projectDirectory.dir("src/main/java"))
    executionData.setFrom(files(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}
