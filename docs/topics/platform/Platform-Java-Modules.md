# Java modules

<link-summary>Module names, module descriptors and strict javac.</link-summary>

<card-summary>Compile a module-info.java that exports Kotlin packages.</card-summary>

```kotlin
kreate {
    platform {
        jvm {
            moduleName = "com.example.library"
            javaWarningsAsErrors = true
        }
    }
}
```

## Module name

When `src/main/java/module-info.java` exists, Kreate hands javac the classes the Kotlin compiler wrote
with `--patch-module <moduleName>=<classes>`. Without it, every `exports` of the descriptor fails,
because javac sees the Kotlin packages on the class path only.

On a Kotlin Multiplatform project the descriptor of the JVM target lives in
`src/jvmMain/java/module-info.java`, javac runs as `compileJvmMainJava` and the jar is `jvmJar`. The
module name and the patch work the same way.

Without a descriptor, `moduleName` becomes the `Automatic-Module-Name` of the jar. A name that is
no Java module name fails the configuration.

## Strict javac

`javaWarningsAsErrors = true` compiles every Java source with `--release` of the
[platform Java version](Platform-Java-Version.md), `-Xlint:all` and `-Werror`. Defaults to `false`.

<seealso>
    <category ref="platform">
        <a href="Platform-Compiler-Options.md">Compiler options</a>
        <a href="Project-Archives.md">Main jar</a>
    </category>
</seealso>
