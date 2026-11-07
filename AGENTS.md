# AGENTS.md

Binding rules for every agent and every person working in this repository. Kreate is the Davils
build convention plugin, and it is built to the standard it enforces.

## Repository

| Build | Artifact | Purpose |
|---|---|---|
| `kreate-plugin` | `com.davils:kreate` | The project plugin `com.davils.kreate` |
| `kreate-settings` | `com.davils:kreate-settings` | The settings plugin `com.davils.kreate.settings`; depends on the Gradle API only |
| `kreate-detekt-rules` | `com.davils:kreate-detekt-rules` | The Kreate Detekt rule set |
| `build-logic` | - | Conventions that build the three artifacts |
| `:example` | - | A consumer project that exercises the published DSL end to end |
| `:selfcheck` | - | Puts Kreate's own dependencies through its gate; reads OSV.dev, so it stays out of `verify` |

The three artifacts are included builds. Root task names do not reach them.

## Verify

```bash
./gradlew verify
```

Runs `check` of every included build and of `:example`: compilation, Detekt with the Kreate rule
set, unit and functional tests, Kover verification and the API check. Nothing is done until it is
green. The build runs on JDK 25; `gradle/gradle-daemon-jvm.properties` pins the daemon to it. Set `KREATE_REQUIRE_CMAKE=true KREATE_REQUIRE_TRIVY=true` so missing tools fail instead of
skipping tests.

## Architecture of the plugin

- `KreatePlugin` registers the `kreate { }` extension and, after evaluation, applies every
  `KreateFeature` listed in `feature/KreateFeatures`, ordered by `FeaturePhase`.
- Each feature is a package `com.davils.kreate.<feature>`:
  - root: the public DSL only (`*Extension`, `*Spec`, enums, `*TaskNames`), documented with KDoc;
  - `task`: internal Gradle tasks extending `task.KreateTask` with a `KreateTaskGroup`;
  - `wiring`: the internal `<Feature>Feature` object and its Gradle wiring;
  - domain packages: internal logic without Gradle (`api.abi`, `benchmark.comparison`, ...).
- Shared internals: `gradle` (plugin ids, source set names, task paths, project naming), `host`
  (the machine Kreate runs on), `tooling` (external executables), `net` (the HTTP transport),
  `archive` (bounded ZIP and TAR reading), `dependency` (resolved graphs and coordinates) and `vcs`
  (Git history through `tooling`, revisions checked before they reach Git). Shared internals and
  every domain package stay free of Gradle types; `GradleFreeGuardTest` lists them and enforces
  that.
- The assurance gate: every source of findings is a `FindingCollectTask` that writes a
  `FindingDocument` and never fails on a finding; `kreateAssurance` alone decides. A check that could
  not finish writes an incomplete document, never a clean one. Each source describes its rules in
  `src/main/resources/assurance/rules/<source>.json`, which `kreateExplain` reads. The DSL of the
  sources lives in `com.davils.kreate.assurance` (`assurance { advisories { } }`, ...), their code in
  the feature packages `advisory`, `inspection`, `provenance`, `governance`, `workspace` and `policy`.
- JSON goes through the tree API of `kotlinx-serialization-json`, without the compiler plugin. HTTP
  goes through the Ktor client behind `net.HttpTransport`, and redirects and retries stay in `net`.
- Task names and DSL property names are the consumer contract and change only in a major version.

## Code rules

Enforced by Detekt with the Kreate rule set, `allRules` and warnings as errors, without a baseline.

- No comments: no `//`, no `/* */`. The license header is the only exception.
- No `else`. Early return; an exhaustive `when` over an enum or sealed type has no `else ->`.
- At most two calls per expression; name intermediate results and computed conditions.
- One top-level type per file, named after it; function files are named after their role.
- KDoc on public API only, with `@param`, `@return`, `@throws`, `@since`. Never on internal or
  private declarations. Existing `@since` values are never changed.
- No `!!`; specific exceptions instead of broad catches; configuration-time values through
  `providers`, never `System.getenv`.
- Build scripts and `build-logic` follow the same rules, although Detekt does not analyse them.

## Tests

Kotest `FunSpec` only, Kotest assertions and MockK. Tests mirror the production package.
TestKit tests live in `kreate-plugin/src/functionalTest` and use `KreateBuildFixture`.
Every change ships with its tests.

## Public API

The `api/*.api` dumps are the contract. A deliberate change is re-recorded with
`./gradlew :kreate-plugin:apiDump :kreate-settings:apiDump :kreate-detekt-rules:apiDump` and gets a
`CHANGELOG.md` entry.

## Git

Work on `develop`, release from `main`. Conventional commits with the module as scope
(`feat(plugin): ...`, `fix(rules): ...`). Commit only when asked. No agent co-author trailers.
