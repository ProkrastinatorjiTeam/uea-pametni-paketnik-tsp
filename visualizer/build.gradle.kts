import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.7.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}
dependencies {
    implementation(project(":core"))

    // Odvisnosti za Compose Desktop
    implementation(compose.desktop.currentOs)

    implementation("org.jetbrains.compose.material3:material3-desktop:1.6.10")
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

compose.desktop {
    application {
        mainClass = "com.prokrastinatorji.visualizer.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TSPVisualizer"
            packageVersion = "1.0.0"
        }
    }
}
