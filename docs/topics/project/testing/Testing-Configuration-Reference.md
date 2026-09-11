# Testing configuration reference

<link-summary>Every property in the tests block.</link-summary>

<card-summary>Parallelism, timeouts, logging, and reports.</card-summary>

## Top-Level Properties

| Property                  | Type                | Default                   | Description                                                             |
|---------------------------|---------------------|---------------------------|-------------------------------------------------------------------------|
| `enabled`                 | `Property<Boolean>` | `false`                   | Master switch — must be `true` for any test configuration to apply      |
| `maxParallelForks`        | `Property<Int>`     | `availableProcessors / 2` | Maximum number of parallel test worker processes for JVM tests          |
| `timeoutMinutes`          | `Property<Long>`    | `10`                      | Per-task test timeout in minutes                                        |
| `ignoreFailures`          | `Property<Boolean>` | `false`                   | When `true`, the build continues even if tests fail                     |
| `alwaysRunTests`          | `Property<Boolean>` | `false`                   | When `true`, disables Gradle's up-to-date check so tests always execute |
| `failOnNoDiscoveredTests` | `Property<Boolean>` | `false`                   | When `true`, fails the build if a test task finds no tests              |
| `legacyTestSourceSet`     | `Property<LegacyTestPolicy>` | `DISABLE`        | What happens to the conventional `test` source set                     |
| `legacySourceDirectories` | `Property<LegacySourceDirectories>` | derived  | What happens to its source directories                                 |
| `excludeSuitesFromCoverage` | `Property<Boolean>` | `true`                  | Keeps the suites' own code out of the coverage denominator             |

### `maxParallelForks`

Controls how many separate JVM processes Gradle spawns to execute tests in parallel. The default
is `Runtime.getRuntime().availableProcessors() / 2`, which balances throughput against resource
usage on the build machine. Setting this to `1` disables parallelism and runs all tests in a
single process — useful for tests that share global state or external resources.

```kotlin
tests {
    enabled = true
    maxParallelForks = 4
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
    enabled = true
    timeoutMinutes = 5
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
    enabled = true
    ignoreFailures = true
}
```

### `alwaysRunTests`

By default, Gradle skips test tasks if their inputs and outputs have not changed since the last
run (incremental build). Setting `alwaysRunTests` to `true` disables this optimization by
returning `false` from `outputs.upToDateWhen`, forcing every invocation of `test` or `check`
to re-execute all tests.

```kotlin
tests {
    enabled = true
    alwaysRunTests = true
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
    enabled = true
    failOnNoDiscoveredTests = true
}
```

## Logging Configuration

The `logging { }` sub-block controls which test lifecycle events are printed to the Gradle
console during a build. Exception details are **always** shown regardless of these settings.

```kotlin
tests {
    enabled = true
    logging {
        logPassedTests = true
        logSkippedTests = true
        logTestStarted = false
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

### `legacyTestSourceSet`

What happens to the conventional `test` source set once the suites take over. Four values, covered
in full in [](Testing-Suites-Migration.md):

```kotlin
import com.davils.kreate.module.project.tests.LegacyTestPolicy

tests {
    legacyTestSourceSet = LegacyTestPolicy.FAIL
}
```

| Value     | `test` task                      | `src/test` sources    |
|-----------|----------------------------------|-----------------------|
| `DISABLE` | disabled, removed from `check`   | cleared               |
| `FAIL`    | build fails while sources remain | —                     |
| `ALIAS`   | runs the unit suite              | adopted by `unitTest` |
| `KEEP`    | untouched                        | untouched             |

### `legacySourceDirectories`

What happens to the legacy source **directories**, separately from what happens to the task.
Derived from `legacyTestSourceSet` unless set explicitly, and rarely worth setting:

| Value   | Effect                                                       | Implied by        |
|---------|--------------------------------------------------------------|-------------------|
| `CLEAR` | directories cleared; the compilation reports `NO-SOURCE`      | `DISABLE`, `FAIL` |
| `ADOPT` | directories added to the unit suite, then cleared             | `ALIAS`           |
| `KEEP`  | left registered on the legacy source set                      | `KEEP`            |

Set both to combine them differently — for instance to disable the legacy task while leaving its
directories registered:

```kotlin
tests {
    legacyTestSourceSet = LegacyTestPolicy.DISABLE
    legacySourceDirectories = LegacySourceDirectories.KEEP
}
```

### `excludeSuitesFromCoverage`

Keeps the suites' own source sets out of the coverage denominator. Defaults to `true`.

The coverage engine recognises a test compilation by the name `test` and nothing else, so a suite
called `unitTest` would otherwise be measured as production code — the build stays green while the
number describes the wrong thing. The exclusion is skipped when
`coverage { sources { includedSourceSets } }` is non-empty, because an explicit include list
already decides what counts.

## Suites

`suites` is a `NamedDomainObjectContainer<TestSuiteExtension>`, pre-registered with `unitTest` and
`integrationTest`. See [](Testing-Suites.md) for what a suite is and how it is laid out.

```kotlin
tests {
    suites {
        named("unitTest") { }          // configure a pre-registered suite
        register("contractTest") { }   // add one
    }
}
```

### Identity and layout

| Property        | Type                    | Default        | Description                                                   |
|-----------------|-------------------------|----------------|---------------------------------------------------------------|
| `enabled`       | `Property<Boolean>`     | `true`         | Whether the suite is created at all                           |
| `sourceSetName` | `Property<String>`      | the suite name | Name of the backing source set, and of the KMP source set tree |
| `srcDirs`       | `ListProperty<String>`  | empty          | Replaces the conventional source directories when set         |
| `description`   | `Property<String>`      | derived        | Description shown for the suite's task                        |

### Wiring

| Property             | Type                   | Default                    | Description                                                        |
|----------------------|------------------------|----------------------------|--------------------------------------------------------------------|
| `associateWithMain`  | `Property<Boolean>`    | `true`                     | Makes `main`'s `internal` declarations visible to the suite        |
| `dependsOnSuites`    | `ListProperty<String>` | empty                      | Other suites whose compiled output this suite may use              |
| `runOnCheck`         | `Property<Boolean>`    | `true`, `false` for `integrationTest` | Whether `check` runs this suite                         |
| `mustRunAfterSuites` | `ListProperty<String>` | empty, `["unitTest"]` for `integrationTest` | Ordering only; does not pull the suite into a build |
| `targets`            | `ListProperty<String>` | empty (every JVM target)   | Multiplatform only; naming a non-JVM target fails the build        |

### Execution

Everything in this table is inherited from the enclosing `tests { }` block unless set on the suite.

| Property                  | Type                | Description                                  |
|---------------------------|---------------------|----------------------------------------------|
| `maxParallelForks`        | `Property<Int>`     | Parallel test worker processes               |
| `timeoutMinutes`          | `Property<Long>`    | Per-task timeout                             |
| `ignoreFailures`          | `Property<Boolean>` | Whether the build continues after failures   |
| `alwaysRun`               | `Property<Boolean>` | Disables the up-to-date check                |
| `failOnNoDiscoveredTests` | `Property<Boolean>` | Fails the build on an empty suite            |

Suite-only:

| Property           | Type                            | Default | Description                                      |
|--------------------|---------------------------------|---------|--------------------------------------------------|
| `includeTags`      | `ListProperty<String>`          | empty   | JUnit Platform tags to run, to the exclusion of the rest |
| `excludeTags`      | `ListProperty<String>`          | empty   | JUnit Platform tags to skip                      |
| `systemProperties` | `MapProperty<String, String>`   | empty   | System properties for the suite's test JVM       |
| `environment`      | `MapProperty<String, String>`   | empty   | Environment variables for the suite's test JVM   |
| `jvmArgs`          | `ListProperty<String>`          | empty   | Additional JVM arguments                         |

### Nested blocks

`logging { }`, `report { }` and `kotest { }` take the same properties as on `tests { }` and
inherit their values from there. `dependencies { }` is suite-only:

| Function                | Description                                                |
|-------------------------|------------------------------------------------------------|
| `implementation(String)`| Compile and runtime classpath                              |
| `compileOnly(String)`   | Compile classpath only                                     |
| `runtimeOnly(String)`   | Runtime classpath only                                     |
| `platform(String)`      | A bill of materials applied to the suite's classpath       |

### `enabled`

Whether the suite is created at all. Defaults to `true`, including for suites you register.
Setting it to `false` on a pre-registered suite is how a project opts out of `integrationTest`
without working around it — no source set, no configurations, no task.

### `sourceSetName`

The name of the source set backing the suite, defaulting to the suite name. Separating the two
lets a task be named one thing and its directory another:

```kotlin
suites {
    register("it") {
        sourceSetName = "integrationTest"   // task `it`, src/integrationTest/kotlin
    }
}
```

On a multiplatform project this is the name of the source set *tree*, from which `common<Name>`
and `<target><Name>` are derived.

### `srcDirs`

Replaces the conventional source directories rather than adding to them. Empty by default, which
leaves `src/<sourceSetName>/kotlin` in place. Paths resolve relative to the project directory.

### `associateWithMain`

Associates the suite's compilation with `main`. Defaults to `true`, which is what makes `internal`
declarations visible to the tests and puts `main`'s own dependencies on the suite's classpath.

Turning it off is a deliberate choice for a black-box suite and a mistake everywhere else: the
tests can then only exercise the published surface.

### `dependsOnSuites`

Other suites whose compiled output — including their `internal` declarations — this suite may use.
The mechanism for shared fixtures.

Naming a suite here does **not** pull it into a build. It is about visibility; `mustRunAfterSuites`
is about order.

### `runOnCheck`

Whether `check` runs this suite. `true` for everything except the pre-registered
`integrationTest`, which cannot be a precondition of every local build when it needs Docker.

### `mustRunAfterSuites`

Ordering only. If both suites are in the same build, this one runs second; asking for this one
alone does not drag the other in. `integrationTest` lists `unitTest` by default, so a build that
runs both reports the cheap failures first.

On a multiplatform project the ordering is applied per target as well, so requesting only
`jvmIntegrationTest` still runs after `jvmUnitTest` when both are present.

### `targets`

Multiplatform only, ignored on Kotlin/JVM. Empty means every JVM target. Naming a Native, JS or
Wasm target fails the build — see
[](Testing-Multiplatform.md#which-targets-can-carry-a-suite).

### `includeTags` and `excludeTags`

JUnit Platform tag expressions applied to this suite's task. `includeTags` is exclusive: a
non-empty list runs those tags and nothing else. Both reach Kotest specs, which map `@Tags` onto
JUnit Platform tags.

### `systemProperties`, `environment` and `jvmArgs`

Passed to this suite's test JVM and no other. This is where an integration suite's switches belong
— a Testcontainers reuse flag, a Spring profile, a larger heap — rather than on every test task in
the project.

### `dependencies { }`

The suite's own dependencies, declared here because a suite's configurations do not exist while
the build script runs.

| Function                 | Configuration                       |
|--------------------------|-------------------------------------|
| `implementation(String)` | `<suite>Implementation`             |
| `compileOnly(String)`    | `<suite>CompileOnly`                |
| `runtimeOnly(String)`    | `<suite>RuntimeOnly`                |
| `platform(String)`       | `<suite>Implementation` as platform |

On a multiplatform project these land on the suite's shared source set.

## Kotest

| Property                   | Type                          | Default               | Description                                    |
|----------------------------|-------------------------------|-----------------------|------------------------------------------------|
| `enabled`                  | `Property<Boolean>`           | `false`               | Whether Kreate adds the Kotest artifacts       |
| `version`                  | `Property<String>`            | `6.2.4`               | One version for the whole bundle               |
| `modules`                  | `ListProperty<KotestModule>`  | `[ASSERTIONS]`        | Optional libraries added beside the runner     |
| `addJUnitPlatformLauncher` | `Property<Boolean>`           | `true`                | Adds the launcher a suite cannot start without |
| `junitPlatformVersion`     | `Property<String>`            | `6.1.3`               | Version of that launcher                       |

### `addJUnitPlatformLauncher`

Adds `org.junit.platform:junit-platform-launcher` to every suite's runtime classpath, whether or
not the rest of the bundle is enabled. Defaults to `true`, and it has to: Gradle supplies a
launcher only to the `test` suite it created itself, so a suite's task otherwise fails before
running a single test with `Failed to load JUnit Platform`.

### `junitPlatformVersion`

The version of that launcher. Defaults to the JUnit 6 line; a project on JUnit 5 should set the
matching `1.x`, because the launcher and the engines it starts are released together.

See [](Testing-Kotest.md).

<seealso>
    <category ref="project">
        <a href="Testing-Overview.md">Overview</a>
        <a href="Testing-Suites.md">Test suites</a>
        <a href="Testing-Suites-Migration.md">Migrating from src/test</a>
        <a href="Testing-Kotest.md">The Kotest bundle</a>
        <a href="Testing-Example.md">Examples</a>
    </category>
</seealso>
