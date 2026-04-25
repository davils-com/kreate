# C-Interop

The C-Interop feature of the Kreate Gradle plugin provides a fully automated pipeline for bridging
Kotlin/Native multiplatform projects with native Rust libraries. When enabled, Kreate orchestrates
every step — from initializing a Rust project via Cargo, adding dependencies, compiling for
multiple native targets, generating C headers with `cbindgen`, producing Kotlin/Native `.def`
files, and wiring the resulting static libraries into your Kotlin Multiplatform build — all without
manual setup.

> **Prerequisites**
>
> - [Rust toolchain](https://rustup.rs/) installed (including `cargo` and `rustup`)
> - Required cross-compilation targets added via `rustup target add <target>`
> - Kotlin Multiplatform plugin applied to your Gradle module
> - Kreate plugin applied to your Gradle module
>
{style="note"}

## Overview

The C-Interop pipeline consists of six ordered Gradle tasks that run automatically before any
Kotlin/Native compilation:

| Step | Task | Description |
|------|------|-------------|
| 1 | `initializeRustProject` | Creates a new Rust library project with `cargo new --lib` |
| 2 | `addRustDependencies` | Adds `libc` and `cbindgen` (or custom crates) via `cargo add` |
| 3 | `configureCargo` | Appends `[lib] crate-type = ["staticlib"]` to `Cargo.toml` |
| 4 | `generateRustBuildScript` | Generates a `build.rs` that runs `cbindgen` to produce C headers |
| 5 | `compileRust` | Runs `cargo build --release --target <target>` for each target |
| 6 | `generateDefinitionFiles` | Writes the Kotlin/Native `.def` file pointing to the compiled artifacts |

After step 6 completes, all `CInteropProcess` tasks automatically depend on
`generateDefinitionFiles`, so your normal `build` or `assemble` invocation drives the entire chain.

## Enabling C-Interop

C-Interop is **disabled by default**. Enable it inside your module's `build.gradle.kts` within the
`kreate` extension block:

```kotlin
kreate {
    platform {
        multiplatform {
            cInterop {
                enabled.set(true)
            }
        }
    }
}
```

## Configuration Reference

The `cInterop { }` block exposes the `CInteropExtension`, which controls all aspects of the
pipeline.

### Top-Level Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | `Property<Boolean>` | `false` | Master switch — must be `true` for any C-Interop tasks to run |
| `nameOverride` | `Property<String>` | *(project name)* | Overrides the name used for the Rust project directory and C-Interop compilation unit |
| `projectDirectory` | `DirectoryProperty` | `cinterop/` | Directory (relative to the Gradle module) where the Rust project is created |
| `packageNameOverride` | `Property<String>` | `<group>.<name>.cinterop` | Overrides the Kotlin package name under which the native bindings are exposed |
| `rustTargets` | `ListProperty<String>` | *(auto-detected)* | Explicit list of Rust target triples to compile for; if empty the host OS/arch is used |

### Target Auto-Detection

When `rustTargets` is not set, Kreate detects the current host and selects a single target
automatically:

| Host OS | Host Architecture | Resolved Target |
|---------|-------------------|-----------------|
| Windows | x86_64 | `x86_64-pc-windows-gnu` |
| Linux | x86_64 | `x86_64-unknown-linux-gnu` |
| Linux | aarch64 | `aarch64-unknown-linux-gnu` |
| macOS | aarch64 | `aarch64-apple-darwin` |

For cross-compilation or multi-target releases, set `rustTargets` explicitly (see examples below).

### Definition File Configuration

The `defFile { }` sub-block configures the `.def` file generated for Kotlin/Native:

```kotlin
cInterop {
    enabled.set(true)
    defFile {
        fileName.set("cinterop.def")   // default: "cinterop.def"
        dirName.set("defs")            // default: "defs"
    }
}
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `fileName` | `Property<String>` | `cinterop.def` | Name of the generated `.def` file |
| `dirName` | `Property<String>` | `defs` | Directory name (inside the Rust project) where the `.def` file is placed |

### Per-Platform Native Target Configuration

You can apply additional Kotlin/Native target configuration for each platform family using the
`mingw { }`, `linux { }`, and `macos { }` blocks. These receive a `KotlinNativeTarget` receiver
and are executed after Kreate wires the C-Interop compilation:

```kotlin
cInterop {
    enabled.set(true)
    mingw {
        // KotlinNativeTarget configuration for mingwX64
        binaries.sharedLib()
    }
    linux {
        // KotlinNativeTarget configuration for linuxX64 / linuxArm64
    }
    macos {
        // KotlinNativeTarget configuration for macosArm64
    }
}
```

### Rust Target → Kotlin/Native Target Mapping

Each Rust target triple is mapped to a Kotlin/Native target automatically:

| Rust Target Triple | Kotlin/Native Target |
|--------------------|----------------------|
| `x86_64-pc-windows-*` / `*mingw*` | `mingwX64` |
| `aarch64-apple-darwin` | `macosArm64` |
| `x86_64-unknown-linux-*` | `linuxX64` |
| `aarch64-unknown-linux-*` | `linuxArm64` |

Any other triple causes a `GradleException` at configuration time.

## Generated Project Layout

After the pipeline runs, the following structure is created inside your Gradle module:

```
<module>/
└── cinterop/                          ← projectDirectory (default)
    └── <projectName>/                 ← Rust project root
        ├── Cargo.toml                 ← patched with [lib] crate-type = ["staticlib"]
        ├── build.rs                   ← generated cbindgen build script
        ├── src/
        │   └── lib.rs                 ← your Rust library source
        ├── include/
        │   └── <projectName>.h        ← C header generated by cbindgen
        ├── defs/
        │   └── cinterop.def           ← Kotlin/Native definition file
        └── target/
            └── <rustTarget>/
                └── release/
                    └── lib<name>.a    ← compiled static library
```

### Generated `build.rs`

The `GenerateRustBuildScript` task writes the following Rust build script if none exists yet (or
if the existing file is empty):

```rust
extern crate cbindgen;

use std::env;
use cbindgen::Language::C;

fn main() {
    let crate_dir = env::var("CARGO_MANIFEST_DIR").unwrap();

    cbindgen::Builder::new()
        .with_crate(crate_dir)
        .with_language(C)
        .generate()
        .expect("Unable to generate bindings")
        .write_to_file("include/<projectName>.h");
}
```

This script runs automatically during `cargo build` and produces the C header consumed by the
`.def` file.

### Generated `.def` File

The `GenerateDefinitionFiles` task writes a `.def` file with the following format:

```
headers = <projectName>.h
staticLibraries = lib<projectName>.a
compilerOpts = -I<cinteropDir>/<projectName>/include
libraryPaths = <cinteropDir>/<projectName>/target/<rustTarget>/release
```

When multiple Rust targets are configured, all their release directories are listed in
`libraryPaths` separated by spaces.

## C-Interop Package Name

By default, the Kotlin package under which the native symbols are exposed is derived from the
Gradle project group and the (lowercased) project name:

```
<project.group>.<projectName.lowercase()>.cinterop
```

Override it with `packageNameOverride`:

```kotlin
cInterop {
    enabled.set(true)
    packageNameOverride.set("com.example.mylib.native")
}
```

## Cargo Command Resolution

On **macOS**, Kreate looks for `cargo` at `~/.cargo/bin/cargo` (the default `rustup` install
path). On all other platforms, it simply invokes `cargo` from the system `PATH`.

## Examples

### Minimal Configuration

```kotlin
kreate {
    platform {
        multiplatform {
            cInterop {
                enabled.set(true)
            }
        }
    }
}
```

This uses the Gradle project name as the Rust project name, auto-detects the host target, and
places all files under `<module>/cinterop/`.

### Custom Name and Directory

```kotlin
kreate {
    platform {
        multiplatform {
            cInterop {
                enabled.set(true)
                nameOverride.set("myRustLib")
                projectDirectory.set(layout.projectDirectory.dir("native"))
            }
        }
    }
}
```

The Rust project is created at `<module>/native/myRustLib/`, and the C-Interop compilation unit is
registered under the name `myRustLib`.

### Multi-Target Cross-Compilation

```kotlin
kreate {
    platform {
        multiplatform {
            cInterop {
                enabled.set(true)
                rustTargets.set(listOf(
                    "x86_64-unknown-linux-gnu",
                    "aarch64-unknown-linux-gnu",
                    "aarch64-apple-darwin",
                    "x86_64-pc-windows-gnu"
                ))
                linux {
                    // applied to both linuxX64 and linuxArm64
                }
                macos {
                    // applied to macosArm64
                }
                mingw {
                    // applied to mingwX64
                }
            }
        }
    }
}
```

All four targets are compiled in sequence and their release directories are listed in the generated
`.def` file.

### Custom Def File Location

```kotlin
kreate {
    platform {
        multiplatform {
            cInterop {
                enabled.set(true)
                defFile {
                    fileName.set("bindings.def")
                    dirName.set("kotlin-native")
                }
            }
        }
    }
}
```

The definition file is written to
`<module>/cinterop/<projectName>/kotlin-native/bindings.def`.

### Full Configuration

```kotlin
kreate {
    platform {
        multiplatform {
            cInterop {
                enabled.set(true)
                nameOverride.set("mylib")
                projectDirectory.set(layout.projectDirectory.dir("rust"))
                packageNameOverride.set("com.davils.myapp.native")
                rustTargets.set(listOf(
                    "x86_64-unknown-linux-gnu",
                    "aarch64-apple-darwin"
                ))
                defFile {
                    fileName.set("cinterop.def")
                    dirName.set("defs")
                }
                linux {
                    compilations.all {
                        kotlinOptions.freeCompilerArgs += listOf("-opt")
                    }
                }
                macos { }
            }
        }
    }
}
```

## Gradle Task Reference

All tasks live in the default Gradle task group and run in strict dependency order. They are
registered only when `enabled` is `true`.

### `initializeRustProject`

**Class:** `InitializeRustProject`

Runs `cargo new --lib <projectName>` inside `projectDirectory`. If the target directory already
exists, the task is skipped (no-op).

| Input | Description |
|-------|-------------|
| `workDir` | The resolved `projectDirectory` |
| `projectName` | The resolved project name |

| Output | Description |
|--------|-------------|
| `outputDir` | `<workDir>/<projectName>/` |

### `addRustDependencies`

**Class:** `AddRustDependencies`

Adds runtime and build dependencies to `Cargo.toml` via `cargo add`. Defaults to adding `libc` as
a runtime dependency and `cbindgen` as a build dependency. Already-present dependencies are
skipped.

| Input | Description |
|-------|-------------|
| `workDir` | The Rust project directory |
| `rustDependencies` | Map of `name → version`; defaults to `{"libc": ""}` |
| `rustBuildDependencies` | Map of `name → version`; defaults to `{"cbindgen": ""}` |

> The `rustDependencies` and `rustBuildDependencies` maps are currently populated by the plugin
> internally. Direct user configuration of these maps is not yet exposed in the DSL.
>
{style="note"}

### `configureCargo`

**Class:** `ConfigureCargo`

Appends the following block to `Cargo.toml` if not already present:

```toml
[lib]
crate-type = ["staticlib"]
```

This ensures Cargo produces a static library (`.a` / `.lib`) suitable for linking into
Kotlin/Native.

| Input | Description |
|-------|-------------|
| `workDir` | The Rust project directory containing `Cargo.toml` |

| Output | Description |
|--------|-------------|
| `outputFile` | `<workDir>/Cargo.toml` |

### `generateRustBuildScript`

**Class:** `GenerateRustBuildScript`

Creates `build.rs` in the Rust project root. The script uses `cbindgen` to generate a C header
at `include/<projectName>.h`. If `build.rs` already exists and is non-empty, the task is skipped.

| Input | Description |
|-------|-------------|
| `workDir` | The Rust project directory |
| `projectName` | Used as the header file name |

| Output | Description |
|--------|-------------|
| `outputFile` | `<workDir>/build.rs` |

### `compileRust`

**Class:** `CompileRust`

Executes `cargo build --target <target> --release` for each resolved Rust target. If compilation
fails for any target, a `GradleException` is thrown with the failing target name.

| Input | Description |
|-------|-------------|
| `workDir` | The Rust project directory |
| `rustTargets` | Optional; resolved from config or auto-detected from host |

| Output | Description |
|--------|-------------|
| `outputDir` | `<workDir>/target/` |

### `generateDefinitionFiles`

**Class:** `GenerateDefinitionFiles`

Creates the directory specified by `defDirName` and writes the `.def` file consumed by
`CInteropProcess`. The file references the compiled static library and the generated C header.

| Input | Description |
|-------|-------------|
| `workDir` | The Rust project directory |
| `rootDir` | The `projectDirectory` (parent of the Rust project) |
| `projectName` | Used in header/library names |
| `defFileName` | File name for the `.def` file |
| `defDirName` | Directory name for the `.def` file |
| `rustTargets` | Optional; used to construct `libraryPaths` |

| Output | Description |
|--------|-------------|
| `outputDir` | `<workDir>/<defDirName>/` |

## Troubleshooting

### `cargo` not found

Ensure the Rust toolchain is installed and `cargo` is available on your `PATH`. On macOS, Kreate
looks at `~/.cargo/bin/cargo`; if you installed Rust differently, verify that path exists.

### Unsupported Rust target

If a target triple in `rustTargets` does not match any of the supported patterns, Kreate throws:

```
GradleException: Unsupported Rust target for Kotlin/Native mapping: <target>
```

Supported patterns are `x86_64-pc-windows-*`, `*mingw*`, `aarch64-apple-darwin`,
`x86_64-unknown-linux-*`, and `aarch64-unknown-linux-*`.

### Missing cross-compilation target

If `cargo build` fails with a message like *"error: toolchain ... does not contain component
rust-std for target ..."*, add the target:

```bash
rustup target add <target>
```

### Failed to create root directory

If the `projectDirectory` cannot be created (e.g. due to filesystem permissions), Kreate throws:

```
GradleException: Failed to create root directory: <path>
```

Ensure the parent directory is writable by the Gradle daemon process.

### Definition file not updated

The `generateDefinitionFiles` task is not incremental in the traditional sense — it always
rewrites the `.def` file. If you change `rustTargets` or directory settings, run a clean build
(`./gradlew clean`) to avoid stale Gradle task caching.