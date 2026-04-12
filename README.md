# 🏗️ Kreate

[![License](https://img.shields.io/badge/License-Apache_2.0-Redtronics?style=for-the-badge&logo=apache&labelColor=white&color=blue)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-Redtronics?style=for-the-badge&logo=kotlin&labelColor=white&color=purple)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.4.0-Redtronics?style=for-the-badge&logo=gradle&labelColor=white&color=02303A)](https://gradle.org)

**Kreate** is an opinionated Gradle helper plugin for building Kotlin Multiplatform (KMP) and JVM projects. It provides a unified DSL to manage platform configurations, native C-Interop (Rust), documentation, testing, and publishing workflows with minimal boilerplate.

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Core Features](#-core-features)
- [Quick Start](#-quick-start)
- [Configuration Reference](#-configuration-reference)
- [Documentation](#-documentation)
- [Third-Party Software](#-third-party-software)
- [Contributing](#-contributing)
- [License](#-license--ethics)

---

## 🔍 Overview

Managing Kotlin Multiplatform configurations can be complex. **Kreate** simplifies this by:

*   **Standardizing Platform Setup**: A consistent DSL for JVM, Linux, macOS, and Windows.
*   **Integrating Rust**: Automated bridge between Rust and Kotlin via C-Interop.
*   **Enforcing Quality Standards**: Sensible defaults like `explicitApi()` and `allWarningsAsErrors`.
*   **Declarative Infrastructure**: Focus on project requirements while the plugin handles the underlying Gradle configuration.

---

## ✨ Core Features

### 🏗️ Platform Support
Kreate detects the project type (JVM, Android, or KMP) and applies appropriate optimizations:
- **JVM Support**: Configures Java 21+ toolchains and compiler options.
- **Multiplatform DSL**: Unified targets for Linux, macOS, and Windows.
- **Consistent Toolchains**: Ensures Java and Kotlin versions are synchronized across modules.

### 🦀 Rust C-Interop
Automates the integration of Rust libraries into Kotlin:
- **Toolchain Integration**: Manages `cargo` and cross-compilation targets.
- **Project Scaffolding**: Can generate Rust library structures if missing.
- **Header Synchronization**: Manages C headers and Kotlin bindings.
- **Multi-Arch Support**: Targets `x86_64`, `aarch64`, and others.

### 🧪 Testing Pipeline
Pre-configured **Kotest** integration for robust validation:
- **Parallel Execution**: Scales based on CPU availability.
- **Standardized Logging**: Clear output for test states (Passed, Skipped, Started).
- **Automated Reporting**: Generates HTML and XML reports for CI/CD.
- **Execution Controls**: Configurable timeouts and failure thresholds.

### 📦 Publishing & POM Management
Standardizes the release process for libraries:
- **Registry Support**: Built-in configurations for Maven Central and GitLab.
- **Publication Signing**: Integrated GPG signing for Maven Central requirements.
- **POM Metadata**: Declarative DSL for licenses, developers, and SCM information.
- **CI Integration**: Automatically utilizes GitLab environment variables for authentication.

---

## 📖 Documentation

Detailed documentation for Kreate is available in the following locations:

- **[Project Docs](./docs)**: Comprehensive guides and topic-specific information.
- **[API Reference](https://davils.github.io/kreate/api)**: Dokka-generated API documentation (if hosted).
- **[Examples](./example)**: A reference implementation demonstrating various configuration scenarios.

To generate the documentation locally, run:
```bash
./gradlew dokkaHtml
```

---

## 🛠️ Quick Start

Apply the plugin in your `build.gradle.kts`:

```kotlin
plugins {
    id("com.davils.kreate") version "1.0.0"
}

kreate {
    platform {
        javaVersion = JavaVersion.VERSION_25
        explicitApi = true
        
        multiplatform {
            cInterop {
                enabled = true
                rustTargets = listOf("x86_64-unknown-linux-gnu", "aarch64-apple-darwin")
            }
        }
    }

    project {
        name = "MyProject"
        description = "A project powered by Kreate"
        
        publish {
            enabled = true
            repositories {
                mavenCentral {
                    enabled = true
                    automaticRelease = true
                }
            }
        }
    }
}
```

---

## ⚙️ Configuration Reference

| Block | Property | Description | Default |
| :--- | :--- | :--- | :--- |
| `platform` | `javaVersion` | Target Java version (21, 25, etc.) | `VERSION_21` |
| `platform` | `explicitApi` | Enforces Kotlin Explicit API mode | `false` |
| `platform` | `allWarningsAsErrors` | Treats all compiler warnings as errors | `true` |
| `project` | `buildConstant` | Generate type-safe Kotlin constants | `Disabled` |
| `project` | `docs` | Configure Dokka documentation generation | `Disabled` |
| `project` | `tests` | Advanced Kotest configuration & reporting | `Enabled` |
| `project` | `publish` | Maven Central / GitLab publishing setup | `Disabled` |

---

## 📦 Third-Party Software

Kreate leverages several open-source technologies to provide its comprehensive feature set
For a full list of third-party libraries and their respective licenses, please refer to our [Third-Party Software](./THIRDPARTY.md) document.

---

## 🤝 Contributing

We welcome and appreciate all contributions! To maintain project quality, please follow these core principles:

- **Documentation**: If your change affects the API or behavior, you **must** update the corresponding docs.
- **Tests**: Ensure your changes are covered by tests and pass the existing suite.
- **Standards**: Follow the established Kotlin style and project conventions.

For detailed instructions on how to set up your environment and submit a pull request, please read our [Contributing Guidelines](CONTRIBUTING.md).

---

## ⚖️ License & Ethics

- **License**: Distributed under the **Apache License 2.0**. See `LICENSE` for details.
- **Code of Conduct**: We adhere to our [Code of Conduct](CODE_OF_CONDUCT.md).

---

<p align="center">
  Maintained by <a href="https://github.com/davils"><b>Davils</b></a>
</p>
