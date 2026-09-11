# Test suites

<link-summary>Named source sets for unit and integration tests.</link-summary>

<card-summary>unitTest and integrationTest instead of one test source set.</card-summary>

<tldr>
<p><b>Default</b>: two suites, <code>unitTest</code> and <code>integrationTest</code></p>
<p><b>Replaces</b>: the conventional <code>test</code> source set</p>
</tldr>

The conventional `test` source set asks one question and gets one answer: is this project's code
correct? In practice a project has two kinds of test with opposite requirements. Unit tests are
fast and hermetic and belong on every build. Integration tests start containers, talk to real
services, and have no business running before someone pushes. Kept in one source set, the slower
of the two answers is forced onto both — and an integration-only dependency such as Testcontainers
ends up on the classpath of every test the project has.

Kreate replaces that source set with **named test suites**. Each suite is a source set, a set of
dependency configurations and a test task, and a project has as many as it needs. Two are
registered by default:

| Suite             | Source set (JVM)          | Task                | On `check` |
|-------------------|---------------------------|---------------------|------------|
| `unitTest`        | `src/unitTest/kotlin`     | `unitTest`          | yes        |
| `integrationTest` | `src/integrationTest/kotlin` | `integrationTest`| no         |

`integrationTest` is deliberately off `check` and ordered after `unitTest`, so a build that runs
both reports the cheap failures before it starts a container. Run it explicitly:

```bash
./gradlew integrationTest
```

## Enabling

Suites come with the testing feature:

```kotlin
kreate {
    project {
        tests {
            enabled = true
        }
    }
}
```

That alone gives `src/unitTest/kotlin` and `src/integrationTest/kotlin`, their configurations and
their tasks. See [](Testing-Suites-Migration.md) for what happens to an existing `src/test`.

## Declaring dependencies

A suite's dependencies are declared **on the suite**, not in the project's top-level
`dependencies { }` block:

```kotlin
tests {
    enabled = true

    suites {
        named("integrationTest") {
            dependencies {
                platform("org.testcontainers:testcontainers-bom:1.20.4")
                implementation("org.testcontainers:postgresql")
                runtimeOnly("org.postgresql:postgresql:42.7.4")
            }
        }
    }
}
```

This is not a preference. A suite's configurations are created after the build script has been
evaluated, so `integrationTestImplementation(...)` at the top level names something that does not
exist yet and fails. Gradle's own `JvmTestSuite` carries its dependencies for the same reason.

Kreate declares nothing of its own here. The one exception is the Kotest bundle, which is the
framework the suite runs on rather than something it talks to — see [](Testing-Kotest.md).

## Seeing `internal` declarations

A suite's compilation is associated with `main`, so tests see `internal` declarations and inherit
`main`'s dependencies. Turn that off for a suite that should only exercise the published surface:

```kotlin
suites {
    named("integrationTest") {
        associateWithMain = false
    }
}
```

## Sharing fixtures between suites

A suite can use another suite's compiled output, including its `internal` declarations:

```kotlin
suites {
    named("integrationTest") {
        dependsOnSuites = listOf("unitTest")
    }
}
```

Ordering is separate from this: `dependsOnSuites` is about visibility, `mustRunAfterSuites` about
execution order, and neither pulls the other suite into a build that did not ask for it.

## Registering another suite

```kotlin
suites {
    register("contractTest") {
        runOnCheck = false
        includeTags = listOf("contract")

        dependencies {
            implementation("org.junit.jupiter:junit-jupiter:6.1.3")
        }
    }
}
```

That gives `src/contractTest/kotlin`, the `contractTest*` configurations and a `contractTest` task.

## Inherited settings

Every execution setting that exists on both `tests { }` and a suite is inherited: set it once and
override it only where a suite genuinely differs.

```kotlin
tests {
    enabled = true
    timeoutMinutes = 10L
    maxParallelForks = 4

    suites {
        named("integrationTest") {
            // A suite whose tests share one database container cannot fork.
            maxParallelForks = 1
            timeoutMinutes = 30L
        }
    }
}
```

Inherited: `maxParallelForks`, `timeoutMinutes`, `ignoreFailures`, `alwaysRun`,
`failOnNoDiscoveredTests`, and every value in `logging { }`, `report { }` and `kotest { }`.

## Multiplatform

On a multiplatform project a suite gives a shared source set and one per JVM target:

```
src/commonUnitTest/kotlin           commonUnitTestImplementation
src/jvmUnitTest/kotlin              task jvmUnitTest
                                    task unitTest  (runs every target's task)
src/commonIntegrationTest/kotlin
src/jvmIntegrationTest/kotlin       task jvmIntegrationTest, task integrationTest
```

Suite dependencies land on the shared source set. Restrict a suite to particular targets with
`targets`:

```kotlin
suites {
    named("integrationTest") {
        targets = listOf("jvm")
    }
}
```

> Only JVM targets can carry a named suite. The Kotlin plugin ties the test binary of a Native
> target and the test run of a JS or Wasm target to the compilation called `test` and offers no
> way to point them elsewhere, so naming one in `targets` fails the build rather than producing a
> source directory that is never compiled. Tests for those targets stay in their conventional test
> source sets; set `legacyTestSourceSet = LegacyTestPolicy.KEEP` to keep them running.
>
{style="warning"}

Kreate extends the Kotlin hierarchy template so the suites' source set trees are wired up. The
default template covers the `main` and `test` trees only, and without the extension
`commonUnitTest` would be created, connected to nothing, and reported as an unused source set
while the tests in it silently did not run. The extension builds on the default template, so
`nativeMain`, `appleMain` and the rest are unaffected.

## Coverage

Suite source sets are excluded from the coverage measurement. The coverage engine recognises a
test compilation by the name `test` and nothing else, so without this a suite's own code would be
counted as production code in the denominator — a number that stays green while describing the
wrong thing. Turn it off with `excludeSuitesFromCoverage = false`.

Dependency locking covers the suite classpaths as well, so an integration suite's dependencies
appear in `gradle.lockfile` and in a Trivy scan over it. Regenerate the lock files after adding a
suite.

<seealso>
    <category ref="related">
        <a href="Testing-Suites-Migration.md">Migrating from src/test</a>
        <a href="Testing-Kotest.md">The Kotest bundle</a>
        <a href="Testing-Configuration-Reference.md">Configuration reference</a>
    </category>
</seealso>
