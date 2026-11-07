# Changelog

Kreate checks a changelog in the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) form,
writes the section of the next release from [Conventional Commits](https://www.conventionalcommits.org/)
and extracts release notes.

```kotlin
kreate {
    project {
        changelog {
            enabled = true
            tagPrefix = "v"
        }
    }
}
```

| Property | Default | Meaning |
|---|---|---|
| `file` | `CHANGELOG.md` of the root project | The changelog |
| `tagPrefix` | none | The prefix of release tags |
| `runOnCheck` | `true` | Whether `check` runs `kreateChangelogCheck` |
| `includeDates` | `true` | Whether a generated section is headed with the release date |

## Check

`kreateChangelogCheck` accepts `## Unreleased` first, then releases as `## 1.2.3`, `## 1.2.3 - 2026-10-04`
or `## [1.2.3] - 2026-10-04`, newest first and each once. Headings inside code blocks are ignored, and
the level three headings below a release are free, so `### Breaking changes` works as well as `### Added`.
A release version of the project, without a pre-release such as `-SNAPSHOT`, must have a section that is
not empty.

## Generate

```bash
./gradlew kreateChangelogGenerate
./gradlew kreateChangelogGenerate --write
```

The section lists the commits since the last release tag: breaking changes first, then `feat` as Added,
`fix` as Fixed, `perf`, `refactor` and `revert` as Changed, and `security`, `deprecate` and `remove` in
their own sections. `docs`, `test`, `chore`, `ci`, `build` and `style` stay out. A commit and its revert
cancel out. The version follows from the commits: major after a breaking change, minor after a feature,
patch otherwise, or the project version when that is higher. `--release` names it, `--from` names
another start, and `--write` inserts the section below `Unreleased`. Without `--write` it is written to
`build/kreate/changelog/next.md`.

## Notes

```bash
./gradlew kreateChangelogNotes --release=1.2.0
```

writes the section of a release to `build/kreate/changelog/notes.md`, for the release page.

Git runs without a pager, prompts, hooks or file system monitor. A revision is checked before it is
passed on, so it cannot be read as an option.
