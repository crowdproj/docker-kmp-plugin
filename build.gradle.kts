group = "com.crowdproj.docker"
version = libs.versions.crowdproj.docker.get()

repositories {
    mavenCentral()
}

tasks {
    register("deploy") {
        dependsOn(gradle.includedBuild("docker-kmp-plugin").task(":deploy"))
    }
}
