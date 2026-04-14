# Platform Configuration

Kreate simplifies the configuration of Java and Kotlin Multiplatform (KMP) targets.

## Java Configuration

The `platform` block allows you to set the target Java version and enforce standard Kotlin practices across your project.

```kotlin
kreate {
    platform {
        // Target Java version (default: JVM 17)
        javaVersion = JavaVersion.VERSION_21
        
        // Enforce Explicit API mode (default: true)
        explicitApi = true
        
        // Treat all warnings as errors (default: false)
        allWarningsAsErrors = false
    }
}
```

### Options

| Option | Description | Default |
|--------|-------------|---------|
| `javaVersion` | The target JVM version for compilation. | `JavaVersion.VERSION_17` |
| `explicitApi` | Whether to enable Kotlin's explicit API mode (requires visibility and return types for public API). | `true` |
| `allWarningsAsErrors` | If `true`, the build will fail on any compiler warnings. | `false` |

## Kotlin Multiplatform

Kreate provides a streamlined DSL for setting up KMP projects.

```kotlin
kreate {
    platform {
        multiplatform {
            // Enable Multiplatform support (default: false)
            enabled = true
        }
    }
}
```

### Targets

Kreate automatically configures common targets when Multiplatform is enabled. You can further customize targets by adding them to the `kotlin` block in your `build.gradle.kts`.

### Native Targets

Kreate supports configuring native targets (Linux, macOS, Windows) and automatically setting up C-Interop tasks (see **[C-Interop with Rust](c-interop.md)** for details).

## Advanced Customization

For configurations that aren't yet exposed via the `kreate` DSL, you can still use the standard Gradle and Kotlin DSLs alongside Kreate.

```kotlin
kreate {
    // Kreate configuration
}

kotlin {
    // Additional Kotlin-specific configuration
}
```
