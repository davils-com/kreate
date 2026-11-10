# JNI configuration reference

<link-summary>
Every property in the platform.jvm.jni block, with defaults and worked examples.
</link-summary>

<card-summary>
The complete JNI DSL: naming, layout, toolchain, headers, and packaging.
</card-summary>

All JNI settings live under `platform.jvm.jni`.

## Minimal

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

Everything else is derived: the project name from your Kreate or Gradle project name, the source
directory from the `jni/` convention, the JDK from your Gradle toolchain, and headers generated
by default.

## Complete

<code-block lang="kotlin" collapsible="true" collapsed-title="Every JNI property">
<![CDATA[
kreate {
    platform {
        jvm {
            jni {
                enabled = true

                // Naming and layout
                nameOverride = "mylib"
                projectDirectory = layout.projectDirectory.dir("native")

                // Toolchain
                buildType = "Release"
                cmakeExecutable = "/opt/homebrew/bin/cmake"
                generator = "Ninja"

                // Compilation
                libraryIncludePaths = listOf("libs/foo/include", "libs/bar/include")

                // Runtime
                libraryRuntimePaths = listOf("/opt/vendor/lib")

                headers {
                    enabled = true
                    fileName = "mylib_bindings.h"
                }

                packaging {
                    enabled = true
                    generateLoader = true
                    resourcePath = "natives"
                }
            }
        }
    }
}
]]>
</code-block>

## Reference

<table>
    <tr><td>Property</td><td>Type</td><td>Default</td><td>Description</td></tr>
    <tr>
        <td><code>enabled</code></td><td><code>Boolean</code></td><td><code>false</code></td>
        <td>Enables the feature and registers its tasks.</td>
    </tr>
    <tr>
        <td><code>nameOverride</code></td><td><code>String?</code></td><td><i>project name</i></td>
        <td>The CMake target name, the directory name, and the name passed to <code>System.loadLibrary</code>.</td>
    </tr>
    <tr>
        <td><code>projectDirectory</code></td><td><code>Directory?</code></td><td><code>&lt;projectDir&gt;/jni</code></td>
        <td>The root that contains the native project folder.</td>
    </tr>
    <tr>
        <td><code>buildType</code></td><td><code>String</code></td><td><code>Release</code></td>
        <td>Passed to CMake as <code>CMAKE_BUILD_TYPE</code> and <code>--config</code>.</td>
    </tr>
    <tr>
        <td><code>cmakeExecutable</code></td><td><code>String?</code></td><td><i>resolved</i></td>
        <td>An explicit CMake path, bypassing discovery.</td>
    </tr>
    <tr>
        <td><code>generator</code></td><td><code>String?</code></td><td><i>CMake default</i></td>
        <td>Passed to <code>cmake -G</code>.</td>
    </tr>
    <tr>
        <td><code>libraryIncludePaths</code></td><td><code>List&lt;String&gt;</code></td><td><i>empty</i></td>
        <td>Additional include directories, forwarded through <code>KREATE_JNI_INCLUDE_DIRS</code>.</td>
    </tr>
    <tr>
        <td><code>externalSources</code></td><td><code>ConfigurableFileCollection</code></td><td><i>empty</i></td>
        <td>Native files outside the native project that the build reads, such as shared CMake modules,
        a library built with <code>add_subdirectory</code> or the headers on <code>libraryIncludePaths</code>.
        They are inputs of the configure and the build step, so a change to them rebuilds the library.
        Since 4.1.0.</td>
    </tr>
    <tr>
        <td><code>libraryRuntimePaths</code></td><td><code>List&lt;String&gt;</code></td><td><i>empty</i></td>
        <td>Additional directories appended to <code>java.library.path</code>.</td>
    </tr>
    <tr>
        <td><code>headers.enabled</code></td><td><code>Boolean</code></td><td><code>true</code></td>
        <td>Generates JNI declarations from compiled classes.</td>
    </tr>
    <tr>
        <td><code>headers.fileName</code></td><td><code>String?</code></td><td><code>&lt;name&gt;_jni.h</code></td>
        <td>The generated header file name.</td>
    </tr>
    <tr>
        <td><code>packaging.enabled</code></td><td><code>Boolean</code></td><td><code>false</code></td>
        <td>Adds the built library to the JAR.</td>
    </tr>
    <tr>
        <td><code>packaging.generateLoader</code></td><td><code>Boolean</code></td><td><code>true</code></td>
        <td>Generates <code>KreateNativeLoader</code> into your sources.</td>
    </tr>
    <tr>
        <td><code>packaging.resourcePath</code></td><td><code>String</code></td><td><code>natives</code></td>
        <td>The directory inside the JAR.</td>
    </tr>
</table>

## Naming

`nameOverride` controls three things at once, which is why it matters more than it looks:

- the CMake `project()` and `add_library()` target name,
- the directory under `projectDirectory`,
- the argument to `System.loadLibrary`.

When absent, the name is derived from `kreate { project { name } }`, falling back to the Gradle
project name. It is then lowercased and hyphens are replaced with underscores, because a hyphen
is not valid in a CMake target or a JNI symbol.

```kotlin
// Gradle project "My-Module" → native project "my_module"
jni {
    enabled = true
    nameOverride = "mylib"   // → jni/mylib/, add_library(mylib), System.loadLibrary("mylib")
}
```

## Include paths

Additional directories for third-party headers:

```kotlin
jni {
    enabled = true
    libraryIncludePaths = listOf(
        "libs/foo/include",
        "/usr/local/include/bar"
    )
}
```

Paths may be absolute or relative to the native project root. They are passed to CMake through
the `KREATE_JNI_INCLUDE_DIRS` cache variable, together with the generated header directory.

<note>
Your <code>CMakeLists.txt</code> must consume that variable for these paths to take effect. The
scaffolded file does:
<code-block lang="cmake">
<![CDATA[
target_include_directories(mylib PRIVATE
    ${JNI_INCLUDE_DIRS}
    include
    ${KREATE_JNI_INCLUDE_DIRS}
)
]]>
</code-block>
If you scaffolded with an older version, <code>kreateJniInitialize</code> warns when the variable
is missing rather than letting the paths disappear silently.
</note>

## Using an existing CMake project

Point `projectDirectory` at the parent of your existing project and set `nameOverride` to its
directory name:

```text
my-module/
└── native/
    └── engine/
        ├── CMakeLists.txt
        └── src/
```

```kotlin
jni {
    enabled = true
    projectDirectory = layout.projectDirectory.dir("native")
    nameOverride = "engine"
}
```

Scaffolding is skipped when `CMakeLists.txt` already exists. To benefit from generated headers,
add `${KREATE_JNI_INCLUDE_DIRS}` to your `target_include_directories`.

## Generators

```kotlin
jni {
    enabled = true
    generator = "Ninja"
}
```

<tip>
Setting this matters mainly on Windows, where it decides between the Visual Studio generator
(multi-configuration) and Ninja (single-configuration). %product% pins the output directories for
both, so the artifact ends up in the same place either way — but Ninja is usually faster.
</tip>

<seealso>
    <category ref="native">
        <a href="JNI-Support.md">JNI support</a>
        <a href="JNI-Scaffolding.md">Scaffolding</a>
        <a href="JNI-Headers.md">Header generation</a>
        <a href="JNI-Packaging.md">Packaging natives</a>
    </category>
</seealso>
