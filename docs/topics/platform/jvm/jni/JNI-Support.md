# Support

Kreate provides first-class support for integrating native C/C++ code into JVM projects via the
Java Native Interface (JNI). The entire build pipeline — from project scaffolding to CMake compilation
and runtime library resolution — is automated through a set of dedicated Gradle tasks.

JNI support was introduced in Kreate **1.1.0** and lives inside the `platform.jvm.jni` DSL block.

## How It Works

When JNI is enabled, Kreate registers and wires up the following automatically:

1. **`initializeJniProject`** — Scaffolds the native C++ project structure if it does not exist yet
2. **`buildNative`** — Configures and builds the shared library via CMake (`cmake -S . -B build` → `cmake --build`)
3. **Compile hook** — `buildNative` runs before every Kotlin compilation task so the native library is always up to date
4. **Runtime path** — All `Test` and `JavaExec` tasks receive `-Djava.library.path` pointing to the CMake build output directory

<note>
CMake 3.20 or later must be installed and available on your system.
On macOS, Kreate searches common installation paths (Homebrew, CMake.app) automatically
because Gradle does not inherit the interactive shell <code>PATH</code>.
</note>

## Prerequisites

- **CMake** 3.20 or later
- A **C++17** capable compiler (GCC, Clang, or MSVC)
- JDK with JNI headers (included in any standard JDK installation)

## Project Layout

A JNI-enabled module follows this directory convention:

```text
my-module/
├── build.gradle.kts
├── src/                        # Kotlin sources
└── jni/                        # JNI root (default, configurable)
    └── <projectName>/
        ├── CMakeLists.txt      # Generated on first run if missing
        └── src/
            └── <projectName>.cpp   # Placeholder generated on first run
```

The layout mirrors the C-Interop convention used for Rust projects,
keeping native source structures consistent across both features.

## Next Steps

- **[](JNI-Configuration.md)**: DSL reference and all available options
- **[](JNI-Scaffolding.md)**: How the `initializeJniProject` task works
- **[](JNI-Build-Pipeline.md)**: CMake build and runtime library path wiring
