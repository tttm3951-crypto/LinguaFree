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
    }
}

rootProject.name = "LinguaFree"

include(":app")
include(":core:domain", ":core:data", ":core:ui")
include(
    ":feature:onboarding",
    ":feature:path",
    ":feature:lesson",
    ":feature:practice",
    ":feature:profile",
    ":feature:leagues",
    ":feature:shop",
)
