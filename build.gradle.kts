// Root build file. Plugin versions are declared here and applied in modules.
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    // Annotation processing for Room (version must track the Kotlin one above).
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
}
