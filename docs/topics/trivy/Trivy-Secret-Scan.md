# Secret Scanning

<link-summary>Finding hard-coded credentials in your sources.</link-summary>

<card-summary>Built-in and custom rules for detecting leaked secrets.</card-summary>

Secret scanning helps you detect accidentally hardcoded credentials, such as API keys, OAuth tokens, and passwords, before they are leaked or committed to version control.

## Overview

Unlike license or vulnerability scans that focus on external dependencies, secret scanning analyzes your own source code and configuration files. Trivy uses an extensive library of rule sets to detect common formats of secrets (e.g., AWS Keys, GitHub Tokens, Google API Keys).

## Configuration Example

```kotlin
kreate {
    trivy {
        enabled = true
        secrets {
            // Fail the build if any secrets are found
            failOnFindings = true

            // Define which severities should be reported
            severity = listOf(SecretSeverity.CRITICAL, SecretSeverity.HIGH)

            // Define which files to scan. setFrom replaces the default scope; from would add to it
            sourceFiles.setFrom(fileTree(projectDir) {
                include("src/**/*.kt", "src/**/*.java", "**/*.yaml", "**/*.properties", "**/*.json")
                exclude("**/build/**")
            })

            // Path to a custom Trivy secret configuration
            secretConfig = rootProject.layout.projectDirectory.file("trivy-secret.yaml")
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

The `kreateTrivySecretScan` task runs Trivy in `fs` (file system) mode with the `secret` scanner enabled. It specifically targets the files defined in `sourceFiles`.

### The default scope, and how to change it

Out of the box the scan covers Kotlin and Java under `src`, plus YAML, `.env`, properties and JSON
anywhere in the project. Generated output is deliberately left out - `build`, `.gradle` and
`.kotlin` - for two reasons. A secret under `build` is a copy of one in a file the scan already
reads, so it is a duplicate finding that costs the time it takes to walk every artifact there. More
importantly, Gradle refuses a build in which one task declares an input that is another task's
output: with `build` in scope, running `kreateTrivySecretScan` in the same invocation as a task that
writes a matching file there fails with a message about an undeclared task dependency rather than
about secrets.

<warning>
<code>from</code> <b>adds</b> to that default scope; only <code>setFrom</code> replaces it. This is
Gradle's behaviour for every file collection, and getting it wrong is invisible: a build that narrows
the scan with <code>sourceFiles.from(fileTree(projectDir) { exclude(...) })</code> still scans
everything the default matched, because the excludes apply to its own tree and not to the default one
beside it. Use <code>setFrom</code> to state the whole scope, and <code>from</code> only when you
mean to add to it.
</warning>

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
Use <code>setFrom</code> with an <code>exclude</code> in the <code>fileTree</code> to keep test
resources holding harmless test keys out of the scan. Generated output is already excluded by the
default scope.
</tip>

## How to Run

To execute the secret scan individually, use the following Gradle command:

<code-block lang="bash">
<![CDATA[
./gradlew kreateTrivySecretScan
]]>
</code-block>

Alternatively, the scan is automatically included when running the lifecycle task:

<code-block lang="bash">
<![CDATA[
./gradlew check
]]>
</code-block>

<seealso>
    <category ref="security">
        <a href="Trivy-Overview.md">Security and compliance</a>
        <a href="Trivy-Configuration-Reference.md">Configuration reference</a>
    </category>
</seealso>
