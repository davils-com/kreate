# Publishing overview

<link-summary>Publishing artifacts to Maven Central, GitLab and your own Maven repository.</link-summary>

<card-summary>Repositories, signing, and POM metadata from one block.</card-summary>

<tldr>
<p><b>Enable</b>: <code>project { publish { enabled = true } }</code></p>
<p><b>Applies</b>: <code>maven-publish</code>, and <code>com.vanniktech.maven.publish</code> for Maven Central</p>
</tldr>

The `publish { }` block inside `kreate { project { } }` integrates artifact publishing into your
Gradle build. Kreate supports three publish targets out of the box:

- **Maven Central** via the [Gradle Maven Publish Plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/central/) by Vanniktech
- **GitLab Package Registry** via the standard Gradle `maven-publish` plugin with CI job token authentication
- **A Maven repository** with separate release and snapshot URLs, such as Reposilite, Nexus or Artifactory

Publishing is **disabled by default**. Enabling it is all it takes: Kreate applies Gradle's
`maven-publish` plugin, and the [Vanniktech Maven Publish Plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/)
as well when Maven Central is one of the targets.

Apply either yourself to pin a version or to reach its own DSL block; Kreate's own application is
then a no-op.

### Kreate Configuration

Enable the publishing module:

```kotlin
kreate {
    project {
        publish {
            enabled = true
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
is only configured when its own `enabled` property is also `true`. The targets are
completely independent and can be active simultaneously.

```
publish { enabled = true }
│
├── repositories.mavenCentral { enabled = true }
│ └── applies com.vanniktech.maven.publish
│ └── calls publishToMavenCentral(automaticRelease)
│ └── sets coordinates(group, name, version)
│ └── configures pom { ... }
│
├── repositories.gitlab { enabled = true }
│ └── applies maven-publish
│ └── registers a `maven` publication unless the project declares one
│ └── reads CI_JOB_TOKEN, CI_PROJECT_ID, CI_API_V4_URL from env
│ └── registers Maven repository with HttpHeaderAuthentication
│ └── configures pom { ... } on all MavenPublication tasks
│
├── repositories.maven { enabled = true }
│ └── applies maven-publish
│ └── registers a `maven` publication unless the project declares one
│ └── picks the releases or the snapshots URL by the version
│ └── reads the user name and the password from env
│ └── registers Maven repository with basic authentication
│ └── configures pom { ... } on all MavenPublication tasks
│
└── signing
└── signs every publication once, if a target that is enabled sets signPublications = true
```

The GitLab and the Maven target create a publication only when the project has none — the
Maven Central plugin brings its own. With several targets enabled they therefore share one
publication rather than uploading the same coordinates twice.

## Top-Level Properties

| Property        | Type                | Default     | Description                                                        |
|-----------------|---------------------|-------------|--------------------------------------------------------------------|
| `enabled`       | `Property<Boolean>` | `false`     | Master switch — must be `true` for any publishing to be configured |
| `inceptionYear` | `Property<Int>`     | `2024`      | Written into the POM `<inceptionYear>` field                       |
| `website`       | `Property<String>`  | *(not set)* | Written into the POM `<url>` field                                 |

All POM metadata is shared between the publish targets. Configure it once in `pom { }` and
Kreate applies it to all of them.


<seealso>
    <category ref="project">
        <a href="Publishing-Maven-Central.md">Maven Central</a>
        <a href="Publishing-Gitlab-Registry.md">GitLab Package Registry</a>
        <a href="Publishing-Maven-Repository.md">Maven repository</a>
        <a href="Publishing-POM-Configuration.md">POM configuration</a>
        <a href="Publishing-Examples.md">Examples</a>
    </category>
</seealso>
