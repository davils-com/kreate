# POM Configuration

The `pom { }` block inside `publish { }` configures the Maven POM metadata that is embedded
in every published artifact. This metadata is required by Maven Central and used by dependency
management tools to display project information. Kreate applies the same POM configuration to
both the Maven Central and GitLab targets.

## POM Structure

```kotlin
kreate {
    project {
        publish {
            enabled.set(true)
            inceptionYear.set(2024)
            website.set("https://github.com/davils-com/mylib")

            pom {
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("davils")
                        name.set("Davils")
                        email.set("contact@davils.com")
                        organization.set("Davils")
                        timezone.set("Europe/Berlin")
                    }
                }
                scm {
                    url.set("https://github.com/davils-com/mylib")
                    connection.set("scm:git:git://github.com/davils-com/mylib.git")
                    developerConnection.set("scm:git:ssh://git@github.com/davils-com/mylib.git")
                }
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/davils-com/mylib/issues")
                }
                ciManagement {
                    system.set("GitLab CI")
                    url.set("https://gitlab.com/davils-com/mylib/-/pipelines")
                }
            }
        }
    }
}
```

## `licenses { license { } }`

Declares the license under which the library is distributed. Maven Central requires at
least one license entry.

| Property       | Type               | Description                                                 |
|----------------|--------------------|-------------------------------------------------------------|
| `name`         | `Property<String>` | Full license name, e.g. `"The Apache License, Version 2.0"` |
| `url`          | `Property<String>` | URL to the license text                                     |
| `distribution` | `Property<String>` | How the artifact is distributed; typically `"repo"`         |

Common license values:

| License    | Name                              | URL                                               |
|------------|-----------------------------------|---------------------------------------------------|
| Apache 2.0 | `The Apache License, Version 2.0` | `https://www.apache.org/licenses/LICENSE-2.0.txt` |
| MIT        | `MIT License`                     | `https://opensource.org/licenses/MIT`             |
| GPL 3.0    | `GNU General Public License v3.0` | `https://www.gnu.org/licenses/gpl-3.0.txt`        |

## `developers { developer { } }`

Declares the people responsible for the project. At least one developer entry is expected
by Maven Central.

| Property       | Type               | Description                               |
|----------------|--------------------|-------------------------------------------|
| `id`           | `Property<String>` | Unique identifier, e.g. a GitHub username |
| `name`         | `Property<String>` | Full display name                         |
| `email`        | `Property<String>` | Contact email address                     |
| `organization` | `Property<String>` | Organization or company name              |
| `timezone`     | `Property<String>` | IANA timezone, e.g. `"Europe/Berlin"`     |

All developer properties are optional. Kreate only sets those that are present — absent
properties are simply omitted from the generated POM.

## `scm { }`

The Source Control Management block describes where the source code lives. Maven Central
requires SCM information for release artifacts.

| Property              | Type               | Description                                                     |
|-----------------------|--------------------|-----------------------------------------------------------------|
| `url`                 | `Property<String>` | Web URL of the repository (e.g. GitHub page)                    |
| `connection`          | `Property<String>` | Read-only SCM URL: `scm:git:git://github.com/org/repo.git`      |
| `developerConnection` | `Property<String>` | Read-write SCM URL: `scm:git:ssh://git@github.com/org/repo.git` |

For GitHub repositories, the standard values are:

```kotlin
scm {
    url.set("https://github.com/<org>/<repo>")
    connection.set("scm:git:git://github.com/<org>/<repo>.git")
    developerConnection.set("scm:git:ssh://git@github.com/<org>/<repo>.git")
}
```

## `issueManagement { }`

Points consumers to where they can file bugs or feature requests.

| Property | Type               | Description                                       |
|----------|--------------------|---------------------------------------------------|
| `system` | `Property<String>` | Name of the issue tracker, e.g. `"GitHub Issues"` |
| `url`    | `Property<String>` | Direct URL to the issue tracker                   |

## `ciManagement { }`

Documents the CI system used to build and verify the project.

| Property | Type               | Description                                                     |
|----------|--------------------|-----------------------------------------------------------------|
| `system` | `Property<String>` | Name of the CI system, e.g. `"GitLab CI"` or `"GitHub Actions"` |
| `url`    | `Property<String>` | URL to the CI pipeline overview                                 |

Both `issueManagement` and `ciManagement` are optional from Maven Central's perspective,
but including them improves discoverability and transparency for library consumers.

> Kreate omits any POM section whose properties are all absent — it never writes empty
> `<issueManagement/>` or `<ciManagement/>` tags into the published POM.
>
{style="tip"}