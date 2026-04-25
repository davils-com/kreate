# Examples

## Minimal Configuration

The simplest possible setup: C-Interop is enabled, and Kreate auto-detects everything else.

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

## Custom Name and Directory

Override the Rust project name and the directory where it is created:

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

## Multi-Target Cross-Compilation

Compile for all four supported native targets at once:

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

All four targets are compiled in sequence and their release directories are all listed in the
generated `.def` file's `libraryPaths`.

> Make sure to add each required Rust target beforehand:
> ```bash
> rustup target add x86_64-unknown-linux-gnu
> rustup target add aarch64-unknown-linux-gnu
> rustup target add aarch64-apple-darwin
> rustup target add x86_64-pc-windows-gnu
> ```
{style="note"}

## Custom Def File Location

Change where the Kotlin/Native definition file is written:

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

## Full Configuration

All available options combined:

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
