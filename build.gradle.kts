group = "com.crowdproj.docker"
version = libs.versions.crowdproj.docker.get()

repositories {
    mavenCentral()
}

tasks.register("test") {
    group = "verification"
    description = "Runs all tests including Docker image verification."
    dependsOn(":docker-kmp-test:check")
}

tasks.register("deploy") {
    dependsOn("test")
    dependsOn(gradle.includedBuild("docker-kmp-plugin").task(":deploy"))
}
