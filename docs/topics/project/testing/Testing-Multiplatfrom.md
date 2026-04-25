# Multiplatform Testing

In Kotlin Multiplatform projects, Kreate configures two distinct categories of test tasks
separately, because they are implemented by different Gradle task types with different APIs.

## Task Types

| Task Type                                      | Kotlin Plugin                         | Targets            | Configured By                  |
|------------------------------------------------|---------------------------------------|--------------------|--------------------------------|
| `org.gradle.api.tasks.testing.Test`            | `kotlin.multiplatform` + `kotlin.jvm` | JVM targets        | `tasks.withType<Test>()`       |
| `org.jetbrains.kotlin.gradle.tasks.KotlinTest` | `kotlin.multiplatform`                | Native, JS targets | `tasks.withType<KotlinTest>()` |

Both task types receive the same `timeout`, `ignoreFailures`, `failOnNoDiscoveredTests`,
`alwaysRunTests`, `logging`, and `report` settings. Only `maxParallelForks` and
`useJUnitPlatform()` are exclusive to JVM `Test` tasks.

## Required Plugins

Kreate validates at configuration time that the following plugins are applied before
`tests { enabled.set(true) }` can proceed in a multiplatform project:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp) // com.google.devtools.ksp
    alias(libs.plugins.kotest.multiplatform) // io.kotest.multiplatform
    alias(libs.plugins.kreate)
}
```

If `ksp` is absent:

```
IllegalStateException: You need to apply the 'ksp' plugin by yourself in multiplatform projects if you want to use kotest.
```

If `kotest` is absent:
```
IllegalStateException: You need to apply the 'kotest' plugin by yourself in multiplatform projects if you want to use kotest.
```


Both checks run immediately during the `afterEvaluate` phase — before any test task
configuration is applied.

## JVM Target

For the JVM target in a multiplatform project, Kreate configures `Test` tasks identically
to a pure Kotlin JVM project: `useJUnitPlatform()` is applied, and `maxParallelForks` takes
effect.

Write JVM-specific Kotest specs in `jvmTest/kotlin/`:

```kotlin
// jvmTest/kotlin/com/example/MySpec.kt
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MySpec : FunSpec({
    test("addition works") {
        (1 + 1) shouldBe 2
    }
})
```

Run with:

```bash
./gradlew jvmTest
```

## Native Targets

For native targets (e.g., `linuxX64`, `macosArm64`, `mingwX64`), Kreate configures
`KotlinTest` tasks. These tasks do not use the JUnit Platform — the Kotlin/Native test
runner is embedded directly in the compiled binary.

Write shared tests in `commonTest/kotlin/`:

```kotlin
// commonTest/kotlin/com/example/CommonSpec.kt
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CommonSpec : FunSpec({
    test("string length") {
        "hello".length shouldBe 5
    }
})
```

Run native tests with:

```bash
./gradlew linuxX64Test
./gradlew macosArm64Test
```

## Configuration Scope

All settings from `tests { }` apply uniformly to every test task across all targets.
There is currently no per-target override mechanism within the `tests { }` block — if you
need target-specific test task configuration, use the standard Kotlin Multiplatform DSL
directly:

```kotlin
kotlin {
    linuxX64 {
        compilations.getByName("test").defaultSourceSet {
            // target-specific test source sets
        }
    }
}
```