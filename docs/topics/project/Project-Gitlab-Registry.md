# Resolving from a GitLab registry

<link-summary>Declaring a GitLab Package Registry to resolve dependencies from.</link-summary>

<card-summary>The token and header combination GitLab wants, assembled for you.</card-summary>

<tldr>
<p><b>Use</b>: <code>repositories { gitlabPackageRegistry(providers) { url = "…" } }</code></p>
<p><b>Works in</b>: <code>settings.gradle.kts</code> and <code>build.gradle.kts</code></p>
</tldr>

GitLab's Maven endpoint does not accept the username and password pair Gradle sends by default. It
wants a token in a header — and the header's *name* depends on which kind of token it is. Get the
combination wrong and you get a `401` that says nothing about why.

`gitlabPackageRegistry` assembles the combination that works.

## Declaring it

```kotlin
// settings.gradle.kts
import com.davils.kreate.publish.gitlabPackageRegistry

dependencyResolutionManagement {
    repositories {
        mavenCentral()

        gitlabPackageRegistry(providers) {
            url = "https://gitlab.example.com/api/v4/groups/42/-/packages/maven"
            content { includeGroup("com.example") }
        }
    }
}
```

<note>
<code>providers</code> is passed in because the helper reads a Gradle property, and a property —
unlike an environment variable — is only reachable through the provider factory of the surrounding
<code>Settings</code> or <code>Project</code>. Both expose it under that name, which is what lets
the same call work in either file.
</note>

### On the settings class path

Kreate is a project plugin and is not loaded yet when a settings file runs, so declaring the
registry there needs it on the settings class path:

```kotlin
// settings.gradle.kts
import com.davils.kreate.publish.gitlabPackageRegistry

buildscript {
    repositories { mavenCentral() }
    dependencies { classpath("com.davils:kreate:%version%") }
}

dependencyResolutionManagement { … }
```

In a build script no such block is needed — applying the plugin is enough.

## Which token is used

<deflist type="wide">
    <def title="CI_JOB_TOKEN, sent as Job-Token">
        Checked first. GitLab injects it into every pipeline job, it is scoped to that pipeline
        and expires with it — so nothing long-lived has to be stored anywhere for CI to resolve
        dependencies.
    </def>
    <def title="The gitlabToken Gradle property, sent as Private-Token">
        Checked next. This is the credential a developer keeps in
        <code>~/.gradle/gradle.properties</code>, which is outside the repository and therefore
        cannot be committed by accident.
    </def>
    <def title="The GITLAB_TOKEN environment variable, sent as Private-Token">
        The last fallback, for shells and containers that carry credentials in the environment.
    </def>
</deflist>

## Configuration

### `url`
- **Required.** The Maven endpoint. GitLab exposes three levels:

| Level    | Endpoint                                                   |
|----------|------------------------------------------------------------|
| Group    | `https://<host>/api/v4/groups/<id>/-/packages/maven`         |
| Project  | `https://<host>/api/v4/projects/<id>/packages/maven`         |
| Instance | `https://<host>/api/v4/packages/maven`                       |

Declaring it without a URL fails during configuration with a message showing the shapes.

### `name`
- **Default**: `GitlabPackageRegistry`. The name Gradle uses in error messages. Worth changing when
  a build resolves from more than one GitLab instance — the name is what tells two `401`s apart.

### `tokenProperty` / `tokenVariable` / `jobTokenVariable`
- **Defaults**: `gitlabToken`, `GITLAB_TOKEN`, `CI_JOB_TOKEN`.

### `tokenHeader`
- **Default**: `Private-Token`. A **deploy token** needs `Deploy-Token` instead — they are
  different credentials and GitLab rejects one sent under the other's header.

```kotlin
gitlabPackageRegistry(providers) {
    url = "https://gitlab.example.com/api/v4/projects/7/packages/maven"
    name = "InternalTooling"
    tokenProperty = "internalGitlabToken"
    tokenHeader = "Deploy-Token"
}
```

### `content`

Passes straight through to Gradle's repository content filtering.

<warning>
<b>Set a content filter.</b> Without one, Gradle asks the registry about <i>every</i> dependency in
the build. That is slow over an authenticated remote, and it tells whoever operates the registry
the name of every artifact you depend on — including the ones that come from somewhere else
entirely.
</warning>

```kotlin
gitlabPackageRegistry(providers) {
    url = "…"
    content {
        includeGroup("com.example")
        includeGroupByRegex("com\\.example\\..*")
    }
}
```

## When no token is available

The repository is still declared, with an empty credential. Resolution then fails with a `401`
from GitLab rather than at configuration time — deliberately, so that a task needing nothing from
the registry still runs on a machine that has no token, such as a fresh clone or an offline build.

If a build resolves from the registry and the token is missing, the `401` is the symptom; the
cause is an unset `gitlabToken` property or `GITLAB_TOKEN` variable.

## Publishing is separate

This page is about *resolving*. Publishing to a GitLab registry is configured through
`kreate { project { publish { repositories { gitlab { } } } } }` — see
[GitLab Package Registry](Publishing-Gitlab-Registry.md).

<seealso>
    <category ref="project">
        <a href="Project-Repositories.md">Repositories</a>
        <a href="Publishing-Gitlab-Registry.md">Publishing to GitLab</a>
    </category>
</seealso>
