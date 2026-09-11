# Testing troubleshooting

<link-summary>Error messages, what causes them and what to change.</link-summary>

<card-summary>Every failure the test suites can produce, and its fix.</card-summary>

## Configuration errors

### Configuration with name 'integrationTestImplementation' not found

```
* What went wrong:
Configuration with name 'integrationTestImplementation' not found.
```

A suite's configurations are created after the build script has been evaluated, so the project's
top level `dependencies { }` block cannot name them. Declare the dependency on the suite:

<compare first-title="Does not work" second-title="Works">

```kotlin
dependencies {
    "integrationTestImplementation"("org.testcontainers:postgresql:1.20.4")
}
```

```kotlin
tests {
    suites {
        named("integrationTest") {
            dependencies {
                implementation("org.testcontainers:postgresql:1.20.4")
            }
        }
    }
}
```

</compare>

### Unresolved reference 'LegacyTestPolicy' / 'KotestModule'

A Kotlin build script needs an import for any type a plugin exposes:

```kotlin
import com.davils.kreate.module.project.tests.LegacyTestPolicy
import com.davils.kreate.module.project.tests.LegacySourceDirectories
import com.davils.kreate.module.project.tests.suite.KotestModule
```

### Test suite '…' uses the source set name '…', which already belongs to …

Two things want the same source set. Asked for a name that is taken, Gradle hands back the
existing source set — a suite called `main` would turn the production code into a test suite and
run it — so %product% refuses instead.

Rename the suite, or give it a `sourceSetName` of its own:

```kotlin
suites {
    register("main") {              // rejected
        sourceSetName = "mainTest"  // …unless you say otherwise
    }
}
```

### Test suites '…' and '…' both use the source set name '…'

Two suites resolved to one source set, which would mean one compilation and two tasks running the
same classes. Give one of them a different `sourceSetName`.

### Test suite '…' refers to suite '…', which is not registered or not enabled

`dependsOnSuites` or `mustRunAfterSuites` names something that does not exist — usually a typo, or
a suite that was disabled. The message lists the suites that do exist.

Note that a disabled suite counts as absent: referring to one is an error rather than a silent
no-op, because a suite that expected to see another suite's fixtures will not compile.

### Test suite '…' requests the target(s) …, which cannot carry a named test suite

Only JVM targets can. See [](Testing-Multiplatform.md#which-targets-can-carry-a-suite).

### Test suite '…' applies to no target

A multiplatform project with no JVM target, or a `targets` list that matched nothing. Add a `jvm()`
target, correct the list, or disable the suite:

```kotlin
suites {
    named("integrationTest") { enabled = false }
}
```

### Named test suites are enabled, and project '…' still has sources under …

The `FAIL` policy doing its job. Move the files, or pick another policy — see
[](Testing-Suites-Migration.md).

## Execution errors

### Failed to load JUnit Platform

```
> Test process encountered an unexpected problem.
   > Could not start Gradle Test Executor 12.
      > Failed to load JUnit Platform.
```

The JUnit Platform launcher is missing from the suite's runtime classpath. %product% adds it by
default, so this means it was turned off:

```kotlin
kotest {
    addJUnitPlatformLauncher = true   // the default
}
```

If you turned it off deliberately, declare the launcher yourself:

```kotlin
suites {
    named("unitTest") {
        dependencies {
            runtimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
        }
    }
}
```

The same message on the conventional `test` task under the `KEEP` policy has the same cause and
the same fix, except that the dependency goes on `testRuntimeOnly` — Gradle only supplies a
launcher automatically when the test suite's framework was selected through
`testing { suites { } }`.

### Unresolved reference 'Test' in an adopted test file

The sources moved to the suite but the framework did not. See
[](Testing-Suites-Migration.md#moving-the-dependencies).

### Unresolved reference to an internal declaration

`associateWithMain` was turned off for that suite, or the declaration is `internal` to another
suite that is not listed in `dependsOnSuites`.

```kotlin
suites {
    named("integrationTest") {
        associateWithMain = true            // the default
        dependsOnSuites = listOf("unitTest")
    }
}
```

## Things that look wrong and are not

### :test reports NO-SOURCE

Expected under the `DISABLE` and `FAIL` policies: the legacy source directories have been cleared,
so the compilation has nothing to build. The task is disabled as well.

### jvmTest is SKIPPED but still in the task list

On a multiplatform project the edge from `check` to the Kotlin plugin's `allTests` task cannot be
removed — its child list has no removal API. The per-target tasks are disabled, so they skip
rather than run.

### A suite task succeeds without running anything

A test task with no discovered tests succeeds by default. Two things to check: whether the sources
are where the suite expects them, and whether a tag filter excluded everything.

To make it an error instead:

```kotlin
tests {
    failOnNoDiscoveredTests = true
}
```

### The coverage number changed after adding a suite

It should not have. Suite source sets are excluded from the measurement — see
[](Testing-Suites.md#coverage). If it did, check whether
`coverage { sources { includedSourceSets } }` is set, which switches the exclusion off because an
explicit include list already decides what counts.

### detekt runs on the suite source sets

Intended. Detekt registers a task per source set, so suite code is analysed like any other code.

## Diagnostics

<deflist type="wide">
    <def title="Which suites exist">
        <code>./gradlew tasks --group verification</code>
    </def>
    <def title="Which per-target tasks a multiplatform suite produced">
        <code>./gradlew tasks --all | grep UnitTest</code>
    </def>
    <def title="What a suite's classpath resolved to">
        <code>./gradlew dependencies --configuration unitTestRuntimeClasspath</code>
    </def>
    <def title="What actually ran">
        <code>build/test-results/&lt;suite&gt;/</code> for the XML, and
        <code>build/reports/tests/&lt;suite&gt;/index.html</code> when HTML reports are on.
    </def>
    <def title="Why a task was skipped">
        <code>./gradlew unitTest --info</code> prints the reason.
    </def>
</deflist>

<seealso>
    <category ref="project">
        <a href="Testing-Overview.md">Overview</a>
        <a href="Testing-Suites.md">Test suites</a>
        <a href="Testing-Suites-Migration.md">Migrating from src/test</a>
        <a href="Testing-Multiplatform.md">Multiplatform testing</a>
    </category>
</seealso>
