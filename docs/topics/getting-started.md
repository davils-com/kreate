# Getting Started

This guide will help you get up and running with Kreate in your project.

## Prerequisites

- **Gradle**: Version 8.0 or later.
- **Kotlin**: Version 2.0.0 or later.
- **Java**: JDK 17 or later (configured for the Gradle runner).

## 1. Apply the Plugin

Add the Kreate plugin to your `build.gradle.kts` (or `plugins` block in `settings.gradle.kts` if you're using a version catalog).

### Using Version Catalogs (Recommended)

In `gradle/libs.versions.toml`:
```toml
[plugins]
kreate = { id = "com.davils.kreate", version = "1.0.0" }
```

In your project's `build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kreate)
}
```

### Direct Application

```kotlin
plugins {
    id("com.davils.kreate") version "1.0.0"
}
```

## 2. Basic Configuration

After applying the plugin, you can start configuring your project using the `kreate` block.

```kotlin
kreate {
    platform {
        javaVersion = JavaVersion.VERSION_21
    }

    project {
        name = "MyAwesomeProject"
        description = "A multi-module project using Kreate"
    }
}
```

## 3. Project Structure

A typical project structure with Kreate might look like this:

```text
.
├── build.gradle.kts        # Root build file
├── settings.gradle.kts     # Gradle settings
├── my-module/
│   ├── build.gradle.kts    # Module-specific configuration
│   └── src/                # Module source code
└── gradle/
    └── libs.versions.toml  # Version catalog
```

## Next Steps

Now that you've applied the plugin and set up a basic configuration, check out the following topics for more detailed information:

- **[Platform Configuration](platform-configuration.md)**: Configure Java versions and Kotlin Multiplatform targets.
- **[C-Interop with Rust](c-interop.md)**: Integrate Rust code into your multiplatform projects.
- **[Publishing](publishing.md)**: Set up automated publishing to GitLab or Maven Central.
- **[Testing and Documentation](testing-and-docs.md)**: Configure test logging, reporting, and Dokka.
