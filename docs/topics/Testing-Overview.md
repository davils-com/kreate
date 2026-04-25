# Testing

The `tests { }` block inside `kreate { project { } }` provides a unified, opinionated testing
configuration for both Kotlin JVM and Kotlin Multiplatform projects. When enabled, Kreate
automatically configures all test tasks with sensible defaults for parallelism, timeouts,
failure handling, console logging, and report generation — without requiring manual `tasks.withType<Test>` boilerplate.

Testing is **disabled by default**. Enable it with:

```kotlin
kreate {
    project {
        tests {
            enabled.set(true)
        }
    }
}
```

> The `tests { }` block is evaluated inside `afterEvaluate`. All settings must be
> declared before the configuration phase ends.
>
{style="note"}

## Test Framework

Kreate's testing support is built around [Kotest](https://kotest.io/) as the test framework
and uses the JUnit Platform as the test engine for JVM targets. No additional `useJUnitPlatform()`
call is needed — Kreate wires this automatically on all `Test` tasks when the Kotlin JVM plugin
is present.

### Kotlin Multiplatform Requirements

In Kotlin Multiplatform projects, Kreate validates at configuration time that two plugins are
applied **by the user** before `tests { enabled.set(true) }` takes effect:

- The `ksp` plugin (`com.google.devtools.ksp`)
- The `kotest` plugin (`io.kotest.multiplatform`)

If either is missing, Kreate throws an `IllegalStateException` immediately during configuration.


A minimal multiplatform `build.gradle.kts` that satisfies this requirement:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.kreate)
}

kreate {
    project {
        tests {
            enabled.set(true)
        }
    }
}
```

For Kotlin JVM projects, no additional plugins are required — Kreate applies `useJUnitPlatform()`
directly on the `Test` task.

## What Kreate Configures

When `enabled` is `true`, Kreate applies the following to every matching test task:

| Setting                   | Applied to       | Description                                          |
|---------------------------|------------------|------------------------------------------------------|
| `useJUnitPlatform()`      | JVM `Test` tasks | Enables the JUnit Platform test engine               |
| `timeout`                 | All tasks        | Sets the per-task execution timeout                  |
| `ignoreFailures`          | All tasks        | Controls whether the build continues after failures  |
| `failOnNoDiscoveredTests` | All tasks        | Fails the build if no tests are found                |
| `outputs.upToDateWhen`    | All tasks        | Controls Gradle's incremental skip logic             |
| `maxParallelForks`        | JVM `Test` tasks | Sets parallel test process count                     |
| `testLogging { }`         | All tasks        | Configures console event output and exception format |
| `reports`                 | All tasks        | Enables or disables XML / HTML reports               |

For Kotlin Multiplatform projects, both `Test` tasks (JVM target) and `KotlinTest` tasks
(native/JS targets) are configured independently.