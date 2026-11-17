# Release guard

<link-summary>Refusing to publish a version that does not belong to the build.</link-summary>

<card-summary>A misconfigured pipeline cannot push anything to the releases repository.</card-summary>

A [Maven repository target](Publishing-Maven-Repository.md) routes a version by its suffix alone.
The release guard makes sure that suffix tells the truth. Every task that publishes to a Maven
repository depends on `kreateReleaseGuard`.

## Configuration

```kotlin
kreate {
    project {
        version { snapshotUnlessTagged = true }

        publish {
            enabled = true
            releaseGuard {
                enabled = true
                releaseBranch = "main"
                remote = "origin"
                tagPrefix = ""
            }
        }
    }
}
```

## Rules

The guard refuses to publish when

- the resolved version differs from the version declared in the
  [version property](Project-Version-Resolution.md), for example because `VERSION` is set;
- a build without a release tag would publish anything but a `-SNAPSHOT`;
- a tag build would publish a snapshot, a version other than the one its tag names, or a version
  that is no Semantic Versioning 2.0.0 release without build metadata;
- the tagged commit is not part of `releaseBranch` on `remote`;
- a tag build runs with a snapshot of Kreate, the Kotlin Gradle plugin or Detekt.

The release tag is read from `GITHUB_REF_TYPE` and `GITHUB_REF_NAME` on GitHub Actions and from
`CI_COMMIT_TAG` on GitLab CI. On a tag build the guard runs `git merge-base --is-ancestor`, so the
pipeline has to fetch the release branch. When Git cannot answer, the guard refuses.

<seealso>
    <category ref="project">
        <a href="Publishing-Maven-Repository.md">Maven repository</a>
        <a href="Project-Version-Resolution.md">Version resolution</a>
    </category>
</seealso>
