# Publishing Locally

<link-summary>What kreateLocalPublish installs, and what it records.</link-summary>

<card-summary>publishToMavenLocal at a snapshot version, plus a record consumers read.</card-summary>

```bash
cd libraries/arc && ./gradlew kreateLocalPublish
```

```
Published arc:3.0.0-SNAPSHOT to /home/dev/.m2/repository.
    com.davils:arc
    com.davils:arc-android
    com.davils:arc-jvm
    com.davils:arc-wasm-js

Other checkouts will now resolve these instead of the released versions.
Undo with ./gradlew kreateLocalClean. Recorded in /home/dev/.gradle/davils/local/com.davils.arc.properties.
```

## What it does

1. Runs `publishToMavenLocal` in every project of the build.
2. Verifies every coordinate really landed in the local Maven repository.
3. Writes a record naming the coordinates and the version.

The publishing itself is Gradle's, not %product%'s. What %product% adds is the version suffix, the
verification, and the record — the last of which is what makes a *different* checkout resolve the
result.

## The version

The version comes from wherever it normally comes from — the configured environment variable, then
the configured project property — and then carries `-SNAPSHOT`:

| `arc.version` | `./gradlew build` | `./gradlew kreateLocalPublish` |
|---------------|-------------------|--------------------------------|
| `3.0.0`       | `3.0.0`           | `3.0.0-SNAPSHOT`               |

The suffix is applied while the build is configured, not by the task, because `maven-publish` reads
the version to fix the coordinates before any task runs.

A build that resolves its version to nothing usable refuses rather than publishing
`unspecified-SNAPSHOT`, which would install and resolve perfectly well and mean nothing.

## Multiplatform and multi-module

Both are handled without being configured. The record is written from the publications the build
actually declares, so a multiplatform library contributes one coordinate per target and a
multi-module library contributes one per module — including its BOM.

`kreateLocalPublish` is registered on the **root project only**. A local publish is an act on a
whole repository, and one record per module would be both racy to write and impossible to remove as
a unit.

## Requesting it another way

`-Pdavils.local.publish=true` requests the same thing without naming the task. This is what
`kreateLocalPublishAll` passes to each repository it drives, and what to use from a script.

## Publishing %product% itself

%product% cannot apply itself — `build-logic` compiles the conventions that build the plugin — so it
carries a hand-written copy of the task:

```bash
./gradlew :kreate-plugin:kreateLocalPublish
```

That installs `com.davils:kreate`, the `com.davils.kreate` plugin marker and the
`com.davils.kreate.settings` marker, all at a snapshot version. A repository that then pins
`kreate = "%version%-SNAPSHOT"` in its catalog compiles its conventions against the working copy.

<seealso>
    <category ref="local">
        <a href="Local-Development-Workspace.md">Publishing a whole workspace</a>
        <a href="Local-Development-Safety.md">Safety rails</a>
    </category>
    <category ref="project">
        <a href="Project-Version-Resolution.md">Version resolution</a>
        <a href="Publishing-Overview.md">Publishing</a>
    </category>
</seealso>
