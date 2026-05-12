# Docker KMP — Gradle plugin for Docker image building

A Gradle plugin for building Docker images with automatic artifact discovery and copying for JVM and Kotlin Multiplatform Native projects.

## Features

- Define multiple Docker images in a single project
- **Automatic JVM support**: detects `java` plugin, applies Shadow, copies fat JAR
- **Automatic Native support**: detects Kotlin Native binaries, copies executables and resources
- Configurable build args, caching, and cleanup

## Usage

Apply the plugin:

```kotlin
plugins {
    id("com.crowdproj.plugins.docker")
}
```

Configure images:

```kotlin
docker {
    images {
        register("my-service") {
            imageName = "mycompany/my-service"
            dockerFile = "Dockerfile"
            imageTag = project.version.toString()
        }
    }
}
```

Build all images:

```bash
./gradlew dockerBuildMyservice
```

### JVM projects

The plugin automatically applies the Shadow plugin and copies the fat JAR before building the Docker image.

```kotlin
plugins {
    id("com.crowdproj.plugins.docker")
    kotlin("jvm")
}

docker {
    images {
        register("jvm-app") {
            imageName = "mycompany/jvm-app"
            dockerFile = "Dockerfile.jvm"
        }
    }
}
```

### Kotlin Multiplatform Native projects

The plugin detects native link tasks and copies the binary and resources.

```kotlin
plugins {
    id("com.crowdproj.plugins.docker")
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
}

docker {
    images {
        register("native-app") {
            imageName = "mycompany/native-app"
            dockerFile = "Dockerfile.native"
        }
    }
}
```

## Extension properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `imageName` | String? | project.name | Docker image name |
| `dockerFile` | String | `Dockerfile` | Path to Dockerfile |
| `imageTag` | String | `latest` | Image tag |
| `buildContext` | String | `./` | Docker build context |
| `buildArgs` | Map<String,String> | `{}` | Build arguments |
| `noCache` | Boolean | `false` | Disable Docker cache |
| `removeIntermediateContainers` | Boolean | `false` | Clean up intermediate containers |
| `copyArtifacts` | Boolean | `true` | Enable automatic artifact copying |
| `artifactTargetDir` | String | `.` | Target directory for artifacts |
| `dependsOnTask` | String? | null | Task dependency |

## Requirements

- JDK 17+
- Docker daemon (for building images)
- Gradle 8.11+

## License

Copyright 2023-2026 CrowdProj team

Licensed under the Apache License, Version 2.0.
