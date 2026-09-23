// Root project build.gradle.kts
// This file configures the overall project build settings

plugins {
    id("com.android.application") version "8.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.21" apply false
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
    id("androidx.room") version "2.6.1" apply false
    id("com.google.devtools.ksp") version "1.9.21-1.0.16" apply false
}
