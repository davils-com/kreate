# Documentation Overview

The `docs { }` block inside `kreate { project { } }` integrates
[Dokka](https://kotl.in/dokka) into your Gradle module. When enabled, Kreate applies the
Dokka Gradle plugin automatically and configures it with your declared settings — no manual
`plugins { }` block or `dokka { }` wiring needed.

Documentation generation is **disabled by default**. Enable it with:

```kotlin
kreate {
    project {
        docs {
            enabled.set(true)
        }
    }
}
```

> The `docs { }` block is evaluated inside `afterEvaluate`. All properties must be set
> before the configuration phase ends.
>
{style="note"}

## How It Works

When `enabled` is `true`, Kreate:

1. Applies the `DokkaPlugin` to the Gradle project
2. Configures `moduleName` on the Dokka extension if set
3. Redirects the `html` publication output to `outputDirectory` if set
4. Sets the `footerMessage` of the Dokka HTML plugin to `copyright` if set

Dokka's `dokkaGenerate` task produces an HTML site from your KDoc comments. Run it with:

```bash
./gradlew dokkaGenerate
```

The output lands in `build/dokka/html/` by default, or in the path you configure via
`outputDirectory`.