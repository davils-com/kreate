# Testing overview

<link-summary>Named test suites, execution settings, logging and reports.</link-summary>

<card-summary>Unit and integration tests as separate suites, configured once.</card-summary>

<tldr>
<p><b>Default</b>: disabled</p>
<p><b>Enable</b>: <code>project { tests { enabled = true } }</code></p>
<p><b>Creates</b>: the <code>unitTest</code> and <code>integrationTest</code> suites</p>
<p><b>Configures</b>: parallelism, timeouts, failure handling, console logging, reports</p>
</tldr>

The `tests { }` block inside `kreate { project { } }` does two things. It configures every test
task in the project — parallelism, timeouts, failure handling, console output, reports — so that
no build script needs a `tasks.withType<Test> { }` block of its own. And it replaces the
conventional `test` source set with **named test suites**.

## Why suites

A conventional `test` source set asks one question and gets one answer: is this project's code
correct? In practice a project has two kinds of test with opposite requirements.

<deflist type="wide">
    <def title="Unit tests">
        Fast, hermetic, no external process. They belong on every build, and a developer should be
        able to run them in seconds.
    </def>
    <def title="Integration tests">
        They start containers, open sockets, talk to a database. They cost minutes, they need
        Docker, and they have no business running before someone pushes.
    </def>
</deflist>

Kept in one source set, the slower of the two answers is forced onto both: `check` becomes a
coffee break, there is no way to run only the fast tests, and every integration-only dependency —
Testcontainers, a JDBC driver, an HTTP client — sits on the compile classpath of every unit test
in the project.

%product% gives each its own source set, its own dependency scope and its own task:

| Suite             | Source set (JVM)             | Task              | On `check` |
|-------------------|------------------------------|-------------------|------------|
| `unitTest`        | `src/unitTest/kotlin`        | `unitTest`        | yes        |
| `integrationTest` | `src/integrationTest/kotlin` | `integrationTest` | no         |

Register as many more as the project needs. See [](Testing-Suites.md).

## Quick start

```kotlin
kreate {
    project {
        tests {
            enabled = true
        }
    }
}
```

That gives both suites, their configurations and their tasks. `./gradlew check` runs `unitTest`;
`./gradlew integrationTest` runs the other one when you ask for it.

An existing `src/test` is not silently abandoned — [](Testing-Suites-Migration.md) covers the four
policies, from a hard build error to leaving the conventional source set exactly as it is.

## What %product% configures

Every test task in the project, whoever created it, receives the project-wide settings:

| Setting                   | Applied to         | Description                                          |
|---------------------------|--------------------|------------------------------------------------------|
| `useJUnitPlatform()`      | JVM `Test` tasks   | Selects the JUnit Platform test engine               |
| `maxParallelForks`        | JVM `Test` tasks   | Number of parallel test worker processes             |
| `timeout`                 | All test tasks     | Per-task execution deadline                          |
| `ignoreFailures`          | All test tasks     | Whether the build continues after a failure          |
| `failOnNoDiscoveredTests` | All test tasks     | Fails the build when a task finds no tests           |
| `outputs.upToDateWhen`    | All test tasks     | Whether Gradle may skip an unchanged task            |
| `testLogging { }`         | All test tasks     | Console events and exception formatting              |
| `reports`                 | All test tasks     | XML and HTML report generation                       |

Each suite then receives its own settings on top, inheriting anything it does not override. That
inheritance is the point: set a timeout once on `tests { }` and name it again only where a suite
genuinely differs.

```kotlin
tests {
    enabled = true
    timeoutMinutes = 10L
    maxParallelForks = 4

    suites {
        named("integrationTest") {
            // These tests share one database container, so they cannot fork.
            maxParallelForks = 1
            timeoutMinutes = 30L
        }
    }
}
```

## Test framework

%product% selects the JUnit Platform for every JVM test task, so no `useJUnitPlatform()` call is
needed in the build script. It does not choose an engine for you — declare JUnit Jupiter, or
`kotlin-test`, or anything else on the suite:

```kotlin
suites {
    named("unitTest") {
        dependencies {
            implementation("org.junit.jupiter:junit-jupiter:6.1.3")
        }
    }
}
```

The one exception is Kotest, which %product% can add for you because it is the framework the suite
runs on rather than a library the suite talks to. See [](Testing-Kotest.md).

<note>
%product% adds <code>junit-platform-launcher</code> to every suite's runtime classpath whether or
not Kotest is enabled. Gradle supplies a launcher only to the <code>test</code> suite it created
itself; a suite's task is registered by %product%, so without it the task fails before running a
single test. See <a href="Testing-Kotest.md">The Kotest bundle</a>.
</note>

## Where the settings are evaluated

The `tests { }` block is read inside `afterEvaluate`, so everything in it must be set while the
build script is still being evaluated. Two consequences are worth knowing before you start:

<deflist type="wide">
    <def title="Suite dependencies go on the suite">
        A suite's configurations are created after the build script has run, so
        <code>integrationTestImplementation(...)</code> in the project's top level
        <code>dependencies { }</code> block names something that does not exist yet. Declare them
        in the suite's own <code>dependencies { }</code> block instead — Gradle's
        <code>JvmTestSuite</code> works the same way, for the same reason.
    </def>
    <def title="Enum values need an import">
        <code>LegacyTestPolicy</code> and <code>KotestModule</code> live in
        <code>com.davils.kreate.module.project.tests</code> and
        <code>...tests.suite</code>. A Kotlin build script needs an <code>import</code> at the top
        of the file, the same as for any other type a plugin exposes.
    </def>
</deflist>

## Knowing what the suite actually reaches

A configured, parallel, well-reported test suite can still be green while never entering half the
codebase. That is a different question from the one this block answers, and
[coverage](Coverage-Overview.md) is what answers it. %product% keeps the suites' own source sets
out of the measurement, so the number describes the code you ship rather than the code that tests
it — see [](Testing-Suites.md#coverage).

<seealso>
    <category ref="project">
        <a href="Testing-Suites.md">Test suites</a>
        <a href="Testing-Suites-Migration.md">Migrating from src/test</a>
        <a href="Testing-Kotest.md">The Kotest bundle</a>
        <a href="Testing-Configuration-Reference.md">Configuration reference</a>
        <a href="Testing-Multiplatform.md">Multiplatform testing</a>
        <a href="Testing-Example.md">Examples</a>
        <a href="Testing-Troubleshooting.md">Troubleshooting</a>
        <a href="Coverage-Overview.md">Coverage</a>
    </category>
</seealso>
