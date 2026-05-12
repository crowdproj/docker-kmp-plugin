# Docker KMP — Gradle plugin for Docker image building

A Gradle plugin for building Docker images with automatic artifact discovery and copying for JVM and Kotlin Multiplatform Native projects.

## Features

- Define multiple Docker images in a single project
- **JVM support**: registers ShadowJar, copies fat JAR — **`imageJvm("name")`**
- **Native support**: detects Kotlin Native link tasks, copies executables and resources — **`imageNative("name")`**
- Sealed type hierarchy: each image type knows what artifacts to copy
- Configurable build args, caching, and cleanup
- Supports Testcontainers CI verification

## Usage

Apply the plugin:

```kotlin
plugins {
    id("com.crowdproj.plugins.docker") version "<version>"
}
```

### JVM project

```kotlin
docker {
    imageJvm("my-service") {
        imageName = "mycompany/my-service"
        dockerFile = "Dockerfile"
        imageTag = project.version.toString()
        mainClass = "com.example.MainKt"
    }
}
```

Build: `./gradlew dockerBuildMyservice`

The plugin automatically applies the Shadow plugin, registers a `shadowJar` task if none exists, and copies the fat JAR into the build context.

### Kotlin Multiplatform Native project

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.crowdproj.plugins.docker")
}

kotlin {
    linuxX64 { binaries.executable() }
}

docker {
    imageNative("my-native") {
        imageName = "mycompany/my-native"
        dockerFile = "Dockerfile.native"
    }
}
```

Build: `./gradlew dockerBuildMynative`

The plugin detects native link tasks (`linkRelease*Executable*`), copies the binary and any listed resources into the build context.

## Extension properties (base)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `imageName` | String? | project.name | Docker image name |
| `dockerFile` | String | `Dockerfile` | Path to Dockerfile |
| `imageTag` | String | `latest` | Image tag |
| `buildContext` | String? | `build/docker-<name>` | Docker build context |
| `buildArgs` | Map<String,String> | `{}` | Build arguments |
| `noCache` | Boolean | `false` | Disable Docker cache |
| `removeIntermediateContainers` | Boolean | `false` | Clean up intermediate containers |
| `dependsOnTask` | String? | null | Task dependency |

### JVM extras (`imageJvm`)

| Property | Type | Description |
|----------|------|-------------|
| `mainClass` | String? | Main class for shadowJar manifest |

### Native extras (`imageNative`)

KMP resources from `src/commonMain/resources/` and native target resource directories are automatically discovered and copied into the build context.

## Requirements

- JDK 21+
- Docker daemon (for building images)
- Gradle 9.5+

## License

Copyright 2023-2026 CrowdProj team

Licensed under the Apache License, Version 2.0.
