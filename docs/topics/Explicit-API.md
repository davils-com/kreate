# Explicit API Mode

The `explicitApi` property enables Kotlin's
[Explicit API mode](https://kotlinlang.org/docs/whatsnew14.html#explicit-api-mode-for-library-authors),
which enforces visibility modifiers and return type declarations on all public API members.

## Default

`explicitApi` is **enabled by default** (`true`) in Kreate.

## Configuration

```kotlin
kreate {
    platform {
        explicitApi = true  // default
    }
}
```

To disable it for application modules or internal projects:

```kotlin
kreate {
    platform {
        explicitApi = false
    }
}
```

## What It Enforces

When enabled, the Kotlin compiler will emit errors for any public declaration that is missing:

- An explicit **visibility modifier** (`public`, `internal`, `private`)
- An explicit **return type** on functions and properties

### Example

```kotlin
// ❌ Fails with explicitApi = true
fun greet() = "Hello"

// ✅ Correct
public fun greet(): String = "Hello"
```

<tip>
Explicit API mode is strongly recommended for any module that is published as a library,
as it prevents accidental exposure of internal implementation details.
</tip>

<note>
For application modules or integration test modules where a public API surface is not a concern,
set <code>explicitApi = false</code> to reduce boilerplate.
</note>