pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // libadb-android + sun-security-android worden via JitPack gepubliceerd (beslispunt 2, herzien).
        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "Android2AndroidMirror"
include(":app")
