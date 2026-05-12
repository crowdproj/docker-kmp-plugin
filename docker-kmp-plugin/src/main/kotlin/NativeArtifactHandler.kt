package com.crowdproj.plugins.docker

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class NativeArtifactHandler : ArtifactHandler {
    override fun onBeforeBuild(project: Project, ext: DockerImageExtension) {}

    override fun resolveDependencies(project: Project, ext: DockerImageExtension): List<Any> {
        val deps = mutableListOf<Any>()
        for (t in project.tasks.matching { it.name.matches(Regex("linkRelease.*Executable.*")) }) {
            deps.add(t)
        }
        val kotlinExt = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
        if (kotlinExt != null) {
            for (target in kotlinExt.targets) {
                if (!target.name.startsWith("jvm") && !target.name.startsWith("js") && !target.name.startsWith("common")) {
                    val t = project.tasks.findByName("${target.name}ProcessResources")
                    if (t != null) deps.add(t)
                }
            }
            val common = project.tasks.findByName("commonProcessResources")
            if (common != null) deps.add(common)
        }
        return deps
    }

    override fun resolveArtifacts(project: Project, ext: DockerImageExtension): List<Any> {
        val files = mutableListOf<Any>()
        for (t in project.tasks.matching { it.name.matches(Regex("linkRelease.*Executable.*")) }) {
            for (f in t.outputs.files) files.add(f)
        }
        val kotlinExt = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
        if (kotlinExt != null) {
            for (target in kotlinExt.targets) {
                if (!target.name.startsWith("jvm") && !target.name.startsWith("js") && !target.name.startsWith("common")) {
                    val t = project.tasks.findByName("${target.name}ProcessResources")
                    if (t != null) for (f in t.outputs.files) files.add(f)
                }
            }
            val common = project.tasks.findByName("commonProcessResources")
            if (common != null) for (f in common.outputs.files) files.add(f)
        }
        return files
    }
}
