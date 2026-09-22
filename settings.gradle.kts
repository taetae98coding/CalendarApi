rootProject.name = "CalendarApi"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }

    versionCatalogs {
        create("ktorLibs") {
            from("io.ktor:ktor-version-catalog:3.4.0")
        }
    }
}
