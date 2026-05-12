package com.crowdproj.plugins.docker

open class DockerExtension {
    val images = DockerImagesExtension()

    fun imageJvm(name: String, configure: JvmDockerImageExtension.() -> Unit = {}): JvmDockerImageExtension {
        return images.imageJvm(name, configure)
    }

    fun imageNative(name: String, configure: NativeDockerImageExtension.() -> Unit = {}): NativeDockerImageExtension {
        return images.imageNative(name, configure)
    }
}
