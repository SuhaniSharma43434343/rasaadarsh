// ── build.gradle.kts  (root / project level) ─────────────────

plugins {
    alias(libs.plugins.android.application)  apply false

    alias(libs.plugins.kotlin.compose)       apply false
    alias(libs.plugins.ksp)                  apply false
}