package com.crowdproj.plugins.docker

sealed class DockerImageExtension {
    var imageName: String? = null
    var buildContext: String? = null
    var dockerFile = "Dockerfile"
    var imageTag = "latest"
    var dependsOnTask: String? = null
    var buildArgs: Map<String, String> = mapOf()
    var noCache = false
    var removeIntermediateContainers = false
}

class JvmDockerImageExtension : DockerImageExtension() {
    var mainClass: String? = null
}

class NativeDockerImageExtension : DockerImageExtension() {
    val resources = mutableListOf<Any>()
}
