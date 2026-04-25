# Gradle Task Reference

All tasks live in the default Gradle task group and run in strict dependency order. They are
registered only when `enabled` is `true`.

## `initializeRustProject`

**Class:** `InitializeRustProject`

Runs `cargo new --lib <projectName>` inside `projectDirectory`. If the target directory already
exists, the task is a no-op — it will not overwrite your existing Rust source files.

| Input | Description |
|-------|-------------|
| `workDir` | The resolved `projectDirectory` |
| `projectName` | The resolved project name |

| Output | Description |
|--------|-------------|
| `outputDir` | `<workDir>/<projectName>/` |

## `addRustDependencies`

**Class:** `AddRustDependencies`

Adds runtime and build dependencies to `Cargo.toml` via `cargo add`. Defaults to adding `libc` as
a runtime dependency and `cbindgen` as a build dependency. Already-present dependencies are
skipped automatically by checking the current `Cargo.toml` content before invoking `cargo add`.

| Input | Description |
|-------|-------------|
| `workDir` | The Rust project directory |
| `rustDependencies` | Map of `name → version`; defaults to `{"libc": ""}` |
| `rustBuildDependencies` | Map of `name → version`; defaults to `{"cbindgen": ""}` |

> The `rustDependencies` and `rustBuildDependencies` maps are currently populated by the plugin
> internally. Direct user configuration of these maps is not yet exposed in the DSL.
>
{style="note"}

## `configureCargo`

**Class:** `ConfigureCargo`

Appends the following block to `Cargo.toml` if not already present:

```toml
[lib]
crate-type = ["staticlib"]
```

This ensures Cargo produces a static library (`.a` / `.lib`) suitable for linking into
Kotlin/Native. If the block is already present, the task is a no-op.

| Input | Description |
|-------|-------------|
| `workDir` | The Rust project directory containing `Cargo.toml` |

| Output | Description |
|--------|-------------|
| `outputFile` | `<workDir>/Cargo.toml` |

## `generateRustBuildScript`

**Class:** `GenerateRustBuildScript`

Creates `build.rs` in the Rust project root. The script uses `cbindgen` to generate a C header
at `include/<projectName>.h`. If `build.rs` already exists and is non-empty, the task is skipped.

| Input | Description |
|-------|-------------|
| `workDir` | The Rust project directory |
| `projectName` | Used as the header file name inside the generated script |

| Output | Description |
|--------|-------------|
| `outputFile` | `<workDir>/build.rs` |

## `compileRust`

**Class:** `CompileRust`

Executes `cargo build --target <target> --release` for each resolved Rust target in sequence.
If compilation fails for any target, a `GradleException` is thrown immediately with the failing
target name.

| Input | Description |
|-------|-------------|
| `workDir` | The Rust project directory |
| `rustTargets` | Optional; resolved from config or auto-detected from host OS/arch |

| Output | Description |
|--------|-------------|
| `outputDir` | `<workDir>/target/` |

## `generateDefinitionFiles`

**Class:** `GenerateDefinitionFiles`

Creates the directory specified by `defDirName` and writes the `.def` file consumed by
`CInteropProcess`. The file references the compiled static library and the generated C header.
This task **always rewrites** the `.def` file — it is not incrementally skipped.

| Input | Description |
|-------|-------------|
| `workDir` | The Rust project directory |
| `rootDir` | The `projectDirectory` (parent of the Rust project) |
| `projectName` | Used in header and library names |
| `defFileName` | File name for the `.def` file |
| `defDirName` | Directory name for the `.def` file |
| `rustTargets` | Optional; used to construct the `libraryPaths` entries |

| Output | Description |
|--------|-------------|
| `outputDir` | `<workDir>/<defDirName>/` |

## Task Dependency Graph

```
initializeRustProject
        │
        ▼
addRustDependencies
        │
        ▼
  configureCargo
        │
        ▼
generateRustBuildScript
        │
        ▼
   compileRust
        │
        ▼
generateDefinitionFiles
        │
        ▼
 CInteropProcess (all)
```
