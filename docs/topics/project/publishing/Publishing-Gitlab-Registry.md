# GitLab Package Registry

<link-summary>Publishing to the GitLab Package Registry.</link-summary>

<card-summary>CI-token authentication with no manual credential handling.</card-summary>

Kreate supports publishing to the [GitLab Package Registry](https://docs.gitlab.com/ee/user/packages/maven_repository/)
using CI job token authentication.

> Kreate applies the `maven-publish` plugin when publishing is enabled; your `plugins { }` block
> does not need to mention it.
>
{style="warning"}

## How Authentication Works

GitLab CI injects three environment variables automatically into every pipeline job:

| Variable     | Default Env Name | Description                                         |
|--------------|------------------|-----------------------------------------------------|
| CI job token | `CI_JOB_TOKEN`   | Short-lived token valid for the duration of the job |
| Project ID   | `CI_PROJECT_ID`  | Numeric ID of the GitLab project                    |
| API base URL | `CI_API_V4_URL`  | e.g. `https://gitlab.com/api/v4`                    |

Kreate reads these at configuration time. If `CI_JOB_TOKEN` is absent (e.g. during a local
build), Kreate logs a lifecycle message and skips the *repository* registration:
```
No CI job token found in CI_JOB_TOKEN, skipping GitLab publish repository
```


This means the GitLab target is safe to leave enabled in your build file — there is simply
nowhere to upload to when you run outside of a GitLab CI pipeline. The publication itself is
still registered, so `publishToMavenLocal` produces exactly the artifacts a pipeline would
push, which makes it the fastest way to check your coordinates and POM before tagging.

If `CI_JOB_TOKEN` is set but the project id or API URL is not, the build fails immediately
and names the variable that is missing, rather than building a URL containing `null` and
failing later inside the transport layer.

## What Kreate Publishes

`maven-publish` on its own registers no publication, and `publish` is a lifecycle task — a
build with a repository but no publication reports `UP-TO-DATE` and succeeds without
uploading anything. Kreate therefore registers a publication named `maven` for you:

| Project type              | Component      | Contents                                     |
|---------------------------|----------------|----------------------------------------------|
| Kotlin/JVM, Java library  | `java`         | the JAR, a sources JAR, and the POM          |
| `java-platform` (a BOM)   | `javaPlatform` | the POM with its `<dependencyManagement>`    |

The coordinates are `project.group`, the Kreate `project { name }` (falling back to the
Gradle project name), and `project.version`.

> Kreate leaves an existing publication alone. If your build already declares one — including
> the one the Maven Central plugin creates — that publication is used as-is and only the POM
> metadata is applied to it.
>
{style="note"}

## Enabling GitLab Publishing

```kotlin
kreate {
    project {
        publish {
            enabled = true

            repositories {
                gitlab {
                    enabled = true
                    name = "MyRegistry"
                }
            }
        }
    }
}
```

## GitLab Properties

| Property       | Type                | Default                 | Description                                              |
|----------------|---------------------|-------------------------|----------------------------------------------------------|
| `enabled`      | `Property<Boolean>` | `false`                 | Activates the GitLab publish target                      |
| `name`         | `Property<String>`  | `GitlabPackageRegistry` | Logical name of the Maven repository in Gradle           |
| `tokenEnv`     | `Property<String>`  | `CI_JOB_TOKEN`          | Environment variable name for the CI job token           |
| `projectIdEnv` | `Property<String>`  | `CI_PROJECT_ID`         | Environment variable name for the GitLab project ID      |
| `apiUrlEnv`    | `Property<String>`  | `CI_API_V4_URL`         | Environment variable name for the GitLab API v4 base URL |

### Custom Environment Variable Names

If your pipeline uses non-standard variable names, override them:

```kotlin
gitlab {
    enabled = true
    tokenEnv = "CUSTOM_DEPLOY_TOKEN"
    projectIdEnv = "CUSTOM_PROJECT_ID"
    apiUrlEnv = "CUSTOM_API_URL"
}
```

## Gradle Tasks

Kreate registers the repository under the name you set in `name`. Gradle derives the task
names from that, so with `name = "MyRegistry"`:

```bash
./gradlew publishAllPublicationsToMyRegistryRepository
```

Or publish every configured repository at once:

```bash
./gradlew publish
```

To check what would be uploaded without a pipeline, print the graph:

```bash
./gradlew publish --dry-run
```

A `publish` that lists nothing but `:publish` itself is publishing nothing. There should be
a `:publishMavenPublicationTo<Name>Repository` entry per module.

## GitLab CI Pipeline Example

```yaml
publish:
  stage: deploy
  script:
    - ./gradlew publish
  only:
    - tags
```

The `CI_JOB_TOKEN`, `CI_PROJECT_ID`, and `CI_API_V4_URL` variables are injected
automatically by GitLab — no manual configuration in the pipeline file is needed.

## Repository URL Structure

The Maven repository URL is constructed from the GitLab environment variables:
```
${CI_API_V4_URL}/projects/${CI_PROJECT_ID}/packages/maven
```

For example, on GitLab.com with project ID `12345678`:
```
https://gitlab.com/api/v4/projects/12345678/packages/maven
```


<seealso>
    <category ref="project">
        <a href="Publishing-Overview.md">Overview</a>
    </category>
    <category ref="reference">
        <a href="CI-Integration.md">CI integration</a>
    </category>
</seealso>
