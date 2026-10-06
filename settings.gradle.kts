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

rootProject.name = "MeatBush"
include(":app")
include(":core:common")
include(":core:data")
include(":core:designsystem")
include(":core:domain")
include(":core:model")
include(":core:navigation")
include(":feature:auth")
include(":feature:superadmin")
include(":feature:admin")
include(":feature:catalog")
include(":feature:customerhome")
include(":feature:cart")
include(":feature:address")
include(":feature:checkout")
include(":feature:orders")
