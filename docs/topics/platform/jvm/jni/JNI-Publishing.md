# Publishing native libraries

<link-summary>Publishing a JNI library as one artifact per platform.</link-summary>

<card-summary>What to do when your infrastructure cannot build every platform you want to support.</card-summary>

<tldr>
<p><b>Enable</b>: <code>jni { packaging { publishing { enabled = true } } }</code></p>
<p><b>Publishes</b>: <code>mylib</code> plus <code>mylib-linux-x86_64</code>, one artifact per platform</p>
</tldr>

A JNI library built on one machine contains one machine's binary. Publish that and you have
published a Linux-only library, or a Windows-only one — %product% files the native library under
`native/<os>-<arch>/` inside the JAR, and only the platform the build ran on ends up there.

The usual answer is a fat JAR: a build matrix over Linux, Windows and macOS, whose results are
merged into one artifact carrying every platform. It needs a runner for every operating system you
support.

This page is about the other answer, for when those runners do not exist: **one published artifact
per platform**, and a release publishes whichever platforms it has.

## What changes

```kotlin
kreate {
    platform {
        jvm {
            jni {
                enabled = true

                packaging {
                    enabled = true

                    publishing {
                        enabled = true
                        platforms = listOf("linux-x86_64")
                        stagingDirectory = layout.projectDirectory.dir("natives")
                    }
                }
            }
        }
    }
}
```

Two things follow from `publishing { enabled = true }`:

<deflist type="wide">
    <def title="The main JAR stops carrying natives">
        It holds the JVM classes and nothing else. No consumer receives whichever platform the
        library happened to be built on without asking for it — which is the failure mode this
        mode exists to remove, because it is invisible until someone runs on a different machine.
    </def>
    <def title="Each platform becomes its own artifact id">
        <code>com.example:mylib-linux-x86_64</code> next to <code>com.example:mylib</code>. A
        classifier would have worked too; a separate artifact id is what Maven tooling handles
        without special cases.
    </def>
</deflist>

<note>
Existing builds are unaffected. Without <code>publishing { enabled = true }</code>,
<code>packaging</code> behaves exactly as before and keeps bundling the host platform into the
main JAR.
</note>

## Selecting platforms

`platforms` is **the selection for this release**, not a promise about every version to come.
Publishing a subset is a supported, ordinary state:

```kotlin
publishing {
    enabled = true
    platforms = listOf("linux-x86_64")   // today
}
```

Later, when another platform's binary becomes available, add it. Consumers already using the
library change nothing.

A pipeline can override the selection without a commit:

```bash
./gradlew publish -Pkreate.jni.publishPlatforms=linux-x86_64,linux-aarch64
```

Valid identifiers are `linux-x86_64`, `linux-aarch64`, `windows-x86_64`, `windows-aarch64`, `macos-x86_64` and
`macos-aarch64` — the same strings the generated loader computes at runtime. Anything else fails
during configuration rather than producing an artifact nobody resolves.

### The one thing that fails

`kreateJniVerifyPlatforms` fails when a **selected** platform has no binary anywhere:

```
No native library was found for every platform selected for publishing.

  windows-x86_64 — looked in:
    /project/native/windows-x86_64
```

Leaving a platform out is a decision. Selecting one you cannot deliver is always an accident, and
without this check the release would upload cleanly and fail later inside a consumer's process.

## Getting binaries you cannot build

`stagingDirectory` is how a platform your machine cannot compile joins a release. Lay the files
out by platform:

```
native/
├── linux-x86_64/libmylib.so
├── windows-x86_64/mylib.dll
└── macos-aarch64/libmylib.dylib
```

Anything in the staging directory is published. A staged binary also **wins over one this build
produced** for the same platform, so a pipeline can publish a binary built under controlled
conditions rather than whatever the publishing runner happened to compile.

Where they come from is up to you: a second runner, a mirrored pipeline on another CI provider, a
colleague's machine, or a release archive.

## Releasing from several runners

A release that carries every platform is built where each platform is native and published from one
place. Every runner stages what it built; one publishing job collects the stages and uploads the
library together with every platform artifact in a single Gradle run.

```bash
./gradlew kreateJniStage
```

`kreateJniStage` builds the host library and writes it to
`build/kreate/jni-stage/<artifact>/<os>-<arch>/` of the root project, next to a `native.properties`:

```properties
format=1
platform=windows-x86_64
group=com.example
artifact=mylib
version=1.4.0
revision=3f2c9e0d7b5a41c6e8f0a9b1c2d3e4f5a6b7c8d9
sha256.mylib.dll=6d1b6f6e...
```

The publishing job points the build at the merged stages:

```bash
./gradlew publish \
    -Pkreate.jni.stage=natives \
    -Pkreate.jni.publishPlatforms=linux-x86_64,linux-aarch64,windows-x86_64,windows-aarch64,macos-x86_64,macos-aarch64
```

The path is resolved against the root project. With `-Pkreate.jni.stage` every platform comes from
the stage, including the one the publishing machine runs on: the job neither needs CMake nor builds
anything native.

### What the publishing build checks

Before a platform artifact is packaged, `kreateJniVerifyPlatforms` reads the `native.properties` of
every staged platform and refuses the release when

- the stage was made for another platform, group, artifact or version;
- a listed file is missing, a file is not listed, or a file's SHA-256 differs from the manifest;
- the stage holds a directory, whose content would ship unchecked;
- for a release, the natives were built from another commit than the one being published. A
  snapshot only warns.

A staged platform without a `native.properties`, such as one laid out by hand under
`stagingDirectory`, is still published, with a warning that Kreate cannot tell which build produced
it.

### GitHub Actions

```yaml
jobs:
  natives:
    strategy:
      matrix:
        runner: [ubuntu-24.04, ubuntu-24.04-arm, windows-2025, windows-11-arm, macos-15-intel, macos-15]
    runs-on: ${{ matrix.runner }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 25 }
      - run: ./gradlew kreateJniStage
      - uses: actions/upload-artifact@v4
        with:
          name: natives-${{ matrix.runner }}
          path: build/kreate/jni-stage/

  publish:
    needs: natives
    runs-on: ubuntu-24.04
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 25 }
      - uses: actions/download-artifact@v4
        with:
          pattern: natives-*
          path: natives
          merge-multiple: true
      - run: >
          ./gradlew publish -Pkreate.jni.stage=natives
          -Pkreate.jni.publishPlatforms=linux-x86_64,linux-aarch64,windows-x86_64,windows-aarch64,macos-x86_64,macos-aarch64
```

Every job checks out the same commit, so the revision check holds. On GitLab the same shape is a
`parallel: matrix:` job on runners tagged per platform, whose `build/kreate/jni-stage/` is an
artifact the publish job `needs`.

## How it stays a plain Maven artifact

Nothing about the per-platform layout needs a plugin on the consumer side. A release of `mylib`
publishes seven ordinary coordinates:

```
com.example:mylib:1.4.0                  classes, a normal POM and Gradle module metadata
com.example:mylib-linux-x86_64:1.4.0     native/linux-x86_64/libmylib.so
com.example:mylib-linux-aarch64:1.4.0
com.example:mylib-windows-x86_64:1.4.0   native/windows-x86_64/mylib.dll
com.example:mylib-windows-aarch64:1.4.0
com.example:mylib-macos-x86_64:1.4.0     native/macos-x86_64/libmylib.dylib
com.example:mylib-macos-aarch64:1.4.0
```

<deflist type="wide">
    <def title="Every platform artifact is a regular JAR">
        It has its own POM without dependencies, its own <code>maven-metadata.xml</code>, the
        checksums Gradle writes for every upload, and a <code>.asc</code> signature whenever the
        target signs. Maven, Gradle, sbt and every repository manager treat it like any other JAR.
    </def>
    <def title="The library does not depend on its platforms">
        Its POM names none of them, so no consumer receives six binaries or the wrong one. The
        consumer picks its platform, and at runtime the loader finds
        <code>native/&lt;os&gt;-&lt;arch&gt;/</code> on the classpath, whichever JAR provides it.
    </def>
    <def title="The platforms are uploaded first">
        Every <code>publishKreateNative…</code> upload runs before the upload of the library itself.
        A repository without staging, such as Reposilite, never serves a library whose platform
        artifacts are missing, even when an upload breaks off halfway.
    </def>
    <def title="Signing covers every artifact">
        A target with <code>signPublications = true</code>, and Maven Central, signs each platform
        publication with the same GPG key as the library.
    </def>
</deflist>

## GitLab CI with Linux only

This is a complete pipeline, not a stopgap:

```yaml
publish:
  stage: publish
  rules:
    - if: $CI_COMMIT_TAG
  script:
    - ./gradlew kreateJniBuild
    - ./gradlew kreateJniVerifyPlatforms
    - ./gradlew publish -Pkreate.jni.publishPlatforms=linux-x86_64
```

When binaries for other platforms appear under `native/`, extend the property. Nothing in the
build script changes.

## What consumers write

```kotlin
dependencies {
    implementation("com.example:mylib:1.0.0")
    runtimeOnly("com.example:mylib-linux-x86_64:1.0.0")
}
```

<warning>
<b>Gradle does not choose the platform artifact for you.</b> Variant selection by operating system
relies on the <code>org.gradle.native.*</code> attributes, which only the native plugins set. A
plain <code>java-library</code> consumer resolves its runtime classpath with no OS attribute at
all, so there is nothing for Gradle to match on. The declaration is explicit, or it comes from a
plugin in the <i>consumer's</i> build that detects the OS and assembles the artifact id.
</warning>

A Maven consumer declares the artifact the same way, or lets a profile pick it per machine:

```xml
<dependencies>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>mylib</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>

<profiles>
    <profile>
        <id>linux-x86_64</id>
        <activation><os><family>unix</family><name>Linux</name><arch>amd64</arch></os></activation>
        <dependencies>
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>mylib-linux-x86_64</artifactId>
                <version>1.0.0</version>
                <scope>runtime</scope>
            </dependency>
        </dependencies>
    </profile>
    <!-- one profile per platform: aarch64 on Linux, amd64 and aarch64 on Windows,
         x86_64 and aarch64 on macOS (family "mac") -->
</profiles>
```

A consumer who forgets it gets a message that says exactly what to add:

```
Native library 'mylib' has no packaged copy
at /native/macos-aarch64/libmylib.dylib.
Set -Dkreate.jni.useLibraryPath=true to take it from java.library.path.

Add the platform artifact to your runtime classpath:

    runtimeOnly("com.example:mylib-macos-aarch64:1.0.0")

Platforms published with this version: linux-x86_64
```

The last line is the one that saves an afternoon: it separates "you forgot the dependency" from
"this version does not ship that platform".

## Maven Central

Central rejects any artifact published without a sources and a javadoc JAR. A resource-only
artifact has neither to give, so %product% attaches empty ones to each platform publication when
`mavenCentral` is enabled. Signing is handled by `signAllPublications()`, which covers every
publication in the project, the platform publications included. Central receives all of them in one
deployment.

The GitLab Package Registry asks for none of this, and the empty JARs are not produced for it.

## Two limits worth knowing

<deflist type="medium">
    <def title="glibc and musl look identical">
        <code>linux-x86_64</code> says nothing about the C library. A binary linked against glibc
        fails at runtime on Alpine. If your consumers run containers, this is the most likely bug
        report you will get, and the platform identifier cannot express the difference.
    </def>
    <def title="Extraction needs a writable, executable temp directory">
        The loader extracts the library and calls <code>System.load</code>. On a hardened
        container with <code>noexec</code> on <code>/tmp</code> that fails even though the file is
        present. Affected deployments can pre-install the library, keep the platform artifact off
        the classpath, and set <code>java.library.path</code> together with
        <code>-Dkreate.jni.useLibraryPath=true</code>.
    </def>
</deflist>

<seealso>
    <category ref="native">
        <a href="JNI-Packaging.md">Packaging</a>
        <a href="JNI-Build-Pipeline.md">Build pipeline</a>
        <a href="JNI-Troubleshooting.md">Troubleshooting</a>
    </category>
    <category ref="project">
        <a href="Publishing-Overview.md">Publishing</a>
    </category>
</seealso>
