# License header

<link-summary>Verifying that every file starts with the license notice.</link-summary>

<card-summary>One check for sources, build scripts and configuration files.</card-summary>

Detekt only reads Kotlin sources. `kreateLicenseHeaderCheck` also covers build scripts, Java sources
such as module descriptors, and the property and YAML files of the build.

## Configuration

```kotlin
kreate {
    project {
        licenseHeader {
            enabled = true
            apacheLicense("Copyright 2026 Example")
        }
    }
}
```

| Property      | Default                                                                 | Meaning                                                        |
|---------------|-------------------------------------------------------------------------|----------------------------------------------------------------|
| `enabled`     | `false`                                                                 | Registers `kreateLicenseHeaderCheck`                           |
| `notice`      | none, required                                                          | The notice without comment markers                             |
| `includes`    | `**/*.kt`, `**/*.kts`, `**/*.java`, `**/*.properties`, `**/*.yaml`, `**/*.yml` | Ant patterns of the checked files, relative to the project |
| `excludes`    | `build`, `.gradle`, `.kotlin`, `.idea`, `.git`, `gradle/wrapper`, `gradle/gradle-daemon-jvm.properties` | Ant patterns that are never checked |
| `runOnCheck`  | `true`                                                                  | Whether `check` runs the check                                 |

`apacheLicense(copyright)` sets `notice` to the copyright line followed by the notice of the Apache
License 2.0. Any other notice is set as text:

```kotlin
licenseHeader {
    enabled = true
    notice = """
        Copyright 2026 Example
        All rights reserved.
    """.trimIndent()
    excludes.add(".port/**")
}
```

## Comment styles

Kotlin, Kotlin script and Java files start with a block comment, property, YAML, TOML and shell
files with `#` lines:

```kotlin
/*
 * Copyright 2026 Example
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 ...
 */
```

A byte order mark and Windows line breaks are accepted. Anything before the header, even an empty
line, fails the check.

## Output

The check names every file without the header and writes the verdict of every checked file to
`build/kreate/license/headers.txt`. Applied to the root project, it checks the whole repository
except the excluded directories.

<seealso>
    <category ref="project">
        <a href="Detekt-Overview.md">Detekt</a>
        <a href="Task-Reference.md">Task reference</a>
    </category>
</seealso>
