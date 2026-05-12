package com.crowdproj.plugins.docker

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class DockerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("docker", DockerExtension::class.java)

        project.plugins.apply("com.gradleup.shadow")

        project.afterEvaluate {
            for (dockerImageName in extension.images.registeredNames) {
                val ext = extension.images.images[dockerImageName]!!
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

                if (ext is JvmDockerImageExtension && ext.mainClass != null) {
                    ensureShadowJar(project, ext.mainClass!!)
                }

                val copyTask = registerCopyArtifactsTask(project, "dockerCopyArtifacts$suffix", dockerImageName, ext)

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

    private fun ensureShadowJar(project: Project, mainClass: String) {
        if (project.tasks.findByName("shadowJar") != null) {
            val sj = project.tasks.named("shadowJar", ShadowJar::class.java).get()
            sj.manifest.attributes(mapOf("Main-Class" to mainClass))
            return
        }

        val kotlinExtension = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return
        val jvmTarget = kotlinExtension.targets.findByName("jvm") ?: return
        val mainCompilation = jvmTarget.compilations.findByName("main") ?: return
        val runtimeConfig = project.configurations.findByName("jvmRuntimeClasspath") ?: return

        project.tasks.register("shadowJar", ShadowJar::class.java) {
            from(mainCompilation.output.allOutputs)
            configurations.set(listOf(runtimeConfig))
            manifest {
                attributes["Main-Class"] = mainClass
            }
        }
    }

    private fun registerCopyArtifactsTask(
        project: Project,
        taskName: String,
        imageName: String,
        ext: DockerImageExtension
    ): TaskProvider<Copy> {
        return project.tasks.register(taskName, Copy::class.java) {
            group = "docker"
            description = "Copies build artifacts for Docker image: $imageName"

            when (ext) {
                is JvmDockerImageExtension -> {
                    val shadowTask = project.tasks.findByName("shadowJar")
                    if (shadowTask != null) {
                        dependsOn(shadowTask)
                        from(project.provider {
                            if (shadowTask is Jar) project.files(shadowTask.archiveFile) else project.files()
                        })
                    }
                }
                is NativeDockerImageExtension -> {
                    val releaseLinkTasks = project.tasks.matching { task ->
                        task.name.matches(Regex("linkRelease.*Executable.*"))
                    }
                    dependsOn(releaseLinkTasks)
                    from(project.provider {
                        val files = mutableListOf<Any>()
                        for (lt in releaseLinkTasks) {
                            lt.outputs.files.forEach { files.add(it) }
                        }
                        files
                    })
                    for (res in ext.resources) {
                        from(project.file(res))
                    }
                }
            }

            from(project.file(ext.dockerFile))
            into(ext.buildContext!!)
        }
    }
}
