pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "RapunzelCore"

include("shared", "paper", "velocity", "server-runner")
