# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.2.1

### Added
- **Trivy Configuration**: Added `disableDependencyLocking` property to `TrivyExtension` to allow manual management of Gradle lockfiles.

### Fixed
- **Trivy Initialization**: Fixed a `ConcurrentModificationException` and resolution strategy errors by ensuring dependency locking is only activated for unresolved configurations.

## 1.2.0

### Added
- **Trivy Security Integration**: Integrated [Trivy](https://trivy.dev/) for automated security and compliance scanning.
  - **Vulnerability Scanning**: Automatically scan project dependencies (via lockfiles) for known CVEs.
  - **License Compliance**: Verify dependency licenses against forbidden or restricted lists to ensure legal compliance.
  - **Secret Detection**: Scan source files, configuration files, and environment files for hardcoded secrets and credentials.
- **Detekt Integration**: Integrated [Detekt](https://detekt.dev/) for automated static code analysis to enforce clean code architecture and design patterns.
- **Unified Security & Quality DSL**: New `trivy { }` and `detekt { }` configuration blocks to easily manage security and code quality settings.
- **Optimized Execution**: Trivy scans are optimized to run in a single aggregate process per project, significantly reducing build times compared to file-by-file scanning.
- **Lifecycle Aggregation**: Added a global `trivyScan` task that serves as a single entry point for all enabled security checks.

### Changed
- **Plugin Application (Breaking Change)**: To resolve lifecycle ordering issues, especially in Kotlin Multiplatform projects, `kreate` no longer automatically applies `maven-publish`, `detekt`, or the Vanniktech Maven Publish plugin. These must now be applied manually in the `plugins { }` block.
- **KDoc Standards**: All new components follow strict professional KDoc guidelines, including mandatory `@param`, `@return`, and `@since` tags.
- **Task Logging**: Improved console output for security findings with better formatting and clear error messages.

### Fixed
- **Publishing Lifecycle**: Fixed a critical bug where Maven and GitLab publication settings were not correctly initialized in Kotlin Multiplatform projects because the required plugins were applied too late by the framework.

## 1.1.1

### Changed
- **Task Naming Convention**: Standardized all Gradle tasks to use a consistent `kreate-<module>-<task>` prefix (e.g., `kreate-jni-build`, `kreate-c-interop-compile`).
- **Task Organization**: Organized plugin tasks into specific Gradle groups (`kreate c-interoperation`, `kreate jni`, `kreate build-constants`) for better visibility and navigation.

## 1.1.0

### Added
- **JNI Support**: Added comprehensive support for Java Native Interface (JNI) in JVM modules.
  - **Automated C++ Integration**: Seamlessly bridge C/C++ libraries with JVM projects using CMake.
  - **Project Scaffolding**: Built-in task to initialize new CMake-based C++ projects for JNI.
  - **Automated Build Pipeline**: Native builds are automatically hooked into the Kotlin compilation process.
  - **Runtime Configuration**: Automatic configuration of `java.library.path` for tests and execution tasks to resolve native libraries.
- **Improved Platform DSL**: Added `jvm` configuration block to the platform DSL for better organization of JVM-specific settings.

### Changed
- **Naming Convention**: Standardized internal naming resolution for native features (JNI, C-Interop) to ensure compatibility across different platforms and toolchains.
- **Documentation**: Updated KDoc across all new JNI components to follow strict professional standards.

## 1.0.1

### Added
- **KDoc Documentation**: Added comprehensive KDoc documentation across the entire project for improved clarity and professionalism.
- **Project Configuration**: Added `.gitignore` and copyright configuration files for project management.
- **Dependency Management**: Added Dependabot configuration for automated Gradle and GitHub Actions updates.
- **C-Interop Docs**: Added detailed documentation and examples for integrating Rust with Kotlin Multiplatform via C-Interop.

### Fixed
- **Versioning**: Corrected Kotlin and KSP versions in `libs.versions.toml` to 2.3.0.
- **Legal**: Updated copyright year to 2026 in multiple files and LICENSE.
- **DSL**: Updated `projectName` parameter to be non-nullable in GitLab and MavenCentral configuration functions for better reliability.
- **Build**: Updated Gradle build commands in documentation to use `--no-daemon`.

### Changed
- **Architecture**: Simplified project and publication configuration by removing the `Davils` object.
- **Docs**: Updated README with more detailed installation and configuration instructions.
- **Build**: Updated various dependencies (Kotest, Gradle Wrapper, GitHub Actions, KSP).

## 1.0.0

### Added

#### Core Architecture & Platform Support
- **Intelligent Platform Detection**: Automatic identification and configuration of `org.jetbrains.kotlin.jvm`, `org.jetbrains.kotlin.multiplatform`, and Android projects.
- **Unified Platform DSL**: Simplified configuration for target Java versions (supporting Java 21+ toolchains).
- **Strict Quality Defaults**:
  - Optional `explicitApi()` mode enforcement.
  - `allWarningsAsErrors` enabled by default for cleaner codebases.
- **Native Multiplatform Support**: Pre-configured targets for Linux (x64), macOS (x64, arm64), and Windows (x64).

#### Rust C-Interop (KMP)
- **Automated Rust Integration**: Seamlessly bridge Rust libraries with Kotlin Multiplatform using `cinterop`.
- **Cargo Toolchain Support**: Automated execution of `cargo build` with cross-compilation support.
- **Project Scaffolding**: Built-in task to initialize new Rust library projects within the Kotlin workspace.
- **Header & Definition Management**: Simplified DSL for `.def` files and C-header synchronization.
- **Multi-Arch Compilation**: Support for major architectures including `x86_64-unknown-linux-gnu` and `aarch64-apple-darwin`.

#### Testing Pipeline
- **Kotest Integration**: Deep integration with Kotest for advanced testing capabilities.
- **Execution Engine**:
  - Configurable parallel test execution (max forks based on CPU availability).
  - Customizable timeouts and failure thresholds.
- **Enhanced Test Logging**: Clear, colorized output for test states (Started, Passed, Skipped, Failed).
- **Reporting**: Automated generation of comprehensive XML and HTML test reports for CI/CD pipelines.

#### Publishing & POM Management
- **Declarative POM DSL**: Easy configuration of metadata including licenses, developers, SCM, and issue management.
- **Registry Support**:
  - **Maven Central**: Streamlined publishing with automatic release and GPG signing.
  - **GitLab Package Registry**: Native support for GitLab CI environments using environment variables (`CI_JOB_TOKEN`, etc.).
- **Security**: Integrated GPG signing for all publications.

#### Documentation & Constants
- **Integrated Dokka Support**: Simplified generation of API documentation via Gradle.
- **Build Constants**: Generate type-safe Kotlin constants from Gradle properties to bridge build-time information into runtime code.

#### Project Management
- **Centralized Versioning**: Global management of project group and version across all modules.
- **Standardized Repositories**: Automatic configuration of Maven Central and Google repositories.

### Changed
- Initial stable release. Transitioned from internal development to a public Gradle plugin.

---
[1.2.1]: https://github.com/davils/kreate/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/davils/kreate/compare/v1.1.1...v1.2.0
[1.1.1]: https://github.com/davils/kreate/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/davils/kreate/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/davils/kreate/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/davils/kreate/releases/tag/v1.0.0
