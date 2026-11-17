# Version Resolution

<link-summary>How the project version is resolved, and in what order.</link-summary>

<card-summary>CI tag, then Gradle property, then a logged fallback.</card-summary>

The project version is **not** set directly inside `project { }`. Instead, Kreate resolves it
automatically from an environment variable or a Gradle property, configured via the nested
`version { }` block:

```kotlin
kreate {
    project {
        name = "Example"
        description = "Example project"
        
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

## Snapshots {id="snapshots"}

A build can be asked to carry a snapshot version. Kreate then appends `-SNAPSHOT` to the version
it resolved, unless the version already ends in it:

| Source                  | Key                        | Priority |
|-------------------------|----------------------------|----------|
| DSL                     | `version { snapshot = … }` | 1st      |
| Gradle property         | `-Pkreate.snapshot=true`   | 2nd      |
| Environment variable    | `KREATE_SNAPSHOT=true`     | 3rd      |

Without any of them a build is a release. `snapshot = false` in the DSL rules a snapshot out,
whatever the property and the variable say.

```kotlin
kreate {
    project {
        version {
            snapshot = providers.environmentVariable("CI_COMMIT_TAG").map { false }.orElse(true)
        }
    }
}
```

The block above makes every build without a tag a snapshot. `snapshotUnlessTagged` does the same for
GitHub Actions and GitLab CI, which name a release tag in `GITHUB_REF_TYPE` and `GITHUB_REF_NAME` or in
`CI_COMMIT_TAG`; a local build is always a snapshot. An explicit `snapshot` wins over it:

```kotlin
kreate {
    project {
        version {
            snapshotUnlessTagged = true
        }
    }
}
```

A
[Maven repository target](Publishing-Maven-Repository.md) publishes a snapshot to its `snapshots`
URL and everything else to `releases`, so the same task serves both.

<seealso>
    <category ref="project">
        <a href="Project-Metadata.md">Project metadata</a>
        <a href="Publishing-Overview.md">Publishing</a>
    </category>
    <category ref="reference">
        <a href="CI-Integration.md">CI integration</a>
    </category>
</seealso>
