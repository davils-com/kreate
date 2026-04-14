# Overview

Kreate is a powerful Gradle plugin designed to streamline Kotlin Multiplatform development. It provides a structured and opinionated way to manage multi-module projects, platform configurations, and automated workflows.

## Key Features

- **Structured Configuration**: Use a clean, hierarchical DSL to configure your projects.
- **Kotlin Multiplatform Support**: Simplified setup for JVM and Multiplatform targets.
- **Rust/Cargo C-Interop**: Out-of-the-box support for integrating Rust code into Kotlin Multiplatform projects.
- **Automated Publishing**: Pre-configured support for GitLab Package Registry and Maven Central (via OSSRH).
- **Test Management**: Enhanced test logging and automated HTML/XML reporting.
- **Documentation**: Integrated Dokka setup for generating project documentation.
- **Build Constants**: Easily generate Kotlin classes containing build-time constants.

## Why Kreate?

Managing complex Kotlin Multiplatform projects often leads to repetitive and boilerplate-heavy Gradle scripts. Kreate aims to reduce this complexity by providing high-level abstractions for common tasks like publishing, testing, and platform-specific configurations.

By using Kreate, you can focus on writing code while the plugin handles the heavy lifting of Gradle plumbing.
