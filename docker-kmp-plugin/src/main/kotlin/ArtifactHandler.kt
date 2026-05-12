package com.crowdproj.plugins.docker

import org.gradle.api.Project

interface ArtifactHandler {
    fun onBeforeBuild(project: Project, ext: DockerImageExtension)
    fun resolveDependencies(project: Project, ext: DockerImageExtension): List<Any>
    fun resolveArtifacts(project: Project, ext: DockerImageExtension): List<Any>
}
