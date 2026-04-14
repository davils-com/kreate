# Project Configuration

The `project` block provides settings for common project metadata, versioning, and build-time constants.

## Metadata

Set your project's name, description, and group within the `project` block.

```kotlin
kreate {
    project {
        name = "MyProject"
        description = "A great project"
        projectGroup = "com.example"
    }
}
```

## Versioning

Kreate can automatically manage your project's version based on environment variables (useful for CI/CD) or Gradle properties.

```kotlin
kreate {
    project {
        version {
            // Environment variable to check for the version (e.g., Git tag)
            environment = "CI_COMMIT_TAG"
            
            // Fallback Gradle property (e.g., from gradle.properties)
            property = "version"
        }
    }
}
```

## Build Constants

Easily generate a Kotlin class containing project metadata or custom constants that are available at compile-time.

```kotlin
kreate {
    project {
        buildConstant {
            enabled = true
            className = "ProjectBuildInfo"
            path = "com/example/generated"
            
            // Add custom constants
            constant("api_version", "1.0.0")
            constant("build_id", 42)
        }
    }
}
```

### Automatically Included Constants

By default, the following constants are included:
- `PROJECT_NAME`: The name of the project.
- `PROJECT_VERSION`: The current version of the project.
- `PROJECT_GROUP`: The project's group.

## Examples

Refer to the **[Getting Started](getting-started.md)** guide for a complete example of a `project` block.
