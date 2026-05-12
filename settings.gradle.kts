rootProject.name = "crowdproj-docker"

pluginManagement {
    includeBuild("docker-kmp-plugin")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.gradleup.shadow") version "9.2.2"
    }
}
includeBuild("docker-kmp-plugin")
include("docker-kmp-test")
