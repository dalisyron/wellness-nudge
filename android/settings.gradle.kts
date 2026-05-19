pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // mimik public SDK repo — no auth required.
        // The mim-OE/mim-OE-SE-Android README documents this URL.
        maven { url = uri("https://s3-us-west-2.amazonaws.com/mimik-android-repo") }
    }
}

rootProject.name = "wellness-nudge"
include(":app")
