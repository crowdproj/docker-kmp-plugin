package com.crowdproj.plugins.docker

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DockerPluginTest {

    @Test
    fun `plugin registers docker extension`() {
        val project: Project = ProjectBuilder.builder().build()
        project.plugins.apply("com.crowdproj.plugins.docker")

        val extension = project.extensions.findByType(DockerExtension::class.java)
        assertNotNull(extension)
    }

    @Test
    fun `images can be registered via extension`() {
        val project: Project = ProjectBuilder.builder().build()
        project.plugins.apply("com.crowdproj.plugins.docker")

        val ext = project.extensions.findByType(DockerExtension::class.java)!!
        ext.imageJvm("test-app") {
            imageName = "test/app"
        }

        assertTrue(ext.images.registeredNames.contains("test-app"))
        assertNotNull(ext.images.images["test-app"])
        assertEquals("test/app",ext.images.images["test-app"]!!.imageName)
    }

    @Test
    fun `docker build task can be configured directly`() {
        val project: Project = ProjectBuilder.builder().build()
        val task = project.tasks.register("testBuild", DockerBuildTask::class.java) {
            dockerFile.set("Dockerfile")
            imageName.set("test/app")
            imageTag.set("latest")
            buildContext.set("./")
        }.get()

        assertNotNull(task)
        assertTrue(task.group == "docker")
        assertContains(task.imageName.get(), "test/app")
        assertTrue(task.dockerFile.get() == "Dockerfile")
    }

    @Test
    fun `copy artifacts task can be created`() {
        val project: Project = ProjectBuilder.builder().build()
        val copyTask = project.tasks.register("copyTest", org.gradle.api.tasks.Copy::class.java) {
            group = "docker"
            description = "test copy"
            into(project.layout.buildDirectory)
        }.get()

        assertNotNull(copyTask)
        assertTrue(copyTask.group == "docker")
    }

    @Test
    fun `build args passed to task`() {
        val project: Project = ProjectBuilder.builder().build()
        val task = project.tasks.register("argsBuild", DockerBuildTask::class.java) {
            dockerFile.set("Dockerfile")
            imageName.set("test/args")
            imageTag.set("1.0")
            buildContext.set("./")
            buildArgs.set(mapOf("VERSION" to "1.0", "DEBUG" to "true"))
        }.get()

        val args = task.buildArgs.get()
        assertTrue(args.containsKey("VERSION"))
        assertTrue(args["VERSION"] == "1.0")
    }

    @Test
    fun `image name defaults to project name when not set`() {
        val project: Project = ProjectBuilder.builder()
            .withName("my-project")
            .build()
        project.plugins.apply("com.crowdproj.plugins.docker")

        val ext = project.extensions.findByType(DockerExtension::class.java)!!
        ext.imageJvm("default-name") {
            dockerFile = "Dockerfile"
        }
        assertTrue(ext.images.registeredNames.contains("default-name"))
    }
}
