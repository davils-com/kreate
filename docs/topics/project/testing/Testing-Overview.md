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

Kreate uses the JUnit Platform as the test engine for JVM targets. No additional `useJUnitPlatform()`
call is needed — Kreate wires this automatically on all `Test` tasks when the Kotlin JVM plugin
is present.

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

## Next Steps

- **[Configuration Reference](Testing-Configuration-Reference.md)**: Detailed DSL options for parallelism and logging
- **[Kotlin Multiplatform Testing](Testing-Multiplatfrom.md)**: Platform-specific considerations for KMP
- **[Examples](Testing-Example.md)**: Practical testing scenarios and configurations