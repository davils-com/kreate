# Version Resolution

The project version is **not** set directly inside `project { }`. Instead, Kreate resolves it
automatically from an environment variable or a Gradle property, configured via the nested
`version { }` block:

```kotlin
kreate {
    project {
        name = "Example"
        description = "Example project"
        projectGroup = group.toString()
        
        version {
            environment = "VERSION"   // default — checked first
            property = "version"      // default — fallback
        }
    }
}
```

| Source                  | Key                      | Priority       |
|-------------------------|--------------------------|----------------|
| Environment variable    | `VERSION` (configurable) | 1st            |
| Gradle project property | `version` (configurable) | 2nd (fallback) |

This design makes the version injectable from CI/CD pipelines without modifying build files.