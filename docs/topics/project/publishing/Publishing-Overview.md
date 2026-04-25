# Publishing

The `publish { }` block inside `kreate { project { } }` integrates artifact publishing into your
Gradle build. Kreate supports two publish targets out of the box:

- **Maven Central** via the [Gradle Maven Publish Plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/central/) by Vanniktech
- **GitLab Package Registry** via the standard Gradle `maven-publish` plugin with CI job token authentication

Publishing is **disabled by default**. Enable it with:

```kotlin
kreate {
    project {
        publish {
            enabled.set(true)
        }
    }
}
```

> The `publish { }` block is evaluated inside `afterEvaluate`. All properties and nested
> blocks must be configured before the Gradle configuration phase ends.
>
{style="note"}

## How It Works

When `enabled` is `true`, Kreate evaluates each repository target independently. A repository
is only configured when its own `enabled` property is also `true`. The two targets are
completely independent and can be active simultaneously.

```
publish { enabled = true }
│
├── repositories.mavenCentral { enabled = true }
│ └── applies MavenPublishBasePlugin
│ └── calls publishToMavenCentral(automaticRelease)
│ └── calls signAllPublications() if signPublications = true
│ └── sets coordinates(group, name, version)
│ └── configures pom { ... }
│
└── repositories.gitlab { enabled = true }
└── applies maven-publish plugin
└── reads CI_JOB_TOKEN, CI_PROJECT_ID, CI_API_V4_URL from env
└── registers Maven repository with HttpHeaderAuthentication
└── configures pom { ... } on all MavenPublication tasks
```

## Top-Level Properties

| Property        | Type                | Default     | Description                                                        |
|-----------------|---------------------|-------------|--------------------------------------------------------------------|
| `enabled`       | `Property<Boolean>` | `false`     | Master switch — must be `true` for any publishing to be configured |
| `inceptionYear` | `Property<Int>`     | `2024`      | Written into the POM `<inceptionYear>` field                       |
| `website`       | `Property<String>`  | *(not set)* | Written into the POM `<url>` field                                 |

All POM metadata is shared between both Maven Central and GitLab publish targets. Configure
it once in `pom { }` and Kreate applies it to both.