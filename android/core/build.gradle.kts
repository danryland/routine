import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Platform-independent model, storage and legacy import. Plain Kotlin/JVM so
// its tests run fast without an emulator: ./gradlew :core:test
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

tasks.test {
    // Shared with the iOS tests, at the repository root.
    systemProperty("fixturesDir", file("../../fixtures").absolutePath)
}
