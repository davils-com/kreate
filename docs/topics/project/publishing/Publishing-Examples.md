# Publishing examples

<link-summary>Worked publishing configuration examples.</link-summary>

<card-summary>Complete publishing setups you can copy.</card-summary>

## Maven Central Only — Minimal

```kotlin
kreate {
    project {
        name = "MyLibrary"
        description = "A Kotlin Multiplatform library."
        projectGroup = group.toString()

        publish {
            enabled = true
            inceptionYear = 2026
            website = "https://github.com/davils-com/mylib"

            repositories {
                mavenCentral {
                    enabled = true
                }
            }

            pom {
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "davils"
                        name = "Davils"
                        email = "contact@davils.com"
                    }
                }
                scm {
                    url = "https://github.com/davils-com/mylib"
                    connection = "scm:git:git://github.com/davils-com/mylib.git"
                    developerConnection = "scm:git:ssh://git@github.com/davils-com/mylib.git"
                }
            }
        }
    }
}
```

Publish with:

```bash
./gradlew publishToMavenCentral
```

## GitLab Package Registry Only

```kotlin
kreate {
    project {
        publish {
            enabled = true

            repositories {
                gitlab {
                    enabled = true
                    name = "GitLabRegistry"
                }
            }

            pom {
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "davils"
                        name = "Davils"
                    }
                }
                scm {
                    url = "https://gitlab.com/davils-com/mylib"
                    connection = "scm:git:git://gitlab.com/davils-com/mylib.git"
                    developerConnection = "scm:git:ssh://git@gitlab.com/davils-com/mylib.git"
                }
            }
        }
    }
}
```

## Both Targets Simultaneously

Both Maven Central and GitLab can be active in the same build. Maven Central is used
for public releases; GitLab is used for internal pre-release distribution.

```kotlin
kreate {
    project {
        publish {
            enabled = true
            inceptionYear = 2026
            website = "https://github.com/davils-com/mylib"

            repositories {
                mavenCentral {
                    enabled = true
                    automaticRelease = true
                    signPublications = true
                }
                gitlab {
                    enabled = true
                    name = "InternalRegistry"
                }
            }

            pom {
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "davils"
                        name = "Davils"
                        email = "contact@davils.com"
                        organization = "Davils"
                        timezone = "Europe/Berlin"
                    }
                }
                scm {
                    url = "https://github.com/davils-com/mylib"
                    connection = "scm:git:git://github.com/davils-com/mylib.git"
                    developerConnection = "scm:git:ssh://git@github.com/davils-com/mylib.git"
                }
                issueManagement {
                    system = "GitHub Issues"
                    url = "https://github.com/davils-com/mylib/issues"
                }
                ciManagement {
                    system = "GitLab CI"
                    url = "https://gitlab.com/davils-com/mylib/-/pipelines"
                }
            }
        }
    }
}
```

## A Maven Repository With Releases and Snapshots

```kotlin
kreate {
    project {
        name = "my-library"
        description = "A Kotlin library published to the company repository"

        publish {
            enabled = true
            website = "https://example.com/my-library"

            repositories {
                maven {
                    name = "Company"
                    releases = "https://repo.example.com/releases"
                    snapshots = "https://repo.example.com/snapshots"
                    usernameEnv = "REPOSITORY_NAME"
                    passwordEnv = "REPOSITORY_SECRET"
                    signPublications = true
                }
            }
        }
    }
}
```

```bash
# Publishes 1.4.0 to releases
VERSION=1.4.0 ./gradlew publishAllPublicationsToCompanyRepository --no-configuration-cache

# Publishes 1.5.0-SNAPSHOT to snapshots
VERSION=1.5.0 KREATE_SNAPSHOT=true ./gradlew publishAllPublicationsToCompanyRepository --no-configuration-cache
```

See [Maven repository](Publishing-Maven-Repository.md) for the routing and the credentials.

## Manual Release (No Auto-Release)

Upload to the Central Portal staging area and release manually via the web UI.

```kotlin
repositories {
    mavenCentral {
        enabled = true
        automaticRelease = false
        signPublications = true
    }
}
```

After `./gradlew publishToMavenCentral`, go to
[central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments)
and click **Publish**.

## GitLab CI Pipeline

Full pipeline that publishes on every tag:

```yaml
stages:
  - publish

publish:maven-central:
  stage: publish
  script:
    - ./gradlew publishToMavenCentral
  only:
    - tags
  variables:
    ORG_GRADLE_PROJECT_mavenCentralUsername: $MAVEN_CENTRAL_USERNAME
    ORG_GRADLE_PROJECT_mavenCentralPassword: $MAVEN_CENTRAL_PASSWORD
    ORG_GRADLE_PROJECT_signingInMemoryKey: $GPG_PRIVATE_KEY
    ORG_GRADLE_PROJECT_signingInMemoryKeyId: $GPG_KEY_ID
    ORG_GRADLE_PROJECT_signingInMemoryKeyPassword: $GPG_KEY_PASSWORD

publish:gitlab:
  stage: publish
  script:
    - ./gradlew publish
  only:
    - tags
```

Store `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `GPG_PRIVATE_KEY`,
`GPG_KEY_ID`, and `GPG_KEY_PASSWORD` as **masked CI/CD variables** in your GitLab
project settings under **Settings → CI/CD → Variables**.

<seealso>
    <category ref="project">
        <a href="Publishing-Overview.md">Overview</a>
    </category>
</seealso>
