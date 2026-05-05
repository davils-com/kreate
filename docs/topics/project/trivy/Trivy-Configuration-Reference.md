# Trivy Configuration Reference

This page provides a complete reference of all configuration properties available in the `trivy { }` block.

## Global Configuration

The `trivy` block is located at `kreate { project { trivy { ... } } }`.

| Property        | Type                | Default | Description                                           |
|:----------------|:--------------------|:--------|:------------------------------------------------------|
| `enabled`       | `Property<Boolean>` | `false` | Enables or disables the Trivy module for the project. |
| `disableDependencyLocking` | `Property<Boolean>` | `false` | Disables automatic dependency locking for all configurations. |
| `license`       | `Nested`            | -       | Configuration for license scanning.                   |
| `vulnerability` | `Nested`            | -       | Configuration for vulnerability scanning.             |
| `secrets`       | `Nested`            | -       | Configuration for secret scanning.                    |

---

## License Scanning (`license { }`)

| Property          | Type                            | Default                     | Description                                                                |
|:------------------|:--------------------------------|:----------------------------|:---------------------------------------------------------------------------|
| `failOnForbidden` | `Property<Boolean>`             | `true`                      | Fails the build if forbidden licenses are detected.                        |
| `severity`        | `ListProperty<LicenseSeverity>` | `[CRITICAL, HIGH, UNKNOWN]` | Severities to include in the scan.                                         |
| `ignoredLicenses` | `ListProperty<String>`          | `[]`                        | List of licenses to ignore (e.g., "MIT", "Apache-2.0").                    |
| `lockFiles`       | `ConfigurableFileCollection`    | `*.lockfile`                | The collection of lockfiles to be scanned.                                 |

---

## Vulnerability Scanning (`vulnerability { }`)

| Property         | Type                         | Default                         | Description                                      |
|:-----------------|:-----------------------------|:--------------------------------|:-------------------------------------------------|
| `failOnFindings` | `Property<Boolean>`          | `true`                          | Fails the build if vulnerabilities are detected. |
| `score`          | `ListProperty<Score>`        | `[CRITICAL, HIGH, MEDIUM, LOW]` | Vulnerability scores to include in the scan.     |
| `lockFiles`      | `ConfigurableFileCollection` | `*.lockfile`                    | The collection of lockfiles to be scanned.       |

---

## Secret Scanning (`secrets { }`)

| Property         | Type                           | Default                         | Description                                  |
|:-----------------|:-------------------------------|:--------------------------------|:---------------------------------------------|
| `failOnFindings` | `Property<Boolean>`            | `true`                          | Fails the build if secrets are detected.     |
| `severity`       | `ListProperty<SecretSeverity>` | `[CRITICAL, HIGH, MEDIUM, LOW]` | Severities of secrets to include.            |
| `secretConfig`   | `RegularFileProperty`          | `trivy-secret.yaml`             | Path to the Trivy secret configuration file. |
| `sourceFiles`    | `ConfigurableFileCollection`   | (src/**/*.kt, etc.)             | The files to be scanned for secrets.         |

---

## Enums Reference

### LicenseSeverity
`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `UNKNOWN`

### Score (Vulnerability)
`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`

### SecretSeverity
`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`
