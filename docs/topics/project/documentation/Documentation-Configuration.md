# Configuration Reference

## Properties

| Property          | Type                | Default                         | Description                                              |
|-------------------|---------------------|---------------------------------|----------------------------------------------------------|
| `enabled`         | `Property<Boolean>` | `false`                         | Master switch — applies and configures Dokka when `true` |
| `moduleName`      | `Property<String>`  | *(not set)*                     | The module name shown in the generated documentation     |
| `copyright`       | `Property<String>`  | *(not set)*                     | Footer message rendered on every page of the HTML output |
| `outputDirectory` | `Property<String>`  | *(Dokka default: `dokka/html`)* | Output path relative to the Gradle `build/` directory    |

All properties are optional except `enabled`. When a property is not set, Kreate skips
that part of the Dokka configuration and leaves the Dokka default in place.

## `moduleName`

Sets `DokkaExtension.moduleName`. This value appears as the top-level title in the
generated HTML site and in the module list when multiple modules are aggregated.

```kotlin
docs {
    enabled.set(true)
    moduleName.set("MyLibrary")
}
```

## `copyright`

Sets the `footerMessage` of Dokka's HTML plugin parameters. The string is rendered as
plain HTML in the footer of every generated page, so HTML tags are supported.

```kotlin
docs {
    enabled.set(true)
    copyright.set("© 2026 Davils. Licensed under Apache 2.0.")
}
```

If `copyright` is not present, the footer configuration block is skipped entirely and
Dokka's default (empty footer) is used.

## `outputDirectory`

Redirects the `html` Dokka publication output to a custom subdirectory inside `build/`.
The path is relative to the build directory.

```kotlin
docs {
    enabled.set(true)
    outputDirectory.set("docs/api")
}
```

The HTML site is then written to `build/docs/api/` instead of the default `build/dokka/html/`.

> Only the `html` publication output is redirected. If you use other Dokka publication
> formats, configure them manually via the standard Dokka DSL after Kreate has applied the
> plugin.
>
{style="note"}