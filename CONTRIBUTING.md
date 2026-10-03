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

That is exactly what CI and the release workflow run. It covers compilation, Detekt with the Kreate
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

## Commit Messages

Conventional commits with the module as scope, in the imperative mood:

- `feat(plugin): add support for NewTarget`
- `fix(cinterop): resolve memory leak in the Rust build`
- `docs(publish): update the publishing guide`
- `refactor(rules): split the KDoc helpers`

---

## License

By contributing, you agree that your contributions will be licensed under the project's **Apache License 2.0**.
