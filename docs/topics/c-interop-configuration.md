# Configuration Reference

The `cInterop { }` block exposes the `CInteropExtension`, which controls all aspects of the
pipeline.

## Top-Level Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | `Property<Boolean>` | `false` | Master switch — must be `true` for any C-Interop tasks to run |
| `nameOverride` | `Property<String>` | *(project name)* | Overrides the name used for the Rust project directory and C-Interop compilation unit |
| `projectDirectory` | `DirectoryProperty` | `cinterop/` | Directory (relative to the Gradle module) where the Rust project is created |
| `packageNameOverride` | `Property<String>` | `<group>.<name>.cinterop` | Overrides the Kotlin package name under which the native bindings are exposed |
| `rustTargets` | `ListProperty<String>` | *(auto-detected)* | Explicit list of Rust target triples to compile for; if empty the host OS/arch is used |

## Target Auto-Detection

When `rustTargets` is not set, Kreate detects the current host and selects a single target
automatically:

| Host OS | Host Architecture | Resolved Target |
|---------|-------------------|-----------------|
| Windows | x86_64 | `x86_64-pc-windows-gnu` |
| Linux | x86_64 | `x86_64-unknown-linux-gnu` |
| Linux | aarch64 | `aarch64-unknown-linux-gnu` |
| macOS | aarch64 | `aarch64-apple-darwin` |

For cross-compilation or multi-target releases, set `rustTargets` explicitly.

## Rust Target → Kotlin/Native Target Mapping

Each Rust target triple is mapped to a Kotlin/Native target automatically:

| Rust Target Triple | Kotlin/Native Target |
|--------------------|----------------------|
| `x86_64-pc-windows-*` / `*mingw*` | `mingwX64` |
| `aarch64-apple-darwin` | `macosArm64` |
| `x86_64-unknown-linux-*` | `linuxX64` |
| `aarch64-unknown-linux-*` | `linuxArm64` |

Any other triple causes a `GradleException` at configuration time.

## Definition File Configuration

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

## Per-Platform Native Target Configuration

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

## Generated `.def` File Format

The `GenerateDefinitionFiles` task writes a `.def` file with the following format:

```
headers = <projectName>.h
staticLibraries = lib<projectName>.a
compilerOpts = -I<cinteropDir>/<projectName>/include
libraryPaths = <cinteropDir>/<projectName>/target/<rustTarget>/release
```

When multiple Rust targets are configured, all their release directories are listed in
`libraryPaths` separated by spaces.

## Generated `build.rs`

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
