# Dependency locking

<link-summary>Pinning resolved versions so builds are reproducible and scans have something to read.</link-summary>

<card-summary>One task that resolves every locked classpath, so `--write-locks` records all of them.</card-summary>

<tldr>
<p><b>Enable</b>: <code>project { dependencyLocking { enabled = true } }</code></p>
<p><b>Task</b>: <code>kreateResolveAndLockAll</code></p>
<p><b>Write the locks</b>: <code>./gradlew kreateResolveAndLockAll --write-locks</code></p>
</tldr>

Without locking, a build that resolves `1.2.+` — or any dependency that itself does —
produces different artifacts on different days. Gradle's dependency locking pins the
resolved versions in a `gradle.lockfile` you commit, and fails the build when resolution
drifts away from it.

It is also what the [Trivy scans](Trivy-Overview.md) read. Without a lock file they have no
version list to check, and they report success without having checked anything.

## Quick start

Locking is **disabled by default**:

```kotlin
kreate {
    project {
        dependencyLocking {
            enabled = true
        }
    }
}
```

```bash
./gradlew kreateResolveAndLockAll --write-locks
```

Commit the resulting `gradle.lockfile`.

<note>
The classpaths of every enabled <a href="Testing-Suites.md">test suite</a> are locked as well —
<code>unitTestCompileClasspath</code>, <code>integrationTestRuntimeClasspath</code> and so on, or
their per-target equivalents on a multiplatform project. An integration suite is usually the only
place a project declares Testcontainers and a database driver; without this, the lock file would be
missing exactly the dependencies that reach outside the process while still looking complete to a
reader and to Trivy.

Regenerate the lock files after adding a suite or a suite dependency.
</note>

## Why there is a task for this

`--write-locks` records only the configurations a build actually resolves. Run it against
an arbitrary task and you get a lock file that covers whatever that task happened to need —
one that looks complete and is not. `kreateResolveAndLockAll` resolves every locked
classpath in a single invocation, so the file is whole.

What it resolves is the dependency graph, not the artifacts. A lock file records module
versions, and those are settled the moment the graph resolves; picking a file for each of
them is a second step that locking never looks at. That distinction matters on a classpath
where artifact selection is ambiguous — an Android classpath consuming another project in
the same build is the common case, because the Android library plugin publishes several
artifact variants under one set of attributes. The versions are never in question there,
and the task no longer pretends they are.

The task refuses to run without `--write-locks`, because resolving classpaths for no reason
is the only thing it would otherwise do:

```
kreateResolveAndLockAll only makes sense with --write-locks:
./gradlew kreateResolveAndLockAll --write-locks
```

> This is the one Kreate task that is not compatible with the configuration cache, and
> declares so. Resolving a configuration is by definition an execution-time interaction
> with the project model. The cache entry is discarded for that invocation only; every
> other task, including the ones in the same build, is unaffected.
>
{style="note"}

## Properties

| Property                | Type                  | Default                                | Purpose                                  |
|-------------------------|-----------------------|----------------------------------------|------------------------------------------|
| `enabled`               | `Property<Boolean>`   | `false`                                | Activates locking and registers the task |
| `lockedClasspaths`      | `SetProperty<String>` | `compileClasspath`, `runtimeClasspath` | The configurations that are locked       |
| `lockAllConfigurations` | `Property<Boolean>`   | `false`                                | Locks every configuration instead        |

## Why not lock everything

`lockAllConfigurations` exists, and defaulting it to `true` would be a mistake.

Locking every configuration also locks the build tools: the Kotlin compiler classpath,
Dokka's generator, Detekt's rule set plugins. A vulnerability scan over that lock file then
reports CVEs in a documentation tool's XML parser as though they were vulnerabilities in
your published artifact. In the measurement behind this default, 2 of 103 locked entries
were dependencies the project actually shipped; the other 101 were noise that a reviewer
has to triage on every scan.

Lock what you ship. Add a configuration to `lockedClasspaths` when you have a reason to.

## Kotlin Multiplatform

A multiplatform project has no `compileClasspath` and no `runtimeClasspath`. A classpath is named
after the target it belongs to - `jvmRuntimeClasspath`, `androidCompileClasspath`,
`wasmJsCompileClasspath`, `metadataCompileClasspath`.

Kreate derives those names from the targets you declared and locks them **in addition** to
whatever `lockedClasspaths` names, so a multiplatform project needs no configuration of its own:

```kotlin
kreate {
    project {
        dependencyLocking {
            enabled = true
        }
    }
}
```

<warning>
Before this was derived, the default matched no configuration on a multiplatform project. Nothing
was locked, the build stayed green, and any <code>gradle.lockfile</code> already in the repository
was never rewritten - it still named <code>compileClasspath</code> and still looked like a lock
file, to a reader and to Trivy. If you are converting a project to multiplatform, delete the file
and regenerate it, and check that the classpaths in it are the ones your targets are named after.
</warning>

## What drift looks like

A lock is applied as a strict constraint, so declaring a *lower* version than the locked one
does not fail — Gradle raises it back. What fails is resolving a module the lock file does
not know about:

```
Resolved 'org.apache.commons:commons-io:1.3.2' which is not part of the dependency lock state
```

Re-run `kreateResolveAndLockAll --write-locks` and commit the updated file.

## Local development

While the [local development workflow](Local-Development-Overview.md) is active, locking is
deactivated and `--write-locks` is refused. A lock file records released versions and cannot
contain a machine-local snapshot, so enforcing it against one would be enforcing a constraint that
can never be satisfied — and recording one would produce a file that passes on the machine that
wrote it and breaks everywhere else.

The committed lock files are never read or written in that mode. Once the local publications are
cleared, locking behaves exactly as it did before 3.2.0.

See [](Local-Development-Locking.md).
