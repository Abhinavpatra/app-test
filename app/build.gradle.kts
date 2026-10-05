plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

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

        buildConfigField("boolean", "FIREBASE_CHAT", "false")
        // Selects the billing-backed entitlement repository. v1 is false: no billing SDK
        // ships until the store release (plan.md Phase 9), so the DataStore-backed
        // implementation stands in behind the same interface.
        buildConfigField("boolean", "BILLING", "false")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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
