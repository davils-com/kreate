# Examples

## Maven Central Only — Minimal

```kotlin
kreate {
    project {
        name = "MyLibrary"
        description = "A Kotlin Multiplatform library."
        projectGroup = group.toString()

        publish {
            enabled.set(true)
            inceptionYear.set(2026)
            website.set("https://github.com/davils-com/mylib")

            repositories {
                mavenCentral {
                    enabled.set(true)
                }
            }

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
                    }
                }
                scm {
                    url.set("https://github.com/davils-com/mylib")
                    connection.set("scm:git:git://github.com/davils-com/mylib.git")
                    developerConnection.set("scm:git:ssh://git@github.com/davils-com/mylib.git")
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
            enabled.set(true)

            repositories {
                gitlab {
                    enabled.set(true)
                    name.set("GitLabRegistry")
                }
            }

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
                    }
                }
                scm {
                    url.set("https://gitlab.com/davils-com/mylib")
                    connection.set("scm:git:git://gitlab.com/davils-com/mylib.git")
                    developerConnection.set("scm:git:ssh://git@gitlab.com/davils-com/mylib.git")
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
            enabled.set(true)
            inceptionYear.set(2026)
            website.set("https://github.com/davils-com/mylib")

            repositories {
                mavenCentral {
                    enabled.set(true)
                    automaticRelease.set(true)
                    signPublications.set(true)
                }
                gitlab {
                    enabled.set(true)
                    name.set("InternalRegistry")
                }
            }

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

## Manual Release (No Auto-Release)

Upload to the Central Portal staging area and release manually via the web UI.

```kotlin
repositories {
    mavenCentral {
        enabled.set(true)
        automaticRelease.set(false)
        signPublications.set(true)
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
