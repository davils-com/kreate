# Testing overview

<link-summary>Test execution, logging, and reporting.</link-summary>

<card-summary>Parallel execution and readable output, configured once.</card-summary>

<tldr>
<p><b>Default</b>: enabled</p>
<p><b>Configures</b>: parallel execution, logging, and HTML/XML reports</p>
</tldr>

The `tests { }` block inside `kreate { project { } }` provides a unified, opinionated testing
configuration for both Kotlin JVM and Kotlin Multiplatform projects. When enabled, Kreate
automatically configures all test tasks with sensible defaults for parallelism, timeouts,
failure handling, console logging, and report generation — without requiring manual `tasks.withType<Test>` boilerplate.

Enabling it also replaces the conventional `test` source set with named **test suites** —
`unitTest` and `integrationTest` by default, each with its own source set, dependency
configurations and task. That is where most of the configuration now lives; see
[](Testing-Suites.md), and [](Testing-Suites-Migration.md) for what happens to an existing
`src/test`.

Testing is **disabled by default**. Enable it with:

```kotlin
kreate {
    project {
        tests {
            enabled = true
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

## Knowing what the suite actually reaches

A configured, parallel, well-reported test suite can still be green while never entering half the
codebase. That is a different question from the one this block answers, and
[coverage](Coverage-Overview.md) is what answers it.

<seealso>
    <category ref="project">
        <a href="Testing-Configuration-Reference.md">Configuration reference</a>
        <a href="Testing-Multiplatfrom.md">Multiplatform testing</a>
        <a href="Testing-Example.md">Examples</a>
        <a href="Coverage-Overview.md">Coverage</a>
    </category>
</seealso>
