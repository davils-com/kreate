# Getting Started

This guide walks you through adding Kreate to an existing Gradle project and configuring it for your use case.

## Prerequisites

Before applying the plugin, make sure the following are available in your environment:

- **Gradle**: 9.4.1 or later
- **Kotlin**: 2.3.21 or later
- **Java**: JDK 25 or later

<tip>
Kreate automatically synchronizes Java and Kotlin toolchain versions across all modules.
You do not need to configure these manually in each subproject.
</tip>

## Apply the Plugin

### Using Version Catalogs (Recommended)

Add the plugin entry to `gradle/libs.versions.toml`:

```toml
[plugins]
kreate = { id = "com.davils.kreate", version = "1.1.0" }
```

Then apply it in your module's `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kreate)
    kotlin("jvm") version "2.3.21"
}
```

### Direct Application

```kotlin
plugins {
    id("com.davils.kreate") version "1.1.0"
}
```

<note>
When using the plugin in a multiplatform project, replace <code>kotlin("jvm")</code> with <code>kotlin("multiplatform")</code>. Kreate detects the project type automatically
and applies the appropriate defaults.
</note>

## Basic Configuration

All configuration lives inside a single `kreate { }` block in your `build.gradle.kts`.
Only configure what you need — everything else falls back to sensible defaults.

```kotlin
import java.time.Year

group = "com.example"

kreate {
    platform {
        javaVersion = JavaVersion.VERSION_21
        explicitApi = true
        allWarningsAsErrors = true
    }

    project {
        name = "MyAwesomeProject"
        description = "A multi-module project using Kreate"
        projectGroup = group.toString()

        version {
            environment = "CI_COMMIT_TAG"
            property = "version"
        }
    }
}
```

## Project Structure

A typical project layout when using Kreate looks like this:

```text
.
├── build.gradle.kts            # Root build file
├── settings.gradle.kts         # Gradle settings
├── gradle/
│   └── libs.versions.toml      # Version catalog
├── my-module/
│   ├── build.gradle.kts        # Module configuration with kreate { }
│   ├── src/                    # Kotlin source code
│   ├── jni/                    # C/C++ sources (JNI, optional)
│   └── cinterop/               # Rust sources (C-Interop, optional)
└── gradle.properties
```

<tip>
The <code>jni/</code> and <code>cinterop/</code> directories are only required when native
integration is enabled. Kreate can scaffold both automatically via built-in Gradle tasks.
</tip>

## Feature Flags

Every major feature in Kreate is explicitly opt-in via an `enabled` flag.
The table below shows the available top-level feature blocks and their defaults:

| Block                             | Property              | Default | Description                                 |
|-----------------------------------|-----------------------|---------|---------------------------------------------|
| `platform`                        | `explicitApi`         | `false` | Enforces Kotlin Explicit API mode           |
| `platform`                        | `allWarningsAsErrors` | `true`  | Treats all compiler warnings as errors      |
| `platform.jvm.jni`                | `enabled`             | `false` | Enables JNI/CMake native integration        |
| `platform.multiplatform.cInterop` | `enabled`             | `false` | Enables Rust C-Interop bridge               |
| `project.buildConstant`           | `enabled`             | `false` | Generates type-safe Kotlin build constants  |
| `project.docs`                    | `enabled`             | `false` | Configures Dokka documentation generation   |
| `project.tests`                   | `enabled`             | `true`  | Configures Kotest integration and reporting |
| `project.publish`                 | `enabled`             | `false` | Enables Maven Central / GitLab publishing   |

## Next Steps

Once the plugin is applied and the basic configuration is in place, explore the following topics:

- **[Platform Configuration](Platform-Java-Version.md)**: Configure Java versions, `explicitApi`, and Kotlin Multiplatform targets (Linux, macOS, Windows).
- **[C-Interop Overview](C-Interoperation-Overview.md)**: Integrate Rust libraries into KMP projects via `cinterop` and `cargo`.
- **[JNI Support](JNI-Support.md)**: Bridge C/C++ code into JVM projects using CMake-based native builds.
- **[Build Constants](Constants-Overview.md)**: Generate type-safe Kotlin constants from Gradle properties at compile time.
- **[Testing Overview](Testing-Overview.md)**: Configure Kotest parallel execution, test logging, reporting, and Dokka.
- **[Publishing Overview](Publishing-Overview.md)**: Set up automated GPG-signed publishing to Maven Central or GitLab Package Registry.