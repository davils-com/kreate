# Testing examples

<link-summary>Complete, working test configurations to copy.</link-summary>

<card-summary>From a minimal setup to a full multi-suite build.</card-summary>

Every example here is a complete `kreate { }` block unless stated otherwise. The Kotlin plugin is
the only one your `plugins { }` block needs — %product% applies the rest. See
[](Plugin-Management.md).

## Minimal

Both default suites, all defaults.

```kotlin
kreate {
    project {
        tests {
            enabled = true
        }
    }
}
```

```kotlin
// src/unitTest/kotlin/com/example/GreeterTest.kt
package com.example

import kotlin.test.Test
import kotlin.test.assertEquals

class GreeterTest {
    @Test
    fun greets() = assertEquals("Hello, world", Greeter().greet("world"))
}
```

Declare the framework on the suite — %product% picks the JUnit Platform, not an engine:

```kotlin
tests {
    enabled = true

    suites {
        named("unitTest") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-junit5:2.4.0")
            }
        }
    }
}
```

```bash
./gradlew check
```

## Kotest instead

```kotlin
kreate {
    project {
        tests {
            enabled = true

            kotest {
                enabled = true
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

Nothing else — no engine, no launcher, no `useJUnitPlatform()`. See [](Testing-Kotest.md).

## Integration tests with Testcontainers

```kotlin
kreate {
    project {
        tests {
            enabled = true
            kotest { enabled = true }

            suites {
                named("integrationTest") {
                    // These tests share one container, so they must not fork.
                    maxParallelForks = 1
                    timeoutMinutes = 30L

                    environment = mapOf("TESTCONTAINERS_REUSE_ENABLE" to "true")

                    dependencies {
                        platform("org.testcontainers:testcontainers-bom:1.20.4")
                        implementation("org.testcontainers:postgresql")
                        implementation("org.testcontainers:junit-jupiter")
                        runtimeOnly("org.postgresql:postgresql:42.7.4")
                    }
                }
            }
        }
    }
}
```

```bash
./gradlew check              # unit tests only
./gradlew integrationTest    # containers, on demand
./gradlew check integrationTest   # both, cheap failures first
```

## Sharing fixtures between suites

```kotlin
tests {
    enabled = true

    suites {
        named("integrationTest") {
            dependsOnSuites = listOf("unitTest")
        }
    }
}
```

```kotlin
// src/unitTest/kotlin/com/example/Fixtures.kt
internal object Fixtures {
    val sampleUser = User(name = "ada", email = "ada@example.com")
}
```

```kotlin
// src/integrationTest/kotlin/com/example/UserRepositoryTest.kt
class UserRepositoryTest {
    @Test
    fun roundTripsAUser() {
        repository.save(Fixtures.sampleUser)
        assertEquals(Fixtures.sampleUser, repository.findByName("ada"))
    }
}
```

`internal` works across the two because the compilations are associated, not because anything was
made public for the test.

## A third suite

```kotlin
tests {
    enabled = true

    suites {
        register("contractTest") {
            runOnCheck = false
            includeTags = listOf("contract")
            mustRunAfterSuites = listOf("unitTest")

            dependencies {
                implementation("org.junit.jupiter:junit-jupiter:6.1.3")
            }
        }
    }
}
```

Gives `src/contractTest/kotlin`, the `contractTest*` configurations and a `contractTest` task.

## Migrating an existing project

Step one — nothing moves, everything keeps running:

```kotlin
import com.davils.kreate.module.project.tests.LegacyTestPolicy

kreate {
    project {
        tests {
            enabled = true
            legacyTestSourceSet = LegacyTestPolicy.ALIAS

            suites {
                named("unitTest") {
                    dependencies {
                        // Moved off `testImplementation`, where they no longer apply.
                        implementation("org.jetbrains.kotlin:kotlin-test-junit5:2.4.0")
                    }
                }
            }
        }
    }
}
```

Step three — the move is done, and the build now says so if anything reappears:

```kotlin
tests {
    enabled = true
    legacyTestSourceSet = LegacyTestPolicy.FAIL
}
```

See [](Testing-Suites-Migration.md) for step two and the rest.

## Multiplatform

```kotlin
kotlin {
    jvm()
    linuxX64()
    wasmJs { nodejs() }
}

kreate {
    project {
        tests {
            enabled = true

            // Native and Wasm cannot carry a named suite, so their conventional test
            // source sets stay in place beside the suites.
            legacyTestSourceSet = LegacyTestPolicy.KEEP

            suites {
                named("integrationTest") {
                    targets = listOf("jvm")
                    maxParallelForks = 1
                }
            }
        }
    }
}
```

```
src/commonUnitTest/kotlin        → compiled by jvmUnitTest
src/jvmUnitTest/kotlin           → jvmUnitTest
src/commonTest/kotlin            → linuxX64Test, wasmJsNodeTest  (kept)
src/jvmIntegrationTest/kotlin    → jvmIntegrationTest
```

```bash
./gradlew unitTest           # every JVM target's unit suite
./gradlew jvmUnitTest        # one target
./gradlew linuxX64Test       # still there, because of KEEP
```

See [](Testing-Multiplatform.md).

## Tuned for CI

```kotlin
tests {
    enabled = true
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    timeoutMinutes = 15L
    failOnNoDiscoveredTests = true
    ignoreFailures = false

    logging {
        // Only failures. A CI log nobody reads is worse than no log.
        logPassedTests = false
        logSkippedTests = false
    }

    report {
        enabled = true
        xml = true
        html = false
    }

    suites {
        named("integrationTest") {
            maxParallelForks = 1
            timeoutMinutes = 45L
            alwaysRun = true
        }
    }
}
```

```yaml
# One job for the fast feedback, one for the slow truth.
- run: ./gradlew check
- run: ./gradlew integrationTest
```

See [](CI-Integration.md).

## Splitting fast and slow within one suite

When the split is a property of individual tests rather than of whole source sets, tags are the
smaller tool:

```kotlin
tests {
    enabled = true

    suites {
        named("unitTest") {
            excludeTags = listOf("slow")
        }

        register("slowTest") {
            runOnCheck = false
            sourceSetName = "unitTest"   // same code, different selection
            includeTags = listOf("slow")
        }
    }
}
```

## Continuing the build after a failure

For a pipeline that collects reports itself and decides afterwards:

```kotlin
tests {
    enabled = true
    ignoreFailures = true

    report {
        enabled = true
        xml = true
    }
}
```

## Everything at once

```kotlin
import com.davils.kreate.module.project.tests.LegacySourceDirectories
import com.davils.kreate.module.project.tests.LegacyTestPolicy
import com.davils.kreate.module.project.tests.suite.KotestModule

kreate {
    project {
        tests {
            enabled = true
            maxParallelForks = 4
            timeoutMinutes = 10L
            ignoreFailures = false
            alwaysRunTests = false
            failOnNoDiscoveredTests = true

            legacyTestSourceSet = LegacyTestPolicy.FAIL
            legacySourceDirectories = LegacySourceDirectories.CLEAR
            excludeSuitesFromCoverage = true

            kotest {
                enabled = true
                version = "6.2.4"
                modules = listOf(KotestModule.ASSERTIONS, KotestModule.PROPERTY)
                addJUnitPlatformLauncher = true
                junitPlatformVersion = "6.1.3"
            }

            logging {
                logPassedTests = true
                logSkippedTests = true
                logTestStarted = false
            }

            report {
                enabled = true
                xml = true
                html = true
            }

            suites {
                named("unitTest") {
                    maxParallelForks = 8
                    excludeTags = listOf("slow")
                }

                named("integrationTest") {
                    runOnCheck = false
                    mustRunAfterSuites = listOf("unitTest")
                    dependsOnSuites = listOf("unitTest")
                    maxParallelForks = 1
                    timeoutMinutes = 45L
                    targets = listOf("jvm")

                    systemProperties = mapOf("spring.profiles.active" to "integration")
                    environment = mapOf("TESTCONTAINERS_RYUK_DISABLED" to "true")
                    jvmArgs = listOf("-Xmx2g")

                    dependencies {
                        platform("org.testcontainers:testcontainers-bom:1.20.4")
                        implementation("org.testcontainers:postgresql")
                        runtimeOnly("org.postgresql:postgresql:42.7.4")
                    }

                    report { html = false }
                }

                register("contractTest") {
                    runOnCheck = false
                    includeTags = listOf("contract")
                    srcDirs = listOf("src/contract/kotlin")

                    kotest {
                        modules = listOf(KotestModule.ASSERTIONS, KotestModule.JUNIT_XML)
                    }
                }
            }
        }
    }
}
```

<seealso>
    <category ref="project">
        <a href="Testing-Overview.md">Overview</a>
        <a href="Testing-Suites.md">Test suites</a>
        <a href="Testing-Configuration-Reference.md">Configuration reference</a>
        <a href="Testing-Troubleshooting.md">Troubleshooting</a>
    </category>
</seealso>
