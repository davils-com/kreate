# Warnings as Errors

The `allWarningsAsErrors` property instructs the Kotlin compiler to treat every compiler warning
as a hard error, causing the build to fail if any warnings are present.

## Default

`allWarningsAsErrors` is **disabled by default** (`false`) in Kreate.

## Configuration

```kotlin
kreate {
    platform {
        allWarningsAsErrors = true
    }
}
```

## Behavior

When enabled, any Kotlin compiler warning — including deprecation notices, unused variable warnings,
and unchecked cast warnings — will cause the compilation task to fail with an error.

This is enforced by passing `-Werror` to the Kotlin compiler options for all compilation tasks
in the module.

### Example Output

```
e: file.kt:12:5: 'foo' is deprecated. Use 'bar' instead.
```

<tip>
Enabling <code>allWarningsAsErrors</code> in CI environments keeps the codebase clean and prevents
deprecated API usage from silently accumulating over time. Consider enabling it at least on your
main branch pipeline.
</tip>

<note>
If you are integrating a third-party library that triggers warnings outside your control,
you can suppress individual warnings with <code>@Suppress("WARNING_NAME")</code> rather than disabling this flag globally.
</note>