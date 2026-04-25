# Troubleshooting

## `cargo` not found

Ensure the Rust toolchain is installed and `cargo` is available on your `PATH`. On macOS, Kreate
looks at `~/.cargo/bin/cargo`; if you installed Rust through a custom path, verify that location
exists or ensure `cargo` is on your `PATH`.

```bash
# Verify cargo is accessible
cargo --version
```

## Unsupported Rust Target

If a target triple in `rustTargets` does not match any of the supported patterns, Kreate throws:

```
GradleException: Unsupported Rust target for Kotlin/Native mapping: <target>
```

Supported patterns and their Kotlin/Native equivalents:

| Pattern                           | Kotlin/Native Target |
|-----------------------------------|----------------------|
| `x86_64-pc-windows-*` / `*mingw*` | `mingwX64`           |
| `aarch64-apple-darwin`            | `macosArm64`         |
| `x86_64-unknown-linux-*`          | `linuxX64`           |
| `aarch64-unknown-linux-*`         | `linuxArm64`         |

If you need a target not listed here, it is not yet supported by the Kreate C-Interop pipeline.

## Missing Cross-Compilation Target

If `cargo build` fails with a message like:

```
error: toolchain '...' does not contain component 'rust-std' for target '...'
```

The required Rust target has not been installed. Add it with:

```bash
rustup target add <target>
```

For example, to add the Linux x86_64 target:

```bash
rustup target add x86_64-unknown-linux-gnu
```

## Failed to Create Root Directory

If the `projectDirectory` cannot be created (e.g. due to filesystem permissions), Kreate throws:

```
GradleException: Failed to create root directory: <path>
```

Ensure the parent directory is writable by the process running the Gradle daemon. Check
filesystem permissions and that you are not building inside a read-only location.

## Failed to Create C-Interop Directory or Definition File

If the `defs/` directory or the `.def` file cannot be created, Kreate throws:

```
GradleException: Failed to create cinterop directory: <path>
GradleException: Failed to create cinterop definition file: <path>
```

Verify that the Rust project directory is writable and that no conflicting file or directory
already exists at those paths.

## Definition File Not Updated After Config Changes

The `generateDefinitionFiles` task always rewrites the `.def` file on execution. However,
Gradle's incremental build cache may prevent the task from re-running if its declared inputs have
not changed. If you change `rustTargets`, `defFileName`, `dirName`, or directory settings and the
`.def` file appears stale, run a clean build:

```bash
./gradlew clean build
```

## Compilation Fails for a Specific Target

If `compileRust` fails with a `GradleException` for a specific target, the full Cargo error
output is printed to the Gradle build log. Run with `--info` or `--debug` for more detail:

```bash
./gradlew compileRust --info
```

Common causes are missing target toolchains, incorrect `Cargo.toml` configuration, or Rust
source code compilation errors.
