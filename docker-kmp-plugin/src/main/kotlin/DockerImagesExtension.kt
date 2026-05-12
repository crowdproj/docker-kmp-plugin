package com.crowdproj.plugins.docker

class DockerImagesExtension {
    private val _images = mutableMapOf<String, DockerImageExtension>()

    val images: Map<String, DockerImageExtension> get() = _images

    fun imageJvm(name: String, configure: JvmDockerImageExtension.() -> Unit = {}): JvmDockerImageExtension {
        val ext = JvmDockerImageExtension()
        _images[name] = ext
        ext.configure()
        return ext
    }

    fun imageNative(name: String, configure: NativeDockerImageExtension.() -> Unit = {}): NativeDockerImageExtension {
        val ext = NativeDockerImageExtension()
        _images[name] = ext
        ext.configure()
        return ext
    }

    val registeredNames: Set<String> get() = _images.keys
}
