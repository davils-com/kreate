# Examples

## Minimal Setup

Apply Dokka with no customization. All Dokka defaults apply.

```kotlin
kreate {
    project {
        name = "MyLibrary"
        projectGroup = group.toString()

        docs {
            enabled.set(true)
        }
    }
}
```

Generated docs are available at `build/dokka/html/` after running:

```bash
./gradlew dokkaGenerate
```

## Module Name and Copyright

```kotlin
kreate {
    project {
        name = "MyLibrary"
        projectGroup = group.toString()

        docs {
            enabled.set(true)
            moduleName.set("MyLibrary")
            copyright.set("© 2026 Davils. All rights reserved.")
        }
    }
}
```

The copyright string appears in the footer on every page of the HTML output.

## Custom Output Directory

Redirect the generated HTML to a dedicated folder for easier publishing:

```kotlin
kreate {
    project {
        docs {
            enabled.set(true)
            outputDirectory.set("docs/api")
        }
    }
}
```

Output is written to `build/docs/api/`. Useful when the `build/` directory is served
directly by a documentation hosting pipeline.

## Full Configuration

All available options combined:

```kotlin
kreate {
    project {
        name = "MyLibrary"
        description = "A high-performance Kotlin/Native library."
        projectGroup = group.toString()

        docs {
            enabled.set(true)
            moduleName.set("MyLibrary")
            copyright.set("© 2026 Davils. Licensed under Apache 2.0.")
            outputDirectory.set("docs/api")
        }
    }
}
```