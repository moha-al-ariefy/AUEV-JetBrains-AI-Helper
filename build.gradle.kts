plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.17.3"
    // I added this! This invokes the spirit of Compose.
    id("org.jetbrains.compose") version "1.5.11"
}

group = "com.hackathon"
version = "0.5b-BETA"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

intellij {
    version.set("2023.2.6")
    type.set("IC")
}

dependencies {
    // I need these to draw pretty rectangles
    implementation(compose.desktop.currentOs)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("255.*")
    }
}