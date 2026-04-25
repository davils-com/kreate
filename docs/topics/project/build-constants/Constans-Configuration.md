# Configuration Reference

## Top-Level Properties

| Property              | Type                | Default             | Description                                                  |
|-----------------------|---------------------|---------------------|--------------------------------------------------------------|
| `enabled`             | `Property<Boolean>` | `false`             | Master switch — must be `true` for the task to be registered |
| `className`           | `Property<String>`  | `BuildConstants`    | Name of the generated Kotlin `object`                        |
| `path`                | `Property<String>`  | `generated/compile` | Output path relative to the Gradle `build/` directory        |
| `packageNameOverride` | `Property<String>`  | *(auto-derived)*    | Overrides the package of the generated file                  |

## Package Name Resolution

The package name for the generated file is derived automatically from the Gradle project
group and project name:

```<projectGroup>.<projectName>.build```

Both components are lowercased and spaces are removed. For example, a project with
`group = "com.example"` and `name = "MyLibrary"` produces:

```com.example.mylibrary.build```

Override the package entirely with `packageNameOverride`:

```kotlin
buildConstant {
    enabled.set(true)
    packageNameOverride.set("com.example.internal")
}
```

When `packageNameOverride` is set, `.build` is appended to the value automatically:

```com.example.internal.build```


## Registering Constants

Use the `constant(key, value)` function to register each constant. The key must not
be blank, otherwise an `IllegalArgumentException` is thrown immediately.

```kotlin
buildConstant {
    enabled.set(true)
    constant("version", project.version) // Any value via toString()
    constant("buildTime", System.currentTimeMillis())
    constant("name", "MyLibrary")
}
```

Both overloads are available:

| Overload                               | Description                                         |
|----------------------------------------|-----------------------------------------------------|
| `constant(key: String, value: String)` | Registers the value as-is                           |
| `constant(key: String, value: Any)`    | Calls `.toString()` on the value before registering |

> If no constants are registered and `enabled` is `true`, the task runs but logs a
> warning: `No properties found for build constants.`
>
{style="warning"}

## Class Name and Output Path

Customize the generated class name and its location inside the `build/` directory:

```kotlin
buildConstant {
    enabled.set(true)
    className.set("AppConfig")
    path.set("generated/sources")
    constant("version", project.version)
}
```

The file is then written to:
```build/generated/sources/<packagePath>/AppConfig.kt```


## Explicit API Mode

The generated class automatically respects the `explicitApi` setting from the platform
extension. When explicit API mode is active, the generated `object` and all its `const val`
properties receive a `public` modifier:

```kotlin
// with explicitApi enabled
public object BuildConstants {
    public const val VERSION: String = "1.0.0"
}
```

No additional configuration is required — this is wired automatically from
`platform { explicitApi }`.