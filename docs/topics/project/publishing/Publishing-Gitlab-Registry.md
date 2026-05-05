# GitLab Package Registry

Kreate supports publishing to the [GitLab Package Registry](https://docs.gitlab.com/ee/user/packages/maven_repository/)
using CI job token authentication.

> You must manually apply the `maven-publish` plugin to your project for this to work.
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
build), Kreate logs a lifecycle message and skips the GitLab repository registration entirely:
```
No CI job token found in CI_JOB_TOKEN, skipping GitLab publish repository
```


This means the GitLab target is safe to leave enabled in your build file — it simply does
nothing when run outside of a GitLab CI pipeline.

## Enabling GitLab Publishing

```kotlin
kreate {
    project {
        publish {
            enabled.set(true)

            repositories {
                gitlab {
                    enabled.set(true)
                    name.set("MyRegistry")
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
    enabled.set(true)
    tokenEnv.set("CUSTOM_DEPLOY_TOKEN")
    projectIdEnv.set("CUSTOM_PROJECT_ID")
    apiUrlEnv.set("CUSTOM_API_URL")
}
```

## Gradle Tasks

Kreate registers the repository under the name you set in `name`. Gradle generates a
`publish` task for it:

```bash
./gradlew publishAllPublicationsToMyRegistryRepository
```

Or publish all configured repositories at once:

```bash
./gradlew publish
```

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


## Gradle Tasks {id="gradle-tasks_1"}

Kreate registers the repository under the name you set in `name`. Gradle generates a
`publish` task for it automatically:

```bash
./gradlew publishAllPublicationsToMyRegistryRepository
```

Or publish all configured repositories at once:

```bash
./gradlew publish
```

## GitLab CI Pipeline

The `CI_JOB_TOKEN`, `CI_PROJECT_ID`, and `CI_API_V4_URL` variables are injected
automatically by GitLab — no manual variable configuration in the pipeline file is needed.

```yaml
publish:
  stage: deploy
  script:
    - ./gradlew publish
  only:
    - tags
```