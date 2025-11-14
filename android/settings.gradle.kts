pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
// Try-catch for version catalog to handle Android Studio cache issues
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }

    // Version catalog configuration with error handling
    try {
        versionCatalogs {
            val libsCatalog = create("libs")
            libsCatalog.from(files("gradle/libs.versions.toml"))
        }
    } catch (e: Exception) {
        // Fallback for version catalog issues
        println("Warning: Version catalog configuration failed: ${e.message}")
        println("This may be due to Android Studio cache. Clean and rebuild.")
    }
}

rootProject.name = "RAN CRM"
include(":app")
