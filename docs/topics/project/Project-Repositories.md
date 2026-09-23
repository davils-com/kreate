# Repositories and applied plugins

<link-summary>
Why %product% does not add repositories or compiler plugins by default, and how to opt in.
</link-summary>

<card-summary>
Two opt-in conveniences that are off by default — and the reason that matters in an enterprise build.
</card-summary>

<tldr>
<p><code>applyDefaultRepositories</code> — default <code>false</code></p>
<p><code>applySerializationPlugin</code> — default <code>false</code></p>
</tldr>

%product% deliberately does not modify your dependency resolution or apply compiler plugins you
did not ask for. Both behaviours are available, and both are off unless you enable them.

<note>
There is exactly one repository %product% injects without being asked, and it does not contradict
the principle above. The
<a href="Local-Development-Overview.md">local development workflow</a>'s settings plugin adds the
local Maven repository — but only while something is published there, only filtered to the exact
coordinates that publish installed, and only with <code>snapshotsOnly()</code>, so it can never
answer a question a mirror would have answered. It cannot be active in CI at all.
</note>

## Repositories

```kotlin
kreate {
    project {
        applyDefaultRepositories = true
    }
}
```

When enabled, Maven Central, the Gradle Plugin Portal, and Google are added to the project's
repositories.

<warning>
Leave this off in any build that resolves through an internal mirror or proxy. Such builds
normally set <code>repositoriesMode</code> to <code>PREFER_SETTINGS</code> or
<code>FAIL_ON_PROJECT_REPOS</code>, where an injected project repository is at best a warning and
at worst a build failure. The genuinely dangerous case is neither: it is a build that silently
resolves <i>past</i> the mirror, defeating the artifact review the mirror exists to perform.
</warning>

The recommended arrangement is to declare repositories centrally, where Gradle expects them:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        maven("https://artifacts.internal.example.com/maven")
    }
}
```

<tip>
This is worth doing even for small projects. Central declaration is what makes it possible to
answer "where did this dependency come from?" without reading every module's build script.
</tip>

## The serialization plugin

```kotlin
kreate {
    project {
        applySerializationPlugin = true
    }
}
```

When enabled, `org.jetbrains.kotlin.plugin.serialization` is applied.

Prefer applying it yourself where you actually use it:

```kotlin
plugins {
    kotlin("jvm") version "%kotlin_api%"
    kotlin("plugin.serialization") version "%kotlin_api%"
    id("%plugin_id%") version "%version%"
}
```

<note>
A compiler plugin is not free — it participates in every compilation. Applying it to modules that
never serialize anything costs build time for no benefit, which is why this is opt-in rather than
automatic.
</note>

## What %product% never does

<deflist type="wide">
    <def title="Apply Detekt">
        The <a href="Detekt-Overview.md">Detekt integration</a> configures the plugin but does not
        apply it, so its version stays under your control instead of being pinned to %product%'s
        release cycle. It fails with an actionable message if the integration is enabled without
        the plugin.
    </def>
    <def title="Apply the Maven Publish plugin">
        Same reasoning. See <a href="Publishing-Overview.md">Publishing</a>.
    </def>
    <def title="Change dependency versions">
        %product% adds no dependencies to your project and enforces no versions. What it needs, it
        carries on its own classpath.
    </def>
    <def title="Modify dependency resolution">
        No resolution strategies, substitutions, or forced versions.
    </def>
</deflist>

<seealso>
    <category ref="project">
        <a href="Project-Metadata.md">Project metadata</a>
        <a href="Project-Version-Resolution.md">Version resolution</a>
    </category>
    <category ref="external">
        <a href="https://docs.gradle.org/current/userguide/centralizing_repositories_declaration.html">Centralizing repositories declaration</a>
    </category>
</seealso>
