# Local Mode and Dependency Locking

<link-summary>Why locking gives way in local mode, and why the lock file never changes.</link-summary>

<card-summary>Locking is off while local mode is on. The committed file is never read or written.</card-summary>

Every Davils library enables dependency locking, and every module has a committed `gradle.lockfile`
pinning exact versions. A locally published snapshot is by definition not one of them, so the two
features have to be reconciled.

## The rule

While local mode is on:

- dependency locking is **deactivated** for every configuration, in projects %product% manages and
  in included builds it does not;
- `--write-locks` is **refused**;
- the committed lock files are **not modified** — before, during or after.

```
Kreate local mode: dependency locking is off for ':leaf-core'. No lock file is read or written.
```

## Why deactivate rather than relax

`--update-locks` has exactly the right selection semantics and the wrong effect: it rewrites the
committed files. `LockMode.LENIENT` still feeds lock state in as version constraints, which is a
second thing that can beat a substitution. Deactivation is the only option that is unambiguous
about what it does.

## Why `--write-locks` fails instead of warning

A lock file recording `3.0.0-SNAPSHOT` is the worst kind of mistake this feature could enable. It
looks ordinary in a diff, it passes on the machine that produced it, and it fails for everyone
else. Refusing it outright is the only response that is proportionate:

```bash
# Clear the local publications first…
./gradlew kreateLocalClean
./gradlew kreateResolveAndLockAll --write-locks

# …or write the locks with local mode switched off.
./gradlew kreateResolveAndLockAll --write-locks -Pdavils.local=false
```

## Regenerating locks after a real release

Nothing changes. Once the library is released and the catalog moved on, run
`kreateResolveAndLockAll --write-locks` as usual — with no local publications present, local mode
is off and locking behaves exactly as it did before 3.2.0.

<seealso>
    <category ref="local">
        <a href="Local-Development-Safety.md">Safety rails</a>
    </category>
    <category ref="project">
        <a href="Dependency-Locking.md">Dependency locking</a>
    </category>
</seealso>
