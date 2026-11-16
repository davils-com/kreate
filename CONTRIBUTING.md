# Contributing to Kreate

Thank you for your interest in contributing to **Kreate**! We welcome all contributions that help improve this project—from bug fixes and documentation improvements to new features.

To ensure a smooth and professional collaboration, please follow these guidelines.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How to Contribute](#how-to-contribute)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Features](#suggesting-features)
  - [Pull Requests](#pull-requests)
- [Development Standards](#development-standards)
  - [One command before you push](#one-command-before-you-push)
  - [Code style](#code-style)
  - [Package structure](#package-structure)
  - [Testing](#testing)
  - [The public API is a gate](#the-public-api-is-a-gate)
  - [Documentation](#documentation)
- [Changing a Dependency](#changing-a-dependency)
- [Snapshots and Releases](#snapshots-and-releases)
- [Commit Messages](#commit-messages)

---

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md). Please read it to understand the expectations for all community members.

---

## How to Contribute

### Reporting Bugs

If you find a bug, please open an **Issue** and include:
- A clear, descriptive title.
- Steps to reproduce the issue.
- Expected vs. actual behavior.
- Environment details (Gradle version, Kotlin version, OS).

### Suggesting Features

We are open to new ideas! For major changes, please open an **Issue** first to discuss your proposal before starting the implementation.

### Pull Requests

1. **Fork** the repository and create your branch from `develop`. `main` carries releases; day-to-day
   work happens on `develop`, and that is where pull requests are merged.
2. If you've added code that should be tested, add **tests**.
3. If you've changed APIs or added features, update the **documentation**.
4. Ensure the checks below pass locally.
5. Submit a **Pull Request** with a clear description of the changes.

---

## Development Standards

> **The plugin, its settings plugin and its Detekt rule set are included builds.** A task name given
> at the root does not reach them. `./gradlew verify` is the one aggregate that does: it runs `check`
> of `kreate-plugin`, `kreate-settings`, `kreate-detekt-rules` and `:example`.

### One command before you push

```bash
./gradlew verify
```

That is exactly what the pipeline runs, for a pull request as for a release. It covers compilation, Detekt with the Kreate
rule set, the unit and functional test suites, coverage verification and the API check. Set
`KREATE_REQUIRE_CMAKE=true` and `KREATE_REQUIRE_TRIVY=true` to turn a missing native toolchain into
a failure instead of skipped tests.

### Code style

Kreate is built to the standard it enforces. The Kreate Detekt rule set runs on this repository's own
sources, with `allRules` and warnings as errors, and there is no baseline:

- **No comments.** Neither `//` nor `/* */`; the license header is the only exception. Code that
  needs an explanation gets a better name or a smaller function.
- **No `else`.** Handle the exceptional case first and return early. An exhaustive `when` over an enum
  or a sealed type never gets an `else ->`.
- **At most two calls per expression.** Name the intermediate results.
- **One top-level type per file**, named after it. A file of functions is named after its role.
- **KDoc on the public DSL only**, with `@param`, `@return`, `@throws` and `@since`. Internal and
  private declarations carry none. `@since` is never raised on an existing declaration.
- Explicit API mode is on, warnings are errors, `!!` is not used, and configuration-time values are
  read through `providers`, never `System.getenv`.

### Package structure

Every feature of the plugin is a package below `com.davils.kreate` with the same layout:

| Package | Holds | Visibility |
|---|---|---|
| `com.davils.kreate.<feature>` | The build script DSL: `*Extension`, `*Spec`, enums, `*TaskNames` | public |
| `….<feature>.task` | Gradle task classes | internal |
| `….<feature>.wiring` | The `<Feature>Feature` and the Gradle wiring behind it | internal |
| `….<feature>.<domain>` | Logic without Gradle, such as `api.abi` or `benchmark.comparison` | internal |

Features are applied by `com.davils.kreate.feature.KreateFeatures` in the order of their
`FeaturePhase`. A new feature implements `KreateFeature` and is added to that list; its order is
pinned by a unit test.

### Testing

All new features and bug fixes ship with their tests in the same change.

- **Kotest** `FunSpec` on the JUnit Platform, Kotest assertions, MockK for doubles. JUnit Jupiter,
  `kotlin.test` and other assertion libraries are not used.
- Unit tests live in `src/test` and mirror the production package. Anything that needs a real Gradle
  build goes in `kreate-plugin/src/functionalTest`, which drives TestKit.
- A Detekt rule's tests lint a snippet through `dev.detekt:detekt-test`. Cover what the rule leaves
  alone, not only what it reports.

```bash
./gradlew :kreate-plugin:test :kreate-plugin:functionalTest
```

The functional suite is the slower of the two. While iterating on something unrelated to it:

```bash
./gradlew :kreate-plugin:functionalTest -Pkreate.test.skipSlow
```

### The public API is a gate

`kreate-plugin/api/kreate-plugin.api`, `kreate-settings/api/kreate-settings.api` and
`kreate-detekt-rules/api/kreate-detekt-rules.api` record the published binary interfaces and are
checked by `apiCheck` on every build. If you deliberately change a public API, re-record it and commit
the result in the same change:

```bash
./gradlew :kreate-plugin:apiDump :kreate-settings:apiDump :kreate-detekt-rules:apiDump
```

### Documentation

If your change affects the public API, the DSL or the behaviour of a build, update the documentation
in the same change:

- the Writerside topics under `docs/topics/`, with any new topic added to `docs/d.tree`;
- the KDoc of the public declarations you touched;
- a `CHANGELOG.md` entry.

---

## Changing a Dependency

The build verifies every dependency against `gradle/verification-metadata.xml`, and the published
modules lock their runtime classpath in `gradle.lockfile`, in strict mode. A dependency bump is
red until both are updated.

For a pull request of Dependabot the `dependencies` stage of the pipeline does that: it
regenerates the lock files and the checksums, commits them to the pull request as
`build(deps): record locks and checksums` and starts the pipeline again on that commit. For a
change of your own, run:

```bash
./gradlew :kreate-plugin:dependencies :kreate-settings:dependencies :kreate-detekt-rules:dependencies --configuration runtimeClasspath --write-locks
```

```bash
./gradlew --write-verification-metadata sha256 --no-configuration-cache verify :selfcheck:kreateAssurance
```

`:selfcheck:kreateAssurance` is part of that command on purpose: the audit of Kreate's own
dependencies reads their POMs as artifacts, which `verify` never resolves, and it runs in the
`security` stage of the pipeline rather than in `verify`.

Read the diff of the metadata, also when the pipeline wrote it: a checksum that changes for a
version that did not is a tampered artifact, not a chore. Kotlin, `kotlinx-serialization` and Ktor are held back on purpose, see
`gradle/libs.versions.toml`.

## Snapshots and Releases

Kreate is published to `https://repo.davils.com`, never to Maven Central.

`gradle.properties` on `develop` carries the version **in development**. Every build without a
release tag is `<version>-SNAPSHOT`; only a tag build produces `<version>`, and it refuses a tag
that does not match `gradle.properties`.

| What | When | Where |
|---|---|---|
| Snapshot | every push to `develop`, by the `snapshot` stage | `repo.davils.com/snapshots`, unsigned, overwritten |
| Release | a tag on `main`, by the `release` stage | `repo.davils.com/releases`, signed, attested, never overwritten |

Both are stages of the one workflow `Pipeline` in `.github/workflows/pipeline.yml`. It calls the
reusable pipeline of [kotlin-workflow](https://github.com/davils-com/kotlin-workflow) at a pinned
commit, and everything it does for Kreate, the tasks, the operating systems per event and the
publications of a release, is set in `.github/kotlin-workflow.yml`. A stage publishes only after
the `check` stage of the same run passed, so a commit is verified once.

Cutting a release:

1. On `develop`, finish the `## <version>` section of `CHANGELOG.md` and set `README.md`,
   `SECURITY.md` and `docs/v.list` to the version. Those three always show the latest release.
   Commit as `chore(release): prepare <version>`.
2. Merge `develop` into `main` and push the tag `<version>`, without a `v`.
3. Approve the `release` environment. The pipeline verifies, uploads, attests and then resolves the
   release the way a consumer does.
4. On `develop`, raise `version` in `gradle.properties` to the next version and commit as
   `chore(release): start <next version>`.

The pipeline needs two secrets: `DAVILS_MAVEN_NAME`, the name of a repository token, and
`DAVILS_MAVEN_SECRET`, its secret. They are organisation secrets, which `pipeline.yml` hands to
the reusable pipeline as `MAVEN_USERNAME` and `MAVEN_PASSWORD`. A secret of one of those two
names on the environment `snapshot` or `release` takes precedence, which is how `snapshot` can be
given a token that writes `/snapshots` only. A release is signed with `SIGNING_KEY`,
`SIGNING_KEY_ID` and `SIGNING_PASSWORD`.

To rehearse a release without uploading anything, start `Pipeline` by hand with `stages` set to
`release` and `dry-run` switched on.

### A release that stopped half way

The releases repository refuses to overwrite a file, so the publish job cannot simply run again
after a failure. The preflight of the `release` stage tells the three states apart: a publication that is not there yet
is uploaded, one that is complete is skipped, and one that is there without being listed in its
`maven-metadata.xml` stops the job and names its directory.

1. Delete the named directory, such as `com/davils/kreate/<version>`, in the repository.
2. Run the failed jobs of the pipeline again.

Never move or re-push the tag: a consumer may already have resolved the publications that did
arrive.

---

## Commit Messages

Conventional commits with the module as scope, in the imperative mood:

- `feat(plugin): add support for NewTarget`
- `fix(cinterop): resolve memory leak in the Rust build`
- `docs(publish): update the publishing guide`
- `refactor(rules): split the KDoc helpers`

---

## License

By contributing, you agree that your contributions will be licensed under the project's **Apache License 2.0**.
