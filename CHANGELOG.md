# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 3.4.0

### Fixed

- **Applying the settings plugin no longer pins the build's Kotlin, Dokka, Detekt and Kover
  versions.** `com.davils.kreate.settings` shipped in the same artifact as the project plugin, and
  that artifact carries the Kotlin, Dokka, Detekt, Kover, benchmark, serialization, allopen and
  publishing plugins as runtime dependencies. A settings plugin is loaded into the settings class
  loader, which is the parent of every project class loader, and Gradle loads parent first - so
  every one of those plugins came from Kreate's dependency, whatever the build declared. Found in
  Arc (ARC-66): its catalog said Kotlin 2.4.20, its `build-logic` resolved 2.4.20, and the Kotlin
  extension was loaded from `kotlin-gradle-plugin-2.4.0`. The only visible symptom was a lock file
  that pinned `kotlin-stdlib` 2.4.0 when it was rewritten.

  The settings plugin is now its own artifact, `com.davils:kreate-settings`, with nothing but the
  Gradle API on its runtime classpath - checked by the build, which fails if anything appears there.
  The plugin id is unchanged and resolves to the new artifact by itself; nothing has to change in a
  consuming build. The local development core both plugins share moved with it and is marked
  `@InternalKreateApi`, which keeps it out of the recorded binary interface.

### Changed

- **The secret scan runs as part of `check`.** `kreateTrivySecretScan` was only ever run when
  somebody called it, so `./gradlew build` could pass with a credential in the tree - found in Arc
  (ARC-67), where three test fixtures shaped like secrets went through a green build and were only
  caught when the scan was run by hand. A secret is leaked by the push, not by the merge, so a scan
  that first runs in CI runs too late. The secret scan reads files already on disk and needs no
  database, so it now hangs off `check` whenever the Trivy module is enabled. `secrets { runOnCheck =
  false }` turns it off for a build whose `check` runs somewhere Trivy is not installed.

  The license and vulnerability scans stay off `check`: both need Trivy's database, which has to be
  downloaded. The documentation claimed that the secret and license scans were already part of
  `check`; neither was, and both topics now say what actually happens.

- **The secret scan runs after the native scaffolding tasks.** `kreateJniInitialize` and
  `kreateCInteropInitialize` write into the source tree the scan reads, and with the scan on `check`
  Gradle would otherwise refuse the two in one build as an undeclared overlap. The example project's
  own secret scope used the `from` pattern described under *Fixed* below and now uses `setFrom`.

- **A failed secret scan names the files it failed on.** The message used to be "Trivy found secrets
  in source files!", which left the reader to search one summary table per scanned file for the one
  that was not empty. It now lists every file with a finding.

### Fixed

- **The secret scan no longer puts generated output in its own scope.** The default `sourceFiles`
  matched `**/*.yaml`, `**/*.properties` and `**/*.json` anywhere under the project directory, with
  nothing excluded, so `build`, `.gradle` and `.kotlin` were scanned along with the sources. The
  visible symptom was not a slow scan but a refused build: Gradle rejects a task whose declared input
  overlaps another task's output, so `./gradlew build kreateTrivySecretScan` failed with

  ```
  Task ':kreateTrivySecretScan' uses this output of task ':compileKotlinWasmJs' without declaring an
  explicit or implicit dependency.
  ```

  naming a directory under `build` and neither the scan nor any secret. Running the scan on its own
  was green, which made it look like a problem with whatever else was in the invocation. The three
  directories are now excluded from the default.

  A project that narrowed the scope itself was not protected by doing so: **`sourceFiles.from(...)`
  adds to the default rather than replacing it**, so its own `exclude("**/build/**")` applied to its
  own tree while the unfiltered default sat beside it. That is ordinary Gradle file collection
  behaviour and is left as it is; what changes is that the default no longer carries the directories
  that make it a problem, and that the property's KDoc and the secret scanning topic now say which of
  `from` and `setFrom` does what. The documented example used `from` and did not do what it claimed.

  Use `setFrom` to state the whole scope. Nothing has to change in a project that does.

- **The documentation build no longer fails on an unresolved category.** The seven local development
  topics added in 3.2.0 point their `<seealso>` block at a category `local`, and `docs/c.list` was
  never given one - it has not been touched since 2.0.0. Writerside reports an unresolved reference
  per topic, which fails the `test` job of the documentation workflow. The category is now declared,
  named after the section the topics already sit in, and `reference` and `external` move down one
  place to keep it beside `project` rather than after the external links.

## 3.3.0

One feature. The comment and KDoc part of the Kreate Kotlin standard was written down, argued about
in reviews, and enforced nowhere — which meant it was enforced unevenly, and that a script which
deleted comments in bulk had become the closest thing to a tool for it. A script that edits source
files cannot tell a sentence that should have been a name from one that records a decision nobody
else wrote down. Detekt can find both and refuses to choose.

### Added

- **A Detekt rule set, `com.davils:kreate-detekt-rules`.** Six rules, all reporting, none
  rewriting:

  - `ForbiddenLineComment` — every `//` comment, standalone or trailing. Block comments and KDoc are
    untouched, so a copyright header is unaffected.
  - `KDocOnNonPublicDeclaration` — KDoc on a declaration no consumer can see. Effective visibility,
    so a `public` member of an `internal` class and anything declared in a function body count;
    Detekt's own rules cover `private` functions and properties only, and not `internal` at all.
  - `KDocWithoutSinceTag` — a documented public declaration that does not say which version
    introduced it.
  - `SingleLineKDocWithBlockTag` — `/** A key exchanger. @since 1.0.0 */`, which does not say what
    it looks like it says: the KDoc lexer recognises a tag only at the start of a line, so no tooling
    ever sees that version.
  - `KDocClosingMarkerOnSharedLine` — a multi-line block whose closing marker shares a line with
    content.
  - `KDocSeparatedFromDeclaration` — a blank line between a block and the declaration it documents.

  **Nothing is switched on by a flag.** A project with Detekt enabled gets the rule set on its
  `detektPlugins` configuration and every rule active, because the artifact ships its own default
  configuration and Kreate runs with `buildUponDefaultConfig = true`. A rule set that has to be asked
  for is one that gets forgotten, and a forgotten one looks exactly like a codebase with no findings.

  **The rules report; they never rewrite, and none is auto-correctable.** A comment is deleted by the
  person who knows whether the sentence it holds belongs in a name, in a test, or nowhere. That
  judgement is the work.

  The artifact is published from this repository at this repository's version, in the same release,
  and Kreate pins it to the version of itself that is running — so the rules a project is analysed by
  are the ones the Kreate it applies was built with. It is a plain Detekt rule set: any build can put
  it on `detektPlugins` without applying Kreate at all.

  Switching it off is ordinary Detekt configuration (`kreate: active: false`, or per rule). To keep
  the artifact off the analysis classpath entirely, `project { detekt { kreateRules = false } }`.

  On an existing codebase the first run reports in the thousands. Record a Detekt baseline and work
  it down per module rather than per rule: the comments in one file are usually one person's
  explanation of one design, and they are worth reading together. See
  [Kreate rule set](https://davils-com.github.io/kreate/detekt-rules.html).

### Changed

- `style>ReturnCount` is configured rather than left at its default in the repository's own Detekt
  configuration: `max: 4` with `excludeGuardClauses: true`. A guard clause per precondition is the
  house style, and the default of two turns each added precondition into a nested `if`.

## 3.2.0

One feature, and it removes a cost that any codebase split across several repositories pays on
every change: a fix in one library can only be tried in another once it exists somewhere both
builds can resolve, which meant tagging a release and waiting for a pipeline to push it to a
registry. That made testing a one line change cost the same as shipping one, and filled the
registry with versions that existed only to be thrown away.

### Added

- **A local development workflow.** `./gradlew kreateLocalPublish` in a producer, an ordinary build
  in a consumer, and the consumer resolves the working copy. Neither repository is edited — not the
  version catalog, not `gradle.properties`, not a lock file.

  The feature ships as **two** plugin ids from the one artifact, and a repository needs both:

  - `com.davils.kreate` — the project plugin, unchanged in what it already did, now also
    registering `kreateLocalPublish`, `kreateLocalPublishAll`, `kreateLocalStatus` and
    `kreateLocalClean`.
  - `com.davils.kreate.settings` — new, applied in `settings.gradle.kts` **and**
    `build-logic/settings.gradle.kts`. It owns resolution only.

  A settings plugin rather than more of the project plugin, for two reasons that are not matters of
  taste. Dependency substitution has to be installed before any configuration resolves, and the
  Kotlin Multiplatform and Android plugins both resolve during their own `afterEvaluate` — a
  project plugin would work on most days, which is not a standard dependency resolution can be held
  to. And it has to reach `build-logic`, which is where the Kreate plugin marker itself is resolved
  and which never applies the project plugin.

  **Nothing is switched on by a flag.** Publishing is what activates local mode and
  `kreateLocalClean` is what ends it. An opt-in flag is a thing to forget, and forgetting it looks
  exactly like the feature not working. Every affected build prints what it is substituting, so a
  build that resolves something other than its catalog says explains itself.

  **The state lives at `$GRADLE_USER_HOME/kreate/local`, not in the repository.** A pipeline that
  points `GRADLE_USER_HOME` inside its own workspace — the usual arrangement — recreates that
  directory empty every job, so it cannot carry local state into a build. That is a structural
  guarantee rather than a check that can be forgotten. A file in the repository would instead be
  one `git add -A` away from turning one developer's local state into everyone's.

  **Lock files are neither read nor written in local mode, and that is a guarantee rather than an
  intention.** `--write-locks` is refused outright, naming the snapshot versions it would have
  recorded. A lock file pinning `3.0.0-SNAPSHOT` looks ordinary in review, passes on the machine
  that wrote it, and breaks every pipeline and every colleague; a warning in the middle of a long
  build is not a proportionate response to that.

  Publishing to a shared registry from a build that resolved local artifacts is refused for the
  same reason. Substitution matches only the exact coordinates a publish recorded, never a group
  wildcard, and the injected repository is declared `snapshotsOnly()` and filtered to those
  modules — so a release can never be shadowed and a sibling artifact that was not published
  locally resolves exactly as it always did.

  `kreateLocalPublishAll` orchestrates a whole workspace in dependency order, from a declaration in
  `kreate { local { workspace { } } }`. The edges are declared rather than derived from each
  repository's version catalog: a catalog records what a library was last *released* against, and
  during a refactor the edge that matters is usually the one not in the catalog yet.
  `--from <library>` republishes it and everything downstream, which is the normal case.

  **Not `includeBuild`.** Gradle's own answer substitutes by matching `group:name` against each
  *project* of the included build, and a multiplatform producer publishes its `-jvm`, `-android`
  and `-wasm-js` modules as variants of one project — there is no `project(":…")` to substitute
  them with. It would also rebuild the producer inside every consumer build, and need a committed
  settings edit per experiment.

### Fixed

- **`kreate-plugin` had no version.** The composite root's `gradle.properties` is the single source
  of truth for the version, and Gradle does not propagate it into an included build. Nothing
  bridged that gap, so every invocation that did not set `CI_COMMIT_TAG` or pass `-Pversion=`
  produced artifacts versioned `unspecified` — which is why the only hand-installed Kreate anyone
  ever had in `~/.m2` was whatever version they had typed out by hand. The convention now reads the
  root's file directly. Releases were never affected; they are tagged.

- The plugin JAR now carries `Implementation-Title` and `Implementation-Version`, so Kreate can
  name its own version at runtime. It records it alongside every local publication, which is what
  lets a record written by an older Kreate be recognised as such.

## 3.1.0

One feature, and it is the counterpart to the one 2.1.0 added. Binary compatibility validation made
a change to a published signature something a reviewer approves deliberately; this does the same for
a configuration schema, which is a promise to every file already written against it.

### Added

- **Configuration schema export and compatibility checking.** Making a field required, narrowing a
  type or removing an enum constant breaks every deployed document, none of those looks dangerous in
  a source diff — they look like ordinary edits — and the first anybody hears of one is a validation
  failure at a customer's boot. Nothing in a build checked it.

  `kreate { project { configurationSchema { } } }` registers three tasks:

  - `kreateConfigSchemaDump` writes each declared schema's JSON Schema export to
    `<module>/config-schema/<name>.json`. Commit it, and give the directory the CODEOWNERS
    treatment the `api/` directory has.
  - `kreateConfigSchemaCheck` compares against the checked-in export and fails on a breaking change.
    It runs as part of `check`, and it is never run in the same invocation as the dump — the two
    read and write the same files, and Gradle refuses the implicit dependency.
  - `kreateConfigValidate` runs a dry run over the repository's own configuration files and fails on
    any that would not load, reporting every one of them rather than stopping at the first.

  **Kreate depends on no configuration library.** Everything is read by reflection over the project's
  own runtime classpath, in a class loader of its own parented to the platform loader — parenting to
  Gradle's would let whatever Gradle happens to carry answer for a class the project declares, which
  is how a build quietly starts depending on the version of a library it never named. A project that
  uses no such library is unaffected, and one that does is not pinned to whichever version this
  plugin was built against.

  A schema is built by Kotlin code rather than written down as data, so there is nothing for a build
  to find by looking. A declaration is addressed by a holder class and a no-argument accessor, both
  spellings of a Kotlin property accepted and members of an `object` reached through `INSTANCE`,
  because which of the two a property compiles to is not something a build file should have to know.

  **What decides that a change is breaking is the library, not this.** A change that would stop an
  existing document loading is breaking when the schema version has not moved, because nothing will
  run to repair it, and merely a migration when it has. The failure message says so, because the
  usual fix is to raise the version and write the migration rather than to re-record the export.

  The dry run names an accessor handing back the reports rather than assembling one. A dry run has to
  resolve the sources, the prefix and the secret resolvers a real load would resolve — a required
  field an environment variable supplies is not missing, and a reference nothing can resolve is a
  failure rather than a value — so a build that assembled those would be a second place the
  declaration is written, and the two would disagree the first time either moved.

  The two facade names the reflection looks for are constants, because a Kotlin top level function is
  a static method on a class named after its file and nothing marks it. A missing facade is reported
  by name with what it belongs to, rather than as a `ClassNotFoundException` out of the middle of a
  task.

### Documentation

- A topic for the feature under Project, and an entry in the task reference for each of the three
  tasks.

## 3.0.0

A major release for two reasons: the conventional `test` source set is no longer built by default,
and Kreate now applies the plugins behind the features you enable. Both are covered below; the
short version for an existing build is:

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.4.0"
    id("com.davils.kreate") version "3.0.0"
    // Kover, Detekt, kotlinx-benchmark and the publishing plugins can come out.
}

kreate {
    project {
        tests {
            enabled = true

            // Keeps src/test running as the unit suite while you move files at your own pace.
            legacyTestSourceSet = LegacyTestPolicy.ALIAS

            suites {
                named("unitTest") {
                    dependencies {
                        // Moved off `testImplementation`, which no longer feeds the tests.
                        implementation("org.junit.jupiter:junit-jupiter:6.1.3")
                    }
                }
            }
        }
    }
}
```

### Breaking

- **The conventional `test` source set is not built when `tests { }` is enabled.** Its task is
  disabled and removed from `check`, and its source directories are cleared. A project that does
  nothing else will find its tests silently stop running, which is why
  `legacyTestSourceSet = LegacyTestPolicy.FAIL` exists — it turns that into a build error naming
  the directories to move. `ALIAS` keeps an unmoved tree running, and `KEEP` restores the previous
  behaviour entirely.
- **`testImplementation` no longer feeds the tests that `check` runs.** A suite's dependencies are
  declared on the suite, because its configurations do not exist while the build script is being
  evaluated. See the snippet above.
- **`TestsExtension` gains a `suites` container and several properties.** Additive at the DSL
  level; the binary compatibility dump records the full surface.
- **A plugin Kreate applies has no typed DSL accessor.** A top-level `detekt { }` or
  `kover { }` block in a build script that does not itself apply the plugin no longer compiles.
  Everything Kreate exposes is reachable through `kreate { }`; apply the plugin yourself to get
  its own block back.

### Added

- **Named test suites replace the conventional `test` source set.** A project's fast tests and its
  Testcontainers-backed ones have opposite requirements — the first belong on every build, the
  second have no business running before someone pushes — and one source set forces the slower
  answer onto both, while putting every integration-only dependency on the classpath of every test.
  `kreate { project { tests { } } }` now creates named suites instead: `unitTest` and
  `integrationTest` by default, each with its own source set, dependency configurations and task,
  and as many more as a project registers. `check` runs `unitTest`; `integrationTest` is opt-in and
  ordered after it.

  Every execution setting on `tests { }` is inherited by each suite and can be overridden per
  suite, alongside suite-only settings for tags, system properties, environment, JVM arguments,
  fixture sharing between suites, and — on multiplatform projects — which targets the suite covers.

  A suite's dependencies are declared on the suite rather than in the project's top-level
  `dependencies { }` block, because a suite's configurations do not exist yet while the build
  script runs. Kreate declares nothing of its own there; the one exception is an optional,
  version-configurable Kotest bundle, which is the framework the suite runs on rather than
  something it talks to.

  What happens to the existing `test` source set is a policy: `DISABLE` (the default) disables the
  task and removes it from `check`, `FAIL` errors while sources remain so a migration cannot leave
  tests silently unrun, `ALIAS` runs an unmoved tree as the unit suite so nothing has to move on
  day one, and `KEEP` leaves it alone.

  On multiplatform projects a suite gives `commonUnitTest` plus one source set and task per JVM
  target. Kreate extends the Kotlin hierarchy template to do it: the default template covers the
  `main` and `test` trees only, so `commonUnitTest` would otherwise be created, connected to
  nothing, and reported as unused while the tests in it did not run. Extending the template rather
  than adding a `dependsOn` edge is deliberate — one manual edge makes the Kotlin plugin abandon
  the whole default hierarchy, and `nativeMain` and the rest stop existing without the build
  failing. Only JVM targets can carry a suite, because the Kotlin plugin binds the test binaries of
  Native, JS and Wasm targets to the `test` compilation; naming one fails the build rather than
  producing a source directory nothing compiles.

  Coverage and dependency locking follow the suites. Suite compilations are excluded from the
  coverage denominator — the coverage engine recognises a test compilation by the name `test` and
  nothing else, so a suite's own code would otherwise be measured as production code and the number
  would stay green while describing the wrong thing. Suite classpaths are locked, so an integration
  suite's dependencies reach `gradle.lockfile` and any scan over it.

### Changed

- **Kreate applies the plugins behind the features you enable.** Until now it configured Detekt,
  Kover, kotlinx-benchmark and the two publishing plugins and refused to run unless the consumer
  had applied them, on the grounds that it kept their versions out of Kreate's release cycle. What
  that produced in practice was the same six plugins repeated in every repository, each with a
  version to keep in step, to reach features already spelled out one block below in `kreate { }`.
  Enabling a feature is the decision; applying its plugin is bookkeeping.

  A build script now needs the Kotlin plugin and Kreate, and nothing else. Kotlin stays the
  consumer's to apply deliberately: which one a project uses is the shape of the project rather
  than a Kreate feature, and its version governs the language the sources are written in.

  Applying is idempotent, so the escape hatch is unchanged in substance — declare
  `id("dev.detekt") version "..."` and that version participates in buildscript classpath
  resolution exactly as before, with Kreate's own application becoming a no-op. Doing so is also
  the way to reach a plugin's own DSL block: Gradle only generates the accessor for
  `detekt { }` when the plugin was applied in the `plugins { }` block, and Kreate applies it too
  late for that. Everything Kreate exposes stays available either way.

  Coverage aggregation applies Kover to each project it aggregates rather than reporting the ones
  that lack it. Naming a project in `aggregate { }` means its coverage is counted, and a project
  cannot contribute any without the plugin.

  kotlinx-benchmark moves from `compileOnly` to a runtime dependency of the plugin as a result. It
  is published to the Gradle Plugin Portal only, which a build resolving its buildscript classpath
  through an internal mirror has to account for.

### Documentation

- **New topic: Plugins Kreate applies.** What is applied and when, why the Kotlin plugin is the
  exception, how to keep control of a version, and the one behavioural consequence — a plugin
  Kreate applies has no typed DSL accessor in your build script, because Gradle generates those
  from the `plugins { }` block before the script runs.
- **New topic: Testing troubleshooting.** Every error the test suites can produce, its cause and
  its fix, plus the outcomes that look wrong and are not — `:test` reporting `NO-SOURCE`, a
  `SKIPPED` `jvmTest` still in the task graph, a suite task that succeeds having discovered
  nothing.
- **Rewritten: the testing topics.** The overview stated that testing was enabled by default while
  the text below said the opposite, and the multiplatform topic still told readers to write tests
  in `jvmTest/kotlin`. Both are now accurate and suite-aware, alongside substantially expanded
  topics for the suites themselves, migration, Kotest, the configuration reference and worked
  examples.
- `Testing-Multiplatfrom.md` is renamed to `Testing-Multiplatform.md`.
- Corrected in passing: `Getting-Started.md` listed `project.tests.enabled` as defaulting to
  `true`; the Trivy licence scan topic recommended `./gradlew dependencies --write-locks`, which
  records only the configurations that one invocation happens to resolve and produces a lock file
  that looks complete and is not.

## 2.3.1

### Fixed

- **`kreateResolveAndLockAll` failed on an Android classpath that consumes another project.** The
  task asked each locked configuration for its files. A lock file records module versions, which
  are settled when the dependency graph resolves; choosing an artifact for each of them is a second
  step locking never looks at. Asking for it anyway made the task fail wherever artifact selection
  is ambiguous — `androidCompileClasspath` reaching another project in the same build, because the
  Android library plugin publishes several artifact variants under one set of attributes and a
  request naming no `artifactType` cannot choose between them. The Android plugin resolves such a
  classpath through an artifact view for exactly that reason. The task now resolves the graph and
  nothing more, which is both what locking records and what every classpath can answer.

  A single-module Android project never hit this: the ambiguity needs a `project(...)` dependency
  on the Android classpath. A multi-module Kotlin Multiplatform build with an Android target hits
  it on the first run, and `verify:lockfiles` in the shared Kotlin pipeline runs that exact command
  on every pipeline.

## 2.3.0

### Fixed

- **The JVM toolchain was not pinned on Kotlin Multiplatform projects.** `platform { javaVersion }`
  set the toolchain from `initializeJvmCompiler`, which only runs under the Kotlin JVM plugin. A
  multiplatform build read the property for `sourceCompatibility` and then compiled against
  whichever JDK happened to be running Gradle, so the artifact declared one Java version and was
  built against another. Nothing reported it: the build is green on a machine whose JDK is new
  enough, and the first symptom is a consumer on the declared version hitting a `NoSuchMethodError`
  for something the newer JDK had. `Thread.getId()` against `Thread.threadId()` is the shape of it —
  deprecated on 21, absent on 17, and neither fact visible from the build that produced the class
  file. The multiplatform path now pins the toolchain exactly as the JVM path does.
- **`javaVersion` failed a multiplatform project that has no JVM or Android target.** The Java
  compatibility settings were applied with `configure<JavaPluginExtension>`, which throws when no
  Java plugin is applied — and nothing applies one for a project targeting only native or Wasm. The
  extension is now configured when it exists and skipped when it does not; the toolchain is pinned
  on the Kotlin extension either way, which every target arrangement has.
- **Dependency locking locked nothing on Kotlin Multiplatform projects.** `lockedClasspaths`
  defaults to `compileClasspath` and `runtimeClasspath`, and the multiplatform plugin registers
  neither: a classpath is named after its target, `jvmRuntimeClasspath` and `wasmJsCompileClasspath`
  and so on. Locking matched no configuration, `kreateResolveAndLockAll` resolved nothing, and any
  `gradle.lockfile` already in the repository was never rewritten — so it kept naming
  `compileClasspath` and kept looking like a lock file, to a reader and to the Trivy scan that reads
  it. The per-target names are now derived from the declared targets and locked in addition to
  whatever `lockedClasspaths` names, so a multiplatform project needs no configuration of its own. A
  derived name that turns out not to exist is harmless; locking matches by name and never matches
  it.

  **If you are converting a project to multiplatform, delete `gradle.lockfile` and regenerate it**,
  then check that the classpaths in it are the ones your targets are named after. The stale file is
  not overwritten by anything that would tell you it was wrong.
- **`check` analysed nothing on Kotlin Multiplatform projects.** Detekt's plugin points `check` at
  the aggregate `detekt` task. Under the Kotlin JVM plugin that task reads `src/main/kotlin` and
  `src/test/kotlin` and the arrangement is correct; under the multiplatform plugin every file
  belongs to a source set and the aggregate is left with no sources at all. It then succeeded
  without reading a line, which is the worst way for a quality gate to fail: static analysis stops
  happening and the build stays green. Kreate now also points `check` at Detekt's per-source-set
  tasks, one per source set, so every file is analysed exactly once. On a project that has no such
  tasks the set is empty and nothing changes.

  The per-*compilation* tasks — `detektMainJvm` and the rest — are deliberately not wired in. They
  run with type resolution and so enable rules the source set tasks cannot evaluate, which is a
  decision about your rule set rather than about where analysis runs, and they overlap: `commonMain`
  belongs to every target's main compilation. Add them yourself if you want them.
- **Detekt reported findings in generated code.** Anything a generator writes below the build
  directory still lands on a real source set — KSP puts test launchers there, and Kreate's own build
  constants generator writes a file to `commonMain`. Detekt read them and reported line length and
  trailing whitespace in code nobody wrote and nobody can fix; one KSP-generated test launcher was
  worth a hundred findings on its own. The build directory is now excluded from every Detekt task.

### Changed

- **Each Detekt task writes its reports into a directory named after itself.** The configured
  `outputLocation` decides the file name and the parent directory, and the task name is inserted
  between them, so `reports/detekt/report.md` becomes
  `reports/detekt/detektJvmMainSourceSet/report.md`.

  One shared path was accurate while there was one task to use it. A multiplatform project has a
  dozen, and pointing all of them at one file makes them overlapping task outputs: they overwrite
  each other in whatever order they happened to run, and the surviving report describes one source
  set while appearing to describe the project. Collecting reports by extension still works and CI
  archiving `**/build/reports/detekt/` still picks up all of them; what changes is that a finding
  can be traced back to the source set it was found in.

## 2.2.0

### Added

- **`gitlabPackageRegistry()` for resolving dependencies.** GitLab's Maven endpoint rejects the
  username and password pair Gradle sends by default; it wants a token in a header whose name
  depends on which kind of token it is, and the wrong combination produces a `401` that explains
  nothing. The helper assembles the working one: the pipeline's `CI_JOB_TOKEN` as `Job-Token` where
  it exists, otherwise a personal token from the `gitlabToken` Gradle property or `GITLAB_TOKEN`
  as `Private-Token` — the property first, so the credential can live in
  `~/.gradle/gradle.properties` rather than in the repository. Repository name, property and
  variable names, header and content filtering are all configurable, and it works from a settings
  file as well as a build script. Nothing about it is specific to any one GitLab instance.
- **Per-platform publishing for JNI libraries**. `jni { packaging { publishing { } } }` publishes
  the native libraries as one artifact per platform — `com.example:mylib-linux-x64` next to
  `com.example:mylib` — instead of bundling them into the main JAR. A JNI library built on one
  machine can only contain that machine's binary, and the usual answer, a fat JAR assembled from a
  matrix over every operating system, needs a runner for each of them. Where those do not exist,
  this publishes what the infrastructure can build and adds platforms later without any consumer
  having to change anything.

  `platforms` is the selection for a release, not a promise about every version to come, so
  publishing a subset is an ordinary state rather than a warning. The one thing that fails is
  selecting a platform with no binary anywhere: `kreateJniVerifyPlatforms` reports it by name,
  because that gap is always an accident and would otherwise upload cleanly and surface as an
  `UnsatisfiedLinkError` in a consumer's process. `stagingDirectory` takes binaries built
  elsewhere, and a staged binary wins over a locally built one for the same platform.

  Enabling it moves the natives out of the main JAR entirely, so no consumer silently receives
  whichever platform the library happened to be built on. Existing builds are untouched: without
  the new block, `packaging` behaves exactly as before.
- **The generated loader names the missing coordinate.** With per-platform artifacts the usual
  cause of a failed load is a dependency the consumer did not declare, so the message now prints
  the exact `runtimeOnly(...)` line and lists the platforms that version shipped — which separates
  "you forgot the dependency" from "this version does not have that platform".
- **Code coverage**. `kreate { project { coverage { enabled = true } } }` configures
  [Kover](https://github.com/Kotlin/kotlinx-kover) — source set selection, instrumentation control,
  report filters, all four report formats, and a verification gate wired into `check`. It closes the
  last gap in the quality chain: Detekt says the code looks right and Trivy says it is safe to ship,
  but nothing said whether the code is ever executed. Kover is configured, not applied, so its
  version stays with the consumer; enabling the integration without the plugin fails with a message
  saying what to add rather than silently measuring nothing.

  The verification bounds deliberately have **no defaults**. A threshold picked before the first
  measurement is either too high, so the first thing anyone does is lower it, or too low, so it can
  never fail — and a gate that cannot fail is indistinguishable from a working one until the day it
  should have caught something. An unset bound registers no rule at all rather than a rule demanding
  zero coverage, and a named rule that declares no bounds is rejected for the same reason.
- **Named coverage rules**. `coverage { verify { rules { create("...") { ... } } } }` covers what a
  single minimum percentage cannot: several bounds under one heading, a per-class floor alongside a
  per-application one, and absolute counts. `maxBound(500, CoverageUnit.LINE,
  Aggregation.MISSED_COUNT)` caps how much untested code may exist at all — a limit a percentage
  cannot express, because at a fixed 80% a codebase that doubles in size doubles its untested code
  while the number on the badge never moves. Rules are named because the name is what the failure
  message shows.
- **Coverage aggregation**. `coverage { aggregate { enabled = true } }` merges the coverage of
  subprojects into one report, so the gate answers for the product rather than for whichever module
  happens to be best tested. Kover's own `merge { }` applies its plugin to the projects it
  aggregates; Kreate wires them through the `kover` configuration instead and reports the ones
  missing the plugin by path, because injecting a plugin into a project whose build script never
  mentions it is the behaviour this plugin exists to avoid.
- **Coverage of Kreate's own build**. The new `kreate.coverage-conventions` build-logic plugin
  measures the plugin itself, with a ratcheted line coverage bound set from a real measurement. That
  figure understates reality and the documentation says so: the functional suite drives Gradle
  through TestKit, which runs builds in a separate daemon process, and Kover instruments the test
  JVM rather than the process it starts.

## 2.1.1

### Fixed

- **`kreateApiCheck` failed on Windows for an interface that had not changed**. Git rewrites text
  files to CRLF when it checks them out on Windows, which is the default on the Windows CI
  images, while the dump Kreate renders always ends its lines with a line feed. The task compared
  the two as raw strings, so every Windows run of a project with a checked-in `.api` file failed.
  The report made the cause hard to see rather than obvious: the diff splits on line boundaries,
  which treats `\r\n` and `\n` alike, so it found no differing line and printed a bare context
  count with nothing underneath it. The checked-in file is now read with its line endings
  normalised, and a real interface change is still reported line by line. The test that pins the
  dump format to the `binary-compatibility-validator` plugin's output compared the same two
  values and failed for the same reason; it reads the dump the same way now.

## 2.1.0

### Added

- **Binary compatibility validation**. `kreate { project { apiValidation { enabled = true } } }`
  registers `kreateApiDump` and `kreateApiCheck`. The dump records every public and protected
  declaration of the compiled classes; the check runs as part of `check` and fails with the
  differing lines and the command to regenerate the file. Kreate reads the bytecode itself with
  ASM, so nothing else has to be applied to the consumer's build, and the file format is the one
  the `binary-compatibility-validator` plugin writes — an existing `api/*.api` file carries over
  unchanged, its line endings normalised on read so a Windows checkout that Git handed out with
  CRLF does not read as an interface change. A test asserts that byte for byte against a dump
  the plugin produced.
  Kotlin `internal` declarations are excluded via the class metadata, along with the default
  argument bridges that would otherwise outlive them, and so is compiler plumbing such as
  `access$` accessors and marker-only constructors. Declarations can also be hidden with an
  annotation of your own through `nonPublicMarkers`, or excluded by package or class name.
- **Dependency locking**. `kreate { project { dependencyLocking { enabled = true } } }` activates
  locking and registers `kreateResolveAndLockAll`, which resolves every locked classpath in one
  invocation. `--write-locks` records only the configurations a build actually resolves, so
  running it against an arbitrary task writes a lock file that looks complete and is not; the
  Trivy scans read those files, which is where an incomplete one does real damage. The default
  locks `compileClasspath` and `runtimeClasspath` rather than everything, because locking the
  build tools too makes a vulnerability scan report CVEs in a documentation tool's XML parser as
  though they were vulnerabilities in the published artifact — 2 of 103 entries were shipped
  dependencies in the measurement behind that default.
- **kotlinx-benchmark integration**. `kreate { project { benchmark { enabled = true } } }` builds
    the `benchmarks` source set, associates it with `main` so the benchmarks reach its `internal`
    declarations and inherit its dependencies, adds `kotlinx-benchmark-runtime`, and applies the
    `allopen` compiler plugin for `org.openjdk.jmh.annotations.State` — a benchmark class that is
    final fails inside JMH with a message that never mentions `allopen`. Measurement profiles are
    configured through `profiles { }` with defaults chosen for reproducibility, including a fixed
    fork count. Kreate configures the kotlinx-benchmark plugin but does not apply it, and depends
    on it `compileOnly` so a 0.4.x artifact never reaches a consumer's buildscript classpath;
    enabling the feature without the plugin fails with an explanation rather than a
    `NoClassDefFoundError`.
- **Benchmark regression gate**. `kreateBenchmarkBaseline` records a committed baseline and
  `kreateBenchmarkCheck` fails the build when a benchmark got measurably slower. The comparison
  knows that `thrpt` is better when higher and `avgt` better when lower, matches on `@Param`
  values, refuses to compare scores recorded in a different mode or unit, and treats a benchmark
  that vanished from the run as a failure — deleting one is otherwise the simplest way to make a
  regression disappear. A regression counts only when it also exceeds the two measurement errors
  combined; without that test the gate fires on ordinary run-to-run variance and is switched off
  within a week. Every run writes a Markdown comparison, passing or failing.
- **`kreateBenchmarkReport`**. kotlinx-benchmark writes its report to a directory named after
  the time of the run, which no task can declare as an output: nothing downstream can depend on
  it, nothing can be up to date against it, and the directory gains an entry per run. This task
  republishes the newest run at a fixed path, which is what makes the gate an ordinary cacheable
  task with declared inputs.
- **A canonical baseline format**. The report JMH writes carries the JVM path, the full argument
  list and a percentile table. The baseline holds the six fields the comparison reads, so a
  committed file does not contain one developer's absolute paths and a diff shows the scores.

### Changed

- **Lock files are no longer ignored by Git**. `*.lockfile` was in `.gitignore`, which is why the
  repository had none. Gradle writes "This file is expected to be part of source control" into
  every lock file it generates, and a lock file that cannot be committed locks nothing.
- **The Trivy workflow writes its locks with `kreateResolveAndLockAll`** rather than
  `:example:dependencies --write-locks`, which did not resolve every locked classpath.
- **The example project uses both new features** instead of the hand-written locking block it
  carried before, and its API dump is checked in CI.
- **The example project benchmarks itself**, with a deliberately wide threshold: it proves the
  pipeline is wired up, not that the machine is fast.
- **A `Benchmark` workflow** runs the gate on a schedule and on demand rather than on every push.
  Shared runners are too noisy for a per-pull-request gate, and the workflow says so.

## 2.0.1

### Fixed

- **GitLab publishing produced no artifacts**. `configureGitlab` registered the repository and
  then configured POM metadata through `publications.withType<MavenPublication>()`, but nothing
  ever created a publication. `publish` is a lifecycle task, so with no
  `PublishToMavenRepository` to depend on it reported `UP-TO-DATE` and the build passed —
  green pipeline, empty registry. Kreate now registers a `maven` publication from the
  project's `java` or `javaPlatform` component. A project that already declares a publication
  of its own, including the one the Maven Central plugin creates, is left untouched.
- **GitLab credentials could not authenticate**. The repository paired `PasswordCredentials`
  with `HttpHeaderAuthentication`, which accepts `HttpHeaderCredentials` and nothing else.
  This never surfaced because the defect above meant the repository was never used; with
  publishing fixed it would have failed every upload. The job token is now passed as
  `HttpHeaderCredentials` under the `Job-Token` header, as GitLab documents.
- **Incomplete registry coordinates failed late and unreadably**. A missing project id or API
  URL was interpolated into the URL as the literal `null`, and the build failed somewhere in
  the transport layer. Both are now validated up front, naming the variables that are unset.
- **Publications were missing outside CI**. The whole GitLab block returned early when no job
  token was present, so a local `publishToMavenLocal` had nothing to publish either. Only the
  remote repository now depends on the token; the publication is always registered.

### Added

- **Sources JAR for published libraries**. Projects with the `java` plugin get `withSourcesJar()`
  when GitLab publishing is enabled. A `java-platform` is skipped — a BOM has no sources.
- **Functional tests for publishing**. Four TestKit tests assert on the task graph rather than
  the exit code, which is what the original defect required: the old build succeeded.

## 2.0.0

### Added

- **JNI header generation**: The new `kreateJniHeaders` task reads the compiled classes, finds every
  method with the `ACC_NATIVE` flag, and emits the exact C declarations the JVM will look up —
  correct mangling, correct types, and long-form disambiguation for overloads. Kotlin has no
  `javac -h` equivalent, so JNI signatures previously had to be transcribed by hand, where a single
  wrong character compiled cleanly on both sides and failed at runtime with `UnsatisfiedLinkError`.
  Enabled by default via `jni { headers { } }`.
- **Native packaging**: `jni { packaging { } }` places the built shared library into the JAR under
  `natives/<os>-<arch>/` and generates a `KreateNativeLoader` object into your sources. The loader
  tries `System.loadLibrary` first and falls back to extracting the packaged copy, so a published
  artifact works without the consumer configuring `java.library.path`.
- **JNI toolchain configuration**: `buildType`, `cmakeExecutable`, and `generator` are now
  configurable rather than fixed.
- **`kreateJniConfigure`**: The CMake configure step is a task of its own, so an ordinary C++ edit
  no longer re-runs the generator.
- **`KreateTasks`**: Every task name the plugin registers is declared in one place and documented.
- **Test suite**: 77 tests where there were none — unit tests for the mangling, naming, version and
  toolchain resolution logic, and TestKit functional tests that drive real Gradle builds, including
  the native pipeline end to end.
- **Gradle compatibility matrix**: The functional suite runs against the declared minimum Gradle
  version and the current one, so the supported range is verified rather than asserted.
- **Binary compatibility validation**: The public DSL is covered by a checked-in API dump; a change
  to any public declaration fails the build until it is recorded.
- **CI**: Workflows for a three-platform, three-JDK build matrix, Detekt with SARIF upload, Trivy
  scanning with SBOM generation, and a reproducible-build check. All actions are pinned to commit
  SHAs and every job declares minimal permissions.
- **Repository hygiene**: `SECURITY.md`, `CODEOWNERS`, and pull request and issue templates.

### Fixed

- **JNI: stale shared libraries**. `kreateJniBuild` declared outputs but no relevant inputs, so
  Gradle considered it up to date indefinitely — edits to C++ sources produced no rebuild and the
  JVM kept loading the previous binary while the build reported success. All native sources,
  generated headers, and the CMake cache are now declared inputs.
- **JNI: unrecoverable CMake cache errors**. The CMake build directory lived inside the source tree,
  so it travelled with the sources. Renaming, moving, or checking the project out at a different
  path produced `CMake Error: The current CMakeCache.txt directory ... is different ...`, which
  `gradle clean` could not repair because it never reached in there. Build output now lives under
  `build/jni/<os>-<arch>/`, and the absolute paths CMake binds to are a declared task input, so a
  relocated project reconfigures automatically.
- **JNI: wrong JDK**. `find_package(JNI)` resolved against whatever JDK the machine defaulted to
  rather than the Gradle toolchain, silently compiling native code against different headers than
  the Kotlin code targeted. The toolchain JDK is now passed to CMake explicitly.
- **JNI: CMake configure failed on Windows**. Paths were handed to CMake with native
  backslashes. A backslash is an escape character in the CMake language, so `FindJNI` re-parsed
  `-DJAVA_HOME=C:\hostedtoolcache\...` as escape sequences and aborted with
  `Invalid character escape '\h'`. Every path passed to CMake now uses forward slashes, which
  are valid on all platforms.
- **JNI: artifacts missing on Windows and macOS**. Multi-configuration generators append the
  configuration name to the output path, so the library landed in `lib/Release/` while
  `java.library.path` pointed at `lib/`. The per-configuration output directories are now pinned.
- **JNI: new source files ignored**. The generated `CMakeLists.txt` globbed without
  `CONFIGURE_DEPENDS`, so a source file added after the first configure was never compiled.
- **JNI: swallowed build failures**. Only the exception message survived a failed CMake invocation.
  The full command, working directory, `JAVA_HOME`, and complete compiler output are now included.
- **JNI in multiplatform projects**: the native build was hooked into every Kotlin compilation task,
  including Kotlin/Native and Kotlin/JS targets, contradicting the documented behaviour. It is now
  wired only where the library is actually needed.
- **Overlapping task inputs and outputs**: `InitializeCppProject` declared the parent directory as
  an input and a directory inside it as an output, making its up-to-date check meaningless.
- **Configuration-time side effects**: directories were created while the build was being
  configured, which did not happen at all on a configuration cache hit.
- **Eager task realization**: `executeTaskBeforeCompile` called `.get()` on a `TaskProvider` and
  `tasks.contains(...)`, defeating configuration avoidance.
- **C-interop: stale native libraries**. `kreateCInteropCompile` declared outputs but no source
  inputs, so an edited Rust or C++ file produced no rebuild — the same defect as the JNI build task.
  The native sources are now declared inputs.
- **C-interop: overlapping task inputs and outputs**. Every task in the pipeline declared the
  native project directory as an input while writing its output into that same directory, which
  made their up-to-date checks meaningless. The directories are now internal and each task
  declares what it actually reads.
- **C-interop: swallowed build failures**. `CompileNative` and `CompileRust` caught every
  exception and replaced it with a generic message, discarding the Cargo or compiler diagnostic
  entirely. Both now use the shared process runner, which attaches the full command, working
  directory, and output to the failure.
- **C-interop: `ConfigureCargo` could never regenerate**. After the input/output overlap was
  removed it had no inputs at all, which would have left an existing project pinned to the
  manifest template of the Kreate version that first generated it. The template is now a declared
  input.
- **Executable resolution**: CMake, Cargo, and Trivy were each resolved by separate logic that
  searched conventional install directories on macOS only. A single resolver now searches the
  `PATH` first — including Windows executable extensions — then the conventional locations, on
  every platform.

- **Example: the vulnerability scan covered the wrong dependencies**. The example locked every
  resolvable configuration, so its lock file described the Kotlin compiler classpath, Dokka's
  HTML generator and Detekt's rule set plugins alongside the two dependencies it actually ships —
  2 shipped entries out of 103. The scan consequently reported CVEs in a documentation tool's XML
  parser as findings against the project. Locking is now restricted to the compile and runtime
  classpaths, and the Trivy documentation explains why.

### Changed

- **Task names are consistent.** The 1.x names followed three conventions at once. All tasks now use
  the `kreate` prefix, the feature, and the action, in camel case:

  | 1.x                             | 2.0.0                          |
  |---------------------------------|--------------------------------|
  | `kreate-jni-initialize`         | `kreateJniInitialize`          |
  | `kreate-jni-build`              | `kreateJniBuild`               |
  | `kreate-c-interop-initialize`   | `kreateCInteropInitialize`     |
  | `kreate-c-interop-dependencies` | `kreateCInteropDependencies`   |
  | `kreate-c-interop-configure`    | `kreateCInteropConfigure`      |
  | `kreate-c-interop-script`       | `kreateCInteropScript`         |
  | `kreate-c-interop-compile`      | `kreateCInteropCompile`        |
  | `kreate-c-interop-definitions`  | `kreateCInteropDefinitions`    |
  | `kreate-build-constants`        | `kreateBuildConstants`         |
  | `trivyScan`                     | `kreateTrivyScan`              |
  | `trivySecretScan`               | `kreateTrivySecretScan`        |
  | `trivyLicenseScan`              | `kreateTrivyLicenseScan`       |
  | `trivyVulnerabilityScan`        | `kreateTrivyVulnerabilityScan` |

- **Repositories are no longer injected.** The plugin added Maven Central, the Gradle Plugin Portal,
  and Google to every project it was applied to. In a build resolving through an internal mirror
  that is at best a warning under `repositoriesMode`, and at worst a silent bypass of the mirror.
  Now opt-in via `project { applyDefaultRepositories = true }`.
- **The serialization plugin is no longer applied unconditionally.** A compiler plugin participates
  in every compilation; projects that never serialize anything were paying for it. Now opt-in via
  `project { applySerializationPlugin = true }`.
- **The JNI pipeline runs after Kotlin compilation.** Headers are derived from compiled `external`
  declarations, so the previous ordering made generating them impossible. It also keeps a native
  build off the critical path of every Kotlin compile; test, run, and packaging tasks depend on the
  library instead.
- **Actionable failure messages.** Missing prerequisite plugins now raise a `GradleException` naming
  the project, the `plugins { }` block to add, and how to disable the integration instead.
- **The version fallback is logged.** Falling back to `1.0.0` when neither the CI environment
  variable nor the project property yields a version is now a warning; a release accidentally
  published as `1.0.0` cannot be withdrawn from a public repository.
- **`java.library.path` is contributed lazily and additively** through a Gradle argument provider,
  rather than by rewriting `jvmArgs` at configuration time — which previously discarded any library
  path the build had configured for its own reasons.
- **The plugin compiles against Kotlin 2.4.0 and Java 17**, the versions embedded in the minimum
  supported Gradle. A plugin built against a newer API fails on the consumer's machine at runtime
  rather than in its own build.
- **Reproducible archives**: pinned timestamps and file order, verified in CI.
- **Documentation rewritten**, including new topics on header generation, native packaging, JNI
  troubleshooting, CI integration, compatibility, and a complete task reference.

### Removed

- **`Process` interface and `Executable` base class.** `Executable` extended `Exec` while also
  declaring an `execute()` task action, giving it two competing actions. It was never used; the
  interface forced every task into a `@TaskAction override fun execute()` shape that bought nothing,
  since Gradle discovers task actions through the annotation.
- **Internal helpers from the public API**: `initializeJni`, `initializeCInterop`, and
  `getProjectVersion` are now `internal`.

### Build

- Gradle 9.7.0, Detekt 2.0.0-alpha.6.
- `buildSrc` replaced by a `build-logic` included build with three convention plugins.
- Strict plugin validation (`failOnWarning`, `enableStricterValidation`) and
  `allWarningsAsErrors` for the plugin's own sources.
- Dependabot now covers the `kreate-plugin` and `build-logic` builds, which as separate Gradle
  builds were never updated before.

## 1.3.1

### Added
- **JNI Library Runtime Paths**: Added `libraryRuntimePaths` option to the JNI configuration block. This allows specifying additional directories to be included in `java.library.path` at runtime, enabling the JVM to resolve external shared libraries that are not part of the primary JNI build.

### Fixed
- **JNI Library Path Merging**: Improved how `java.library.path` is configured for tests and execution tasks. It now correctly merges the default native build output directory with any user-specified runtime paths, preventing issues where external libraries were not correctly resolved.
- **Native Build Error Reporting**: Enhanced the `kreate-jni-build` task to provide more detailed error messages when CMake builds fail, including the underlying cause of the failure.

## 1.3.0

### Added
- **C and C++ Native Interop**: Extended Kotlin Multiplatform C-interop to support C and C++ as first-class native languages alongside Rust. A new `language` option in the `cInterop` block selects the pipeline: `NativeLanguage.RUST` (default) keeps the Cargo/`cbindgen` flow, while `NativeLanguage.C` and `NativeLanguage.CPP` scaffold and build a CMake static-library project that is bridged through a hand-written C header.
- **Multiple JNI Include Paths**: Added a `libraryIncludePaths` option to the JNI configuration block, allowing multiple C++ library include directories to be specified. Each configured path is passed to the compiler via the generated `CMakeLists.txt`, making it easier to depend on multiple external libraries located in different directories.
- **JNI for Multiplatform JVM Targets**: Extended JNI support to Kotlin Multiplatform projects. Native integration can now be configured via `platform.jvm.jni` and is wired into the JVM target's compilation, test, and run tasks without affecting other platform targets.

### Changed
- **C-Interop Pipeline Refactoring**: Internal reorganization of the C-interop initialization logic to support multiple native backends (Cargo for Rust, CMake for C/C++).
- **Task Registration Strategy**: Refined how native tasks are wired into the Kotlin compilation lifecycle to ensure consistent behavior across JVM and Multiplatform projects.

### Fixed
- **JNI Library Path Resolution**: Fixed issues where native libraries were not correctly resolved during tests in Multiplatform projects by ensuring the JVM target's runtime classpath is properly updated.

## 1.2.5

### Changed
- **Task Registration Refactoring**: Replaced property delegates with direct task registrations in JNI and C-Interop initializer classes to optimize configuration time.
- **Dependency Updates**:
  - Updated Kotlin to **2.4.0**.
  - Updated Gradle Wrapper to **9.6.0**.
  - Updated Detekt Gradle plugin.

### Fixed
- **Code Quality**: Cleaned up unused imports in platform-specific initializer classes.

## 1.2.4

### Added
- **Standalone Trivy Module**: Introduced a dedicated `TrivyModule` that operates independently of platform-specific modules (JVM/KMP), allowing security scans to be performed on any project type.

### Changed
- **Trivy DSL Relocation**: Moved the `trivy { }` configuration block from `kreate.project.trivy` to the top-level `kreate.trivy` scope to support platform-agnostic usage.
- **Example Project**: Updated the example `build.gradle.kts` to reflect the new independent Trivy configuration.

### Fixed
- **Module Decoupling**: Resolved the dependency of Trivy features on the `ProjectModule` lifecycle, ensuring security tasks are initialized correctly regardless of other module applications.

## 1.2.3

### Added
- **KDoc Completion**: Completed the KDoc documentation for the `Project.kt` configuration file, ensuring compliance with strict professional standards for all project identity, organization, and legal constants.

### Changed
- **Minimum Java Version**: Updated the minimum required Java version for the plugin and example projects to **Java 17**.
- **Documentation Update**: Synchronized all documentation (README, Getting Started, Platform guides) to reflect Java 17 as the new minimum requirement.

## 1.2.2

### Added
- **Detekt Report Specialization**: Introduced specialized report specifications for HTML, Markdown, Checkstyle, and SARIF formats.
- **Detekt Defaults**: HTML and Markdown reports are now enabled by default (`required = true`).

### Changed
- **Trivy Dependency Locking**: Refactored Trivy integration to require Gradle lockfiles for dependency scanning, removing the `disableDependencyLocking` option to ensure more reliable security audits.
- **Detekt Report Paths**: Standardized default output locations for all Detekt report types under `build/reports/detekt/`.

### Fixed
- **Code Quality**: Removed unused imports in `Initializer.kt` and cleaned up internal configuration logic.
- **Documentation**: Fixed incorrect project group assignments in documentation and updated KDoc to reflect new report specifications.

## 1.2.1

### Added
- **Trivy Configuration**: Added `disableDependencyLocking` property to `TrivyExtension` to allow manual management of Gradle lockfiles.

### Fixed
- **Trivy Initialization**: Fixed a `ConcurrentModificationException` and resolution strategy errors by ensuring dependency locking is only activated for unresolved configurations.

## 1.2.0

### Added
- **Trivy Security Integration**: Integrated [Trivy](https://trivy.dev/) for automated security and compliance scanning.
  - **Vulnerability Scanning**: Automatically scan project dependencies (via lockfiles) for known CVEs.
  - **License Compliance**: Verify dependency licenses against forbidden or restricted lists to ensure legal compliance.
  - **Secret Detection**: Scan source files, configuration files, and environment files for hardcoded secrets and credentials.
- **Detekt Integration**: Integrated [Detekt](https://detekt.dev/) for automated static code analysis to enforce clean code architecture and design patterns.
- **Unified Security & Quality DSL**: New `trivy { }` and `detekt { }` configuration blocks to easily manage security and code quality settings.
- **Optimized Execution**: Trivy scans are optimized to run in a single aggregate process per project, significantly reducing build times compared to file-by-file scanning.
- **Lifecycle Aggregation**: Added a global `trivyScan` task that serves as a single entry point for all enabled security checks.

### Changed
- **Plugin Application (Breaking Change)**: To resolve lifecycle ordering issues, especially in Kotlin Multiplatform projects, `kreate` no longer automatically applies `maven-publish`, `detekt`, or the Vanniktech Maven Publish plugin. These must now be applied manually in the `plugins { }` block.
- **KDoc Standards**: All new components follow strict professional KDoc guidelines, including mandatory `@param`, `@return`, and `@since` tags.
- **Task Logging**: Improved console output for security findings with better formatting and clear error messages.

### Fixed
- **Publishing Lifecycle**: Fixed a critical bug where Maven and GitLab publication settings were not correctly initialized in Kotlin Multiplatform projects because the required plugins were applied too late by the framework.

## 1.1.1

### Changed
- **Task Naming Convention**: Standardized all Gradle tasks to use a consistent `kreate-<module>-<task>` prefix (e.g., `kreate-jni-build`, `kreate-c-interop-compile`).
- **Task Organization**: Organized plugin tasks into specific Gradle groups (`kreate c-interoperation`, `kreate jni`, `kreate build-constants`) for better visibility and navigation.

## 1.1.0

### Added
- **JNI Support**: Added comprehensive support for Java Native Interface (JNI) in JVM modules.
  - **Automated C++ Integration**: Seamlessly bridge C/C++ libraries with JVM projects using CMake.
  - **Project Scaffolding**: Built-in task to initialize new CMake-based C++ projects for JNI.
  - **Automated Build Pipeline**: Native builds are automatically hooked into the Kotlin compilation process.
  - **Runtime Configuration**: Automatic configuration of `java.library.path` for tests and execution tasks to resolve native libraries.
- **Improved Platform DSL**: Added `jvm` configuration block to the platform DSL for better organization of JVM-specific settings.

### Changed
- **Naming Convention**: Standardized internal naming resolution for native features (JNI, C-Interop) to ensure compatibility across different platforms and toolchains.
- **Documentation**: Updated KDoc across all new JNI components to follow strict professional standards.

## 1.0.1

### Added
- **KDoc Documentation**: Added comprehensive KDoc documentation across the entire project for improved clarity and professionalism.
- **Project Configuration**: Added `.gitignore` and copyright configuration files for project management.
- **Dependency Management**: Added Dependabot configuration for automated Gradle and GitHub Actions updates.
- **C-Interop Docs**: Added detailed documentation and examples for integrating Rust with Kotlin Multiplatform via C-Interop.

### Fixed
- **Versioning**: Corrected Kotlin and KSP versions in `libs.versions.toml` to 2.3.0.
- **Legal**: Updated copyright year to 2026 in multiple files and LICENSE.
- **DSL**: Updated `projectName` parameter to be non-nullable in GitLab and MavenCentral configuration functions for better reliability.
- **Build**: Updated Gradle build commands in documentation to use `--no-daemon`.

### Changed
- **Architecture**: Simplified project and publication configuration by removing the `Davils` object.
- **Docs**: Updated README with more detailed installation and configuration instructions.
- **Build**: Updated various dependencies (Kotest, Gradle Wrapper, GitHub Actions, KSP).

## 1.0.0

### Added

#### Core Architecture & Platform Support
- **Intelligent Platform Detection**: Automatic identification and configuration of `org.jetbrains.kotlin.jvm`, `org.jetbrains.kotlin.multiplatform`, and Android projects.
- **Unified Platform DSL**: Simplified configuration for target Java versions (supporting Java 21+ toolchains).
- **Strict Quality Defaults**:
  - Optional `explicitApi()` mode enforcement.
  - `allWarningsAsErrors` enabled by default for cleaner codebases.
- **Native Multiplatform Support**: Pre-configured targets for Linux (x64), macOS (x64, arm64), and Windows (x64).

#### Rust C-Interop (KMP)
- **Automated Rust Integration**: Seamlessly bridge Rust libraries with Kotlin Multiplatform using `cinterop`.
- **Cargo Toolchain Support**: Automated execution of `cargo build` with cross-compilation support.
- **Project Scaffolding**: Built-in task to initialize new Rust library projects within the Kotlin workspace.
- **Header & Definition Management**: Simplified DSL for `.def` files and C-header synchronization.
- **Multi-Arch Compilation**: Support for major architectures including `x86_64-unknown-linux-gnu` and `aarch64-apple-darwin`.

#### Testing Pipeline
- **Kotest Integration**: Deep integration with Kotest for advanced testing capabilities.
- **Execution Engine**:
  - Configurable parallel test execution (max forks based on CPU availability).
  - Customizable timeouts and failure thresholds.
- **Enhanced Test Logging**: Clear, colorized output for test states (Started, Passed, Skipped, Failed).
- **Reporting**: Automated generation of comprehensive XML and HTML test reports for CI/CD pipelines.

#### Publishing & POM Management
- **Declarative POM DSL**: Easy configuration of metadata including licenses, developers, SCM, and issue management.
- **Registry Support**:
  - **Maven Central**: Streamlined publishing with automatic release and GPG signing.
  - **GitLab Package Registry**: Native support for GitLab CI environments using environment variables (`CI_JOB_TOKEN`, etc.).
- **Security**: Integrated GPG signing for all publications.

#### Documentation & Constants
- **Integrated Dokka Support**: Simplified generation of API documentation via Gradle.
- **Build Constants**: Generate type-safe Kotlin constants from Gradle properties to bridge build-time information into runtime code.

#### Project Management
- **Centralized Versioning**: Global management of project group and version across all modules.
- **Standardized Repositories**: Automatic configuration of Maven Central and Google repositories.

### Changed
- Initial stable release. Transitioned from internal development to a public Gradle plugin.
