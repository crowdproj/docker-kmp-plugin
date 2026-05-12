rootProject.name = "crowdproj-docker"

pluginManagement {
    includeBuild("docker-kmp-plugin")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
includeBuild("docker-kmp-plugin")
include("docker-kmp-test")
