# Overview

<link-summary>
What %product% does, the problems it removes, and how its DSL is organised.
</link-summary>

<web-summary>
%product% is an opinionated Gradle plugin for Kotlin JVM and Multiplatform projects. It replaces
platform, native interop, security, testing, documentation and publishing boilerplate with a single
declarative DSL.
</web-summary>

<card-summary>
The problems %product% removes, how the DSL is organised, and where each feature lives.
</card-summary>

A Gradle build for a serious Kotlin project accumulates the same few hundred lines in every
repository: toolchain alignment, compiler flags, a native build shelled out to CMake or Cargo,
Dokka wiring, test logging, POM metadata, signing. None of it is interesting, all of it is easy
to get subtly wrong, and it drifts apart across modules the moment more than one person touches it.

**%product%** replaces that with a single `kreate { }` block.

## What it is not

%product% is a *convention* plugin, not a framework.

- It does not wrap Gradle. Your build script stays a Gradle build script, and every task
  %product% registers is an ordinary task you can depend on, reconfigure, or disable.
- It applies a plugin only for a feature you enabled. Enabling `coverage` applies Kover, and
  nothing applies Kover otherwise. Apply it yourself to pin a version or to reach its own DSL
  block, and %product% leaves it alone. The Kotlin plugin is always yours to apply.
- It does not touch your repositories or dependency resolution unless you ask it to.

<include from="lib.topic" element-id="opt-in-note"/>

## The DSL at a glance

Everything is nested under one extension. The three top-level blocks separate concerns that
change for different reasons: how the code is *compiled*, what the *project* is, and what the
build *verifies*.

```kotlin
kreate {
    platform { /* toolchains, compiler flags, native interop */ }
    project  { /* identity, versioning, docs, tests, publishing */ }
    trivy    { /* vulnerability, licence and secret scanning */ }
}
```

<deflist type="medium">
    <def title="platform">
        Java toolchain and Kotlin compiler configuration, plus the two native bridges:
        <a href="JNI-Support.md">JNI</a> for C/C++ on the JVM and
        <a href="C-Interoperation-Overview.md">C-interop</a> for Rust, C, and C++ on Kotlin/Native.
        See <a href="Platform-Overview.md">Platform configuration</a>.
    </def>
    <def title="project">
        Everything that describes the artifact rather than the compilation:
        <a href="Project-Metadata.md">metadata</a>,
        <a href="Project-Version-Resolution.md">version resolution</a>,
        <a href="Constants-Overview.md">build constants</a>,
        <a href="Documentation-Overview.md">Dokka</a>,
        <a href="Testing-Overview.md">testing</a>,
        <a href="Coverage-Overview.md">coverage</a>,
        <a href="Benchmark-Overview.md">benchmarks</a>,
        <a href="API-Validation-Overview.md">API validation</a>,
        <a href="Detekt-Overview.md">static analysis</a>, and
        <a href="Publishing-Overview.md">publishing</a>.
    </def>
    <def title="trivy">
        Security and compliance scanning, deliberately at the top level because it applies to any
        project type — including ones that use no other %product% feature.
        See <a href="Trivy-Overview.md">Security and compliance</a>.
    </def>
    <def title="local">
        Publishing this repository to the local Maven repository so that another checkout can
        resolve it, without a release. At the top level for the same reason as
        <code>trivy</code>: it is about the repository rather than the artifact. Resolving what
        was published is the job of the companion settings plugin.
        See <a href="Local-Development-Overview.md">Local development</a>.
    </def>
</deflist>

## Platform configuration

%product% reacts to the Kotlin plugin you applied rather than guessing. Apply
`org.jetbrains.kotlin.jvm` and you get the JVM configuration path; apply
`org.jetbrains.kotlin.multiplatform` and you get the multiplatform one.

That is also the only plugin you apply. The plugin behind every feature you enable — Detekt, Kover,
kotlinx-benchmark, Dokka, the publishing plugins — is applied by %product%, and applying one
yourself to pin its version still works. See [](Plugin-Management.md).

| Concern | What you write | What you would otherwise repeat |
|---|---|---|
| Java toolchain | `javaVersion = JavaVersion.VERSION_17` | `jvmToolchain`, `sourceCompatibility`, `targetCompatibility`, per module |
| Explicit API | `explicitApi = true` | `explicitApi()` in every Kotlin block |
| Warning policy | `allWarningsAsErrors = true` | `compilerOptions` on every compile task |

## Native integration

Two separate bridges, because they solve different problems.

<tabs group="native">
<tab title="JNI (JVM)" group-key="jni">

For calling C or C++ from a JVM or Multiplatform-JVM target.

- Scaffolds a CMake project on first build and never overwrites it again.
- **Generates JNI headers from your `external` declarations**, so the C++ compiler verifies
  the signatures the JVM will actually look up. This is the step Kotlin has no equivalent
  of — `javac -h` only works on Java sources.
- Builds with CMake against the JDK from your Gradle toolchain, not whatever the machine
  happens to default to.
- Optionally packages the shared library into your JAR with a generated loader, so consumers
  do not need `-Djava.library.path`.

Start at [JNI support](JNI-Support.md).

</tab>
<tab title="C-interop (Native)" group-key="cinterop">

For calling Rust, C, or C++ from Kotlin/Native targets.

- Rust projects are built with Cargo and bridged through `cbindgen`-generated headers.
- C and C++ projects are built with CMake into a static library and bridged through a
  hand-written C header.
- Generates the `.def` files and wires the `cinterop` compilations for each configured target.
- Maps native target triples to Kotlin/Native targets automatically.

Start at [C-interop overview](C-Interoperation-Overview.md).

</tab>
</tabs>

## Security and compliance

Three scans, each independently configurable, plus an aggregate task that runs whichever are
enabled.

<deflist type="narrow">
    <def title="Vulnerabilities">
        Scans dependency lock files for known CVEs, filtered by the severities you care about.
        See <a href="Trivy-Vulnerability-Scan.md">Vulnerability scanning</a>.
    </def>
    <def title="Licences">
        Checks third-party licences against a forbidden list, with an allow list for the ones
        you have accepted. See <a href="Trivy-License-Scan.md">Licence scanning</a>.
    </def>
    <def title="Secrets">
        Scans source files for credentials, using Trivy's built-in rules plus your own.
        See <a href="Trivy-Secret-Scan.md">Secret scanning</a>.
    </def>
</deflist>

## Quality guarantees

The plugin holds itself to the standard it exists to enforce.

<deflist type="medium">
    <def title="Verified against real builds">
        The test suite drives actual Gradle builds through TestKit, not an in-memory project
        model — including the native pipeline end to end, on Linux, macOS, and Windows.
    </def>
    <def title="Verified across versions">
        Every release is tested against the minimum supported Gradle (%min_gradle%) and the
        current one, on JDK %tested_java%. See <a href="Compatibility.md">Compatibility</a>.
    </def>
    <def title="A guarded public API">
        The DSL is covered by a checked-in binary compatibility dump. A change to any public
        declaration fails the build until it is reviewed and recorded.
    </def>
    <def title="Reproducible artifacts">
        Archives are built with pinned timestamps and file order, and CI verifies that building
        the same source twice produces byte-identical output.
    </def>
</deflist>

<include from="lib.topic" element-id="config-cache-note"/>

<seealso>
    <category ref="start">
        <a href="Getting-Started.md">Getting started</a>
        <a href="Compatibility.md">Compatibility</a>
        <a href="Task-Reference.md">Task reference</a>
    </category>
</seealso>
