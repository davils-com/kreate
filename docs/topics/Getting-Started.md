# Getting started

<link-summary>
Apply the plugin, write your first configuration, and verify it works.
</link-summary>

<card-summary>
From an empty build script to a configured project in four steps.
</card-summary>

<tldr>
<p><b>Apply</b>: <code>id("%plugin_id%") version "%version%"</code></p>
<p><b>Configure</b>: everything lives in one <code>kreate { }</code> block</p>
<p><b>Requires</b>: Gradle %min_gradle%+, JDK %min_java%+</p>
</tldr>

This guide takes an existing Gradle project and gets %product% configured and verified. It
assumes you already have a `build.gradle.kts` and a Kotlin plugin applied.

## Before you start

<deflist type="medium">
    <def title="Gradle %min_gradle% or later">
        Check with <code>./gradlew --version</code>. Older versions are not supported and the
        plugin will refuse to apply rather than fail in a confusing way later.
    </def>
    <def title="JDK %min_java% or later">
        %min_java% is the minimum because it is the minimum for Gradle %min_gradle%. The plugin
        is tested on JDK %tested_java%.
    </def>
    <def title="A Kotlin plugin">
        Either <code>org.jetbrains.kotlin.jvm</code> or
        <code>org.jetbrains.kotlin.multiplatform</code>. %product% configures whichever one it
        finds and does nothing if neither is applied.
    </def>
</deflist>

For the native features you additionally need a toolchain — see
[JNI support](JNI-Support.md) or [C-interop](C-Interoperation-Overview.md).

## Step 1: Apply the plugin

<include from="lib.topic" element-id="apply-plugin"/>

<note>
For a Kotlin Multiplatform project, replace <code>kotlin("jvm")</code> with
<code>kotlin("multiplatform")</code>. %product% detects which one is applied and configures the
matching compilation path; nothing else in your <code>kreate { }</code> block changes.
</note>

<tip>
That is the whole <code>plugins { }</code> block. %product% applies Detekt, Kover,
kotlinx-benchmark, Dokka and the publishing plugins itself, for whichever features you enable — see
<a href="Plugin-Management.md">Plugins %product% applies</a>. The Kotlin plugin stays yours.
</tip>

## Step 2: Declare your repositories

%product% does not add repositories to your project. Declare them where Gradle expects them,
in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

<tip>
If you would rather have %product% add Maven Central, the Gradle Plugin Portal, and Google for
you, set <code>project { applyDefaultRepositories = true }</code>. It is off by default because
a build that resolves through an internal mirror must not have public repositories injected into
it. See <a href="Project-Repositories.md">Repositories and applied plugins</a>.
</tip>

## Step 3: Write the configuration

Only configure what you need. Every property has a documented default.

```kotlin
group = "com.example"

kreate {
    platform {
        javaVersion = JavaVersion.VERSION_17
        explicitApi = true
        allWarningsAsErrors = true
    }

    project {
        name = "MyProject"
        description = "A project configured with Kreate"

        version {
            environment = "CI_COMMIT_TAG"
            property = "version"
        }
    }
}
```

That block does four things: pins the Java toolchain, turns on explicit API mode, promotes
warnings to errors, and resolves the project version from a CI tag with a fallback to a Gradle
property. See [Version resolution](Project-Version-Resolution.md) for the exact precedence.

## Step 4: Verify

<procedure title="Confirm the plugin is configured" id="verify">
    <step>
        <p>List the tasks %product% registered:</p>
        <code-block lang="bash">./gradlew tasks --group kreate</code-block>
        <p>
            With no features enabled this list is empty — that is correct. Tasks appear as you
            enable features.
        </p>
    </step>
    <step>
        <p>Build the project:</p>
        <code-block lang="bash">./gradlew build</code-block>
    </step>
    <step>
        <p>Check that the resolved version is what you expect:</p>
        <code-block lang="bash">./gradlew properties --property version</code-block>
    </step>
</procedure>

## Choosing your next feature

Each of these is a single `enabled = true` away.

<tabs group="first-feature">
<tab title="Build constants" group-key="constants">

Generate a Kotlin object from build values instead of reading properties at runtime.

```kotlin
kreate {
    project {
        buildConstant {
            enabled = true
            className = "BuildConfig"

            constant("apiUrl", "https://api.example.com")
        }
    }
}
```

Continue at [Build constants](Constants-Overview.md).

</tab>
<tab title="Security scanning" group-key="trivy">

Scan dependencies and sources on every build.

```kotlin
dependencyLocking { lockAllConfigurations() }

kreate {
    trivy {
        enabled = true

        vulnerability {
            failOnFindings = true
            lockFiles.from(fileTree(projectDir) { include("*.lockfile") })
        }
    }
}
```

Continue at [Security and compliance](Trivy-Overview.md).

</tab>
<tab title="Native code" group-key="jni">

Call C++ from Kotlin, with generated headers and an automated CMake build.

```kotlin
kreate {
    platform {
        jvm {
            jni {
                enabled = true
            }
        }
    }
}
```

Continue at [JNI support](JNI-Support.md).

</tab>
<tab title="Testing" group-key="tests">

Unit and integration tests as separate suites, each with its own dependencies.

```kotlin
kreate {
    project {
        tests {
            enabled = true

            suites {
                named("integrationTest") {
                    dependencies {
                        implementation("org.testcontainers:postgresql:1.20.4")
                    }
                }
            }
        }
    }
}
```

```bash
./gradlew check             # unit tests
./gradlew integrationTest   # the slow ones, on demand
```

Continue at [Testing overview](Testing-Overview.md), or
[Migrating from src/test](Testing-Suites-Migration.md) if the project already has tests.

</tab>
<tab title="Publishing" group-key="publish">

Signed releases with complete POM metadata.

```kotlin
kreate {
    project {
        publish {
            enabled = true
            inceptionYear = 2026
            website = "https://example.com"
        }
    }
}
```

Continue at [Publishing](Publishing-Overview.md).

</tab>
</tabs>

## Project layout

A project using the native features looks like this. The `jni/` and `cinterop/` directories are
scaffolded for you on the first build.

```text
.
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml
└── my-module/
    ├── build.gradle.kts          # the kreate { } block
    ├── src/main/kotlin/          # Kotlin sources
    ├── src/unitTest/kotlin/      # fast tests, run by `check`
    ├── src/integrationTest/kotlin/  # slow tests, run on demand
    ├── jni/                      # JNI sources (optional)
    │   └── my_module/
    │       ├── CMakeLists.txt
    │       └── src/
    └── cinterop/                 # C-interop sources (optional)
        └── my_module/
```

<warning>
Native <b>build output</b> never lands in these directories. Everything generated goes under
<code>build/</code>, so <code>gradle clean</code> resets it and nothing produced by a build ends
up in version control.
</warning>

## Feature defaults

Every feature is off unless listed otherwise.

| Block | Property | Default |
|---|---|---|
| `platform` | `explicitApi` | `false` |
| `platform` | `allWarningsAsErrors` | `true` |
| `platform.jvm.jni` | `enabled` | `false` |
| `platform.jvm.jni.headers` | `enabled` | `true` (once JNI is on) |
| `platform.jvm.jni.packaging` | `enabled` | `false` |
| `platform.multiplatform.cInterop` | `enabled` | `false` |
| `project` | `applyDefaultRepositories` | `false` |
| `project` | `applySerializationPlugin` | `false` |
| `project.buildConstant` | `enabled` | `false` |
| `project.docs` | `enabled` | `false` |
| `project.tests` | `enabled` | `false` |
| `project.tests` | `legacyTestSourceSet` | `DISABLE` |
| `project.tests` | `excludeSuitesFromCoverage` | `true` |
| `project.tests.kotest` | `enabled` | `false` |
| `project.tests.suites["unitTest"]` | `runOnCheck` | `true` |
| `project.tests.suites["integrationTest"]` | `runOnCheck` | `false` |
| `project.detekt` | `enabled` | `false` |
| `project.coverage` | `enabled` | `false` |
| `project.benchmark` | `enabled` | `false` |
| `project.apiValidation` | `enabled` | `false` |
| `project.dependencyLocking` | `enabled` | `false` |
| `project.publish` | `enabled` | `false` |
| `trivy` | `enabled` | `false` |

<seealso>
    <category ref="start">
        <a href="Overview.md">Overview</a>
        <a href="Plugin-Management.md">Plugins %product% applies</a>
        <a href="Compatibility.md">Compatibility</a>
        <a href="Task-Reference.md">Task reference</a>
    </category>
    <category ref="project">
        <a href="Project-Metadata.md">Project metadata</a>
        <a href="Project-Repositories.md">Repositories and applied plugins</a>
    </category>
</seealso>
