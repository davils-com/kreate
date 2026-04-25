# Examples

## Minimal Setup — Kotlin JVM

Enable testing with all defaults. Tests run on the JUnit Platform for JVM targets with half the available
CPU cores, a 10-minute timeout, and passed/skipped events logged to the console.

```kotlin
kreate {
    project {
        tests {
            enabled.set(true)
        }
    }
}
```

## Minimal Setup — Kotlin Multiplatform

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kreate)
}

kreate {
    project {
        tests {
            enabled.set(true)
        }
    }
}
```

## CI-Optimized Configuration

Maximizes parallelism, enforces a strict timeout, ensures tests always run, fails on empty
test suites, and produces XML reports for the CI test parser.

```kotlin
tests {
    enabled.set(true)
    maxParallelForks.set(Runtime.getRuntime().availableProcessors())
    timeoutMinutes.set(15)
    alwaysRunTests.set(true)
    failOnNoDiscoveredTests.set(true)
    ignoreFailures.set(false)

    report {
        enabled.set(true)
        xml.set(true)
        html.set(false)
    }
}
```

## Silent Failures for Reporting Pipelines

Continue the build even when tests fail, so downstream tasks like artifact collection
or report publishing still run.

```kotlin
tests {
    enabled.set(true)
    ignoreFailures.set(true)

    report {
        enabled.set(true)
        xml.set(true)
    }
}
```

## Verbose Console Output

Log every test lifecycle event — useful during local development to see exactly which
tests are running and in what order.

```kotlin
tests {
    enabled.set(true)
    logging {
        logPassedTests.set(true)
        logSkippedTests.set(true)
        logTestStarted.set(true)
    }
}
```

## HTML Reports for Local Review

Generate a browsable HTML test report alongside the standard XML output.

```kotlin
tests {
    enabled.set(true)
    report {
        enabled.set(true)
        xml.set(true)
        html.set(true)
    }
}
```

Open the report after the build at:
```build/reports/tests/<taskName>/index.html```


## Full Configuration

All available options combined:

```kotlin
kreate {
    project {
        name = "MyLibrary"
        projectGroup = group.toString()

        tests {
            enabled.set(true)
            maxParallelForks.set(4)
            timeoutMinutes.set(10)
            ignoreFailures.set(false)
            alwaysRunTests.set(false)
            failOnNoDiscoveredTests.set(true)

            logging {
                logPassedTests.set(true)
                logSkippedTests.set(true)
                logTestStarted.set(false)
            }

            report {
                enabled.set(true)
                xml.set(true)
                html.set(true)
            }
        }
    }
}
```
