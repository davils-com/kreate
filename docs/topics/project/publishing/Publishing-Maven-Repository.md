# Maven repository

<link-summary>Publishing to a Maven repository with separate release and snapshot URLs.</link-summary>

<card-summary>Releases and snapshots routed by version, with credentials from the environment.</card-summary>

<tldr>
<p><b>Enable</b>: <code>project { publish { repositories { maven { } } } }</code></p>
<p><b>Routing</b>: a version ending in <code>-SNAPSHOT</code> goes to <code>snapshots</code>, every other to <code>releases</code></p>
<p><b>Credentials</b>: user name and password from two environment variables</p>
</tldr>

Kreate publishes to any Maven repository that keeps releases and snapshots apart, such as
[Reposilite](https://reposilite.com), Nexus or Artifactory. One block declares both URLs, and the
version decides which one a build uploads to.

> Kreate applies the `maven-publish` plugin when publishing is enabled; your `plugins { }` block
> does not need to mention it.
>
{style="warning"}

## Enabling the target

```kotlin
kreate {
    project {
        publish {
            enabled = true

            repositories {
                maven {
                    name = "Company"
                    releases = "https://repo.example.com/releases"
                    snapshots = "https://repo.example.com/snapshots"
                    usernameEnv = "REPOSITORY_NAME"
                    passwordEnv = "REPOSITORY_SECRET"
                }
            }
        }
    }
}
```

Calling `maven { }` enables the target. The repository is named after `name`, so the block above
registers `publishAllPublicationsToCompanyRepository`.

## Configuration reference

| Property                | Type                | Default          | Description                                                  |
|-------------------------|---------------------|------------------|--------------------------------------------------------------|
| `enabled`               | `Property<Boolean>` | `false`          | Set to `true` by calling `maven { }`                         |
| `name`                  | `Property<String>`  | `Maven`          | Name of the repository and part of the publish task names    |
| `releases`              | `Property<String>`  | *(not set)*      | URL release versions are published to                        |
| `snapshots`             | `Property<String>`  | *(not set)*      | URL versions ending in `-SNAPSHOT` are published to          |
| `usernameEnv`           | `Property<String>`  | `MAVEN_USERNAME` | Environment variable that holds the user name                |
| `passwordEnv`           | `Property<String>`  | `MAVEN_PASSWORD` | Environment variable that holds the password or token        |
| `signPublications`      | `Property<Boolean>` | `false`          | Sign every publication                                       |
| `allowInsecureProtocol` | `Property<Boolean>` | `false`          | Accept a plain `http` URL                                    |

## How a version is routed

| Version          | Repository  |
|------------------|-------------|
| `1.4.0`          | `releases`  |
| `1.4.0-rc.1`     | `releases`  |
| `1.4.0-SNAPSHOT` | `snapshots` |

Kreate never falls back to the other repository. When the URL for the kind of version at hand is
not declared, the repository is not registered and Kreate says so:

```
Version 1.4.0 is a release and the Maven target declares no `releases` URL, skipping Maven publish repository
```

A target that declares neither URL fails the build, because it could never upload anything.

To publish a build as a snapshot without changing the version it declares, ask for one:

```bash
./gradlew publishAllPublicationsToCompanyRepository -Pkreate.snapshot=true
```

See [Version resolution](Project-Version-Resolution.md#snapshots) for the other ways to ask.

## How authentication works

Kreate reads the two variables at configuration time and sends them as HTTP basic authentication
with every request.

| Variables set | What happens                                                                       |
|---------------|------------------------------------------------------------------------------------|
| both          | The repository is registered with the credentials                                  |
| neither       | Kreate logs a lifecycle message and skips the *repository* registration            |
| only one      | The build fails and names the variable that is empty, never the value of the other |

```
No credentials found in REPOSITORY_NAME and REPOSITORY_SECRET, skipping Maven publish repository
```

The target is therefore safe to leave enabled in your build file: outside a pipeline there is
simply nowhere to upload to. The publication itself is still registered, so `publishToMavenLocal`
produces exactly the artifacts a pipeline would push.

> `publish` is a lifecycle task and succeeds when a repository was skipped. Let a pipeline call
> the task of the repository itself, such as `publishAllPublicationsToCompanyRepository`: it does
> not exist when the repository was skipped, so a missing secret fails the job instead of
> publishing nothing.
>
{style="warning"}

Credentials never belong in a URL. Kreate refuses a URL that carries them and does not print it.

> The values become part of the publishing configuration, so a build that reuses the
> configuration cache stores them in it. Run publishing tasks with `--no-configuration-cache` in a
> pipeline.
>
{style="note"}

## Which URLs are accepted

| URL                         | Accepted                                |
|-----------------------------|-----------------------------------------|
| `https://…`                 | yes                                     |
| `file:///…`                 | yes, and it needs no credentials        |
| `http://…`                  | only with `allowInsecureProtocol = true` |
| a URL with `user:secret@`   | no                                      |
| any other scheme            | no                                      |

A `file` URL publishes into a directory. That is the quickest way to look at what a build would
upload, and it is what Kreate's own tests do.

## What Kreate publishes

Kreate registers a publication named `maven` for a project that declares none, exactly as the
[GitLab target](Publishing-Gitlab-Registry.md#what-kreate-publishes) does:

| Project type              | Component      | Contents                                     |
|---------------------------|----------------|----------------------------------------------|
| Kotlin/JVM, Java library  | `java`         | the JAR, a sources JAR, and the POM          |
| `java-platform` (a BOM)   | `javaPlatform` | the POM with its `<dependencyManagement>`    |

The [POM metadata](Publishing-POM-Configuration.md) of `publish { pom { } }` is applied to every
publication. With Maven Central enabled as well, the publication of the Maven Central plugin is
used and none is added.

## Signing

`signPublications = true` signs every publication with the key Gradle is given in memory:

| Gradle property                 | Environment variable                            |
|---------------------------------|-------------------------------------------------|
| `signingInMemoryKey`            | `ORG_GRADLE_PROJECT_signingInMemoryKey`         |
| `signingInMemoryKeyId`          | `ORG_GRADLE_PROJECT_signingInMemoryKeyId`       |
| `signingInMemoryKeyPassword`    | `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword` |

Signatures belong to a publication, not to a repository. A project that signs for one target
therefore uploads the signatures to every target it publishes to.

## Combining targets

The Maven target, [GitLab](Publishing-Gitlab-Registry.md) and
[Maven Central](Publishing-Maven-Central.md) can be enabled together and share one publication.
Each needs a repository name of its own; Kreate fails the build when two would collide.

<seealso>
    <category ref="project">
        <a href="Publishing-Overview.md">Publishing overview</a>
        <a href="Publishing-POM-Configuration.md">POM configuration</a>
        <a href="Publishing-Examples.md">Examples</a>
        <a href="Project-Version-Resolution.md">Version resolution</a>
    </category>
    <category ref="reference">
        <a href="CI-Integration.md">CI integration</a>
    </category>
</seealso>
