# CI integration

<link-summary>
Wiring %product%'s checks into GitHub Actions, GitLab CI, or any other runner.
</link-summary>

<card-summary>
Ready-to-adapt pipelines, plus the pitfalls that make a green build meaningless.
</card-summary>

%product% adds ordinary Gradle tasks, so any runner that can execute `./gradlew` can use them.
What follows is what a pipeline should actually run, and the mistakes that make one pass without
verifying anything.

## What to run

<deflist type="wide">
    <def title="./gradlew build">
        Compiles, assembles, and runs everything wired to <code>check</code> — which with
        <code>project.tests.enabled</code> means the <code>unitTest</code> suite, with the
        configured reporting. It does <i>not</i> include <code>integrationTest</code>.
    </def>
    <def title="./gradlew integrationTest">
        The suite that needs Docker or a network, which is deliberately off <code>check</code>.
        Give it its own step, so a container that fails to start is reported as an infrastructure
        failure rather than as a test failure. See <a href="Testing-Suites.md">Test suites</a>.
    </def>
    <def title="./gradlew kreateTrivyScan">
        The security and compliance scans. Needs Trivy installed and dependency lock files
        present.
    </def>
    <def title="./gradlew detekt">
        Static analysis, when the <a href="Detekt-Overview.md">integration</a> is enabled.
    </def>
    <def title="./gradlew koverXmlReport koverLog koverVerify">
        Coverage measurement, the log line a CI system parses, and the threshold gate, when the
        <a href="Coverage-Overview.md">coverage integration</a> is enabled. <code>koverVerify</code>
        already runs as part of <code>check</code>; name it explicitly only when you want the
        failure reported as its own step.
    </def>
    <def title="./gradlew kreateJniBuild">
        Included in <code>build</code> transitively, but useful as an explicit step when you want
        the native failure reported separately from the JVM one.
    </def>
</deflist>

### Staging the test suites

The two default suites exist so that a pipeline can fail fast on the cheap signal:

<code-block lang="bash">
./gradlew check              # unit tests, static analysis, coverage gate
./gradlew integrationTest    # containers and external services
</code-block>

Run them as separate steps rather than as <code>./gradlew check integrationTest</code>. A single
invocation gives one red step for two very different causes, and on a shared runner the slow suite
holds the build long after the fast one has already told you what you needed to know.

<tip>
Every suite writes JUnit XML to <code>build/test-results/&lt;suite&gt;/</code>. Point the report
collector at the directory rather than at one file, so a suite you add later is picked up without
another pipeline edit:
<code-block>**/build/test-results/*/TEST-*.xml</code-block>
</tip>

## Two pitfalls that produce a meaningless green build

<warning>
<b>Composite builds are not reached by a root-level task name.</b>
If your plugin or build logic lives in an <code>includeBuild</code>, running
<code>./gradlew build</code> from the root does <i>not</i> run that build's tests or checks — it
only builds it far enough to satisfy the consumer. Name the tasks explicitly:
<code-block lang="bash">./gradlew :my-included-build:build build</code-block>
</warning>

<warning>
<b>A skipped native test is not a passing native test.</b>
Test suites that skip when a toolchain is missing are right to do so on a developer machine and
wrong to do so in CI, where a missing toolchain is an infrastructure failure. Install the
toolchain and assert it is present:
<code-block lang="bash">cmake --version || exit 1</code-block>
</warning>

## Guarding against local development state

The [local development workflow](Local-Development-Overview.md) keeps its state under
`$GRADLE_USER_HOME/kreate/local`. Every pipeline that points `GRADLE_USER_HOME` inside the build
directory recreates it per job, so the directory cannot survive — and a build that finds it
anyway fails rather than resolving from it.

A dedicated job makes that structural property an asserted one, for the cost of one `test`:

```yaml
local-state:
  stage: security
  needs: []
  script:
    - test ! -d "$GRADLE_USER_HOME/kreate/local"
```

An artifact built against a locally published dependency cannot be reproduced by anyone else, and
nothing in its published metadata would say so. This is the one failure mode worth a job of its
own.

## GitHub Actions

<code-block lang="yaml" collapsible="true" collapsed-title=".github/workflows/ci.yml">
<![CDATA[
name: CI

on:
  push:
    branches: ['**']
  pull_request:

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

permissions: {}

jobs:
  build:
    runs-on: ${{ matrix.os }}
    permissions:
      contents: read
    strategy:
      fail-fast: false
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
        java: ['17']
    steps:
      - uses: actions/checkout@v7

      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: ${{ matrix.java }}

      - uses: gradle/actions/setup-gradle@v6
        with:
          validate-wrappers: true

      - name: Install CMake
        if: runner.os == 'Linux'
        run: sudo apt-get update && sudo apt-get install -y cmake

      - name: Install CMake
        if: runner.os == 'macOS'
        run: brew install cmake

      - run: ./gradlew build --configuration-cache --stacktrace
]]>
</code-block>

<tip>
A native feature is exactly the kind of thing that works on Linux and fails on Windows. A
three-platform matrix is the only way to find that out before a user does — multi-configuration
generator behaviour, shared library naming, and path handling all differ.
</tip>

### Publishing findings to code scanning

Detekt and Trivy both emit SARIF, which GitHub renders as inline pull request annotations:

```yaml
      - name: Run Detekt
        run: ./gradlew detekt --continue

      - name: Upload SARIF
        if: always()
        uses: github/codeql-action/upload-sarif@v4
        with:
          sarif_file: build/reports/detekt/detekt.sarif
          category: detekt
```

The `if: always()` matters — the upload is most valuable precisely on the runs where the analysis
failed.

## GitLab CI

<code-block lang="yaml" collapsible="true" collapsed-title=".gitlab-ci.yml">
<![CDATA[
stages: [build, verify, publish]

variables:
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"

default:
  image: eclipse-temurin:21-jdk
  before_script:
    - apt-get update && apt-get install -y cmake
    - cmake --version

build:
  stage: build
  script:
    - ./gradlew build --configuration-cache
  artifacts:
    when: always
    reports:
      junit: "**/build/test-results/*/TEST-*.xml"
    paths:
      - "**/build/reports/"
    expire_in: 1 week

coverage:
  stage: verify
  script:
    - ./gradlew koverXmlReport koverLog koverVerify
  coverage: '/application line coverage: (\d+\.?\d*)%/'
  artifacts:
    when: always
    reports:
      coverage_report:
        coverage_format: jacoco
        path: build/reports/kover/report.xml
    paths:
      - build/reports/kover/
    expire_in: 1 week

security:
  stage: verify
  before_script:
    - apt-get update && apt-get install -y wget gnupg
    - wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --dearmor > /usr/share/keyrings/trivy.gpg
    - echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb generic main" > /etc/apt/sources.list.d/trivy.list
    - apt-get update && apt-get install -y trivy
  script:
    - ./gradlew dependencies --write-locks
    - ./gradlew kreateTrivyScan

publish:
  stage: publish
  rules:
    - if: $CI_COMMIT_TAG
  script:
    - ./gradlew publish
]]>
</code-block>

<note>
The GitLab publishing integration reads <code>CI_JOB_TOKEN</code>, <code>CI_PROJECT_ID</code>, and
<code>CI_API_V4_URL</code>, all of which GitLab provides automatically. See
<a href="Publishing-Gitlab-Registry.md">GitLab Package Registry</a>.
</note>

### Coverage in merge requests

The `coverage` job above feeds GitLab through two separate channels, and they are easy to confuse
because only one of them produces the number on the badge.

<deflist type="wide">
    <def title="The percentage — coverage: keyword">
        GitLab scans the <b>job log</b> with the regular expression given under
        <code>coverage:</code> and stores the first match. The line it matches is printed by
        <code>koverLog</code>, which is why <code>koverLog</code> is in the script and why
        %product% defaults the log format to
        <code>&lt;entity&gt; line coverage: &lt;value&gt;%</code>. The format and the expression
        are one contract: change either and you have to change both.
    </def>
    <def title="The annotated diff — artifacts:reports:coverage_report">
        GitLab renders line-by-line coverage in the merge request diff from the <b>XML report</b>.
        Kover writes JaCoCo-format XML, which GitLab reads directly as
        <code>coverage_format: jacoco</code> — no Cobertura conversion step is involved.
    </def>
</deflist>

<warning>
<b><code>when: always</code> is not optional here.</b> The gate is what fails the job, so the
default (<code>on_success</code>) uploads the report on every run except the one where somebody
needs it.
</warning>

<tip>
Coverage in a merge request is a diff, not an absolute. A change that adds well-tested code to a
poorly-tested codebase raises the number, and one that deletes dead untested code raises it too —
neither says anything about the change under review. The annotated diff is what tells you whether
the lines <i>this</i> change added are covered.
</tip>

## Versioning from CI

Configure the version to come from your CI's tag variable, with a local fallback:

```kotlin
kreate {
    project {
        version {
            environment = "CI_COMMIT_TAG"   // GitHub: use a step that exports this
            property = "version"            // falls back to gradle.properties
        }
    }
}
```

<warning>
%product% logs a warning when neither source yields a version and it falls back to
<code>1.0.0</code>. Treat that warning as an error in a release pipeline — a release accidentally
published as <code>1.0.0</code> cannot be taken back from a public repository.
</warning>

See [Version resolution](Project-Version-Resolution.md).

## Caching

<deflist type="medium">
    <def title="Gradle caches">
        Use your runner's Gradle integration (<code>gradle/actions/setup-gradle</code> on GitHub, the Gradle
        cache on GitLab) rather than caching directories by hand.
    </def>
    <def title="The native build directory">
        Cache it only if the workspace path is stable across runs. CMake bakes absolute paths into
        its cache, so a cache restored under a different path forces a reconfiguration — correct,
        but no faster than not caching it.
    </def>
    <def title="The configuration cache">
        Enable it with <code>--configuration-cache</code>. All %product% tasks support it.
    </def>
</deflist>

## Reproducibility

If you build artifacts you intend to be independently verifiable, assert it:

```bash
./gradlew jar
sha256sum build/libs/*.jar > first.sha256
./gradlew clean jar
sha256sum build/libs/*.jar > second.sha256
diff first.sha256 second.sha256
```

This requires pinned archive timestamps and file order in your build configuration.

<seealso>
    <category ref="reference">
        <a href="Task-Reference.md">Task reference</a>
        <a href="Compatibility.md">Compatibility</a>
    </category>
    <category ref="security">
        <a href="Trivy-Overview.md">Security and compliance</a>
    </category>
    <category ref="project">
        <a href="Publishing-Overview.md">Publishing</a>
        <a href="Testing-Overview.md">Testing</a>
        <a href="Testing-Suites.md">Test suites</a>
    </category>
</seealso>
