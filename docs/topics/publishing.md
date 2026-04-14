# Publishing

Kreate streamlines the process of publishing your project to GitLab Package Registry or Maven Central (via OSSRH).

## POM Configuration

The `pom` block defines the project metadata required for publishing to any repository.

```kotlin
kreate {
    project {
        publish {
            enabled = true
            inceptionYear = 2026
            website = "https://example.com"

            pom {
                licenses {
                    license {
                        name = "Apache 2.0"
                        url = "https://example.com/LICENSE"
                    }
                }
                developers {
                    developer {
                        id = "jdoe"
                        name = "John Doe"
                        email = "jdoe@example.com"
                    }
                }
                scm {
                    url = "https://github.com/example/my-project"
                }
            }
        }
    }
}
```

## GitLab Package Registry

To publish to GitLab, use the `gitlab` block. It automatically uses environment variables commonly available in GitLab CI/CD.

```kotlin
kreate {
    project {
        publish {
            repositories {
                gitlab {
                    enabled = true
                    name = "GitLab"
                    tokenEnv = "CI_JOB_TOKEN" // Default
                    projectIdEnv = "CI_PROJECT_ID" // Default
                    apiUrlEnv = "CI_API_V4_URL" // Default
                }
            }
        }
    }
}
```

## Maven Central (OSSRH)

To publish to Maven Central, use the `mavenCentral` block. Kreate handles the configuration of the `MavenPublish` plugin and the `Signing` plugin for you.

```kotlin
kreate {
    project {
        publish {
            repositories {
                mavenCentral {
                    enabled = true
                    automaticRelease = true // If true, automatically release from staging
                    signPublications = true // Enable artifact signing (requires GPG setup)
                }
            }
        }
    }
}
```

### Signing Artifacts

When `signPublications` is enabled, Kreate expects the following Gradle properties to be set (typically in `gradle.properties` or as environment variables):
- `signing.keyId`
- `signing.password`
- `signing.secretKeyRingFile` (or `signing.key`)

## Usage

To publish your project, run the standard Gradle `publish` task:

```bash
./gradlew publish
```
