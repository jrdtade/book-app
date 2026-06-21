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
        // Mihon's own injekt fork (com.github.mihonapp:injekt) is only published here,
        // not to Maven Central — same dependency the real Mihon app builds against.
        maven(url = "https://www.jitpack.io")
    }
}

rootProject.name = "Folio"
include(":app")
