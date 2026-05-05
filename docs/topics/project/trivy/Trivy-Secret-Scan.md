# Secret Scanning

Secret scanning helps you detect accidentally hardcoded credentials, such as API keys, OAuth tokens, and passwords, before they are leaked or committed to version control.

## Overview

Unlike license or vulnerability scans that focus on external dependencies, secret scanning analyzes your own source code and configuration files. Trivy uses an extensive library of rule sets to detect common formats of secrets (e.g., AWS Keys, GitHub Tokens, Google API Keys).

## Configuration Example

```kotlin
kreate {
    project {
        trivy {
            enabled.set(true)
            secrets {
                // Fail the build if any secrets are found
                failOnFindings.set(true)

                // Define which severities should be reported
                severity.set(listOf(SecretSeverity.CRITICAL, SecretSeverity.HIGH))

                // Define which files to scan
                sourceFiles.from(fileTree(projectDir) {
                    include("src/**/*.kt", "src/**/*.java", "**/*.yaml", "**/*.properties", "**/*.json")
                    exclude("**/build/**")
                })

                // Path to a custom Trivy secret configuration
                secretConfig.set(rootProject.layout.projectDirectory.file("trivy-secret.yaml"))
            }
        }
    }
}
```

## Customizing Secret Detection

Trivy allows you to customize secret detection using a configuration file (e.g., `trivy-secret.yaml`). In this file, you can define custom regex patterns or set up allowlists (exceptions).

By default, Kreate looks for a file named `trivy-secret.yaml` in the root directory of your project.

Example `trivy-secret.yaml`:

```yaml
secrets:
  rules:
    - id: custom-api-key
      name: My Custom API Key
      regex: 'my-app-[a-z0-9]{32}'
```

## How it Works

The `trivySecretScan` task runs Trivy in `fs` (file system) mode with the `secret` scanner enabled. It specifically targets the files defined in `sourceFiles`.

### Severity Levels

Severity levels help assess the risk of a finding:

| Severity | Example Findings |
| :--- | :--- |
| `CRITICAL` | Highly sensitive secrets (e.g., AWS Secret Access Keys, Private SSH Keys). |
| `HIGH` | Important credentials (e.g., GitHub Personal Access Tokens). |
| `MEDIUM` | General secrets or API tokens with restricted permissions. |
| `LOW` | Low-risk tokens or test credentials. |

## Tips & Best Practices

*   **Pre-commit Hooks**: While Kreate runs the scan as a Gradle task, it is highly recommended to also use pre-commit hooks to get immediate feedback before a `git commit` is even performed.
*   **Clean History**: If a secret is found, **it is not enough** to just delete it and commit again. The secret remains in your Git history.
    1.  Revoke the secret (rotation) immediately!
    2.  If necessary, remove it from the history using tools like `bfg-repo-cleaner` or `git-filter-repo`.
*   **Narrow Scope**: Only scan files that might actually contain secrets. Scanning large binary files or build artifacts will slow down the task unnecessarily and may lead to false positives.

<warning>
Once a secret is published, it must be considered compromised. Rotating the secret is the only secure countermeasure.
</warning>

<tip>
Use the <code>exclude</code> block in the <code>fileTree</code> of <code>sourceFiles</code> to exclude generated files or test resources that contain harmless test keys from the scan.
</tip>

## How to Run

To execute the secret scan individually, use the following Gradle command:

<code-block lang="bash">
./gradlew trivySecretScan
</code-block>

Alternatively, the scan is automatically included when running the lifecycle task:

<code-block lang="bash">
./gradlew check
</code-block>