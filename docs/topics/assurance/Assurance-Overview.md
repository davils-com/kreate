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
when that fails. `reports { formats }` selects any of:

| Format | File | For |
|---|---|---|
| `JSON` | `report.json` | Scripts; a stable schema |
| `SARIF` | `report.sarif` | GitHub code scanning; carries the `kreate/v1` fingerprint |
| `TEXT` | `report.txt` | Reading |
| `HTML` | `report.html` | A self-contained dashboard with filters |
| `MARKDOWN` | `report.md` | A pull request comment or a job summary |
| `JUNIT` | `report.junit.xml` | Test result views of CI servers |
| `CHECKSTYLE` | `report.checkstyle.xml` | Tools that read Checkstyle |
| `GITLAB` | `report.gitlab.json` | The code quality widget of GitLab |
| `SONARQUBE` | `report.sonarqube.json` | The generic issue import of SonarQube |

The dashboard allows only its own style and script, by their SHA-256 hashes in a content security
policy, loads nothing from elsewhere and escapes everything that comes from a finding.

With `reports { ingestQuality = true }`, the default, `kreateAssuranceIngest` reads the SARIF reports of
Detekt, the coverage XML of Kover and the API dumps into the gate, as findings of the source `quality`
that only inform: `DETEKT_ISSUE` by the rule of Detekt, `COVERAGE_BELOW_MINIMUM` against
`coverage { verify { minLineCoverage } }` and `API_DUMP_OUTDATED`. Those tools keep deciding whether the
build fails; the report shows everything in one place.

### Aggregation

```kotlin
assurance {
    aggregate { enabled = true }
}
```

Every project with `aggregate` shares the outcome of its gate; `kreateAssuranceAggregate` in the root
project collects them through a consumable variant, safe under Isolated Projects, and writes
`build/kreate/assurance/aggregate/` as JSON, text, Markdown and HTML, as far as `formats` selects them.
It fails with the worst status of any project. A project of the build that does not share its outcome
is listed as not checked. The members are found as for the workspace check.

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

## Inspection

```kotlin
assurance {
    inspection {
        enabled = true
        trustedHosts = setOf("corp.example")
    }
}
```

`kreateDependencyInspect` reads what the resolved dependencies contain: their class files, resources,
nested archives and POMs. It looks for process execution and dynamic class loading, for markers of
miners and reverse shells, for addresses on hosts you do not trust, for invisible Unicode in text files,
for entries that escape the archive, and for code that runs at build time, such as annotation processors,
compiler plugins and Java agents. Nothing from a jar is executed or unpacked to disk.

**Single findings only inform.** Many sound libraries start processes or open sockets, so a build that
failed on each of them would be useless. Findings are summed per module to a risk score out of 100,
counting each rule once, and a module at `failScore`, 30 by default, gets a `RISK_SCORE` finding.

That finding also only informs until you set `failOnRisk = true`. Of the 105 modules Kreate itself
depends on, six reach the threshold, among them the Kotlin Gradle plugin and FreeMarker, so look at your
report first and accept what you reviewed with `ignore("RISK_SCORE") { module = "..."; reason = "..." }`.

POMs are fetched for every resolved module, also for those without a jar. If your build uses dependency
verification, record their checksums once, or the build stops with a verification failure:

```bash
./gradlew --write-verification-metadata sha256 kreateDependencyInspect --no-configuration-cache
```

The inspection is a filter, not a verdict: it shows what deserves a look. A clean result says nothing
about logic that uses no sensitive API.

## Trust

```kotlin
assurance {
    trust {
        enabled = true
        freshReleaseHours = 24
        minScorecard = 4.0
    }
}
```

`kreateDependencyTrust` asks deps.dev when every dependency and every plugin on the build script class
path was first published, when the resolved version was published, and how its source repository
scores on the OpenSSF Scorecard. Most malicious packages are new and are caught within a day or two, so:

| Rule | Severity | When |
|---|---|---|
| `FRESH_PACKAGE` | Critical | The module first appeared fewer than `freshPackageDays` (7) days ago |
| `YOUNG_PACKAGE` | Medium | The module first appeared fewer than `youngPackageDays` (30) days ago |
| `FRESH_RELEASE` | Critical | The version was published fewer than `freshReleaseHours` (24) hours ago |
| `LOW_SCORECARD` | Medium | The source repository scores below `minScorecard` (4.0 of 10); `0` turns it off |
| `TRUST_DATA_UNAVAILABLE` | Low | deps.dev knows nothing; reported, never blocking |

Missing data is never a risk: a module without a scorecard is simply not rated. Modules of
`internalGroups` are never sent. Offline mode and a failing service make the assessment incomplete.
Answers are cached for a day in the Gradle user home; `kreate.network.depsDevUrl` names a mirror.

## Confusion

```kotlin
assurance {
    confusion {
        enabled = true
        allow = setOf("org.openapi.generator")
    }
}
```

`kreateDependencyConfusion` works without the network:

- `TYPOSQUAT_GROUP` (critical): a group one or two characters away from the owner of a popular module,
  such as `com.goog1e`, or its name under another top-level domain, such as `com.slf4j`. A namespace
  that starts with no top-level domain, such as `androidx` or `jakarta`, is only compared by distance,
  so `androidx.annotation` does not imitate `jakarta.annotation`.
- `TYPOSQUAT_ARTIFACT` (medium): the name of a popular artifact under a foreign group.
- `SUSPICIOUS_VERSION` (medium): a major version above 50 that is not a date, the trademark of a
  dependency confusion attack.
- `PLUGIN_MARKER_MISMATCH` (medium): a plugin marker on the build script class path whose
  implementation lies outside the plugin id's namespace.

Maven Central verifies who owns a namespace, so a module under the owner of a popular module, such as
any `com.google.*`, is never a typosquat. Extend the built-in list with `popularModules`.

With `checkPublicRepository = true`, every module of `internalGroups` is looked up in
`publicRepository`, Maven Central by default. One that is published there is `DEPENDENCY_CONFUSION`
(high): a repository asked first, or one without content filtering, can serve the public module instead
of yours. **This sends your internal coordinates to that repository**, which is why it is off by
default. Declare your own repository with `exclusiveContent` either way.

Plugins applied through the `plugins` block of `settings.gradle.kts` are not on a project's class path
and are not checked.

## Dependency policy

```kotlin
assurance {
    dependencyPolicy {
        enabled = true
        deny("log4j:log4j", "End of life. Use org.apache.logging.log4j:log4j-core.")
        allowed = setOf("org.apache", "com.fasterxml.jackson", "org.jetbrains")
        allowedRepositoryHosts = setOf("repo.maven.apache.org", "maven.corp.example")
        checkOutdated = true
        maxMajorBehind = 1
    }
}
```

A module is named as `group:artifact`, as `group:*`, or as a group with its subgroups.
`kreateDependencyPolicy` works offline and reports:

| Rule | Severity | When |
|---|---|---|
| `DENIED_MODULE` | High | A denied module is resolved, also transitively |
| `MODULE_NOT_ALLOWED` | High | A direct dependency is not on `allowed`; an empty list allows all |
| `DISALLOWED_REPOSITORY` | High | A repository points to a host outside `allowedRepositoryHosts` |
| `INSECURE_REPOSITORY` | High | A repository sets `allowInsecureProtocol` |
| `INSECURE_WRAPPER` | High | `gradle-wrapper.properties` downloads Gradle over http |
| `MAVEN_LOCAL_REPOSITORY` | Medium | The build resolves from `mavenLocal()` |
| `DYNAMIC_VERSION` | Medium | A direct dependency is `1.+`, `latest.release` or a range |
| `CHANGING_VERSION` | Medium | A direct dependency is a snapshot |
| `TEST_LIBRARY_IN_PRODUCTION` | Medium | JUnit, Kotest, MockK and the like on a production class path |
| `DEPENDENCY_OVERRIDE` | Medium | `resolutionStrategy.force` pins a module |
| `LOCKING_DISABLED` | Medium | The project resolves external modules without a lock file |
| `VERIFICATION_DISABLED` | Medium | No `gradle/verification-metadata.xml`, or verification is lenient or off |
| `WRAPPER_UNVERIFIED` | Medium | The wrapper names no `distributionSha256Sum` |
| `LENIENT_LOCKING` | Low | Locking does not run in `LockMode.STRICT` |

A forced module stays reported until an ignore rule accepts it, with a reason and an expiry. Gradle
does not show the repositories of `settings.gradle.kts` to a project, so only the repositories of the
project and of its build script are checked. The copies of the `pluginManagement` repositories that
Gradle places on every build script, named `__plugin_repository__...`, belong to the settings and are
left out as well.

With `checkOutdated = true`, `kreateDependencyOutdated` asks deps.dev how far every direct dependency lags
behind. It counts only major releases that exist, without pre-releases, and adds up libyears.
`maxMajorBehind` reports `OUTDATED_MAJOR` and `maxLibyears` reports `LIBYEAR_EXCEEDED`, both medium.
Modules of `internalGroups` are never sent.

## Adding and fixing dependencies

The root project gets two tasks that edit `gradle/libs.versions.toml` and keep its comments and order.

```bash
./gradlew kreateDependencyAdd --module=com.squareup.okio:okio
```

`kreateDependencyAdd` takes the latest release from deps.dev, or the version you name, and checks
exactly that version: advisories from OSV.dev, the trust assessment, typosquatting and the dependency
policy. A high advisory, a critical trust finding, a typosquat or a denied module stops it. `--force`
adds the module anyway, but a check that could not complete, offline for instance, always stops it.
`--alias` names the entry and `--dry-run` only checks. The version is written as it was checked, so the
build cannot pick a different, unchecked release.

```bash
./gradlew kreateDependencyFix
./gradlew kreateDependencyFix --apply
```

`kreateDependencyFix` raises every version that OSV.dev reports as vulnerable to the highest fixed
version and pins dynamic versions to the version that resolved. Without `--apply` it only writes the diff
to `build/kreate/dependencies/catalog-fix.diff`. A version reference that several libraries share is
left alone with a note, because raising it changes the others too.

## Workspace

```kotlin
assurance {
    workspace {
        enabled = true
        forbid(":core:**", ":app:**")
    }
}
```

Every project with the workspace check describes what it depends on; the root project collects the
descriptions through a consumable variant, so the check works with Isolated Projects. With the settings
plugin `com.davils.kreate.settings` the root project knows every project of the build; without it,
name them in `members`. `kreateWorkspaceCheck` feeds the gate:

| Rule | Severity | When |
|---|---|---|
| `DEPENDENCY_CYCLE` | High | Projects depend on each other in a cycle |
| `FORBIDDEN_DEPENDENCY` | High | A dependency matches a `forbid` rule; `*` stays within a path segment, `**` crosses them |
| `WORKSPACE_VERSION_MISMATCH` | Medium | Projects resolve a module in different versions; `alignVersions = false` turns it off |
| `WORKSPACE_MEMBER_MISSING` | Low | A member shares nothing because it does not enable the check; never blocking |

`kreateWorkspaceAffected --base=origin/main` lists the projects whose files changed since the revision,
uncommitted and untracked files included, with every project depending on them, in
`build/kreate/workspace/affected.txt`. A change of shared build files, such as `gradle/` or
`settings.gradle.kts`, affects every project. `kreateWorkspaceGraph` draws the projects and
`kreateDependencyGraph` the resolved dependencies of one project, as `--format=text`, `dot` or
`mermaid`.

## Explaining rules and checking the configuration

`kreateExplain --rule=FRESH_RELEASE` explains a rule of the catalog, and without `--rule` lists all
of them. An advisory id such as `GHSA-...` points to its record on OSV.dev.

`kreateAssuranceConfig` prints the effective configuration and marks with `*` what differs from the
defaults. `kreateAssuranceConfigLint` feeds the gate with what weakens it: `reportOnly`
(`CONFIG_GATE_NOT_FAILING`), a `minSeverity` that drops findings, ignore rules without expiry or
expired, service URLs over plain HTTP (loopback excepted), a disabled or unpinned Trivy, a baseline
without `maxSeverity`, no source of findings at all, and a public lookup of internal modules.

## Organisation policy

A build cannot police itself: whoever may edit `build.gradle.kts` may also switch its checks off. An
organisation therefore publishes a policy as a versioned artifact, and CI names it:

```bash
./gradlew kreateAssurance -Pkreate.assurance.policy=com.acme:assurance-policy:3
```

```json
{
  "schemaVersion": 1,
  "name": "acme",
  "require": { "enabled": "true", "reportOnly": "false", "advisories.enabled": "true" },
  "minimum": { "failOn": "HIGH", "maxAdvisoryAge": "P3D", "trust.freshReleaseHours": "24" },
  "forbidIgnoreOf": ["CRITICAL"],
  "ignoreRulesNeedExpiry": true,
  "maxIgnoreDays": 90,
  "denied": { "log4j:log4j": "End of life. Use org.apache.logging.log4j:log4j-core." }
}
```

- `require` fixes options to a value; the names are those `kreateAssuranceConfig` prints.
- `minimum` sets the weakest allowed value of an option with an order: `failOn`, `minSeverity` and
  `baseline.maxSeverity`, where a less severe value is stricter; `maxAdvisoryAge` and `reuseVerdictFor`,
  where shorter is stricter; and the thresholds of the inspection, the trust assessment and the
  dependency policy.
- `forbidIgnoreOf` adds severities that no ignore rule may hide, `ignoreRulesNeedExpiry` and
  `maxIgnoreDays` limit ignore rules, and `denied` forbids modules, also transitively.

`kreateAssurancePolicyCheck` reports every violation as a critical finding of the source `policy`.
No ignore rule, baseline, `minSeverity` or `reportOnly` hides it, and with a policy the gate runs even
where the build switched it off. A policy that cannot be resolved leaves the gate incomplete; a
malformed one fails the build. The policy resolves like any dependency, so dependency verification
covers it. A path ending in `.json`, relative to the root project, works as well, for a first try.

`kreateAssurancePolicyTemplate` writes `build/kreate/assurance/policy-template.json` from the current
settings, as a start. Publish it with `maven-publish` as an artifact with the extension `json`.

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
