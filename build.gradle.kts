// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
    // Declared here so :app can apply it conditionally — only when google-services.json exists
    // (plan.md §5.3). Without the file the build must still work: chat falls back to the
    // in-memory repository.
    alias(libs.plugins.google.services) apply false
}
