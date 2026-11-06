# Software assurance

<link-summary>One gate over every finding: filter them, report them, decide.</link-summary>

<card-summary>Collect findings, accept the ones you have a reason for, and fail on the rest.</card-summary>

<tldr>
<p><b>Enable</b>: <code>assurance { enabled = true }</code></p>
<p><b>Tasks</b>: <code>kreateAssurance</code>, <code>kreateAssuranceReport</code></p>
<p><b>Output</b>: <code>build/kreate/assurance/</code></p>
</tldr>

A build has many places that can find a problem, and each of them has its own output and its own way
to suppress it. The assurance gate puts one decision on top of them: findings are collected, filtered
through your ignore rules and your baseline, written to one report, and then judged against one
threshold.

## Quick start

```kotlin
kreate {
    assurance {
        enabled = true
        failOn = Severity.HIGH

        ignore("GHSA-xxxx-yyyy-zzzz") {
            module = "com.example:library"
            reason = "We never call the affected method."
            expires = "2027-01-31"
        }
    }
}
```

```bash
./gradlew kreateAssurance
```

## What decides the outcome

| Status | Meaning | `exitCode` |
|:-------|:--------|:-----------|
| `PASSED` | Nothing reached `failOn` | `0` |
| `FINDINGS` | Findings reached `failOn`, or stale baseline entries with `failOnStale` | `1` |
| `INVALID` | A finding document or the baseline could not be read | `65` |
| `INCOMPLETE` | A source could not finish | `69` |

Gradle only knows success and failure, so `build/kreate/assurance/result.json` carries the distinction
for CI scripts. Every failure message starts with `Kreate assurance [STATUS]:`.

**A check that could not finish is never reported as clean.** `reportOnly` hides findings, but it
cannot hide `INCOMPLETE` or `INVALID`.

An advisory without a rating counts as `HIGH`, and `minSeverity` cannot drop it: an unrated
vulnerability is not a harmless one.

## Accepting a finding

An ignore rule needs a reason, and it should have an end. A rule stops matching the day after
`expires`, and the report names it.

For an existing code base, record what is there and judge only what is new:

```bash
./gradlew kreateAssuranceBaselineCreate
```

Commit `kreate-baseline.json`. Entries hold no line numbers and no versions, so moving code does not
make a finding new, but a further occurrence does. `kreateAssuranceBaselinePrune` removes what no
longer occurs and never adds anything.

```kotlin
assurance {
    baseline {
        maxSeverity = Severity.HIGH
        failOnStale = true
    }

    forbidIgnoreOf = setOf(Severity.CRITICAL)
}
```

`maxSeverity` keeps anything more severe out of the baseline. `forbidIgnoreOf` keeps a severity out of
reach of both the ignore rules and the baseline.

## Reports

`kreateAssuranceReport` writes `build/kreate/assurance/reports/` and runs after `kreateAssurance`, even
when that fails. `reports { formats }` selects `JSON`, `SARIF` and `TEXT`. Upload the SARIF file to
GitHub code scanning; it carries the `kreate/v1` fingerprint, so a finding keeps its identity across
runs.

## Trivy

With `trivy { enabled = true }` the scans feed the gate: they stop failing on their own, and
`kreateAssurance` runs them and decides. See
[the configuration reference](Trivy-Configuration-Reference.md).

## Advisories

```kotlin
assurance {
    enabled = true
    internalGroups = setOf("com.acme")

    advisories {
        enabled = true
    }
}
```

`kreateAdvisoryAudit` asks [OSV.dev](https://osv.dev) about the resolved dependencies, including the
transitive ones. `configurations` defaults to the classpaths of the main code.

- **Privacy.** Modules of `internalGroups` and their subgroups are never sent, because the name of an
  internal module is itself worth protecting. They are not audited either.
- **Freshness.** An advisory can appear without anything in the build changing. The audit stands for
  `reuseVerdictFor`, 6 hours by default, and runs again after that. `maxAdvisoryAge`, 3 days by
  default, makes older data `INCOMPLETE`.
- **Severity.** Computed from the CVSS 3 vector. An advisory with only a CVSS 4 vector takes the rating
  of its database, and one without any rating counts as `HIGH`.
- **Offline.** `--offline` makes the audit `INCOMPLETE`, as does a dependency that does not resolve:
  what nobody looked at is not clean.

Point `-Pkreate.network.osvUrl` at an internal mirror.

## Findings from other tools

`externalFindings.from("findings.json")` reads a finding document:

```json
{
  "source": "external",
  "status": "COMPLETE",
  "evaluatedAt": "2026-11-06T12:00:00Z",
  "findings": [
    {
      "ruleId": "EXAMPLE",
      "source": "external",
      "severity": "HIGH",
      "title": "What was found",
      "module": "com.example:library",
      "file": "src/main/kotlin/Example.kt",
      "line": 12
    }
  ]
}
```

`status` is `COMPLETE`, `INCOMPLETE` or `SKIPPED`. Report `INCOMPLETE` when your tool could not finish:
the gate then fails rather than treating silence as a pass.

<seealso>
    <category ref="security">
        <a href="Trivy-Overview.md">Security and compliance</a>
    </category>
</seealso>
