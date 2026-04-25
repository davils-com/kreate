# Java Version

The `javaVersion` property in the `platform` block controls the Java toolchain version used for compilation
across all modules in your project.

## Default

Kreate defaults to `JavaVersion.VERSION_25` if `javaVersion` is not explicitly set.

## Configuration

```kotlin
kreate {
    platform {
        javaVersion = JavaVersion.VERSION_21
    }
}
```

## Behavior

When `javaVersion` is configured, Kreate applies the following automatically:

- Sets the **Java toolchain** for Kotlin and Java compilation tasks
- Aligns the **source and target compatibility** across all submodules
- Ensures Gradle's toolchain resolver fetches the correct JDK if not already installed

<note>
Kreate uses Gradle's toolchain API under the hood. Your Gradle runner JDK does not need to match
<code>javaVersion</code> — Gradle will download and use the correct version automatically.
</note>

## Supported Versions

Any `JavaVersion` enum value from Gradle's API is accepted. Kreate is tested against the following:

| Value                    | Java Release      |
|--------------------------|-------------------|
| `JavaVersion.VERSION_21` | Java 21 (LTS)     |
| `JavaVersion.VERSION_25` | Java 25 (default) |

<tip>
For libraries published to Maven Central, Java 21 (LTS) is the recommended minimum target
to maximize compatibility with downstream consumers.
</tip>