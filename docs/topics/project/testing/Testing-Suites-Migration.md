# Migrating from src/test

<link-summary>What happens to the conventional test source set.</link-summary>

<card-summary>Four policies, from a hard error to leaving it alone.</card-summary>

<tldr>
<p><b>Default</b>: <code>LegacyTestPolicy.DISABLE</code></p>
<p><b>While migrating</b>: <code>ALIAS</code>, then <code>FAIL</code> when done</p>
</tldr>

Gradle's `java` plugin and the Kotlin plugin both create a `test` source set before Kreate is
asked, and neither offers a way to remove it. The question is therefore not whether it exists but
whether it still runs — and a project part-way through a migration needs a different answer from
one that has finished.

```kotlin
kreate {
    project {
        tests {
            enabled = true
            legacyTestSourceSet = LegacyTestPolicy.ALIAS
        }
    }
}
```

The enum lives in `com.davils.kreate.module.project.tests`, so the build script needs an import.

## The policies

| Policy    | `test` task                       | `src/test` sources          |
|-----------|-----------------------------------|-----------------------------|
| `DISABLE` | disabled, removed from `check`    | cleared                     |
| `FAIL`    | build fails while sources remain  | —                           |
| `ALIAS`   | runs the unit suite               | adopted by `unitTest`       |
| `KEEP`    | untouched                         | untouched                   |

### `DISABLE`

The default. The `test` task is disabled and removed from `check`, and the legacy source
directories are cleared so nothing is compiled twice.

The task itself stays in the task graph — Gradle has no API for removing it — but it is skipped
rather than executed. On multiplatform projects the edge from `check` to the Kotlin plugin's
aggregate test task cannot be unwired either, for the same reason; the per-target test tasks are
still disabled, so the outcome is the same.

### `FAIL`

Fails the build while `src/test/kotlin` (or, on multiplatform, `src/commonTest/kotlin` and
`src/<target>Test/kotlin`) still contains sources, naming the directories and their file counts.

This is the policy to finish a migration with. Under `DISABLE`, a file left behind in `src/test`
stops running without anything failing, which is the failure mode worth spending a build error on.
Set it once the move is done and it stays a guard.

### `ALIAS`

`./gradlew test` runs the unit suite, so every tool and habit that hard-codes that name keeps
working. The legacy source directories are adopted by the `unitTest` suite rather than cleared, so
an unmoved tree keeps compiling and running from where it is — on multiplatform, tier for tier:
`commonTest` into `commonUnitTest`, `jvmTest` into `jvmUnitTest`.

The `test` task itself is disabled and only carries the dependency; left enabled it would fail on
an empty source set.

> Adopting the sources does not adopt the dependencies. Anything declared with
> `testImplementation(...)` still belongs to the legacy configuration, and the adopted files will
> not compile until it is declared on the suite instead:
>
> ```kotlin
> suites {
>     named("unitTest") {
>         dependencies {
>             implementation("org.jetbrains.kotlin:kotlin-test-junit5:2.4.10")
>         }
>     }
> }
> ```
>
{style="warning"}

### `KEEP`

The conventional source set, its task and its `check` edge stay exactly as they are, with the
suites beside them. The escape hatch for a project that is not ready to migrate, and the policy
to use on a multiplatform project whose Native, JS or Wasm targets still need their conventional
test source sets — those targets cannot carry a named suite.

## Source directories on their own

`legacySourceDirectories` decides what happens to the directories, separately from what happens to
the task. It is derived from the policy and rarely needs setting:

| Value    | Effect                                                        | Implied by         |
|----------|---------------------------------------------------------------|--------------------|
| `CLEAR`  | legacy directories cleared; the compilation reports NO-SOURCE  | `DISABLE`, `FAIL`  |
| `ADOPT`  | legacy directories added to the unit suite, then cleared       | `ALIAS`            |
| `KEEP`   | left registered on the legacy source set                       | `KEEP`             |

Set it explicitly to combine them differently — for instance to disable the legacy task while
leaving its directories in place.

## A migration in three steps

1. `legacyTestSourceSet = LegacyTestPolicy.ALIAS`. Nothing moves; the existing tree runs as the
   unit suite. Move `testImplementation` declarations onto the suite.
2. Move the files. Fast tests to `src/unitTest/kotlin`, anything that needs a container or a
   network to `src/integrationTest/kotlin`, and add the integration-only dependencies to that
   suite.
3. `legacyTestSourceSet = LegacyTestPolicy.FAIL`. Regenerate `gradle.lockfile` if the project uses
   dependency locking — the suite classpaths are locked too, and the integration dependencies are
   new to it.

<seealso>
    <category ref="related">
        <a href="Testing-Suites.md">Test suites</a>
        <a href="Testing-Configuration-Reference.md">Configuration reference</a>
    </category>
</seealso>
