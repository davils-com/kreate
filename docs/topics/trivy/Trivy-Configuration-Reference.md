# Trivy configuration reference

<link-summary>Every property in the trivy block.</link-summary>

<card-summary>The complete security and compliance DSL.</card-summary>

This page provides a complete reference of all configuration properties available in the `trivy { }` block.

## Global Configuration

The `trivy` block is located at `kreate { trivy { ... } }`.

| Property        | Type                | Default | Description                                           |
|:----------------|:--------------------|:--------|:------------------------------------------------------|
| `enabled`       | `Property<Boolean>` | `false` | Enables or disables the Trivy module for the project. |
| `license`       | `Nested`            | -       | Configuration for license scanning.                   |
| `vulnerability` | `Nested`            | -       | Configuration for vulnerability scanning.             |
| `secrets`       | `Nested`            | -       | Configuration for secret scanning.                    |

---

## License Scanning (`license { }`)

| Property          | Type                            | Default                     | Description                                                                |
|:------------------|:--------------------------------|:----------------------------|:---------------------------------------------------------------------------|
| `failOnFindings`  | `Property<Boolean>`             | `true`                      | Fails the build if forbidden licenses are detected.                        |
| `severity`        | `ListProperty<Severity>` | `[CRITICAL, HIGH, UNKNOWN]` | Severities to include in the scan.                                         |
| `ignoredLicenses` | `ListProperty<String>`          | `[]`                        | List of licenses to ignore (e.g., "MIT", "Apache-2.0").                    |
| `configurations`  | `SetProperty<String>`           | main compile and runtime classpaths | Lock file configurations whose dependencies are checked. Since 4.0.0.  |
| `failOnMissingLockFiles` | `Property<Boolean>` | `true` | Fails the build when there is no lock file to scan. |
| `lockFiles`       | `ConfigurableFileCollection`    | `*.lockfile`                | The collection of lockfiles to be scanned.                                 |

---

## Vulnerability Scanning (`vulnerability { }`)

| Property         | Type                         | Default                         | Description                                      |
|:-----------------|:-----------------------------|:--------------------------------|:-------------------------------------------------|
| `failOnFindings` | `Property<Boolean>`          | `true`                          | Fails the build if vulnerabilities are detected. |
| `severity`       | `ListProperty<Severity>`     | `[CRITICAL, HIGH, MEDIUM, LOW]` | Vulnerability scores to include in the scan.     |
| `failOnMissingLockFiles` | `Property<Boolean>` | `true` | Fails the build when there is no lock file to scan. |
| `lockFiles`      | `ConfigurableFileCollection` | `*.lockfile`                    | The collection of lockfiles to be scanned.       |

---

## Secret Scanning (`secrets { }`)

| Property         | Type                           | Default                         | Description                                  |
|:-----------------|:-------------------------------|:--------------------------------|:---------------------------------------------|
| `failOnFindings` | `Property<Boolean>`            | `true`                          | Fails the build if secrets are detected.     |
| `runOnCheck`     | `Property<Boolean>`            | `true`                          | Makes `check` (and so `build`) run the secret scan. Since 3.4.0. |
| `severity`       | `ListProperty<Severity>` | `[CRITICAL, HIGH, MEDIUM, LOW]` | Severities of secrets to include.            |
| `secretConfig`   | `RegularFileProperty`          | `trivy-secret.yaml`             | Path to the Trivy secret configuration file. |
| `sourceFiles`    | `ConfigurableFileCollection`   | sources, build scripts, config and key files, excluding `build` | The files to be scanned for secrets. `from` adds to the default, `setFrom` replaces it. |

---

## `misconfig` and `filesystem`

Both are off by default and read the whole project directory: `kreateTrivyMisconfigScan` for insecure
configuration, and `kreateTrivyFilesystemScan` for vulnerabilities, secrets and misconfiguration in one
run.

| Property          | Type                     | Default                             | Description                         |
|:------------------|:-------------------------|:------------------------------------|:------------------------------------|
| `enabled`         | `Property<Boolean>`      | `false`                             | Whether the scan runs               |
| `severity`        | `ListProperty<Severity>` | `[CRITICAL, HIGH, MEDIUM, LOW]`     | The severities to report            |
| `failOnFindings`  | `Property<Boolean>`      | `true`                              | Fails the build if anything is found |
| `skipDirectories` | `ListProperty<String>`   | `[build, .gradle, .kotlin, .git]`   | Directories the scan skips          |

## Together with the assurance gate

With <a href="Assurance-Overview.md"><code>assurance { enabled = true }</code></a>, the scans no longer
fail on their own. They write their findings to `build/kreate/assurance/documents/`, and
`kreateAssurance` decides, so ignore rules and the baseline apply. A scan with `failOnFindings = false`
stays in the report without blocking, and a crash of Trivy is `INCOMPLETE` rather than a pass.

## `.trivyignore` and `trivy.yaml`

Kreate runs Trivy from the temporary directory of the task, so neither file is read. A committed
`.trivyignore` would otherwise hide a finding without a trace in the build. Accept a finding with an
`ignore` rule of the gate, which needs a reason and can expire, or with `ignoredLicenses`.

## Enums Reference

### Severity
`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `UNKNOWN`, from `com.davils.kreate.assurance.Severity`

<seealso>
    <category ref="security">
        <a href="Trivy-Overview.md">Security and compliance</a>
    </category>
</seealso>
