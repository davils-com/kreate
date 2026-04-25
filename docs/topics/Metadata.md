# Project Metadata

The `project { }` block inside `kreate { }` is the central place to declare the identity of your
Gradle module. Kreate reads these values and applies them to the Gradle project at evaluation time,
making them available to other subsystems like publishing, documentation, and build constants.

```kotlin
kreate {
    project {
        name = "Example"
        description = "Example project"
        projectGroup = group.toString()
    }
}
```

> The `project { }` block is evaluated inside `afterEvaluate`, so all three properties must be set
> before the configuration phase ends.
>
{style="note"}

## Properties

### `name`

**Type:** `Property<String>`  
**Required:** yes

Sets the display name of the project. Other Kreate subsystems — such as publishing and build
constants generation — use this value to identify the artifact and generate derived names.

```kotlin
project {
    name = "MyLibrary"
}
```

### `description`

**Type:** `Property<String>`  
**Default:** `"A Kreate project."`

A human-readable description of the project. Kreate writes this value directly to
`project.description` during the `afterEvaluate` phase, so it appears in generated POM files and
documentation outputs.

```kotlin
project {
    description = "A high-performance Kotlin/Native library."
}
```

### `projectGroup`

**Type:** `Property<String>`  
**Required:** yes

The Maven group identifier (e.g. `com.example`). It is conventional to derive this from the
Gradle `group` property so both stay in sync:

```kotlin
project {
    projectGroup = group.toString()
}
```

You can also set it to a literal string if the group differs from what Gradle infers:

```kotlin
project {
    projectGroup = "com.davils.mylib"
}
```

> Kreate uses `projectGroup` together with `name` to construct the default C-Interop package name
> (`<projectGroup>.<name>.cinterop`) and publishing coordinates. Keep both consistent with your
> `settings.gradle.kts` and `gradle.properties`.
>
{style="tip"}
