package com.crowdproj.plugins.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import kotlin.reflect.KClass

@Suppress("unused")
class DockerPlugin : Plugin<Project> {
    private val handlerMap: Map<KClass<out DockerImageExtension>, ArtifactHandler> = mapOf(
        JvmDockerImageExtension::class to JvmArtifactHandler(),
        NativeDockerImageExtension::class to NativeArtifactHandler()
    )

    override fun apply(project: Project) {
        val extension = project.extensions.create("docker", DockerExtension::class.java)

        project.plugins.apply("com.gradleup.shadow")

        project.afterEvaluate {
            for (dockerImageName in extension.images.registeredNames) {
                val ext = extension.images.images[dockerImageName]!!
                val handler = handlerMap[ext::class]
                    ?: error("No handler for ${ext::class.simpleName}")
                val suffix = dockerImageName.replace(Regex("[^A-Za-z0-9]+"), "")
                val taskName = "dockerBuild$suffix"

                val imgName = ext.imageName.takeIf { it?.isNotBlank() ?: false } ?: project.name

                if (ext.buildContext == null) {
                    ext.buildContext = project.layout.buildDirectory
                        .dir("docker-$dockerImageName")
                        .get()
                        .asFile
                        .path
                }

                handler.onBeforeBuild(project, ext)

                val copyTask = registerCopyArtifactsTask(
                    project, "dockerCopyArtifacts$suffix", dockerImageName, ext, handler
                )

                project.tasks.register(taskName, DockerBuildTask::class.java) {
                    dockerFile.set(ext.dockerFile)
                    imageName.set(imgName.lowercase())
                    imageTag.set(ext.imageTag)
                    buildContext.set(ext.buildContext)
                    noCache.set(ext.noCache)
                    removeIntermediateContainers.set(ext.removeIntermediateContainers)
                    buildArgs.set(ext.buildArgs)

                    dependsOn(copyTask)
                    ext.dependsOnTask?.let { dependsOn(project.tasks.named(it)) }
                }
            }
        }
    }

    private fun registerCopyArtifactsTask(
        project: Project,
        taskName: String,
        imageName: String,
        ext: DockerImageExtension,
        handler: ArtifactHandler
    ): TaskProvider<Copy> {
        return project.tasks.register(taskName, Copy::class.java) {
            group = "docker"
            description = "Copies build artifacts for Docker image: $imageName"

            for (dep in handler.resolveDependencies(project, ext)) {
                dependsOn(dep)
            }
            from(project.provider { handler.resolveArtifacts(project, ext) })
            from(project.file(ext.dockerFile))

            duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE
            into(ext.buildContext!!)
        }
    }
}
