package com.crowdproj.plugins.docker

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class DockerPluginIntegrationTest {

    private fun isDockerAvailable(): Boolean {
        return try {
            ProcessBuilder("docker", "info")
                .redirectErrorStream(true)
                .start()
                .waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: Exception) {
            false
        }
    }

    @Test
    fun `docker build for JVM project succeeds`(@TempDir tempDir: Path) {
        assumeTrue(isDockerAvailable(), "Docker is not available")

        val testDir = tempDir.toFile()
        val buildFile = File(testDir, "build.gradle.kts")
        buildFile.writeText("""
            plugins {
                id("java")
                id("com.gradleup.shadow") version "9.2.2"
                id("com.crowdproj.plugins.docker")
            }
            repositories { mavenCentral() }
            group = "test"
            version = "1.0"
            tasks.named("shadowJar") {
                (this as org.gradle.jvm.tasks.Jar).manifest.attributes["Main-Class"] = "Main"
            }
            docker {
                imageJvm("app") {
                    imageName = "crowdproj/itest-jvm"
                    dockerFile = "Dockerfile"
                }
            }
        """.trimIndent())

        val srcDir = File(testDir, "src/main/java")
        srcDir.mkdirs()
        File(srcDir, "Main.java").writeText("""
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello from integration test!");
                }
            }
        """.trimIndent())

        File(testDir, "Dockerfile").writeText("""
            FROM eclipse-temurin:21-jre-alpine
            WORKDIR /app
            COPY *.jar app.jar
            ENTRYPOINT ["java", "-jar", "app.jar"]
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(testDir)
            .withPluginClasspath()
            .withArguments("dockerBuildapp", "--info")
            .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))

        val runOutput = ProcessBuilder("docker", "run", "--rm", "crowdproj/itest-jvm:latest")
            .redirectErrorStream(true)
            .start()
            .inputStream.bufferedReader().readText()
            .trim()

        assertTrue(runOutput.contains("Hello from integration test!"))
    }
}
