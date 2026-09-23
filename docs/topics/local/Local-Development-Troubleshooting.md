# Troubleshooting

<link-summary>Why a fix is not being picked up, and what to check first.</link-summary>

<card-summary>Start with kreateLocalStatus. It answers most of these by itself.</card-summary>

```bash
./gradlew kreateLocalStatus
```

It reports what is published, verifies each recorded coordinate is really installed, and — when
local mode is off — says why.

## My change is not being picked up

**Is anything published?** `kreateLocalStatus` says so if not. Publishing is what switches local
mode on; there is no separate flag.

**Did the consumer apply the settings plugin?** Check both `settings.gradle.kts` and
`build-logic/settings.gradle.kts`. The second is the one that gets forgotten, and it is the one
that decides which %product% the repository's conventions compile against.

**Did you republish after the last edit?** The banner shows an age for exactly this reason:

```
    arc            3.0.0-SNAPSHOT       2 h ago        4 module(s)  /workspace/libraries/arc
```

**Is the library name right?** In a workspace declaration the name has to match the root project
name of the library's build.

## The consumer resolves the released version instead

Check the banner appeared at all. If it did not, local mode is off — `kreateLocalStatus` names the
reason.

If it did appear but the library is not listed, the coordinate was not part of the publication.
`kreateLocalStatus` lists every coordinate a publication installed; substitution matches those
exactly and nothing else.

## Could not find `com.davils:…:3.0.0-SNAPSHOT`

The record and the artifacts disagree — usually a `kreateLocalClean` in another shell, or a manual
`rm` under `~/.m2`. Republish:

```bash
cd libraries/arc && ./gradlew kreateLocalPublish
```

If `maven.repo.local` is set for one shell and not another, the publish and the resolution are
looking at different directories. `kreateLocalStatus` prints the repository it resolved.

## Everything is stale after switching branches

```bash
./gradlew kreateLocalClean
```

Local publications survive a branch switch, because they live outside the repository. After a
rebase or a branch change, clearing and republishing is faster than working out what is current.

## A build fails only in CI

Almost always a lock file that was regenerated while local mode was on — which 3.2.0 refuses, but
a file committed before the upgrade can still carry it. Check for a `-SNAPSHOT` in any
`gradle.lockfile`, and regenerate with `-Pdavils.local=false`.

## I want this machine to never take part

```properties
# ~/.gradle/gradle.properties
davils.local=false
```

## Undo everything

```bash
./gradlew kreateLocalClean

# If the repository is inconsistent because records were removed before the artifacts:
./gradlew kreateLocalClean -Pdavils.local.clean.all=true
```

<seealso>
    <category ref="local">
        <a href="Local-Development-Overview.md">Overview</a>
        <a href="Local-Development-Setup.md">Setup</a>
        <a href="Local-Development-Safety.md">Safety rails</a>
    </category>
</seealso>
