# Compiler options

<link-summary>Options of the Kotlin compiler set on every compilation.</link-summary>

<card-summary>Progressive mode, extra warnings and pinned language versions in one place.</card-summary>

`platform { compiler { } }` sets options on every Kotlin compilation of the project, in JVM and in
multiplatform projects. An option that is left unset keeps the value of the Kotlin Gradle plugin or
of the build.

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

kreate {
    platform {
        compiler {
            progressiveMode = true
            extraWarnings = true
            languageVersion = KotlinVersion.KOTLIN_2_4
            apiVersion = KotlinVersion.KOTLIN_2_4
            jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        }
    }
}
```

| Property          | Default | Meaning                                                          |
|-------------------|---------|------------------------------------------------------------------|
| `progressiveMode` | `false` | Fixes deprecations and bugs right away                           |
| `extraWarnings`   | `false` | Reports the additional warnings of the compiler                  |
| `languageVersion` | unset   | The Kotlin language version                                      |
| `apiVersion`      | unset   | The version of the standard library API the sources may use      |
| `jvmDefault`      | unset   | How JVM compilations emit default methods of interfaces          |

Together with [warnings as errors](Platform-Warning-As-Errors.md), the extra warnings fail the build.

<seealso>
    <category ref="platform">
        <a href="Platform-Overview.md">Platform configuration</a>
        <a href="Platform-Java-Modules.md">Java modules</a>
    </category>
</seealso>
