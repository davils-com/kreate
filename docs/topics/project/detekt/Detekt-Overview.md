# Detekt Static Analysis

Detekt is a static code analysis tool for the Kotlin programming language. It operates on the abstract syntax tree provided by the Kotlin compiler and focuses on finding code smells, complexity issues, and potential bugs.

The `detekt { }` block inside `kreate { project { } }` provides a deeply integrated configuration for Detekt. Kreate automates the setup of the Detekt Gradle plugin, manages rule-set application, and configures reporting with professional defaults tailored for Kotlin development.

## Quick Start

Detekt is **disabled by default**. To activate it, set the `enabled` property to `true`:

```kotlin
kreate {
    project {
        detekt {
            enabled.set(true)
        }
    }
}
```

Once enabled, Kreate will automatically apply the `dev.detekt` plugin to your project and configure the standard tasks.

> Since Kreate applies the official Detekt Gradle plugin under the hood, you can still use all standard Detekt features.
>
{style="tip"}

## Why use Detekt?

Static analysis helps maintain a high-quality codebase by:
- **Enforcing Consistency**: Ensures all developers follow the same coding standards.
- **Reducing Technical Debt**: Catches complex or hard-to-maintain code early.
- **Finding Bugs**: Identifies potential issues that might not be caught by the compiler.
- **Education**: Helps developers learn Kotlin best practices through rule descriptions.

## Key Features in Kreate

- **Zero-Configuration**: Sensible defaults are provided out of the box.
- **Nested DSL**: Familiar configuration structure matching the original Detekt plugin.
- **Automated Reporting**: Generates HTML, SARIF, XML, and Markdown reports by default.
- **Baseline Support**: Easily use custom `detekt.yaml` configurations.
- **Extensible**: Add third-party rule sets like `detekt-formatting` with ease.



## Further Reading

To learn more about how to customize Detekt in your Kreate project, check out the following pages:

- [**Detekt Configuration**](Detekt-Configuration.md): Detailed reference of all core configuration properties.
- [**Detekt Reports**](Detekt-Reports.md): Learn how to configure and customize analysis reports.
