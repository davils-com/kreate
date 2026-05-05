# Detekt Reports

Kreate provides a dedicated `reports { }` block within the `detekt { }` configuration to manage the output of the static analysis. You can enable multiple report formats simultaneously to satisfy different requirements (e.g., HTML for human reading, SARIF for CI integration).

## Report Types

Four report formats are supported:

### 1. HTML Report (`html`)
Generates a stylized web page that allows for easy navigation through all detected issues, including code snippets and rule descriptions.

### 2. SARIF Report (`sarif`)
The **Static Analysis Results Interchange Format**. This is a standard JSON-based format supported by many CI/CD platforms like GitHub and GitLab to display security and quality warnings directly in pull requests.

### 3. Checkstyle Report (`checkstyle`)
An XML format compatible with the Checkstyle tool. Useful for integration with legacy CI systems or IDE plugins that expect Checkstyle-formatted data.

### 4. Markdown Report (`markdown`)
A simple text-based report in Markdown format. Useful for quick reviews or attaching to build artifacts in a readable form.

## Configuring Reports

Each report type is configured using a nested block. For example:

```kotlin
detekt {
    reports {
        html {
            required.set(true)
            outputLocation.set(layout.buildDirectory.file("qa/detekt-report.html"))
        }
        sarif {
            required.set(true)
        }
    }
}
```

### Common Properties

Every report block (`html`, `sarif`, `checkstyle`, `markdown`) supports the following properties:

- **`required`**: (`Property<Boolean>`) Whether this report should be generated.
- **`outputLocation`**: (`RegularFileProperty`) The file where the report will be written.

## Default Paths

If `outputLocation` is not specified, Kreate uses the following default locations (relative to the project's build directory):

| Report Type | Default Path                    |
|-------------|---------------------------------|
| HTML        | `reports/detekt/report.html`    |
| SARIF       | `reports/detekt/report.sarif`   |
| Checkstyle  | `reports/detekt/checkstyle.xml` |
| Markdown    | `reports/detekt/report.md`      |

## Advanced Report Customization

By default, Kreate ensures that all reports use the project's root build directory for centralized reporting if possible, or the local project build directory for subprojects. This ensures that you can always find your reports in a consistent location regardless of your project structure.
