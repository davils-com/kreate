# Multiplatform testing

<link-summary>How suites and test settings map onto Kotlin Multiplatform targets.</link-summary>

<card-summary>Shared and per-target suite source sets, and the targets that cannot have them.</card-summary>

<tldr>
<p><b>Suites</b>: JVM targets only</p>
<p><b>Shared source set</b>: <code>src/common&lt;Suite&gt;/kotlin</code></p>
<p><b>Per target</b>: <code>src/&lt;target&gt;&lt;Suite&gt;/kotlin</code> and a task of the same name</p>
</tldr>

A multiplatform project has two categories of test task, implemented by different Gradle types
with different APIs, and %product% configures each accordingly.

| Task type                                      | Targets            | Gets                                            |
|------------------------------------------------|--------------------|-------------------------------------------------|
| `org.gradle.api.tasks.testing.Test`            | JVM targets        | everything, including `useJUnitPlatform()` and `maxParallelForks` |
| `org.jetbrains.kotlin.gradle.tasks.KotlinTest` | Native, JS, Wasm   | `timeout`, `ignoreFailures`, `failOnNoDiscoveredTests`, `alwaysRunTests`, logging, reports |

The Kotlin/Native and Kotlin/JS test runners are embedded in the compiled binary rather than
started through the JUnit Platform, which is why the platform and forking settings do not apply
there.

## What a suite looks like

For a project with `jvm()` and `wasmJs()` targets and the two default suites:

```
src/commonMain/kotlin                 commonMain
src/jvmMain/kotlin                    jvmMain
src/wasmJsMain/kotlin                 wasmJsMain

src/commonUnitTest/kotlin             commonUnitTestImplementation
src/jvmUnitTest/kotlin                jvmUnitTestImplementation
                                      task jvmUnitTest        (KotlinJvmTest)
                                      task unitTest           (aggregate)

src/commonIntegrationTest/kotlin      commonIntegrationTestImplementation
src/jvmIntegrationTest/kotlin         jvmIntegrationTestImplementation
                                      task jvmIntegrationTest (KotlinJvmTest)
                                      task integrationTest    (aggregate)
```

Code in `src/commonUnitTest/kotlin` is compiled by every JVM target the suite covers. Code in
`src/jvmUnitTest/kotlin` is compiled by that target alone.

### Task names

Per-target names follow the Kotlin plugin's own rule — target classifier, suite name without its
`Test` suffix, then `test`:

| Target | Suite             | Task                 |
|--------|-------------------|----------------------|
| `jvm`  | `unitTest`        | `jvmUnitTest`        |
| `jvm`  | `integrationTest` | `jvmIntegrationTest` |
| `jvm`  | `contractTest`    | `jvmContractTest`    |

That is the same shape as the `jvmTest` they replace, so nothing in `./gradlew tasks` looks
bolted on.

### The aggregate task

Each suite also gets a lifecycle task named after the suite, depending on every per-target task.
`./gradlew unitTest` therefore means the same thing on a multiplatform project as on a JVM one.

## Which targets can carry a suite

Only JVM targets, and this is a hard limit rather than a decision.

<warning>
The Kotlin plugin binds the test binary of a Native target and the test run of a JS or Wasm target
to the compilation literally named <code>test</code>, and offers no public way to point either
somewhere else. A named suite on those targets would give you a source directory that is never
compiled and a task that never runs.
</warning>

%product% therefore fails the build when a suite names one:

```kotlin
suites {
    named("integrationTest") {
        targets = listOf("wasmJs")   // fails at configuration time
    }
}
```

```
Test suite 'integrationTest' requests the target(s) wasmJs, which cannot carry a named test suite.

Only JVM targets can. The Kotlin plugin binds the test binary of a Native target and the test run
of a JS or Wasm target to the compilation called 'test', and offers no way to point them at
another one - so the suite would give you a source directory that is never compiled and never run.
```

Failing is the point. Silently skipping the target would leave a directory that looks like tests,
compiles nothing and reports success.

### Tests for the other targets

Keep them where the Kotlin plugin puts them — `src/commonTest`, `src/nativeTest`,
`src/wasmJsTest` — and tell %product% to leave the conventional source set alone:

```kotlin
tests {
    enabled = true
    legacyTestSourceSet = LegacyTestPolicy.KEEP
}
```

The suites are then created beside the conventional test source set rather than instead of it, and
`linuxX64Test`, `wasmJsNodeTest` and friends keep running. See [](Testing-Suites-Migration.md).

## Restricting a suite to some targets

`targets` is empty by default, which means every JVM target. Name them to narrow it:

```kotlin
kotlin {
    jvm()
    jvm("server")
    wasmJs { nodejs() }
}

kreate {
    project {
        tests {
            enabled = true

            suites {
                named("integrationTest") {
                    // Only the server target starts containers.
                    targets = listOf("server")
                }
            }
        }
    }
}
```

A suite that ends up with no applicable target fails the build, naming the project and the targets
it does have — a `commonIntegrationTest` directory that nothing compiles is the same silent
failure as an unsupported target.

## Dependencies

A suite's `dependencies { }` block adds to the **shared** source set — `commonUnitTestImplementation`
for the `unitTest` suite:

```kotlin
suites {
    named("integrationTest") {
        dependencies {
            implementation("org.testcontainers:postgresql:1.20.4")
        }
    }
}
```

Since a suite only ever covers JVM targets, a JVM-only artifact such as Testcontainers resolves
correctly from the shared source set. For something that must go to one target only, use that
target's configuration directly once it exists — for example in a `dependencies { }` block guarded
by `afterEvaluate`, or by declaring it on the Kotlin source set.

## How the source set tree is wired

This section matters if you maintain a build that also configures the Kotlin hierarchy.

A Kotlin compilation named `unitTest` is classified by the Kotlin plugin into a `unitTest` *source
set tree*, whose root is `commonUnitTest`. Kotlin even predefines that tree, along with
`integrationTest`. The default hierarchy template, however, declares itself applicable to the
`main` and `test` trees only — so without further action `commonUnitTest` is created, connected to
nothing, and reported as an unused source set while the tests inside it silently do not run.

%product% extends the default template rather than adding an edge:

```kotlin
// what Kreate does, in effect
kotlin.applyHierarchyTemplate(
    KotlinHierarchyTemplate.default.extend {
        withSourceSetTree(KotlinSourceSetTree.unitTest, KotlinSourceSetTree.integrationTest)
    }
)
```

<warning>
%product% never calls <code>dependsOn</code> on a Kotlin source set, and neither should a build
script that uses suites. The Kotlin plugin abandons its <i>entire</i> default hierarchy the moment
it finds one manually added refines edge anywhere in the project — <code>nativeMain</code>,
<code>appleMain</code>, <code>iosMain</code> and the rest stop existing, usually without the build
failing. A single <code>dependsOn</code> in your own build script has the same effect and would
take the suites down with it.
</warning>

Because %product% applies a hierarchy template, the Kotlin plugin no longer applies its own — which
is why the template it applies is the default one, extended. Everything the default template did
still happens.

## Coverage

Per-target suite tasks are `KotlinJvmTest` tasks carrying their target name. That is the only
shape the coverage engine's multiplatform locator recognises; a plain `Test` task would leave the
suite out of the measurement while every test still passed. Nothing to configure — it is worth
knowing only if you replace a suite task with one of your own.

## Legacy test tasks

Under the default policy the conventional per-target test tasks — `jvmTest`, `linuxX64Test`,
`wasmJsNodeTest` — are disabled and skipped.

<note>
On a multiplatform project the edge from <code>check</code> to the Kotlin plugin's aggregate
<code>allTests</code> task cannot be removed: its child list has no removal API. The per-target
tasks are still disabled, so they are reported as <code>SKIPPED</code> rather than executed, and
the outcome is the same. They remain visible in the task graph.
</note>

<seealso>
    <category ref="project">
        <a href="Testing-Overview.md">Overview</a>
        <a href="Testing-Suites.md">Test suites</a>
        <a href="Testing-Suites-Migration.md">Migrating from src/test</a>
        <a href="Testing-Troubleshooting.md">Troubleshooting</a>
    </category>
</seealso>
