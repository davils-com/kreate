# Overview

The Build Constants feature generates a Kotlin `object` at compile time that exposes
key-value pairs as `const val` string properties. This allows you to embed information
like the project version, name, or any custom value directly into your compiled code —
without runtime lookups or manual maintenance.

Build constants generation is **disabled by default**. Enable it inside the
`buildConstant { }` block within `kreate { project { } }`:

```kotlin
kreate {
    project {
        buildConstant {
            enabled.set(true)
        }
    }
}
```

> The `buildConstant { }` block is evaluated in `afterEvaluate`. All constants must be
> registered before the configuration phase ends.
>
{style="note"}

## How It Works

When enabled, Kreate registers a `kreate-build-constants` Gradle task that runs
automatically before compilation. The task uses [KotlinPoet](https://square.github.io/kotlinpoet/)
to write a `.kt` source file into the build directory and wires it into the project's
source sets so it is compiled alongside your regular code.

The generated file is placed at:

```build/generated/compile/<packagePath>/<ClassName>.kt```

It is added automatically to `commonMain` for Kotlin Multiplatform projects, or to
`main` for Kotlin JVM projects.

## Generated Output

Given the following configuration:

```kotlin
kreate {
    project {
        name = "MyLibrary"
        projectGroup = "com.example"

        buildConstant {
            enabled.set(true)
            constant("version", project.version)
            constant("name", "MyLibrary")
        }
    }
}
```

Kreate generates a file at `build/generated/compile/com/example/mylibrary/build/BuildConstants.kt`:

```kotlin
// This file is generated automatically.
// Do not edit or modify! Changes will be overwritten on the next build.
// Generated on 2026-04-25 10:00:00.

package com.example.mylibrary.build

/**
 * Auto-generated build constants.
 */
object BuildConstants {
    const val VERSION: String = "1.0.0"
    const val NAME: String = "MyLibrary"
}
```

You can then import and use it anywhere in your code:

```kotlin
import com.example.mylibrary.build.BuildConstants

println(BuildConstants.VERSION)
println(BuildConstants.NAME)
```

> Constant keys are automatically converted to `UPPER_SNAKE_CASE` in the generated class.
>
{style="tip"}

## Next Steps

- **[](Constans-Configuration.md)**: Customize package names, class names, and target source sets
- **[](Constants-Examples.md)**: Advanced usage, including environment variables and dynamic values