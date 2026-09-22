pluginManagement {
    includeBuild("build-logic")

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    versionCatalogs {
        create("ktorLibs") {
            from("io.ktor:ktor-version-catalog:3.6.0")
        }
    }
}

rootProject.name = "CalendarApi"

include(":domain")
include(":datasource")
include(":data")
include(":app")
