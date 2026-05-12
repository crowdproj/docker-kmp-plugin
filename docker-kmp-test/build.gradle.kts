plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.crowdproj.plugins.docker")
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.testcontainers)
    }
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
    jvm { }
    linuxX64 {
        binaries.executable()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

    }
}

docker {
    imageJvm("jvm-app") {
        imageName = "crowdproj/test-jvm"
        dockerFile = file("Dockerfile.jvm").absolutePath
        buildContext = layout.buildDirectory.dir("docker-jvm").get().asFile.path
        mainClass = "MainKt"
    }
    imageNative("native-app") {
        imageName = "crowdproj/test-native"
        dockerFile = file("Dockerfile.native").absolutePath
        buildContext = layout.buildDirectory.dir("docker-native").get().asFile.path
    }
}

tasks.register("verifyJvmImage") {
    dependsOn("dockerBuildjvmapp")
    doLast {
        val container = org.testcontainers.containers.GenericContainer("crowdproj/test-jvm:latest")
        container.start()
        Thread.sleep(3000)
        val logs = container.logs
        container.stop()
        if (!logs.contains("Hello from crowdproj-docker!")) {
            throw GradleException("JVM image: expected output not found in logs:\n$logs")
        }
    }
}

tasks.register("verifyNativeImage") {
    dependsOn("dockerBuildnativeapp")
    doLast {
        val container = org.testcontainers.containers.GenericContainer("crowdproj/test-native:latest")
        container.start()
        Thread.sleep(3000)
        val logs = container.logs
        container.stop()
        if (!logs.contains("Hello from crowdproj-docker!")) {
            throw GradleException("Native image: expected output not found in logs:\n$logs")
        }

        val resContainer = org.testcontainers.containers.GenericContainer("crowdproj/test-native:latest")
        resContainer.withCreateContainerCmdModifier { cmd ->
            cmd.withEntrypoint("tail", "-f", "/dev/null")
        }
        resContainer.start()
        try {
            val exec = resContainer.execInContainer("cat", "config.properties")
            if (exec.exitCode != 0 || !exec.stdout.contains("app.name=crowdproj-docker")) {
                throw GradleException("Native image: resource config.properties not found\nstdout:${exec.stdout}\nstderr:${exec.stderr}")
            }
        } finally {
            resContainer.stop()
        }
    }
}

tasks.named("jvmTest") { dependsOn("verifyJvmImage") }
tasks.named("linuxX64Test") { dependsOn("verifyNativeImage") }
