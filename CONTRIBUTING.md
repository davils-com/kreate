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
  - [Code Style](#code-style)
  - [Documentation in code](#documentation-in-code)
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

> **The plugin lives in an included build, and so does its Detekt rule set.** `kreate-plugin` and
> `kreate-detekt-rules` are pulled in with `includeBuild`, and a task name given at the root does
> **not** reach them — `./gradlew test` runs the example's tests and none of theirs, and reports
> success. Every command below names the builds explicitly for that reason. This is the same pitfall
> the [CI integration guide](docs/topics/CI-Integration.md) warns about.

### One command before you push

```bash
./gradlew :kreate-plugin:build :kreate-detekt-rules:build build
```

That is exactly what CI runs, and it covers compilation, detekt, both test suites, coverage
verification and the API check.

### Code Style

- Follow the existing code style (Kotlin coding conventions).
- Use meaningful variable and function names.
- Explicit API mode is on for the plugin: every public declaration needs an explicit visibility and
  return type.
- Warnings are errors. So is every detekt rule — the configuration runs with `allRules`.

```bash
./gradlew :kreate-plugin:detekt :kreate-detekt-rules:detekt :example:detekt
```

The rules in `kreate-detekt-rules` are **not** applied to this repository's own sources. They
enforce the Kreate standard for consumers — no `//` comments, KDoc on the published surface only —
and the plugin's sources deliberately carry both. `:example` is where the rule set is exercised
end to end, as a consumer would get it.

### Documentation in code

Every public declaration needs KDoc carrying `@param`, `@return` and `@since`. `@property` is not
used; document a constructor parameter inline on the parameter itself. These are enforced by
detekt's `UndocumentedPublic*` rules, so a missing one fails the build rather than review.

### Testing

All new features and bug fixes should include unit or functional tests.

- **JUnit Jupiter** is the runner; **Kotest assertions** (`io.kotest:kotest-assertions-core-jvm`)
  are the assertion library. Kotest's own spec styles are not used.
- Unit tests live in `kreate-plugin/src/test`. Anything that needs a real Gradle build goes in
  `kreate-plugin/src/functionalTest`, which drives TestKit.
- A Detekt rule's tests live in `kreate-detekt-rules/src/test` and lint a snippet through
  `dev.detekt:detekt-test`. Cover what the rule leaves alone, not only what it reports.

```bash
./gradlew :kreate-plugin:test :kreate-plugin:functionalTest
```

The functional suite is the slower of the two. While iterating on something unrelated to it:

```bash
./gradlew :kreate-plugin:functionalTest -Pkreate.test.skipSlow
```

### The public API is a gate

`kreate-plugin/api/kreate-plugin.api` and `kreate-detekt-rules/api/kreate-detekt-rules.api` record
the published binary interfaces and are checked by `apiCheck` on every build. If you deliberately
change a public API, re-record it and commit the result in the same change:

```bash
./gradlew :kreate-plugin:apiDump :kreate-detekt-rules:apiDump
```

### Documentation

**Critical Rule:** If your changes affect the public API, DSL, or project behavior, you **must**
update the relevant documentation.

- Update the Writerside topics under `docs/topics/`, and add any new topic to `docs/d.tree` — a
  topic that is not in the tree is not published.
- Update KDoc comments in the source code.
- Add a `CHANGELOG.md` entry.
- Generate the API documentation to verify the KDoc still renders:
  ```bash
  ./gradlew :example:dokkaGenerateHtml
  ```

---

## Commit Messages

We prefer clear and concise commit messages. Use the imperative mood (e.g., "Add feature" instead of "Added feature").

Example:
- `feat: add support for NewTarget`
- `fix: resolve memory leak in C-Interop`
- `docs: update publishing guide`
- `refactor: clean up PlatformModule`

---

## License

By contributing, you agree that your contributions will be licensed under the project's **Apache License 2.0**.
