# Setup

<link-summary>The four lines a repository adds to take part.</link-summary>

<card-summary>Two settings files, one plugin id each. No catalog or lock file changes.</card-summary>

## Apply the settings plugin

In `settings.gradle.kts`:

```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("%plugin_id%.settings") version "%version%"
}
```

And in `build-logic/settings.gradle.kts` — this one is easy to skip and is the half that matters
most, because it is where the %product% plugin marker itself is resolved:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("%plugin_id%.settings") version "%version%"
}

dependencyResolutionManagement {
    // unchanged
}
```

That is the whole change. **No** edit to `gradle/libs.versions.toml`, **no** edit to any
`gradle.lockfile`, and **no** edit to `settings-gradle.lockfile`.

> The settings plugin is always resolved from Maven Central at a released version, never from the
> local Maven repository. It substitutes the local snapshot for the *project* plugin; it does not
> substitute itself. The two live on different class paths and never conflict.
>
{style="note"}

## The project plugin

Nothing to do, provided the **root project** applies `com.davils.kreate`. The tasks are
registered there rather than per module, because a local publish is an act on a whole repository.

If your root project applies no convention of its own, add the plugin to it:

```kotlin
// build.gradle.kts, at the root
plugins {
    id("%plugin_id%") version "%version%"
}
```

## Check it works

```bash
./gradlew kreateLocalStatus
```

```
Local Maven repository: /home/dev/.m2/repository
State directory:        /home/dev/.gradle/kreate/local

Nothing is published locally.

Publish a library with:  cd <library> && ./gradlew kreateLocalPublish
```

## Configuration

Both plugins work without configuration. The knobs exist for the cases that need them.

```kotlin
// settings.gradle.kts
kreateSettings {
    // Never resolve locally published artifacts in this repository, whatever is installed.
    enabled = false

    // The name the injected repository is reported under in resolution errors.
    repositoryName = "KreateLocal"

    // The variables whose presence means CI. Must match the project plugin's list.
    ciEnvironmentVariables = listOf("CI", "GITLAB_CI", "GITHUB_ACTIONS", "CI_PIPELINE_ID")
}
```

```kotlin
// build.gradle.kts
kreate {
    local {
        // Do not register the local development tasks in this repository.
        enabled = false

        // The suffix a local publication carries. Rarely worth changing — `snapshotsOnly()`
        // on the consumer side is keyed to it.
        snapshotSuffix = "-SNAPSHOT"
    }
}
```

## Command line

| Property                      | Effect                                                              |
|-------------------------------|---------------------------------------------------------------------|
| `-Pkreate.local=false`        | switches local mode off for one build                               |
| `-Pkreate.local=true`         | demands it, and fails if nothing is published                       |
| `-Pkreate.local.only=core,net`| narrows it to the named libraries                                    |
| `-Pkreate.local.publish=true` | requests a local publish without naming the task                    |
| `-Pkreate.local.clean.all=true`| widens `kreateLocalClean` into a full sweep                        |
| `-Pkreate.local.state.dir=…`  | relocates the state directory; for tests and unusual setups          |

`KREATE_LOCAL` works as an environment variable equivalent of `kreate.local`, for a machine that
should never take part. Put `kreate.local=false` in `~/.gradle/gradle.properties` for the same
effect.

<seealso>
    <category ref="local">
        <a href="Local-Development-Overview.md">Overview</a>
        <a href="Local-Development-Publishing.md">Publishing</a>
    </category>
</seealso>
