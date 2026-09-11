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

A consumer who forgets it gets a message that says exactly what to add:

```
Native library 'mylib' is not on java.library.path and no packaged copy was found
at /native/macos-aarch64/libmylib.dylib.

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
publication in the project.

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
        present. The loader tries <code>System.loadLibrary</code> first, so affected deployments
        can pre-install the library and set <code>java.library.path</code>.
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
