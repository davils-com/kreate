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
  - [Code Style](#code-style)
  - [Testing](#testing)
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

1. **Fork** the repository and create your branch from `main`.
2. If you've added code that should be tested, add **tests**.
3. If you've changed APIs or added features, update the **documentation**.
4. Ensure the test suite passes.
5. Submit a **Pull Request** with a clear description of the changes.

---

## Development Standards

### Code Style

- Follow the existing code style (Kotlin coding conventions).
- Use meaningful variable and function names.
- Ensure `explicitApi()` requirements are met where applicable.
- Run `./gradlew detekt` (if configured) or ensure no linting errors exist.

### Testing

All new features and bug fixes should include unit or functional tests.
- Use **Kotest** for testing as it is the project's standard.
- Ensure all tests pass locally before submitting a PR:
  ```bash
  ./gradlew test
  ```

### Documentation

**Critical Rule:** If your changes affect the public API, DSL, or project behavior, you **must** update the relevant documentation.
- Update Markdown files in the `docs/` directory.
- Update KDoc comments in the source code.
- Verify that documentation still generates correctly:
  ```bash
  ./gradlew dokkaHtml
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
