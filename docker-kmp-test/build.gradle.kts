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
        // Override ENTRYPOINT to keep container alive, then exec the app
        val container = org.testcontainers.containers.GenericContainer("crowdproj/test-jvm:latest")
        container.withCreateContainerCmdModifier { cmd ->
            cmd.withEntrypoint("tail", "-f", "/dev/null")
        }
        container.start()
        try {
            val exec = container.execInContainer("java", "-jar", "/app/app.jar")
            if (exec.exitCode != 0) {
                throw GradleException("JVM image: exit ${exec.exitCode}, stderr: ${exec.stderr}")
            }
            if (!exec.stdout.contains("Hello from crowdproj-docker!")) {
                throw GradleException("JVM image: expected output not found in stdout:\n${exec.stdout}")
            }
        } finally {
            container.stop()
        }
    }
}

tasks.register("verifyNativeImage") {
    dependsOn("dockerBuildnativeapp")
    doLast {
        val container = org.testcontainers.containers.GenericContainer("crowdproj/test-native:latest")
        container.withCreateContainerCmdModifier { cmd ->
            cmd.withEntrypoint("tail", "-f", "/dev/null")
        }
        container.start()
        try {
            val exec = container.execInContainer("./docker-kmp-test.kexe")
            if (exec.exitCode != 0) {
                throw GradleException("Native image: exit ${exec.exitCode}, stderr: ${exec.stderr}")
            }
            if (!exec.stdout.contains("Hello from crowdproj-docker!")) {
                throw GradleException("Native image: expected output not found in stdout:\n${exec.stdout}")
            }
        } finally {
            container.stop()
        }
    }
}

tasks.named("jvmTest") { dependsOn("verifyJvmImage") }
tasks.named("linuxX64Test") { dependsOn("verifyNativeImage") }
