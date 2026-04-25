# Maven Central

Kreate uses the [Gradle Maven Publish Plugin by Vanniktech](https://vanniktech.github.io/gradle-maven-publish-plugin/central/)
to publish to Maven Central. When enabled, Kreate applies `MavenPublishBasePlugin` automatically
and configures coordinates, signing, and POM metadata from your `kreate { }` block — no manual
`mavenPublishing { }` block is required.

## Prerequisites

Before publishing to Maven Central, you need:

1. A **Central Portal account** at [central.sonatype.com](https://central.sonatype.com)
2. A **registered namespace** (your `group`, e.g. `com.example`) verified in the portal
3. A **GPG key pair** for signing artifacts

Create a GPG key pair and distribute the public key to a keyserver:

```bash
gpg --gen-key
gpg --keyserver keyserver.ubuntu.com --send-keys <keyId>
```

## Enabling Maven Central

Calling `repositories { mavenCentral { } }` automatically sets `enabled = true` for the
Maven Central target:

```kotlin
kreate {
    project {
        publish {
            enabled.set(true)

            repositories {
                mavenCentral {
                    // automaticRelease and signPublications default to true
                }
            }
        }
    }
}
```

## Maven Central Properties

| Property           | Type                | Default | Description                                                                                                             |
|--------------------|---------------------|---------|-------------------------------------------------------------------------------------------------------------------------|
| `enabled`          | `Property<Boolean>` | `false` | Activates the Maven Central publish target (set automatically when using the `mavenCentral { }` block)                  |
| `automaticRelease` | `Property<Boolean>` | `true`  | When `true`, runs `publishAndReleaseToMavenCentral`; when `false`, only uploads — manual release required on the portal |
| `signPublications` | `Property<Boolean>` | `true`  | Calls `signAllPublications()` — required by Maven Central for releases                                                  |

### `automaticRelease`

When `true`, Kreate passes `automaticRelease = true` to `publishToMavenCentral()`. The
Vanniktech plugin then:

1. Uploads the artifacts to a new deployment
2. Polls the deployment status every 5 seconds until it reaches `VALIDATED` or `FAILED`
3. Automatically releases the deployment to Maven Central

The validation timeout is 60 minutes by default. After a successful release, artifacts
become available in Maven Central within 10–30 minutes.

When `false`, the plugin only uploads the deployment. Go to
[Deployments on the Central Portal](https://central.sonatype.com/publishing/deployments)
and click **Publish** manually afterward.

### `signPublications`

Signing is a hard requirement for Maven Central releases. Kreate calls `signAllPublications()`
when this is `true`. Set it to `false` only for snapshot publishing where signing is not required.

## Gradle Tasks

| Task                              | Description                                                            |
|-----------------------------------|------------------------------------------------------------------------|
| `publishToMavenCentral`           | Uploads artifacts; releases automatically if `automaticRelease = true` |
| `publishAndReleaseToMavenCentral` | Always uploads and releases, ignoring the `automaticRelease` setting   |

```bash
./gradlew publishToMavenCentral
```

## Credentials and Signing

> Never commit credentials to version control. Use `~/.gradle/gradle.properties` locally
> and environment variables in CI.
>
{style="warning"}

### Local (`~/.gradle/gradle.properties`)

```properties
mavenCentralUsername=your_token_username
mavenCentralPassword=your_token_password

signing.keyId=12345678
signing.password=your_gpg_passphrase
signing.secretKeyRingFile=/Users/you/.gnupg/secring.gpg
```

> `mavenCentralUsername` and `mavenCentralPassword` are **not** your portal login
> credentials. Generate dedicated user tokens at
> [central.sonatype.com → Account → User Token](https://central.sonatype.com/account).
>
{style="note"}

### CI — In-Memory GPG Signing

For CI environments, use in-memory signing to avoid placing private key files on the build
agent. The Vanniktech plugin reads the private key directly from a Gradle property.

**Step 1 — Export only the Base64 key content:**

```bash
gpg --export-secret-keys --armor <keyId> \
  | grep -v '\-\-\-\-\-' \
  | grep -v '^=' \
  | grep -v '^$' \
  | tr -d '\n'
```

This strips the `-----BEGIN PGP PRIVATE KEY BLOCK-----` / `-----END PGP PRIVATE KEY BLOCK-----`
headers, the `=checksum` trailer line, and all blank lines, leaving only the raw Base64 content.

> Do **not** include the `-----BEGIN`/`-----END` separator lines or the `=checksum` line.
> `signingInMemoryKey` expects the raw Base64 block only.
>
{style="warning"}

**Step 2 — Store as a masked CI/CD variable:**

Store the single-line Base64 output as `GPG_PRIVATE_KEY` in your pipeline settings under
**Settings → CI/CD → Variables**. Enable **Masked** to prevent it from appearing in logs.

**Step 3 — Set Gradle properties in the pipeline:**

```bash
ORG_GRADLE_PROJECT_mavenCentralUsername=$MAVEN_CENTRAL_USERNAME
ORG_GRADLE_PROJECT_mavenCentralPassword=$MAVEN_CENTRAL_PASSWORD

ORG_GRADLE_PROJECT_signingInMemoryKey=$GPG_PRIVATE_KEY
ORG_GRADLE_PROJECT_signingInMemoryKeyId=12345678        # optional
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=$GPG_KEY_PASSWORD
```

`signingInMemoryKeyId` is optional — only needed when the keyring contains multiple
keys and you want to target a specific one.

## Coordinates

Kreate derives the Maven coordinates automatically from your `kreate { project { } }` block:

| Coordinate   | Source                    | Fallback               |
|--------------|---------------------------|------------------------|
| `group`      | `projectGroup`            | Gradle `project.group` |
| `artifactId` | `name`                    | Gradle `project.name`  |
| `version`    | `version` Gradle property | —                      |

## Snapshot Publishing

Set the project version to end in `-SNAPSHOT` and run:

```bash
./gradlew publishToMavenCentral
```

Snapshots are available immediately in the Central Portal snapshot repository after the task
completes. Signing is not required for snapshots — but if credentials are present, the
artifacts will still be signed.