plugins {
    id("java")
    // Kotlin 2.1 because we are living in the future and i never tried the new one..
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    // The NEW plugin. The old "org.jetbrains.intellij" is dead. RIP.
    id("org.jetbrains.intellij.platform") version "2.10.4"
}

group = "com.hackathon"
version = "0.5-BETA"

repositories {
    mavenCentral()
    // New requirement: IntelliJ Platform repositories are now explicit
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // 3 AM NOTE: If this conflicts, delete .gradle folder and pray.
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    intellijPlatform {
        // "IC" (Community) is gone in 2025.3. It's all "intellijIdea" now.
        // We target the exact version i have.
        intellijIdea("2025.3.2")

        // Essential plugins. Without "java", we are just editing fancy text files.
        bundledPlugin("com.intellij.java")
    }
}

kotlin {
    // Java 21 is required for 2025.3+
    jvmToolchain(21)
}

tasks {
    patchPluginXml {
        sinceBuild.set("253") // Matches the test 2025.3 build
        untilBuild.set("253.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
    buildSearchableOptions {
        enabled = false
    }
}

// 4 AM NOTE: If it says "Unresolved Reference",
// you probably forgot to hit the little elephant icon (Load Gradle Changes).