# Configuration Reference

## Top-Level Properties

| Property                  | Type                | Default                   | Description                                                             |
|---------------------------|---------------------|---------------------------|-------------------------------------------------------------------------|
| `enabled`                 | `Property<Boolean>` | `false`                   | Master switch — must be `true` for any test configuration to apply      |
| `maxParallelForks`        | `Property<Int>`     | `availableProcessors / 2` | Maximum number of parallel test worker processes for JVM tests          |
| `timeoutMinutes`          | `Property<Long>`    | `10`                      | Per-task test timeout in minutes                                        |
| `ignoreFailures`          | `Property<Boolean>` | `false`                   | When `true`, the build continues even if tests fail                     |
| `alwaysRunTests`          | `Property<Boolean>` | `false`                   | When `true`, disables Gradle's up-to-date check so tests always execute |
| `failOnNoDiscoveredTests` | `Property<Boolean>` | `false`                   | When `true`, fails the build if a test task finds no tests              |

### `maxParallelForks`

Controls how many separate JVM processes Gradle spawns to execute tests in parallel. The default
is `Runtime.getRuntime().availableProcessors() / 2`, which balances throughput against resource
usage on the build machine. Setting this to `1` disables parallelism and runs all tests in a
single process — useful for tests that share global state or external resources.

```kotlin
tests {
    enabled.set(true)
    maxParallelForks.set(4)
}
```

> `maxParallelForks` applies only to JVM `Test` tasks. Native and JS `KotlinTest` tasks
> do not support this property and are not affected.
>
{style="note"}

### `timeoutMinutes`

Sets a hard deadline for each test task. If the task has not completed within the configured
duration, Gradle cancels it and marks the build as failed. This prevents hung test suites from
blocking CI pipelines indefinitely.

```kotlin
tests {
    enabled.set(true)
    timeoutMinutes.set(5)
}
```

The timeout is applied via `Duration.ofMinutes(n)` to both JVM `Test` tasks and
Multiplatform `KotlinTest` tasks.

### `ignoreFailures`

When set to `true`, the build continues even if test assertions fail. Kreate writes this
directly to `task.ignoreFailures`. This is useful in scenarios where test results are
collected separately (e.g., a CI report step) and a failing test should not block a
downstream task like publishing.

```kotlin
tests {
    enabled.set(true)
    ignoreFailures.set(true)
}
```

### `alwaysRunTests`

By default, Gradle skips test tasks if their inputs and outputs have not changed since the last
run (incremental build). Setting `alwaysRunTests` to `true` disables this optimization by
returning `false` from `outputs.upToDateWhen`, forcing every invocation of `test` or `check`
to re-execute all tests.

```kotlin
tests {
    enabled.set(true)
    alwaysRunTests.set(true)
}
```

This is particularly useful in CI environments where a clean workspace is not always guaranteed,
or when tests depend on external state that Gradle cannot track as a task input.

### `failOnNoDiscoveredTests`

When `true`, any test task that starts but finds zero tests to run fails the build immediately.
This guards against misconfigured source sets or missing test class patterns that would otherwise
silently pass.

```kotlin
tests {
    enabled.set(true)
    failOnNoDiscoveredTests.set(true)
}
```

## Logging Configuration

The `logging { }` sub-block controls which test lifecycle events are printed to the Gradle
console during a build. Exception details are **always** shown regardless of these settings.

```kotlin
tests {
    enabled.set(true)
    logging {
        logPassedTests.set(true)
        logSkippedTests.set(true)
        logTestStarted.set(false)
    }
}
```

### Logging Properties

| Property          | Type                | Default | Gradle Event           |
|-------------------|---------------------|---------|------------------------|
| `logPassedTests`  | `Property<Boolean>` | `true`  | `TestLogEvent.PASSED`  |
| `logSkippedTests` | `Property<Boolean>` | `true`  | `TestLogEvent.SKIPPED` |
| `logTestStarted`  | `Property<Boolean>` | `false` | `TestLogEvent.STARTED` |

`TestLogEvent.FAILED` is **always included** and cannot be disabled — failed tests are
unconditionally printed to the console.

The following exception formatting is always applied regardless of the logging settings:
