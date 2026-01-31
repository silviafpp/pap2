pluginManagement {
    repositories {
        google()          // 👈 REQUIRED
        mavenCentral()
        gradlePluginPortal()
    }
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
