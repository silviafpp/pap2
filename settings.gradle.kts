pluginManagement {
    repositories {
        google()          // 👈 REQUIRED
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()          // 👈 REQUIRED (THIS WAS MISSING)
        mavenCentral()
    }
}

rootProject.name = "BusCardApp"
include(":app")
