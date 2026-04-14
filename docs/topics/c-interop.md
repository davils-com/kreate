# C-Interop with Rust

Kreate provides a dedicated DSL for setting up C-Interop between Kotlin Multiplatform and Rust (using Cargo). This allows you to write performance-critical logic in Rust and call it from your Kotlin/Native code.

## Configuration

The `cInterop` block within `multiplatform` defines your Rust project settings and compilation targets.

```kotlin
kreate {
    platform {
        multiplatform {
            cInterop {
                enabled = true
                nameOverride = "example" // The name of the C-Interop library
                projectDirectory = file("cinterop/rust-project") // Path to your Rust project
                packageNameOverride.set("com.example.cinterop") // Kotlin package for generated bindings
                rustTargets = listOf("x86_64-unknown-linux-gnu", "aarch64-apple-darwin") // Rust compilation targets
                
                defFile {
                    fileName = "cinterop.def" // Your C-Interop definition file
                    dirName = "defs" // Directory containing the .def file
                }
            }
        }
    }
}
```

## How it works

When `cInterop` is enabled, Kreate automatically creates the following Gradle tasks:

1. **`cargoBuild`**: Compiles your Rust code for each specified target.
2. **`generateCInteropDefinition`**: Generates or prepares the C-Interop definition file (`.def`).
3. **`cinterop<Name><Target>`**: Standard Kotlin/Native C-Interop tasks are configured to depend on the compiled Rust libraries.

## Target-specific Configuration

You can provide target-specific settings for Windows (MinGW), Linux, and macOS.

```kotlin
kreate {
    platform {
        multiplatform {
            cInterop {
                linux {
                    // Linux-specific configuration
                }
                macos {
                    // macOS-specific configuration
                }
                mingw {
                    // Windows-specific configuration
                }
            }
        }
    }
}
```

## Directory Structure

A common layout for a project with Rust C-Interop:

```text
.
├── build.gradle.kts
├── src/
│   └── nativeMain/kotlin/      # Native Kotlin code
├── cinterop/
│   ├── rust-project/           # Rust source code
│   │   ├── Cargo.toml
│   │   └── src/lib.rs          # Rust functions exposed via C API
│   └── defs/                   # C-Interop definition files
│       └── cinterop.def
```

## Example Rust Code

To expose a Rust function to Kotlin, use the `#[no_mangle]` attribute and `extern "C"`:

```rust
// src/lib.rs
#[no_mangle]
pub extern "C" fn add(a: i32, b: i32) -> i32 {
    a + b
}
```

## Example Definition File

```text
# defs/cinterop.def
headers = example.h
package = com.example.cinterop
```

> **Tip**: Ensure your Rust project generates a static or dynamic library that is compatible with your target platforms.
