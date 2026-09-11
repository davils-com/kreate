# The Kotest bundle

<link-summary>The one dependency Kreate declares for a suite.</link-summary>

<card-summary>Kotest artifacts and the JUnit Platform launcher.</card-summary>

<tldr>
<p><b>Default</b>: disabled</p>
<p><b>Adds</b>: the Kotest runner and the modules you list</p>
</tldr>

Kreate creates a suite's source set and its configurations and otherwise declares nothing: the
Testcontainers modules, drivers and clients a suite needs are the project's decision and the
project's versions. Kotest is the exception, because it is not something a suite talks to but what
the suite runs on — a test source set without a framework is an empty task that passes, and
pinning one version across a repository is the kind of decision a build convention exists to make.

It is off by default, so a project that brings its own framework is unaffected.

```kotlin
kreate {
    project {
        tests {
            enabled = true

            kotest {
                enabled = true
                version = "6.2.4"
                modules = listOf(KotestModule.ASSERTIONS, KotestModule.PROPERTY)
            }
        }
    }
}
```

Set on `tests { }` it applies to every suite; set inside `suites { named("...") { kotest { } } }`
it applies to that suite alone, overriding the value it would otherwise inherit.

## What gets added

| Platform                  | Artifact                          |
|---------------------------|-----------------------------------|
| Kotlin/JVM                | `io.kotest:kotest-runner-junit5`  |
| Multiplatform, shared     | `io.kotest:kotest-framework-engine` |
| Multiplatform, JVM target | `io.kotest:kotest-runner-junit5`  |

The runner is not configurable: it follows from the platform. The optional libraries are:

| `KotestModule` | Artifact                          |
|----------------|-----------------------------------|
| `ASSERTIONS`   | `kotest-assertions-core` (default) |
| `PROPERTY`     | `kotest-property`                 |
| `DATATEST`     | `kotest-framework-datatest`       |
| `JUNIT_XML`    | `kotest-extensions-junitxml`      |

One version applies to the whole bundle: the Kotest modules are released together, and mixing them
produces link errors rather than a resolution failure.

`kotest-runner-junit5` is a JUnit Platform engine, so it needs no extra configuration — the suite
already runs on the JUnit Platform, and `includeTags` and `excludeTags` reach Kotest specs
unchanged.

## The JUnit Platform launcher

Whether or not the Kotest bundle is enabled, Kreate adds `org.junit.platform:junit-platform-launcher`
to every suite's runtime classpath. Gradle adds a launcher on its own only to the `test` suite it
created itself; a suite's task is registered by Kreate, so without this it fails before running a
single test with `Failed to load JUnit Platform`.

The version defaults to the JUnit 6 line. A project still on JUnit 5 should say so, because the
launcher and the engines it starts are released and versioned together:

```kotlin
kotest {
    junitPlatformVersion = "1.11.4"
}
```

Or declare the launcher yourself and turn Kreate's off:

```kotlin
kotest {
    addJUnitPlatformLauncher = false
}
```

> Kotest on Native, JS or Wasm needs the Kotest Gradle plugin to generate an entry point. It never
> comes up here, because a named suite only exists on JVM targets.
>
{style="note"}

## A complete example

```kotlin
import com.davils.kreate.module.project.tests.suite.KotestModule

kreate {
    project {
        tests {
            enabled = true

            kotest {
                enabled = true
                modules = listOf(KotestModule.ASSERTIONS, KotestModule.PROPERTY)
            }

            suites {
                named("integrationTest") {
                    kotest {
                        // This suite writes its own JUnit XML for the CI parser.
                        modules = listOf(KotestModule.ASSERTIONS, KotestModule.JUNIT_XML)
                    }
                }
            }
        }
    }
}
```

```kotlin
// src/unitTest/kotlin/com/example/GreeterSpec.kt
package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GreeterSpec : StringSpec({
    "greets by name" {
        Greeter().greet("world") shouldBe "Hello, world"
    }
})
```

```bash
./gradlew unitTest
```

Nothing else is declared: no framework in the top level `dependencies { }` block, no
`useJUnitPlatform()`, no launcher.

## Tag filtering

Kotest maps its own `@Tags` onto JUnit Platform tags, so a suite's `includeTags` and `excludeTags`
reach Kotest specs unchanged:

```kotlin
suites {
    named("unitTest") {
        excludeTags = listOf("slow")
    }
}
```

## Version alignment

One version applies to the whole bundle. The Kotest modules are released together, and mixing them
produces link errors at run time rather than a resolution failure at configuration time — which is
why the version is a single property rather than one per module.

The JUnit Platform launcher is versioned separately because it belongs to JUnit, not to Kotest.
Keep it in step with whatever JUnit line the project is on:

| Project uses    | `junitPlatformVersion` |
|-----------------|------------------------|
| JUnit 6.x       | `6.x` (the default)    |
| JUnit 5.x       | the matching `1.x`     |

## Using a different framework

Leave the bundle disabled and declare whatever you use on the suite:

```kotlin
suites {
    named("unitTest") {
        dependencies {
            implementation("org.junit.jupiter:junit-jupiter:6.1.3")
        }
    }
}
```

The launcher is still added, because that is what starts the JUnit Platform rather than part of
any one framework.

<seealso>
    <category ref="related">
        <a href="Testing-Overview.md">Overview</a>
        <a href="Testing-Suites.md">Test suites</a>
        <a href="Testing-Configuration-Reference.md">Configuration reference</a>
        <a href="Testing-Troubleshooting.md">Troubleshooting</a>
    </category>
</seealso>
