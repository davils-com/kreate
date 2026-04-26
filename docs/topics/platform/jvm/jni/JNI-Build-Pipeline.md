# Build Pipeline

When JNI is enabled, Kreate wires a fully automated native build pipeline into your Gradle build.
This page describes the individual tasks, their execution order, and how the compiled library
is made available at runtime.

## Task Graph

```
compileKotlin
    └── kreate-jni-build
            └── kreate-jni-initialize
```

`kreate-jni-build` is hooked into the Kotlin compile pipeline via `executeTaskBeforeCompile`,
ensuring the shared library is always rebuilt before Kotlin compilation begins.

## `kreate-jni-initialize`

Scaffolds the native C++ project if it does not exist. See
[](JNI-Scaffolding.md) for the full details on what is generated.

| Property  | Value                                            |
|-----------|--------------------------------------------------|
| Task type | `InitializeCppProject`                           |
| Input     | `projectDirectory` (root JNI dir), `projectName` |
| Output    | `<projectDirectory>/<projectName>/`              |
| Runs      | Before `kreate-jni-build`                         |

## `kreate-jni-build`

Invokes CMake in two steps to configure and build the shared library.

**Step 1 — Configure:**

```bash
cmake -S . -B <projectDir>/build -DCMAKE_BUILD_TYPE=Release
```

**Step 2 — Build:**

```bash
cmake --build <projectDir>/build --config Release
```

The build type defaults to `Release`. The compiled shared library (`.so` / `.dylib` / `.dll`)
is placed in `<projectDirectory>/<projectName>/build/`.

| Property           | Value                                       |
|--------------------|---------------------------------------------|
| Task type          | `BuildNative`                               |
| Input              | `workDir` (native project dir), `buildType` |
| Output             | `<workDir>/build/`                          |
| Default build type | `Release`                                   |
| Runs               | Before every `compileKotlin` task           |

<note>
On macOS, Kreate resolves the CMake executable by searching common installation paths
(<code>/opt/homebrew/bin/cmake</code>, <code>/usr/local/bin/cmake</code>,
<code>/Applications/CMake.app/Contents/bin/cmake</code>)
because Gradle does not inherit the interactive shell PATH.
On Linux and Windows, <code>cmake</code> is resolved via the system <code>PATH</code>.
</note>

## Runtime Library Path

After the native build, Kreate automatically configures all `Test` and `JavaExec` tasks
with the correct `java.library.path` so that `System.loadLibrary()` resolves the shared library
without any manual setup.

The following JVM argument is injected:

```
-Djava.library.path=<projectDirectory>/<projectName>/build
```

Both task types also gain an explicit `dependsOn("kreate-jni-build")` dependency,
ensuring the library is compiled before any test or run task executes.

### Loading the Library in Kotlin

```kotlin
object NativeLib {
    init {
        System.loadLibrary("example") // matches CMake target name
    }

    external fun hello(): String
}
```

<tip>
The library name passed to <code>System.loadLibrary()</code> must match the CMake
<code>add_library</code> target name, which equals the resolved <code>projectName</code> (or <code>nameOverride</code> if set).
</tip>