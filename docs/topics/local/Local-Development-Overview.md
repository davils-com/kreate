# Local Development

<link-summary>Test a change in one repository from another, without tagging a release.</link-summary>

<card-summary>Publish locally, build the consumer, done. No file in either repository changes.</card-summary>

A fix in a library can only be tried in the library above it once it exists somewhere both builds
can resolve. Until %product% 3.2.0 that meant tagging a version and waiting for a pipeline to push
it to a registry — which made the cost of testing a one line change the same as the cost of
shipping one, and produced a registry full of versions that existed only to be thrown away.

The local development workflow closes that loop:

```bash
cd libraries/arc && ./gradlew kreateLocalPublish
cd ../leaf       && ./gradlew build
```

The second build resolves the `arc` that was just built. Nothing in either repository is edited —
not the version catalog, not `gradle.properties`, not a lock file.

## The two halves

The feature is two plugins, and a repository needs both.

| Plugin                       | Applied to                                       | Does                                                  |
|------------------------------|--------------------------------------------------|-------------------------------------------------------|
| `com.davils.kreate`          | projects                                         | publishes, and registers the tasks that manage it     |
| `com.davils.kreate.settings` | `settings.gradle.kts`, `build-logic/settings.gradle.kts` | resolves what was published                   |

Resolution needs a settings plugin for two reasons. Dependency substitution has to be installed
before anything resolves, and it has to reach `build-logic` — which is where the %product% plugin
marker itself is resolved, and which never applies the project plugin. A project plugin can do
neither.

## Nothing happens until you publish

There is no switch to turn on. Applying the plugins changes nothing about how a build resolves; a
`kreateLocalPublish` does, and a `kreateLocalClean` undoes it. That is deliberate — an opt-in flag
is a thing to forget, and forgetting it looks exactly like the feature not working.

Each publish is announced, so a build that resolves something other than its catalog says explains
itself:

```
Kreate local mode is ON — 1 library resolved from /home/dev/.m2/repository:
    arc            3.0.0-SNAPSHOT       4 min ago      4 module(s)  /workspace/libraries/arc
  Dependency locking is off. No lock file is read or written.
  Switch off with -Pdavils.local=false; clear with ./gradlew kreateLocalClean.
```

## What makes it safe

- **Snapshot versions.** A local build of `3.0.0` is installed as `3.0.0-SNAPSHOT`, and the
  injected repository is declared `snapshotsOnly()`. A local build can never be served in place of
  a release of the same version.
- **Exact coordinates.** Only what a publish actually installed is substituted, and the injected
  repository is filtered to those modules. A sibling artefact in the same group still resolves the
  way it always did.
- **Never in CI.** The state lives under `GRADLE_USER_HOME`, which every pipeline recreates per
  job. A runner that somehow carries it fails rather than resolving from it.
- **Never published onward.** A build resolving local artefacts refuses to push its output to a
  shared registry.
- **Never written to a lock file.** `--write-locks` in local mode is refused outright.

See [](Local-Development-Safety.md) for each of those in full.

## Tasks

| Task                     | Does                                                             |
|--------------------------|------------------------------------------------------------------|
| `kreateLocalPublish`     | installs this repository locally and records it                  |
| `kreateLocalPublishAll`  | runs the above across a declared workspace, in dependency order   |
| `kreateLocalStatus`      | reports what is published, or why local mode is off              |
| `kreateLocalClean`       | removes the publications and the records                         |

<seealso>
    <category ref="local">
        <a href="Local-Development-Setup.md">Setup</a>
        <a href="Local-Development-Publishing.md">Publishing</a>
        <a href="Local-Development-Workspace.md">Workspaces</a>
        <a href="Local-Development-Safety.md">Safety rails</a>
        <a href="Local-Development-Troubleshooting.md">Troubleshooting</a>
    </category>
</seealso>
