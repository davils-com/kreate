# Overview

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

## Pipeline Overview

The C-Interop pipeline consists of six ordered Gradle tasks that run automatically before any
Kotlin/Native compilation:

| Step | Task                      | Description                                                             |
|------|---------------------------|-------------------------------------------------------------------------|
| 1    | `kreate-c-interop-initialize`   | Creates a new Rust library project with `cargo new --lib`               |
| 2    | `kreate-c-interop-dependencies` | Adds `libc` and `cbindgen` (or custom crates) via `cargo add`           |
| 3    | `kreate-c-interop-configure`    | Appends `[lib] crate-type = ["staticlib"]` to `Cargo.toml`              |
| 4    | `kreate-c-interop-script`       | Generates a `build.rs` that runs `cbindgen` to produce C headers        |
| 5    | `kreate-c-interop-compile`      | Runs `cargo build --release --target <target>` for each target          |
| 6    | `kreate-c-interop-definitions`  | Writes the Kotlin/Native `.def` file pointing to the compiled artifacts |

After step 6 completes, all `CInteropProcess` tasks automatically depend on
`kreate-c-interop-definitions`, so your normal `build` or `assemble` invocation drives the entire chain.

## Native Language Selection

Since Kreate **1.3.0**, C-Interop supports C and C++ as first-class native languages alongside Rust.
The `language` option selects the underlying pipeline:

| `language`             | Toolchain        | Pipeline summary                                                                 |
|------------------------|------------------|----------------------------------------------------------------------------------|
| `NativeLanguage.RUST`  | Cargo + cbindgen | _(default)_ Initializes a Cargo project and generates headers with `cbindgen`    |
| `NativeLanguage.C`     | CMake            | Scaffolds a CMake C project and builds a static library bridged via a C header   |
| `NativeLanguage.CPP`   | CMake            | Scaffolds a CMake C++ project and builds a static library bridged via a C header |

For `C` and `CPP`, the pipeline is reduced to three tasks:

| Step | Task                           | Description                                                              |
|------|--------------------------------|--------------------------------------------------------------------------|
| 1    | `kreate-c-interop-initialize`  | Scaffolds a CMake project with `CMakeLists.txt`, `include/` and `src/`   |
| 2    | `kreate-c-interop-compile`     | Runs `cmake` to build the static library `lib<name>.a`                   |
| 3    | `kreate-c-interop-definitions` | Writes the Kotlin/Native `.def` file pointing to the compiled artifacts  |

The C/C++ flow requires **CMake 3.20 or later** and a C/C++ compiler instead of the Rust toolchain.
The public API is declared in a hand-written header `include/<projectName>.h` (using an
`extern "C"` boundary for C++), which Kotlin/Native consumes directly.

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

Once enabled, all six pipeline tasks are registered automatically and execute in order before the
first `CInteropProcess` task runs.

## Next Steps

- **[](C-Interoperation-Configuration-Reference.md)**: DSL reference and all available options
- **[Gradle Tasks](C-Interoperation-Gradle-Task.md)**: Details on the individual pipeline tasks
- **[](C-Interoperation-Examples.md)**: Practical examples and usage patterns
- **[](C-Interoperation-Troubleshooting.md)**: Common issues and solutions

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
