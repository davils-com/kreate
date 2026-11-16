# Where Kreate is published

<link-summary>The repository Kreate is resolved from, and how to move a build to it.</link-summary>

<card-summary>Kreate lives on repo.davils.com from 4.2.0 on.</card-summary>

<tldr>
<p><b>Releases</b>: <code>https://repo.davils.com/releases</code>, public, signed</p>
<p><b>Up to 4.1.0</b>: Maven Central</p>
<p><b>Declare it twice</b>: for plugins and for dependencies</p>
</tldr>

From version 4.2.0 on, %product% is published to `https://repo.davils.com/releases`. Versions up
to 4.1.0 stay on Maven Central, and no newer version is published there.

## Declaring the repository

A build needs the repository in two places, both in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        exclusiveContent {
            forRepository { maven("https://repo.davils.com/releases") }
            filter { includeGroupAndSubgroups("com.davils") }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { maven("https://repo.davils.com/releases") }
            filter { includeGroupAndSubgroups("com.davils") }
        }
        mavenCentral()
    }
}
```

`pluginManagement` resolves the plugin and its marker. `dependencyResolutionManagement` is the
half that is easy to miss: when [Detekt is enabled](Detekt-Rules.md), %product% adds
`com.davils:kreate-detekt-rules` in its own version to the project, and that artifact is resolved
like any other dependency.

`exclusiveContent` keeps the two worlds apart: everything in the group `com.davils` comes from
this repository and from nowhere else, and nothing else is ever asked of it.

> A build that keeps its conventions in an included build, such as <code>build-logic</code>,
> declares the repository in the <code>settings.gradle.kts</code> of that build as well. That is
> where the %product% plugin is resolved when a convention plugin applies it.
>
{style="warning"}

## Moving a build from 4.1.0

<procedure title="Upgrade to 4.2.0" id="upgrade">
    <step>Add the repository to both blocks of every <code>settings.gradle.kts</code>, as above.</step>
    <step>Raise the %product% version.</step>
    <step>
        <p>
            If the build restricts repositories through the assurance gate, allow the host:
        </p>
        <code-block lang="kotlin">
            kreate {
                assurance {
                    dependencyPolicy {
                        allowedRepositoryHosts = setOf("repo.maven.apache.org", "repo.davils.com")
                    }
                }
            }
        </code-block>
    </step>
    <step>
        <p>
            If the build uses dependency verification or locking, record the new artifacts:
        </p>
        <code-block lang="bash">
            ./gradlew --write-verification-metadata pgp,sha256 --export-keys help
        </code-block>
    </step>
</procedure>

## Verifying what you resolve

Every release is signed, and each JAR carries a build provenance attestation and an SBOM
attestation made by the GitHub Actions run that built it:

```bash
gh attestation verify kreate-%version%.jar --repo davils-com/kreate --signer-repo davils-com/kotlin-workflow
```

The signer is the reusable pipeline of
[kotlin-workflow](https://github.com/davils-com/kotlin-workflow) that builds every release from
4.2.0 on. A release up to 4.1.0 was built by a workflow of Kreate itself and is verified without
`--signer-repo`.

## Snapshots

Every push to the development branch publishes `<next version>-SNAPSHOT` to
`https://repo.davils.com/snapshots`. That repository is private and meant for the builds of
Davils; a snapshot is unsigned and carries no attestation.

<seealso>
    <category ref="reference">
        <a href="Getting-Started.md">Getting started</a>
        <a href="Compatibility.md">Compatibility</a>
        <a href="Local-Development-Setup.md">Local development setup</a>
    </category>
</seealso>
