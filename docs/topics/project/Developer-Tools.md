# Developer tools

## Git pre-commit hook

```kotlin
kreate {
    project {
        gitHooks { enabled = true }
    }
}
```

`kreateGitHookInstall` writes a pre-commit hook that runs `./gradlew --quiet kreateGitHookRun`. The
script never changes, so nothing from the build reaches a shell. `kreateGitHookRun` runs the fast checks
the project registers: Detekt, the API check, the changelog check, the dependency policy, the confusion
check and the configuration lint; `tasks` names others. The hook goes where Git looks for it, so
`core.hooksPath` and worktrees work. A hook Kreate did not write is never replaced, and
`kreateGitHookUninstall` removes only its own. Skip it once with `git commit --no-verify`.

## Doctor

`./gradlew kreateDoctor` checks the machine: the Java and Gradle versions, Git, the pinned Trivy, the
cache, and whether OSV.dev, deps.dev, Maven Central and the Trivy releases are reachable. It also finds
the proxy trap: `HTTPS_PROXY` set while Gradle has no `systemProp.https.proxyHost`, or the other way
round, means dependencies and Kreate's checks take different routes. Credentials in a proxy URL are
never printed. A failed check fails the task.

## Formatting

```kotlin
kreate {
    project {
        detekt {
            enabled = true
            formatting = true
        }
    }
}
```

`formatting` adds the ktlint rules of Detekt, so `kreateDetekt` reports formatting as well, and
`kreateFormatApply` rewrites the Kotlin sources under `src` to follow them.
