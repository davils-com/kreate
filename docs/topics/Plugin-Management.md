# Plugins %product% applies

<link-summary>Which plugins Kreate applies, when, and how to keep control of one.</link-summary>

<card-summary>One plugins block: Kotlin and Kreate.</card-summary>

<tldr>
<p><b>You apply</b>: the Kotlin plugin</p>
<p><b>%product% applies</b>: the plugin behind every feature you enable</p>
<p><b>Override</b>: apply it yourself; %product%'s own application becomes a no-op</p>
</tldr>

A %product% build script needs two entries:

```kotlin
plugins {
    kotlin("jvm") version "2.4.0"
    id("com.davils.kreate") version "%version%"
}
```

Everything else follows from what you enable in the `kreate { }` block. Enabling a feature is the
decision; applying the plugin behind it is bookkeeping, and %product% does it.

## What gets applied, and when

Nothing is applied speculatively. A plugin appears only when the feature that needs it is enabled.

| Plugin                           | Applied when                                          |
|----------------------------------|-------------------------------------------------------|
| `dev.detekt`                     | `project { detekt { enabled = true } }`                |
| `org.jetbrains.kotlinx.kover`    | `project { coverage { enabled = true } }`              |
| `org.jetbrains.kotlinx.benchmark`| `project { benchmark { enabled = true } }`             |
| `maven-publish`                  | `project { publish { enabled = true } }`               |
| `com.vanniktech.maven.publish`   | `publish { repositories { mavenCentral { enabled = true } } }` |
| `org.jetbrains.dokka`            | `project { docs { enabled = true } }`                  |
| `org.jetbrains.kotlin.plugin.allopen` | `project { benchmark { enabled = true } }` — JMH subclasses every `@State` class |
| `org.jetbrains.kotlin.plugin.serialization` | `project { applySerializationPlugin = true }` |

Coverage aggregation additionally applies Kover to each project it aggregates: a project cannot
contribute coverage without the plugin, so naming it in `aggregate { }` and applying the plugin
there are the same decision. See [](Coverage-Aggregation.md).

A project that enables nothing gets nothing. `./gradlew tasks` on a bare %product% project shows
no new tasks at all.

## Why the Kotlin plugin is yours

Which Kotlin plugin a project applies — `kotlin("jvm")` or `kotlin("multiplatform")` — is the shape
of the project rather than a %product% feature, and its version governs the language your sources
are written in. Neither is something a convention plugin should decide on your behalf, and both
change what the rest of the build means.

%product% reacts to whichever you applied. Every feature that behaves differently on the two
branches on the plugin id, so a multiplatform project gets multiplatform handling without saying
so twice.

## Keeping control of a version

Declare the plugin yourself:

```kotlin
plugins {
    kotlin("jvm") version "2.4.0"
    id("dev.detekt") version "2.0.0-alpha.6"
    id("com.davils.kreate") version "%version%"
}
```

Applying a plugin is idempotent, so %product%'s own application becomes a no-op and the version
you asked for participates in buildscript classpath resolution exactly as it always did. Nothing
else about the feature changes.

<note>
Before 3.0.0, %product% configured these plugins and refused to run unless you had applied them —
the reasoning being that it kept their versions out of %product%'s release cycle. That reasoning
still holds where it matters, and the escape hatch above is what preserves it. What changed is
that you no longer have to take it to get a coverage report.
</note>

## Reaching a plugin's own DSL block

This is the one behavioural difference worth knowing.

Gradle generates the typed accessor for a block like `detekt { }` from the plugins declared in the
`plugins { }` block, before your build script runs. %product% applies its plugins inside
`afterEvaluate` — it cannot do so earlier, because which features are enabled is only known once
your `kreate { }` block has been read. A plugin %product% applied therefore has no accessor:

```kotlin
// Unresolved reference, unless you applied dev.detekt yourself.
detekt {
    baseline = file("config/detekt/baseline.xml")
}
```

Two ways out, in order of preference:

<procedure title="Use the Kreate block" id="use-the-kreate-block">
<step>
Check whether %product% already exposes the setting. Most of what a project configures is
available — <code>config</code>, <code>buildUponDefaultConfig</code>, <code>allRules</code> and
every report for Detekt; the full report, filter, verification and aggregation surface for Kover.
</step>
<step>
Set it in <code>kreate { project { detekt { } } }</code>, which works whoever applied the plugin.
</step>
</procedure>

<procedure title="Apply the plugin yourself" id="apply-the-plugin-yourself">
<step>
Add <code>id("dev.detekt") version "..."</code> to your <code>plugins { }</code> block.
</step>
<step>
Its own DSL block now resolves, and %product% configures the same plugin on top. Anything you set
in the raw block that %product% also sets is overwritten by %product%; anything it does not touch
is yours.
</step>
</procedure>

## Where the plugins come from

%product% carries them as ordinary runtime dependencies, so they are on your buildscript classpath
as soon as %product% is. Two consequences:

<deflist type="wide">
    <def title="Versions are Kreate's unless you say otherwise">
        The versions %product% ships are the ones it is tested against. Declaring a plugin in your
        own <code>plugins { }</code> block with a version overrides that through normal conflict
        resolution.
    </def>
    <def title="kotlinx-benchmark is Plugin Portal only">
        Unlike the others it is not published to Maven Central. A build resolving its buildscript
        classpath through an internal mirror has to account for that — see
        <a href="Benchmark-Overview.md">Benchmarks</a>.
    </def>
</deflist>

## What %product% never applies

<deflist type="wide">
    <def title="The Kotlin plugin">
        Yours, deliberately. See above.
    </def>
    <def title="Repository declarations">
        <code>project { applyDefaultRepositories = true }</code> is opt-in and off by default.
        %product% does not touch dependency resolution unless you ask. See
        <a href="Project-Repositories.md">Repositories</a>.
    </def>
    <def title="Anything for a feature you did not enable">
        There is no "apply everything" mode. The feature switch is the only trigger.
    </def>
</deflist>

<seealso>
    <category ref="start">
        <a href="Getting-Started.md">Getting started</a>
        <a href="Overview.md">Overview</a>
        <a href="Compatibility.md">Compatibility</a>
    </category>
</seealso>
