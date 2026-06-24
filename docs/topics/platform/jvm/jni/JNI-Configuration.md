# Configuration

All JNI settings are nested under `platform.jvm.jni` inside the `kreate { }` block.

## Minimal Setup

```kotlin
kreate {
    platform {
        jvm {
            jni {
                enabled = true
            }
        }
    }
}
```

With only `enabled = true`, Kreate resolves all other values from the project configuration automatically.

## Full Configuration

```kotlin
kreate {
    platform {
        jvm {
            jni {
                enabled = true
                nameOverride = "mylib"
                projectDirectory = layout.projectDirectory.dir("jni")
                libraryIncludePaths = listOf("include", "libs/foo/include", "libs/bar/include")
            }
        }
    }
}
```

## DSL Reference

| Property              | Type            | Default            | Description                                                                      |
|-----------------------|-----------------|--------------------|----------------------------------------------------------------------------------|
| `enabled`             | `Boolean`       | `false`            | Enables the JNI feature and registers all related tasks                          |
| `nameOverride`        | `String?`       | _(project name)_   | Overrides the native project name used for CMake target and directory resolution |
| `projectDirectory`    | `Directory?`    | `<projectDir>/jni` | Root directory that contains the native project folder                           |
| `libraryIncludePaths` | `List<String>`  | _(empty)_          | Additional C++ library include directories passed to the compiler via CMake      |

### `enabled`

Controls whether the entire JNI feature is active. When `false` (the default),
no tasks are registered and no build pipeline changes are applied.

### `nameOverride`

By default, Kreate derives the native project name from `project.name` in `kreate { project { name = ... } }`,
falling back to the Gradle project name. The resolved name is sanitized before use.
Use `nameOverride` when you need a specific CMake target name that differs from the Kotlin project name.

```kotlin
jni {
    enabled = true
    nameOverride = "mylib"  // CMake target: mylib, dir: jni/mylib/
}
```

### `projectDirectory`

Overrides the root directory where Kreate looks for (and creates) the native project folder.
The actual native project lives at `<projectDirectory>/<projectName>/`.

```kotlin
jni {
    enabled = true
    projectDirectory = layout.projectDirectory.dir("native")
    // Native project: native/<projectName>/
}
```

### `libraryIncludePaths`

Declares additional C++ library include directories that are passed to the compiler.
This is useful when the native project depends on multiple external libraries located in
different directories. Each configured path is added to the generated `CMakeLists.txt`
via `target_include_directories`, alongside the conventional `include` directory and the
JNI headers. Paths may be absolute or relative to the native project root
(`<projectDirectory>/<projectName>/`).

```kotlin
jni {
    enabled = true
    libraryIncludePaths = listOf("include", "libs/foo/include", "libs/bar/include")
}
```

<note>
The configured include paths are only written into the <code>CMakeLists.txt</code> during scaffolding.
If the file already exists, Kreate does not modify it; adjust the include directories there manually.
</note>

<tip>
If you already have an existing CMake project, point <code>projectDirectory</code> to its parent
and set <code>nameOverride</code> to match the existing directory name.
Kreate skips scaffolding if <code>CMakeLists.txt</code> already exists.
</tip>
