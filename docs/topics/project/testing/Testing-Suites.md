# Test suites

<link-summary>Named source sets, configurations and tasks for each kind of test.</link-summary>

<card-summary>unitTest and integrationTest instead of one test source set.</card-summary>

<tldr>
<p><b>Default</b>: <code>unitTest</code> and <code>integrationTest</code></p>
<p><b>Replaces</b>: the conventional <code>test</code> source set</p>
<p><b>Extend</b>: <code>suites { register("contractTest") { } }</code></p>
</tldr>

A suite is what the conventional `test` source set should have been: a name, a place on disk, a
dependency scope and a task, all of which can exist more than once in a project. Two are
registered by default, and both are ordinary members of a container you can add to.

## What one suite creates

On a Kotlin/JVM project, a suite named `integrationTest` gives you:

<deflist type="wide">
    <def title="A source set">
        <code>src/integrationTest/kotlin</code>, <code>src/integrationTest/java</code> and
        <code>src/integrationTest/resources</code>. The Kotlin plugin mirrors every Java source set
        into a Kotlin compilation, so the Kotlin directory appears without being asked for.
    </def>
    <def title="Dependency configurations">
        <code>integrationTestImplementation</code>, <code>integrationTestApi</code>,
        <code>integrationTestCompileOnly</code>, <code>integrationTestRuntimeOnly</code>,
        <code>integrationTestCompileClasspath</code> and
        <code>integrationTestRuntimeClasspath</code> — the same set the <code>test</code> source
        set has.
    </def>
    <def title="A compilation">
        <code>compileIntegrationTestKotlin</code>, associated with <code>main</code> so that
        <code>internal</code> declarations are visible.
    </def>
    <def title="A test task">
        <code>integrationTest</code>, in Gradle's <code>verification</code> group, running on the
        JUnit Platform.
    </def>
</deflist>

## The two default suites

| Suite             | Source set                   | Task              | On `check` | Ordered after |
|-------------------|------------------------------|-------------------|------------|---------------|
| `unitTest`        | `src/unitTest/kotlin`        | `unitTest`        | yes        | —             |
| `integrationTest` | `src/integrationTest/kotlin` | `integrationTest` | no         | `unitTest`    |

`integrationTest` is deliberately off `check`. A suite that needs Docker or a network cannot be a
precondition of every local build, and a developer who cannot run `check` offline stops running it
at all. Run it when you mean to:

```bash
./gradlew integrationTest
```

The ordering is `mustRunAfter`, not `dependsOn`. Asking for `integrationTest` alone does **not**
drag the unit suite into the build; asking for both runs the cheap failures first.

```bash
./gradlew check integrationTest   # unitTest, then integrationTest
./gradlew integrationTest         # only integrationTest
```

## Declaring dependencies

A suite's dependencies are declared **on the suite**:

```kotlin
tests {
    enabled = true

    suites {
        named("integrationTest") {
            dependencies {
                platform("org.testcontainers:testcontainers-bom:1.20.4")
                implementation("org.testcontainers:postgresql")
                implementation("org.junit.jupiter:junit-jupiter:6.1.3")
                runtimeOnly("org.postgresql:postgresql:42.7.4")
            }
        }
    }
}
```

| Function                 | Configuration                       | Use for                                        |
|--------------------------|-------------------------------------|------------------------------------------------|
| `implementation(String)` | `<suite>Implementation`             | Anything the tests reference by name           |
| `compileOnly(String)`    | `<suite>CompileOnly`                | Annotations and API that is not needed at run  |
| `runtimeOnly(String)`    | `<suite>RuntimeOnly`                | Drivers, logging backends, service providers   |
| `platform(String)`       | `<suite>Implementation` as platform | A bill of materials aligning several versions  |

<warning>
This is not a stylistic preference. A suite's configurations are created after the build script
has been evaluated, so naming <code>integrationTestImplementation</code> in the project's top level
<code>dependencies { }</code> block fails with <code>Configuration with name
'integrationTestImplementation' not found</code>. Gradle's own <code>JvmTestSuite</code> carries
its dependencies for exactly the same reason.
</warning>

%product% declares nothing of its own here. Testcontainers modules, drivers and clients are the
project's decision and the project's versions. The one exception is the Kotest bundle, which is
the framework the suite runs on rather than a library it talks to — see [](Testing-Kotest.md).

### What the suite already has

A suite associated with `main` inherits `main`'s own dependencies, so anything on the production
compile classpath is on the suite's too. You only declare what the tests themselves need.

## Seeing `internal` declarations

Every suite's compilation is associated with `main`. That association is what puts `main` on the
classpath **and** what registers the friend path that makes `internal` declarations visible:

```kotlin
// src/main/kotlin/com/example/Greeter.kt
internal fun normalize(name: String): String = name.trim()

// src/unitTest/kotlin/com/example/NormalizeTest.kt — compiles
class NormalizeTest {
    @Test
    fun trims() = assertEquals("x", normalize("  x  "))
}
```

Turn it off for a suite that should only exercise the published surface:

```kotlin
suites {
    named("integrationTest") {
        associateWithMain = false
    }
}
```

<note>
This is why %product% does not build suites out of Gradle's <code>JvmTestSuite</code>. A test
suite wires itself to the code under test with <code>implementation(project())</code>, which is
the <i>published</i> view of the project: <code>internal</code> declarations stay invisible, and
restoring them means adding the compilation association anyway — at which point the production
classes are on the classpath twice.
</note>

## Sharing fixtures between suites

A suite can use another suite's compiled output, including its `internal` declarations:

```kotlin
suites {
    named("integrationTest") {
        dependsOnSuites = listOf("unitTest")
    }
}
```

```kotlin
// src/unitTest/kotlin/com/example/Fixtures.kt
internal object Fixtures {
    val sampleUser = User("ada")
}

// src/integrationTest/kotlin/com/example/UserRepositoryTest.kt — compiles
class UserRepositoryTest {
    @Test
    fun roundTrips() = repository.save(Fixtures.sampleUser)
}
```

Two settings are easy to confuse, so they are deliberately separate:

| Setting              | Answers                                      | Pulls the other suite into the build? |
|----------------------|----------------------------------------------|---------------------------------------|
| `dependsOnSuites`    | Can this suite *see* that suite's classes?   | No                                    |
| `mustRunAfterSuites` | If both run, which runs *first*?             | No                                    |

Neither implies the other. A suite can share fixtures without any ordering relationship, and can
be ordered after a suite whose classes it never touches.

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

That gives `src/contractTest/kotlin`, the `contractTest*` configurations and a `contractTest` task,
exactly like the two registered by default. A new suite starts with `enabled = true`,
`runOnCheck = true` and `associateWithMain = true`, and inherits every execution setting from the
enclosing `tests { }` block.

### Pointing a suite at existing directories

`srcDirs` replaces the conventional layout rather than adding to it:

```kotlin
suites {
    register("smokeTest") {
        srcDirs = listOf("src/smoke/kotlin", "src/smoke/generated")
    }
}
```

Paths are resolved relative to the project directory. Leave it empty to keep the conventional
`src/<suite>/kotlin`.

### Giving a suite a different source set name

`sourceSetName` separates the task name from the directory name:

```kotlin
suites {
    register("it") {
        // task `it`, directory src/integrationTest/kotlin
        sourceSetName = "integrationTest"
    }
}
```

## Filtering by tag

`includeTags` and `excludeTags` are JUnit Platform tag expressions applied to that suite alone:

```kotlin
suites {
    named("unitTest") {
        excludeTags = listOf("slow")
    }

    register("contractTest") {
        includeTags = listOf("contract")
    }
}
```

`includeTags` is exclusive: a non-empty list runs those tags and nothing else. Both work with
Kotest's `@Tags` as well, because Kotest maps them onto JUnit Platform tags.

## Per-suite execution settings

Everything a test JVM needs can be set per suite, which is usually the point — an integration
suite's switches have no business on every other test task in the project:

```kotlin
suites {
    named("integrationTest") {
        maxParallelForks = 1
        timeoutMinutes = 45L
        ignoreFailures = false
        alwaysRun = true

        systemProperties = mapOf("spring.profiles.active" to "integration")
        environment = mapOf("TESTCONTAINERS_RYUK_DISABLED" to "true")
        jvmArgs = listOf("-Xmx2g", "-XX:+HeapDumpOnOutOfMemoryError")

        logging { logPassedTests = false }
        report { enabled = true; xml = true }
    }
}
```

### Inheritance

| Inherited from `tests { }` | Suite-only                                      |
|----------------------------|-------------------------------------------------|
| `maxParallelForks`         | `includeTags`, `excludeTags`                    |
| `timeoutMinutes`           | `systemProperties`, `environment`, `jvmArgs`    |
| `ignoreFailures`           | `srcDirs`, `sourceSetName`, `description`       |
| `alwaysRun`                | `associateWithMain`, `dependsOnSuites`          |
| `failOnNoDiscoveredTests`  | `runOnCheck`, `mustRunAfterSuites`, `targets`   |
| `logging { }`              |                                                 |
| `report { }`               |                                                 |
| `kotest { }`               |                                                 |

A value set on the suite always wins. A value set on neither falls back to the documented default
in [](Testing-Configuration-Reference.md).

<note>
<code>alwaysRunTests</code> on <code>tests { }</code> is called <code>alwaysRun</code> on a suite.
The shorter name reads better in a block that is already about one suite; the behaviour is
identical.
</note>

## Turning a default suite off

Both default suites are ordinary container members, so a project that wants only one disables the
other rather than working around it:

```kotlin
suites {
    named("integrationTest") { enabled = false }
}
```

A disabled suite creates no source set, no configurations and no task.

## Multiplatform

On a multiplatform project a suite gives a shared source set and one per JVM target:

```
src/commonUnitTest/kotlin          commonUnitTestImplementation
src/jvmUnitTest/kotlin             jvmUnitTestImplementation
                                   task jvmUnitTest       (KotlinJvmTest)
                                   task unitTest          (runs every target's task)
src/commonIntegrationTest/kotlin   commonIntegrationTestImplementation
src/jvmIntegrationTest/kotlin      task jvmIntegrationTest, task integrationTest
```

Per-target task names follow the Kotlin plugin's own rule, so a suite's task sits beside the
`jvmTest` it replaces rather than looking bolted on. Suite dependencies land on the shared source
set. See [](Testing-Multiplatform.md) for the full picture, including which targets can carry a
suite and which cannot.

## Coverage

Suite source sets are excluded from the coverage measurement. The coverage engine recognises a
test compilation by the name `test` and nothing else, so without this a suite's own code would be
counted as production code in the denominator — a number that stays green while describing the
wrong thing.

```kotlin
tests {
    // Default. Set to false only if you deliberately want the suites measured.
    excludeSuitesFromCoverage = true
}
```

The exclusion is skipped when `coverage { sources { includedSourceSets } }` is non-empty: an
explicit include list already decides what counts, and adding exclusions to it would only be a
second opinion. See [](Coverage-Configuration.md).

## Dependency locking

Suite classpaths are locked along with the rest, so an integration suite's dependencies reach
`gradle.lockfile` and any scan that reads it. Regenerate the lock files after adding a suite or a
suite dependency:

```bash
./gradlew kreateResolveAndLockAll --write-locks
```

Without this the lock file would be missing precisely the dependencies that reach outside the
process, while still looking complete to a reader and to Trivy. See [](Dependency-Locking.md).

## Static analysis

Detekt registers a task per source set, so the suites are analysed like any other code:
`detektUnitTestSourceSet`, `detektIntegrationTestSourceSet`, and one for each suite you register.
Nothing to configure. See [](Detekt-Overview.md).

## API validation and publishing

Neither looks at a suite. ABI validation reads `main`, and a suite's source set is never part of a
publication — no `integrationTest` scope appears in a published POM.

## Validation

%product% fails the build at configuration time rather than producing something subtly wrong:

| Condition                                                | Message                                               |
|----------------------------------------------------------|-------------------------------------------------------|
| A suite's source set name is `main`                       | names the production source set as the owner          |
| A suite's source set name is the benchmark source set     | names the benchmark source set as the owner           |
| A suite's source set name is `test` under `KEEP`          | names the legacy source set as the owner              |
| Two suites share one source set name                      | names both suites                                     |
| `dependsOnSuites` or `mustRunAfterSuites` names a stranger | lists the suites that do exist                        |

The reason these are errors rather than warnings is `maybeCreate`: asked for a name that is
already taken, Gradle hands back the existing source set. A suite called `main` would quietly turn
the production code into a test suite and run it.

<seealso>
    <category ref="project">
        <a href="Testing-Overview.md">Overview</a>
        <a href="Testing-Suites-Migration.md">Migrating from src/test</a>
        <a href="Testing-Kotest.md">The Kotest bundle</a>
        <a href="Testing-Multiplatform.md">Multiplatform testing</a>
        <a href="Testing-Configuration-Reference.md">Configuration reference</a>
        <a href="Testing-Troubleshooting.md">Troubleshooting</a>
    </category>
</seealso>
