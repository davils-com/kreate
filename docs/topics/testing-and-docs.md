# Testing and Documentation

Kreate provides built-in support for enhanced test logging, automated reporting, and integrated documentation generation using Dokka.

## Testing

The `tests` block allows you to configure how your tests are executed and how the results are presented.

```kotlin
kreate {
    project {
        tests {
            enabled = true
            maxParallelForks = 4
            timeoutMinutes = 15L
            ignoreFailures = false
            alwaysRunTests = true // Force tests to run even if they are up-to-date
            
            logging {
                logPassedTests = true // Show passed tests in the console
                logSkippedTests = true
                logTestStarted = true
            }
            
            report {
                enabled = true
                xml = true // Generate JUnit XML reports (useful for CI)
                html = true // Generate human-readable HTML reports
            }
        }
    }
}
```

### Options

| Option | Description | Default |
|--------|-------------|---------|
| `maxParallelForks` | The maximum number of test processes to start. | `1` |
| `timeoutMinutes` | The maximum duration of the test task. | `null` |
| `ignoreFailures` | Whether to continue the build even if tests fail. | `false` |
| `alwaysRunTests` | If `true`, the `test` task will always execute. | `false` |

## Documentation (Dokka)

Kreate makes it easy to generate API documentation for your Kotlin project using Dokka.

```kotlin
kreate {
    project {
        docs {
            enabled = true
            outputDirectory = "build/dokka-docs" // Path for generated docs
            moduleName = "MyProject"
            copyright = "Copyright © 2026 Example Corp"
        }
    }
}
```

### Generating Documentation

To generate your project's documentation, use the standard Dokka task:

```bash
./gradlew dokkaHtml
```

The documentation will be generated in the location specified by `outputDirectory`.
