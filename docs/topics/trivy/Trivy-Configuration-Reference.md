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
| `ignoreUnfixed` | `Property<Boolean>` | `false` | Leaves out vulnerabilities without a released fix. |
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

## Which Trivy runs

Kreate pins a Trivy version and holds the SHA-256 of its releases, so a build does not depend on what
a machine has installed, and a compromised release cannot slip in. The scans resolve Trivy in this
order:

1. `trivy { executable }` or `-Pkreate.trivy.executable`. Nothing else is considered;
2. with `useInstalled = true`, any Trivy on the `PATH`;
3. the pinned version in `caches/kreate/trivy` of the Gradle user home, whose digest is checked on
   every use;
4. an installed Trivy of exactly the pinned version;
5. a download of the pinned version, verified before it is unpacked.

| Property       | Type                        | Default                 | Description                                     |
|:---------------|:----------------------------|:------------------------|:------------------------------------------------|
| `version`      | `Property<String>`          | the version Kreate pins | The exact version                               |
| `useInstalled` | `Property<Boolean>`         | `false`                 | Accepts any installed version                   |
| `executable`   | `Property<String>`          | none                    | The executable to run                           |
| `checksums`    | `MapProperty<String,String>`| empty                   | SHA-256 per platform, such as `Linux-64bit`     |

A version other than the default needs `checksums` for the same protection. Without them Kreate reads
Trivy's own `checksums.txt` and warns: that file comes from the same release, so it proves the download
is intact, not that the release is genuine.

For an air-gapped build, mirror the release assets and set
`-Pkreate.trivy.downloadBaseUrl=https://mirror.example.com/trivy`, or point `executable` at an installed
copy. `kreateTrivyProvision` installs Trivy ahead of time, for example to warm a CI cache. When Trivy
is not available, a scan fails, and the gate reports `INCOMPLETE`.

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
