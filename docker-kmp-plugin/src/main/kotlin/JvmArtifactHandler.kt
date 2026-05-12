package com.crowdproj.plugins.docker

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class JvmArtifactHandler : ArtifactHandler {
    override fun onBeforeBuild(project: Project, ext: DockerImageExtension) {
        val jvmExt = ext as JvmDockerImageExtension
        val mainClass = jvmExt.mainClass ?: return
        if (project.tasks.findByName("shadowJar") != null) {
            val sj = project.tasks.named("shadowJar", ShadowJar::class.java).get()
            sj.manifest.attributes(mapOf("Main-Class" to mainClass))
            return
        }
        val kotlinExt = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return
        val jvmTarget = kotlinExt.targets.findByName("jvm") ?: return
        val mainCompilation = jvmTarget.compilations.findByName("main") ?: return
        val runtimeConfig = project.configurations.findByName("jvmRuntimeClasspath") ?: return
        project.tasks.register("shadowJar", ShadowJar::class.java) {
            from(mainCompilation.output.allOutputs)
            configurations.set(listOf(runtimeConfig))
            manifest { attributes["Main-Class"] = mainClass }
        }
    }

    override fun resolveDependencies(project: Project, ext: DockerImageExtension): List<Any> {
        val task = project.tasks.findByName("shadowJar") ?: return emptyList()
        return listOf(task)
    }

    override fun resolveArtifacts(project: Project, ext: DockerImageExtension): List<Any> {
        val task = project.tasks.findByName("shadowJar") ?: return emptyList()
        return if (task is Jar) listOf(task.archiveFile) else emptyList()
    }
}
