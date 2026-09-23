# Safety Rails

<link-summary>What the feature refuses to do, and why each refusal is a failure rather than a warning.</link-summary>

<card-summary>Six refusals. A local artifact must never reach CI, a registry, or a lock file.</card-summary>

A feature that changes dependency resolution behind the scenes has one way to be genuinely
dangerous: producing something shippable from artifacts that exist on one machine. Every rail here
exists for that, and every one of them is a build failure rather than a warning — a warning during a
long build is a line nobody reads, and by the time anyone would, the damage is committed.

## Local artifacts never reach CI

The state that switches local mode on lives at `$GRADLE_USER_HOME/kreate/local`. A pipeline that
points `GRADLE_USER_HOME` inside its own workspace — the usual arrangement, so that the dependency
cache is scoped to the job — recreates that directory empty every time, so it **cannot** carry
local state into a build. That is a structural guarantee rather than a check that can be
forgotten.

A runner that somehow carries it anyway fails:

```
Kreate found local development state while running in CI.

    State directory: /builds/acme/http/.gradle/kreate/local
    Detected by:     CI_PIPELINE_ID
    Libraries:       core, net

A CI build must never resolve from the local Maven repository: the artefacts there exist on
one machine only, so anything built against them cannot be reproduced and must not be released.
```

Add a guard job as well, cheap and explicit:

```yaml
local-state:
  stage: security
  needs: []
  script:
    - test ! -d "$GRADLE_USER_HOME/kreate/local"
```

## Local artifacts are never published onward

A build resolving local artifacts refuses to push its output to a shared registry:

```
Refusing to publish to a remote repository while resolving from the local Maven repository.

This build resolves: core:3.0.0-SNAPSHOT
```

Only remote targets are blocked. `publishToMavenLocal` — the publication local mode exists to
produce — is a different task type and is unaffected.

## Lock files are neither read nor written

This is the rail worth the most. A lock file pinning `3.0.0-SNAPSHOT` looks entirely plausible in
review, passes on the machine that wrote it, and breaks every pipeline and every colleague.

```
Refusing to write lock files while resolving from the local Maven repository.

These versions would have been recorded into a committed lock file:

com.example:core:3.0.0-SNAPSHOT
com.example:core-jvm:3.0.0-SNAPSHOT

They exist on this machine only, so the lock file would break every pipeline and every
other checkout.
```

Locking is also deactivated while local mode is on — a lock file records released versions and
cannot contain a machine-local snapshot, so enforcing it against one would be enforcing a
constraint that can never be satisfied.

## A release is never shadowed

Two independent mechanisms, and either alone would do:

- A local publication always carries `-SNAPSHOT`, so it is a different coordinate from the release
  of the same version.
- The injected repository is declared `mavenContent { snapshotsOnly() }`, so it cannot serve a
  release whatever ends up in the directory.

`kreateLocalPublish` refuses outright if the version does not carry the suffix.

## Only what was published is substituted

Substitution matches exact `group:name` pairs recorded by a publish, never a group wildcard, and
the injected repository is filtered with `includeModule` to the same set. A sibling artifact in the
same group that was not published locally resolves exactly as it always did, and no request for an
unrelated artifact is ever sent to the local repository.

## A missing artifact is named

If a recorded coordinate is not actually installed — most often because a `kreateLocalClean` ran in
another shell — the build says so and names the fix, rather than failing much later with a "could
not find" naming a version nobody typed.

## Clean refuses to go wide

`kreateLocalClean` deletes exactly the coordinates the records name, at the recorded versions. A
release sitting in the same repository was put there by something else and is never touched. It
also refuses to run in CI.

<seealso>
    <category ref="local">
        <a href="Local-Development-Overview.md">Overview</a>
        <a href="Local-Development-Locking.md">Dependency locking</a>
    </category>
    <category ref="reference">
        <a href="CI-Integration.md">CI integration</a>
    </category>
</seealso>
