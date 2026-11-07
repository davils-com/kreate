# Kreate on itself

<link-summary>What the assurance gate finds in Kreate's own dependencies, measured.</link-summary>

<card-summary>Kreate puts its own dependencies through its gate. These are the numbers.</card-summary>

A security tool that is never pointed at itself has only promises to offer. The `:selfcheck` project
of this repository depends on the plugin and enables every source of the gate:

```kotlin
kreate {
    assurance {
        enabled = true
        advisories { enabled = true }
        inspection { enabled = true }
        trust { enabled = true }
        confusion { enabled = true }
        dependencyPolicy {
            enabled = true
            checkOutdated = true
        }
        aggregate { enabled = true }
        reports { formats = ReportFormat.entries.toSet() }
    }
}
```

`./gradlew :selfcheck:kreateAssurance` reads OSV.dev and deps.dev, so it stays out of `verify` and
runs in the Trivy workflow instead. The numbers below were measured on 7 November 2026, for Kreate
4.0.0 on Gradle 9.9.0 and JDK 25.

## Results

| Source | Read | Findings | Blocking |
|---|---|---|---|
| Advisories (OSV.dev) | 105 modules | 0 | 0 |
| Inspection | 105 modules, jars and POMs | 82 | 0 |
| Trust (deps.dev) | 107 modules, build script included | 1 | 0 |
| Confusion | 107 modules, 1 plugin marker | 0 | 0 |
| Dependency policy | 2 configurations, repositories, wrapper | 1 | 0 |
| Outdated | 1 direct dependency | 0 | 0 |
| Configuration lint | the settings above | 1 | 0 |

The gate passes. What the findings are:

- **Inspection.** 43 addresses on hosts outside the trusted list, mostly documentation links; nine
  Gradle plugin descriptors; eight places that define classes at runtime; four that deserialise and
  four that start processes. Six modules reach the risk score of 30: the Kotlin Gradle plugin at 100,
  FreeMarker at 40, Dokka's plugin and core, `kotlin-native-utils` and `kotlin-daemon-client`. All of
  them do what their purpose needs, which is why single findings only inform.
- **Trust.** The source repository of `org.jetbrains:annotations` scores 2.6 of 10 on the OpenSSF
  Scorecard.
- **Dependency policy.** `:selfcheck` has no lock file, by design: it resolves what the plugin
  resolves, and the plugin's lock file is the one that matters.
- **Configuration lint.** Trivy is not enabled in `:selfcheck`; it scans the repository in its own
  workflow.

## What the gate found earlier

Before it reached this state, the gate changed Kreate:

- The vulnerability scan of the new lock files found **CVE-2026-84939**, critical, in FreeMarker, which
  Kover brings, and four high advisories in Jackson. Kreate now constrains both to fixed versions.
- Measured on these dependencies, the first version of the inspection reported 123 findings, 20 of
  them high entropy strings that were method descriptors and regular expressions. The rule now only
  reports strings that look like keys, and the inspection reports 82.
- The semantic versioning check, run against 3.4.0, reports 145 breaking and 110 additive changes, so
  4.0.0 is a major release for a reason: every public type moved to a feature package.

## How the repository holds itself to it

- Every dependency of the build is verified against a SHA-256 in `gradle/verification-metadata.xml`,
  and the published modules lock their runtime classpath in strict mode.
- Every GitHub Action is pinned by commit, and the default token has no permissions.
- Trivy is the version Kreate pins, checked against a checksum built into the plugin.
- `./gradlew verify` runs Detekt with every rule, the unit and functional tests on the minimum and the
  current Gradle, coverage verification and the API check, and nothing merges unless it is green.
