# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.0.0

### Added

#### 🏗️ Core Architecture & Platform Support
- **Intelligent Platform Detection**: Automatic identification and configuration of `org.jetbrains.kotlin.jvm`, `org.jetbrains.kotlin.multiplatform`, and Android projects.
- **Unified Platform DSL**: Simplified configuration for target Java versions (supporting Java 21+ toolchains).
- **Strict Quality Defaults**:
  - Optional `explicitApi()` mode enforcement.
  - `allWarningsAsErrors` enabled by default for cleaner codebases.
- **Native Multiplatform Support**: Pre-configured targets for Linux (x64), macOS (x64, arm64), and Windows (x64).

#### 🦀 Rust C-Interop (KMP)
- **Automated Rust Integration**: Seamlessly bridge Rust libraries with Kotlin Multiplatform using `cinterop`.
- **Cargo Toolchain Support**: Automated execution of `cargo build` with cross-compilation support.
- **Project Scaffolding**: Built-in task to initialize new Rust library projects within the Kotlin workspace.
- **Header & Definition Management**: Simplified DSL for `.def` files and C-header synchronization.
- **Multi-Arch Compilation**: Support for major architectures including `x86_64-unknown-linux-gnu` and `aarch64-apple-darwin`.

#### 🧪 Testing Pipeline
- **Kotest Integration**: Deep integration with Kotest for advanced testing capabilities.
- **Execution Engine**:
  - Configurable parallel test execution (max forks based on CPU availability).
  - Customizable timeouts and failure thresholds.
- **Enhanced Test Logging**: Clear, colorized output for test states (Started, Passed, Skipped, Failed).
- **Reporting**: Automated generation of comprehensive XML and HTML test reports for CI/CD pipelines.

#### 📦 Publishing & POM Management
- **Declarative POM DSL**: Easy configuration of metadata including licenses, developers, SCM, and issue management.
- **Registry Support**:
  - **Maven Central**: Streamlined publishing with automatic release and GPG signing.
  - **GitLab Package Registry**: Native support for GitLab CI environments using environment variables (`CI_JOB_TOKEN`, etc.).
- **Security**: Integrated GPG signing for all publications.

#### 📖 Documentation & Constants
- **Integrated Dokka Support**: Simplified generation of API documentation via Gradle.
- **Build Constants**: Generate type-safe Kotlin constants from Gradle properties to bridge build-time information into runtime code.

#### 🛠️ Project Management
- **Centralized Versioning**: Global management of project group and version across all modules.
- **Standardized Repositories**: Automatic configuration of Maven Central and Google repositories.

### Changed
- Initial stable release. Transitioned from internal development to a public Gradle plugin.

---
[1.0.0]: https://github.com/davils/kreate/releases/tag/v1.0.0
