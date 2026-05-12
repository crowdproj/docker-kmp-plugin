package com.crowdproj.plugins.docker

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.work.DisableCachingByDefault
import java.io.File
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@DisableCachingByDefault(because = "Builds a Docker image — depends on external Docker daemon state")
abstract class DockerBuildTask : DefaultTask() {

    init {
        group = "docker"
    }

    @get:Input
    abstract val dockerFile: Property<String>

    @get:Input
    abstract val imageName: Property<String>

    @get:Input
    abstract val imageTag: Property<String>

    @get:Input
    abstract val buildContext: Property<String>

    @get:Input
    @get:Optional
    abstract val buildArgs: MapProperty<String, String>

    @get:Input
    @get:Optional
    abstract val noCache: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val removeIntermediateContainers: Property<Boolean>

    @get:Inject
    abstract val execOps: ExecOperations

    @TaskAction
    fun build() {
        val df = dockerFile.get()
        val dockerfilePath = if (File(df).isAbsolute) df else "${buildContext.get()}/$df"
        val fullImageName = "${imageName.get()}:${imageTag.get()}"
        logger.lifecycle("Building Docker image: $fullImageName")

        val command = mutableListOf<String>("docker", "build", "-t", fullImageName)
        command.add("-f"); command.add(dockerfilePath)
        if (noCache.getOrElse(false)) command.add("--no-cache")
        if (removeIntermediateContainers.getOrElse(false)) command.add("--rm")
        buildArgs.getOrElse(emptyMap()).forEach { (key, value) ->
            command.add("--build-arg"); command.add("$key=$value")
        }
        command.add(buildContext.get())

        logger.lifecycle("Executing: ${command.joinToString(" ")}")
        val outputStream = ByteArrayOutputStream()
        val execResult = execOps.exec {
            commandLine(command)
            standardOutput = outputStream
            errorOutput = outputStream
            isIgnoreExitValue = true
            workingDir = project.projectDir
        }
        if (execResult.exitValue != 0) {
            logger.error("Docker build failed:\n${outputStream}")
            throw StopExecutionException("Docker build failed with exit code ${execResult.exitValue}")
        }
        logger.lifecycle("Docker image built successfully: $fullImageName")
        logger.quiet(outputStream.toString())
    }
}
