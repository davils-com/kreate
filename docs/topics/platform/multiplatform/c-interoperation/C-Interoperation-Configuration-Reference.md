# Configuration Reference

The `cInterop { }` block exposes the `CInteropExtension`, which controls all aspects of the
pipeline.

## Top-Level Properties

| Property              | Type                   | Default                   | Description                                                                            |
|-----------------------|------------------------|---------------------------|----------------------------------------------------------------------------------------|
| `enabled`             | `Property<Boolean>`    | `false`                   | Master switch — must be `true` for any C-Interop tasks to run                          |
| `language`            | `Property<NativeLanguage>` | `NativeLanguage.RUST` | Selects the native language/pipeline: `RUST` (Cargo + cbindgen), `C` or `CPP` (CMake)  |
| `nameOverride`        | `Property<String>`     | *(project name)*          | Overrides the name used for the native project directory and C-Interop compilation unit |
| `projectDirectory`    | `DirectoryProperty`    | `cinterop/`               | Directory (relative to the Gradle module) where the Rust project is created            |
| `packageNameOverride` | `Property<String>`     | `<group>.<name>.cinterop` | Overrides the Kotlin package name under which the native bindings are exposed          |
| `rustTargets`         | `ListProperty<String>` | *(auto-detected)*         | Explicit list of Rust target triples to compile for; if empty the host OS/arch is used |

## Target Auto-Detection

When `rustTargets` is not set, Kreate detects the current host and selects a single target
automatically:

| Host OS | Host Architecture | Resolved Target             |
|---------|-------------------|-----------------------------|
| Windows | x86_64            | `x86_64-pc-windows-gnu`     |
| Linux   | x86_64            | `x86_64-unknown-linux-gnu`  |
| Linux   | aarch64           | `aarch64-unknown-linux-gnu` |
| macOS   | aarch64           | `aarch64-apple-darwin`      |

For cross-compilation or multi-target releases, set `rustTargets` explicitly.

## Rust Target → Kotlin/Native Target Mapping

Each Rust target triple is mapped to a Kotlin/Native target automatically:

| Rust Target Triple                | Kotlin/Native Target |
|-----------------------------------|----------------------|
| `x86_64-pc-windows-*` / `*mingw*` | `mingwX64`           |
| `aarch64-apple-darwin`            | `macosArm64`         |
| `x86_64-unknown-linux-*`          | `linuxX64`           |
| `aarch64-unknown-linux-*`         | `linuxArm64`         |

Any other triple causes a `GradleException` at configuration time.

## Native Language (`language`)

The `language` property selects which native toolchain backs the interop. It accepts a
`NativeLanguage` value:

```kotlin
import com.davils.kreate.module.platform.multiplatform.cinterop.NativeLanguage

cInterop {
    enabled.set(true)
    language.set(NativeLanguage.CPP) // RUST (default), C, or CPP
}
```

- `NativeLanguage.RUST` *(default)* — runs the full Cargo/`cbindgen` pipeline and honors
  `rustTargets`.
- `NativeLanguage.C` / `NativeLanguage.CPP` — scaffold and build a CMake project, producing a
  static library `lib<projectName>.a` in the project's `build/` directory. The public API is
  declared in a hand-written `include/<projectName>.h` header (the C++ scaffold wraps it in an
  `extern "C"` boundary). These modes require **CMake 3.20+** and a C/C++ compiler.

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

| Property   | Type               | Default        | Description                                                              |
|------------|--------------------|----------------|--------------------------------------------------------------------------|
| `fileName` | `Property<String>` | `cinterop.def` | Name of the generated `.def` file                                        |
| `dirName`  | `Property<String>` | `defs`         | Directory name (inside the Rust project) where the `.def` file is placed |

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

For `NativeLanguage.C` and `NativeLanguage.CPP`, `libraryPaths` instead points to the single
CMake build directory:

```
headers = <projectName>.h
staticLibraries = lib<projectName>.a
compilerOpts = -I<cinteropDir>/<projectName>/include
libraryPaths = <cinteropDir>/<projectName>/build
```

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
