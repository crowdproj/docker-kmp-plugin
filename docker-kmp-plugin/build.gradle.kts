plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    alias(libs.plugins.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc)
}

group = "com.crowdproj.docker"
version = libs.versions.crowdproj.docker.get()

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.shadow.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)

    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

gradlePlugin {
    website.set("https://github.com/crowdproj/docker-kmp")
    vcsUrl.set("https://github.com/crowdproj/docker-kmp.git")
    plugins {
        register("com.crowdproj.plugins.docker") {
            id = "com.crowdproj.plugins.docker"
            displayName = "CrowdProj Docker build"
            description = "Gradle plugin for building Docker images with automatic artifact detection and copying for JVM and Native projects"
            @Suppress("UnstableApiUsage")
            tags.set(listOf("docker", "crowdproj", "kotlin", "multiplatform", "jvm", "native"))
            implementationClass = "com.crowdproj.plugins.docker.DockerPlugin"
            version = project.version
        }
    }
}

dokka {
    dokkaPublications.javadoc {
        outputDirectory.set(layout.buildDirectory.dir("dokka/javadoc"))
    }
}

val dokkaJavadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    group = "publishing"
    dependsOn("dokkaGeneratePublicationJavadoc")
    from(layout.buildDirectory.dir("dokka/javadoc"))
    archiveClassifier.set("javadoc")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        reports {
            junitXml.required.set(true)
        }
    }

    publishPlugins {
        dependsOn(build)
    }

    register("deploy") {
        group = "build"
        dependsOn(publishPlugins)
    }
}
