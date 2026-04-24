# Overview

Managing Kotlin Multiplatform (KMP) and JVM projects typically requires significant Gradle boilerplate — juggling
platform targets, native interop, toolchain versions, and publishing pipelines across multiple modules.
**Kreate** eliminates that overhead with a single, opinionated DSL that handles all of it declaratively.

## How It Works

Instead of manually wiring together Gradle plugins and configuration blocks for each platform, Kreate automatically
detects your project type — JVM, Android, or Multiplatform — and applies the correct defaults out of the box.

This includes:

- Java toolchain synchronization across all modules
- Kotlin compiler flags (`explicitApi()`, `allWarningsAsErrors`) enforced by default
- Cross-platform target registration for Linux, macOS, and Windows

## Native Integration

For projects that go beyond pure Kotlin, Kreate provides first-class native code integration.

### Rust via C-Interop

- Automates the full Rust → C-Interop bridge using `cargo`
- Manages multi-architecture compilation targets (`x86_64`, `aarch64`, and more)
- Can scaffold Rust library structures if they are missing
- Handles C header synchronization and Kotlin binding generation

### C/C++ via JNI

- CMake-based native builds with zero manual wiring
- Automatic `java.library.path` configuration for test and runtime execution
- Follows a structured source layout consistent with C-Interop conventions

## Publishing

Publishing is a first-class concern in Kreate.

| Registry | Feature |
|---|---|
| Maven Central | GPG signing + automatic release |
| GitLab | Built-in registry configuration |

A declarative DSL covers all POM metadata: licenses, developer information, and SCM links.

## Testing

Kreate ships a pre-configured **Kotest** integration — no setup required.

- Parallel test execution scaled to available CPU cores
- Standardized logging output for Passed, Skipped, and Started states
- Automatic HTML and XML report generation for CI/CD pipelines

<tip>
All features are opt-in via the <code>kreate { }</code> block in your <code>build.gradle.kts</code>.
Sensible defaults are applied automatically so you only configure what you need.
</tip>