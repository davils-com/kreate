<p align="center">
  <img src="docs/images/kreate.svg" alt="Kreate Logo" width="250">
</p>

<h1 align="center">Kreate</h1>

<p align="center">
  <a href="https://opensource.org/licenses/Apache-2.0">
    <img src="https://img.shields.io/badge/License-Apache_2.0-Redtronics?style=for-the-badge&logo=apache&labelColor=white&color=blue" alt="License">
  </a>
  <a href="https://kotlinlang.org">
    <img src="https://img.shields.io/badge/Kotlin-2.4-Redtronics?style=for-the-badge&logo=kotlin&labelColor=white&color=purple" alt="Kotlin">
  </a>
  <a href="https://gradle.org">
    <img src="https://img.shields.io/badge/Gradle-9.0%2B-Redtronics?style=for-the-badge&logo=gradle&labelColor=white&color=02303A" alt="Gradle">
  </a>
  <a href="https://adoptium.net">
    <img src="https://img.shields.io/badge/JDK-17%2B-Redtronics?style=for-the-badge&logo=openjdk&labelColor=white&color=ED8B00" alt="JDK">
  </a>
</p>

<p align="center">
  <strong>Kreate</strong> is an opinionated Gradle plugin for Kotlin JVM and Multiplatform projects.
  It replaces the platform, native interop, security, testing, documentation and publishing
  boilerplate that every serious build accumulates with a single declarative DSL.
</p>

---

## Table of Contents

- [Why Kreate](#why-kreate)
- [Core Features](#core-features)
- [Quick Start](#quick-start)
- [Configuration Reference](#configuration-reference)
- [Compatibility](#compatibility)
- [Documentation](#documentation)
- [Third-Party Software](#third-party-software)
- [Contributing](#contributing)
- [License & Ethics](#license--ethics)

---

## Why Kreate

Kreate is a *convention* plugin, not a framework.

- **It does not wrap Gradle.** Your build script stays a Gradle build script, and every task
  Kreate registers is an ordinary task you can depend on, reconfigure, or disable.
- **It applies a plugin only for a feature you enabled.** Enabling `coverage` applies Kover, and
  nothing applies Kover otherwise. Apply it yourself to pin a version or to reach its own DSL
  block, and Kreate leaves it alone. The Kotlin plugin is always yours to apply.
- **It does not touch your repositories or dependency resolution** unless you explicitly ask.
- **Everything is opt-in.** Applying the plugin on its own registers no tasks and changes no
  behaviour.

---

## Core Features

### Platform Configuration

Kreate reacts to the Kotlin plugin you applied rather than guessing at your project type.

- **Toolchain alignment**: one `javaVersion` drives the Kotlin toolchain *and*
  `sourceCompatibility`/`targetCompatibility`, which is where "works on my machine" bytecode
  mismatches usually come from.
- **Compiler policy**: `explicitApi` and `allWarningsAsErrors` applied consistently across modules.
- **Multiplatform aware**: JVM and Multiplatform projects follow the appropriate configuration path.

### JNI (C/C++ on the JVM)

- **Generated headers**: `kreateJniHeaders` reads your compiled classes and emits the exact C
  declarations the JVM will look up — correct mangling, correct types, overload disambiguation.
  Kotlin has no `javac -h` equivalent, so this is the only automated way to stop a mistyped symbol
  from becoming a runtime `UnsatisfiedLinkError`.
- **Correct CMake builds**: the JDK comes from your Gradle toolchain rather than the machine
  default, output paths are pinned so multi-configuration generators behave predictably, and the
  full compiler output is surfaced on failure.
- **Reliable incremental builds**: every file the native build reads is a declared task input, so a
  C++ edit rebuilds and an untouched project does not.
- **Distributable artifacts**: optionally package the shared library into your JAR with a generated
  loader, so consumers need no `java.library.path` setup.

### Binary Compatibility Validation

- **A dump you review**: `kreateApiDump` records every public and protected declaration of your
  compiled classes in a file you commit; `kreateApiCheck` runs as part of `check` and fails the
  build when the two disagree. A removed overload or a widened return type becomes a line in a
  diff rather than a bug report from a consumer.
- **No extra plugin**: Kreate reads the bytecode itself with ASM, so there is no plugin to apply
  and no version to keep in step with your Kotlin release.
- **Kotlin aware**: `internal` declarations are excluded even though the bytecode calls them
  public, along with the bridges generated for their default arguments — otherwise a rename
  inside your module would read as a breaking change.
- **Drop-in format**: the dump is the one the `binary-compatibility-validator` plugin writes, so
  an existing `api/*.api` file carries over untouched.

### Benchmarks

- **Setup, not boilerplate**: enabling benchmarks builds the `benchmarks` source set, associates
  it with `main` so it can reach your `internal` declarations, adds the kotlinx-benchmark runtime,
  and applies the `allopen` compiler plugin JMH needs. Getting that last one wrong produces a JMH
  error that never mentions `allopen`.
- **A regression gate**: `kreateBenchmarkBaseline` records a committed baseline;
  `kreateBenchmarkCheck` runs the benchmarks and fails when one got measurably slower. The
  comparison knows that `thrpt` is better when higher and `avgt` better when lower, refuses to
  compare scores recorded in different units, and treats a benchmark that vanished as a failure.
- **Noise-aware**: a regression counts only when it also exceeds the combined measurement error.
  Without that, a gate on shared CI hardware fires on ordinary variance and gets switched off.
- **A report you can depend on**: kotlinx-benchmark writes to a timestamped directory, which no
  task can declare as an output. Kreate republishes the newest run at a fixed path.

### Dependency Locking

- **Reproducible resolution**: pins resolved versions in a committed `gradle.lockfile`.
- **A complete lock file**: `kreateResolveAndLockAll --write-locks` resolves every locked
  classpath in one invocation. `--write-locks` on its own records only what the build happened
  to resolve, which produces a lock file that looks complete and is not.
- **Scoped by default**: locks `compileClasspath` and `runtimeClasspath` rather than everything,
  so a vulnerability scan reports on what you ship instead of on Dokka's XML parser.

### Local Development

- **Test a fix across repositories without a release**: `./gradlew kreateLocalPublish` in the
  producer, an ordinary build in the consumer. Neither repository is edited — not the version
  catalog, not `gradle.properties`, not a lock file.
- **Nothing to switch on**: publishing activates it, `kreateLocalClean` ends it, and every
  affected build announces what it is substituting.
- **Impossible in CI**: the state lives under `GRADLE_USER_HOME`, which pipelines recreate per
  job; a runner that carries it fails rather than resolving from it.
- **Lock files untouched**: locking is deactivated and `--write-locks` refused, so a snapshot can
  never reach a committed lock file.
- **Whole workspaces**: `kreateLocalPublishAll --from arc` republishes a library and everything
  downstream of it, in dependency order.

### C-Interoperability (Kotlin/Native)

- **Multi-language**: Rust via Cargo, C and C++ via CMake.
- **Automatic scaffolding** of the native project structure for the selected language.
- **Binding generation**: manages C headers and `.def` files for you.
- **Multi-architecture**: targets `x86_64`, `aarch64`, and other native triples.
- **Per-platform publishing**: publish a JNI library as `mylib` plus `mylib-linux-x64`, and ship
  whichever platforms your infrastructure can actually build. Selecting a subset is a supported
  release; selecting a platform with no binary fails the build instead of shipping a hole.

### Security & Compliance

- **Vulnerability scanning**: CVEs in dependencies, from Gradle lock files.
- **Licence compliance**: verify third-party licences against a forbidden list.
- **Secret detection**: built-in and custom rules for hard-coded credentials.
- **Platform-agnostic**: the `trivy { }` block works in any project, including ones using no other
  Kreate feature.

### Project & Release

- **Build constants**: generate a type-safe Kotlin object from build values.
- **Testing**: parallel execution, readable logging, and HTML/XML reports.
- **Code coverage**: Kover configured from one block — reports, filters, named verification rules
  and a threshold gate that runs on `check`, plus multi-project aggregation. Ships with no default
  threshold on purpose: you measure first, then set one.
- **Documentation**: Dokka configured from the same metadata as your POM.
- **Publishing**: signed releases to Maven Central and the GitLab Package Registry, with a
  declarative DSL for licences, developers, and SCM information.

---

## Quick Start

### Installation

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.4.0"
    id("com.davils.kreate") version "3.1.0"
}
```

> **Note**
> That is the whole plugins block. Kreate applies Detekt, Kover, kotlinx-benchmark, Dokka and the
> publishing plugins itself, for whichever integrations you enable — apply them yourself only to
> pin a version or to reach their own DSL blocks, in which case Kreate leaves them alone. The
> Kotlin plugin is the exception and stays yours: which one a project uses is the shape of the
> project, and its version governs the language your sources are written in.

### Configuration

```kotlin
group = "com.example"

kreate {
    platform {
        javaVersion = JavaVersion.VERSION_17
        explicitApi = true
        allWarningsAsErrors = true

        jvm {
            jni {
                enabled = true

                headers { enabled = true }      // generate JNI declarations
                packaging { enabled = true }    // ship natives inside the JAR
            }
        }
    }

    project {
        name = "MyProject"
        description = "A project powered by Kreate"

        version {
            environment = "CI_COMMIT_TAG"
            property = "version"
        }

        buildConstant {
            enabled = true
            className = "BuildConfig"
            constant("apiUrl", "https://api.example.com")
        }
    }

    trivy {
        enabled = true

        vulnerability {
            failOnFindings = true
            lockFiles.from(fileTree(projectDir) { include("*.lockfile") })
        }
    }
}
```

### Tasks

```bash
./gradlew tasks --group kreate      # what is registered
./gradlew kreateJniBuild            # build the native library
./gradlew kreateTrivyScan           # run all enabled security scans
./gradlew kreateBuildConstants      # regenerate build constants
./gradlew kreateApiDump             # record the public binary interface
./gradlew kreateResolveAndLockAll --write-locks   # write the dependency lock file
./gradlew kreateBenchmarkCheck      # run benchmarks and compare against the baseline
./gradlew kreateLocalPublish        # install this repository for other checkouts to resolve
./gradlew kreateLocalStatus         # what is published locally, or why local mode is off
./gradlew kreateLocalClean          # undo it
```

---

## Configuration Reference

| Block                             | Property                   | Description                                | Default      |
|:----------------------------------|:---------------------------|:-------------------------------------------|:-------------|
| `platform`                        | `javaVersion`              | Java toolchain and bytecode target         | `VERSION_17` |
| `platform`                        | `explicitApi`              | Kotlin explicit API mode                   | `false`      |
| `platform`                        | `allWarningsAsErrors`      | Compiler warnings become errors            | `true`       |
| `platform.jvm.jni`                | `enabled`                  | CMake-based JNI integration                | `false`      |
| `platform.jvm.jni`                | `buildType`                | CMake build type                           | `Release`    |
| `platform.jvm.jni`                | `cmakeExecutable`          | Explicit CMake path                        | *resolved*   |
| `platform.jvm.jni.headers`        | `enabled`                  | Generate JNI headers from compiled classes | `true`       |
| `platform.jvm.jni.packaging`      | `enabled`                  | Package natives into the JAR               | `false`      |
| `platform.multiplatform.cInterop` | `enabled`                  | Native interop for Kotlin/Native           | `false`      |
| `platform.multiplatform.cInterop` | `language`                 | `RUST`, `C`, or `CPP`                      | `RUST`       |
| `project`                         | `applyDefaultRepositories` | Add public repositories to the project     | `false`      |
| `project`                         | `applySerializationPlugin` | Apply the serialization compiler plugin    | `false`      |
| `project.buildConstant`           | `enabled`                  | Generate type-safe Kotlin constants        | `false`      |
| `project.docs`                    | `enabled`                  | Dokka documentation                        | `false`      |
| `project.tests`                   | `enabled`                  | Test execution and reporting               | `true`       |
| `project.detekt`                  | `enabled`                  | Static analysis configuration              | `false`      |
| `project.coverage`                | `enabled`                  | Code coverage through Kover                | `false`      |
| `project.coverage.verify`         | `minLineCoverage`          | Coverage threshold enforced on `check`     | unset        |
| `project.coverage.aggregate`      | `enabled`                  | Merge subproject coverage into one report  | `false`      |
| `project.apiValidation`           | `enabled`                  | Binary compatibility validation            | `false`      |
| `project.dependencyLocking`       | `enabled`                  | Gradle dependency locking                  | `false`      |
| `project.benchmark`               | `enabled`                  | kotlinx-benchmark integration              | `false`      |
| `project.benchmark.regression`    | `maxRegressionPercent`     | Benchmark regression threshold             | `10.0`       |
| `project.publish`                 | `enabled`                  | Maven Central / GitLab publishing          | `false`      |
| `trivy`                           | `enabled`                  | Security and compliance scanning           | `false`      |

---

## Compatibility

| Component        | Minimum | Verified in CI                     |
|:-----------------|:--------|:-----------------------------------|
| Gradle           | 9.0     | 9.0 and the current release        |
| JDK              | 17      | 17                                 |
| Kotlin           | 2.4.0   | 2.4.0                              |
| Operating system | —       | Linux, macOS, Windows              |
| CMake            | 3.20    | Required only for native features  |

Kreate is compiled against the Kotlin and Java versions embedded in the *minimum* supported
Gradle. A plugin built against a newer API does not fail in its own repository — it fails at
runtime on a consumer's machine, which is exactly what building against the floor prevents.

---

## Documentation

- **[Documentation site](https://davils-com.github.io/kreate/)**: guides, configuration
  references, and troubleshooting.
- **[Example project](./example)**: a working reference covering JNI, C-interop, Trivy, build
  constants, testing, and publishing.
- **[Changelog](./CHANGELOG.md)**: what changed in each release.
- **[Security policy](./SECURITY.md)**: how to report a vulnerability.

---

## Third-Party Software

Kreate leverages various open-source technologies. For a full list of libraries and licenses,
please refer to the [Third-Party Software](./THIRDPARTY.md) document.

---

## Contributing

Contributions are welcome. To keep the quality bar where it is:

- **Tests**: new or changed behaviour needs a test. The suite drives real Gradle builds through
  TestKit, so a behavioural change is genuinely verifiable.
- **Documentation**: API and behaviour changes must be reflected in `docs/topics/`.
- **Public API**: run `./gradlew apiDump` and commit the result if the DSL changed.
- **Standards**: follow the KDoc rules in `.junie/AGENTS.md` — every public declaration carries
  `@param`, `@return`, and `@since`, and Detekt enforces it.

Detailed instructions are in the [Contributing Guidelines](CONTRIBUTING.md).

---

## License & Ethics

- **License**: Published under the **Apache License 2.0**. See `LICENSE` for details.
- **Code of Conduct**: We adhere to our [Code of Conduct](CODE_OF_CONDUCT.md).

---

<p align="center">
  Maintained by <a href="https://github.com/davils-com"><b>Davils</b></a>
</p>
