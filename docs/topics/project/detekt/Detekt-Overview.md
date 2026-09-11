# Detekt overview

<link-summary>Wiring Detekt into your build through Kreate.</link-summary>

<card-summary>Static analysis configured from one place, with reports in every format.</card-summary>

<tldr>
<p><b>Enable</b>: <code>project { detekt { enabled = true } }</code></p>
<p><b>Applies</b>: the <code>dev.detekt</code> plugin</p>
</tldr>

Detekt is a static code analysis tool for the Kotlin programming language. It operates on the abstract syntax tree provided by the Kotlin compiler and focuses on finding code smells, complexity issues, and potential bugs.

The `detekt { }` block inside `kreate { project { } }` provides a deeply integrated configuration for Detekt. Kreate automates the setup of the Detekt Gradle plugin, manages rule-set application, and configures reporting with professional defaults tailored for Kotlin development.

## Quick Start

Detekt is **disabled by default**. To activate it, set the `enabled` property to `true`:

```kotlin
kreate {
    project {
        detekt {
            enabled = true
        }
    }
}
```

Enabling it is all it takes — Kreate applies the `dev.detekt` plugin and configures the standard tasks and reports.

Apply it yourself when you want something Kreate does not expose: a pinned version, or Detekt's own `detekt { }` block, which only exists in your build script if the plugin was applied there. Kreate's own application is then a no-op.

```kotlin
plugins {
    id("dev.detekt") version "<version>"
}
```

## Which source sets are analysed

Detekt registers a task per source set, so everything the project compiles is analysed — including
the [test suites](Testing-Suites.md). A project with the two default suites gets
`detektMainSourceSet`, `detektUnitTestSourceSet` and `detektIntegrationTestSourceSet`, plus one for
every suite it registers. Nothing to configure.

## What `check` runs

Detekt's own plugin makes `check` depend on the aggregate `detekt` task. On a Kotlin JVM project that task reads `src/main/kotlin` and `src/test/kotlin` and the arrangement is correct.

Under the Kotlin Multiplatform plugin it reads nothing at all: every file belongs to a source set, and the aggregate is left with no sources. It then succeeds without analysing a line, which is the worst way for a quality gate to fail - the build stays green and static analysis silently stops happening.

Kreate therefore also points `check` at Detekt's per-source-set tasks:

```
:detektCommonMainSourceSet
:detektCommonTestSourceSet
:detektJvmMainSourceSet
:detektWasmJsMainSourceSet
...
```

One task per source set means every file is analysed exactly once. On a project that has no such tasks - anything using the Kotlin JVM plugin - the set is empty and nothing changes.

<note>
Detekt also registers a task per <i>compilation</i> (<code>detektMainJvm</code>, <code>detektTestJvm</code>, …). Those run <b>with type resolution</b> and so enable rules the source set tasks cannot evaluate, at the cost of analysing shared source sets once per target. Kreate does not wire them into <code>check</code>: turning them on changes which rules apply, which is a decision about your rule set rather than about where analysis runs. Add them yourself if you want them.
</note>

## Generated sources are not analysed

Anything under the build directory is left out. Code generators put files on real source sets - KSP writes test launchers there, Kreate's own build constants generator writes a file to `commonMain` - and Detekt would otherwise report formatting findings in code nobody wrote and nobody can fix.

## Why use Detekt?

Static analysis helps maintain a high-quality codebase by:
- **Enforcing Consistency**: Ensures all developers follow the same coding standards.
- **Reducing Technical Debt**: Catches complex or hard-to-maintain code early.
- **Finding Bugs**: Identifies potential issues that might not be caught by the compiler.
- **Education**: Helps developers learn Kotlin best practices through rule descriptions.

## Key Features in Kreate

- **Zero-Configuration**: Sensible defaults are provided out of the box.
- **Nested DSL**: Familiar configuration structure matching the original Detekt plugin.
- **Automated Reporting**: Generates HTML, SARIF, XML, and Markdown reports by default.
- **Baseline Support**: Easily use custom `detekt.yaml` configurations.
- **Extensible**: Add third-party rule sets like `detekt-formatting` with ease.




<seealso>
    <category ref="project">
        <a href="Detekt-Configuration.md">Configuration</a>
        <a href="Detekt-Reports.md">Reports</a>
    </category>
</seealso>
