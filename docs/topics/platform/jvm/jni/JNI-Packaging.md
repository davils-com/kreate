# Packaging natives

<link-summary>
Shipping the shared library inside your JAR so consumers need no native setup.
</link-summary>

<card-summary>
Put the .so, .dylib, or .dll into your artifact and let the generated loader extract it at runtime.
</card-summary>

<tldr>
<p><b>Enable</b>: <code>jni { packaging { enabled = true } }</code></p>
<p><b>Result</b>: <code>native/&lt;os&gt;-&lt;arch&gt;/libfoo.so</code> inside your JAR</p>
<p><b>Default</b>: off</p>
</tldr>

## The problem

A JAR containing native code is not usable on its own. `System.loadLibrary` only resolves against
`java.library.path`, which a consumer of your artifact has no reason to have configured. Locally
your build sets it for you; a downstream project gets:

```
java.lang.UnsatisfiedLinkError: no my_module in java.library.path
```

Packaging closes that gap. The library travels inside the JAR, and a generated loader extracts it
on first use.

## Enabling it

```kotlin
kreate {
    platform {
        jvm {
            jni {
                enabled = true

                packaging {
                    enabled = true
                    resourcePath = "native"
                    digestManifest = true
                }
            }
        }
    }
}
```

<deflist type="medium">
    <def title="enabled">
        Whether the built library is added to the JAR. Defaults to <code>false</code>, so
        upgrading %product% never changes the contents of an existing artifact.
    </def>
    <def title="resourcePath">
        The directory inside the JAR. Defaults to <code>native</code>; the
        <code>&lt;os&gt;-&lt;arch&gt;</code> segment is appended automatically. It was
        <code>natives</code> before 3.0.0, which no loader ever looked in.
    </def>
    <def title="digestManifest">
        Whether <code>digests.properties</code> is written beside the libraries. Defaults to
        <code>true</code>. See <a href="#the-digest-manifest">The digest manifest</a>.
    </def>
    <def title="generateLoader">
        Whether <code>KreateNativeLoader</code> is generated into your sources. Defaults to
        <code>false</code> since 3.0.0 — see <a href="#loading-the-library">Loading the library</a>.
    </def>
</deflist>

## What ends up in the JAR

```text
my-module-1.0.0.jar
├── com/example/Native.class
└── native/
    ├── digests.properties
    └── linux-x86_64/
        └── libmy_module.so
```

Verify it with:

```bash
./gradlew jar
unzip -l build/libs/my-module-1.0.0.jar | grep native
```

## The platform directory

`<os>-<arch>` is not a name %product% is free to choose. It is the directory a loader looks in at
runtime, and the loader a Davils program runs resolves it through
`com.davils.arc.platform.Platform.identifier`:

| | %product% writes | A loader reads |
|---|---|---|
| 64-bit x86 | `linux-x86_64` | `linux-x86_64` |
| 64-bit ARM | `macos-aarch64` | `macos-aarch64` |

Before 3.0.0 %product% wrote `linux-x64` and `macos-arm64`, and the root was `natives` rather than
`native`. Both halves published cleanly and the binary was never found, which is the failure the
closed platform vocabulary exists to prevent — made against %product% itself.

**If you published with 2.3.x or earlier**, consumers pinned to those artifacts are looking in the
old directory. Either republish, or set `resourcePath = "natives"` and keep the old layout until
they move.

## The digest manifest

Packaging writes `native/digests.properties`:

```properties
# Written by Kreate. Do not edit - it is rewritten on every build.
linux-x86_64=9f2c4d1e8a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3d2e1f0a9b8c7d6e5f4a3b2c1d
```

It is **the value a consumer pins**, so that nobody has to hash a release artifact by hand:

```kotlin
val library = nativeLibrary("my_module") {
    expect("linux-x86_64", sha256 = "9f2c…")
}
```

It is **not a check that runs itself**, and `com.davils:sira-native` deliberately does not read it
out of the JAR it is checking. A manifest that travels in the same artifact as the binary was
written by whoever wrote the binary: it catches a truncated download and says nothing whatsoever
about tampering. The digest only means something once it has reached the consumer by a path the
publisher does not control.

## Loading the library

**Use `com.davils:sira-native`.** It is the loader this packaging is built for: it searches an
operator's override, `java.library.path`, a mount and the classpath in a declared order, checks the
binary against a digest you pinned, extracts it into a directory only you can write to, and reports
every place it looked rather than the last thing that failed.

```kotlin
private val module = nativeLibrary("my_module")

suspend fun main() {
    module.prepare()
}

internal object Native {
    init {
        module.link()
    }

    external fun greet(): String
}
```

### The generated loader

`generateLoader = true` still writes a `KreateNativeLoader` into your sources. It defaults to
**off** since 3.0.0: it is the fallback for an artifact that must load with nothing on the classpath
but itself, and having it on by default made the weakest available loader the one most builds got.

What it does not do:

- it checks no digest, so it cannot tell your binary from one somebody replaced;
- it extracts into `Files.createTempDirectory`, which on a shared machine is readable by every
  other user and writable by them too;
- it marks the copy `deleteOnExit`, so every run extracts again;
- it reports the first thing that went wrong rather than every place it looked.

Replace `System.loadLibrary` with the generated loader:

```kotlin
package com.example

import com.example.my_module.jni.KreateNativeLoader

class Native {
    init {
        KreateNativeLoader.load("my_module")
    }

    external fun greet(): String
}
```

The loader tries `System.loadLibrary` **first**. That ordering matters: during local development
your library path is already configured, and loading the freshly built binary is exactly what you
want. Only when that fails does it fall back to extracting the packaged copy into a temporary
directory and loading it from there.

<deflist type="wide">
    <def title="Why generated source rather than a runtime library">
        A separate <code>kreate-jni-runtime</code> artifact would have to be version-matched
        against the plugin by every consumer, and would add a compile dependency to a project
        that otherwise needs none. Generating the loader avoids both.
    </def>
    <def title="Idempotent">
        Repeated calls for the same library are ignored, so putting the call in an
        <code>init</code> block of a class instantiated many times costs nothing.
    </def>
</deflist>

## Supporting several platforms

The `<os>-<arch>` segment means one JAR can carry binaries for several platforms. A single build
produces the binary for the machine it ran on, so multi-platform artifacts are assembled by
building on each platform and merging the results.

<procedure title="Build a multi-platform artifact in CI" id="multiplatform-natives">
    <step>
        <p>Build on each target platform and publish the native output as a build artifact:</p>
        <code-block lang="yaml">
<![CDATA[
            strategy:
              matrix:
                os: [ubuntu-latest, macos-latest, windows-latest]
            steps:
              - run: ./gradlew kreateJniBuild
              - uses: actions/upload-artifact@v7
                with:
                  name: natives-${{ matrix.os }}
                  path: my-module/build/jni/
]]>
        </code-block>
    </step>
    <step>
        <p>
            In a job that depends on all three, download every artifact back into
            <code>build/jni/</code>. Because each platform writes to its own
            <code>&lt;os&gt;-&lt;arch&gt;</code> directory, they merge without collisions.
        </p>
    </step>
    <step>
        <p>
            Build the JAR. Every present platform directory is packaged, and the loader picks the
            matching one at runtime.
        </p>
        <code-block lang="bash">./gradlew jar</code-block>
    </step>
</procedure>

<warning>
The loader throws <code>UnsatisfiedLinkError</code> when the running platform has no packaged
binary and none is on the library path. If you publish an artifact built on one platform only,
say so in your documentation — a consumer on a different architecture will otherwise discover it
at runtime.
</warning>

## Extraction behaviour

<deflist type="medium">
    <def title="Where">
        A temporary directory created per JVM process.
    </def>
    <def title="Cleanup">
        Both the file and its directory are registered for deletion on JVM exit.
    </def>
    <def title="Cost">
        Once per library per process. Subsequent calls return immediately.
    </def>
</deflist>

<tip>
On a read-only or <code>noexec</code> temporary filesystem — some hardened containers — extraction
cannot work. In that environment, ship the library separately and set
<code>java.library.path</code>, which the loader tries first anyway.
</tip>

## One JAR holds one platform

Packaging places the library of the machine that ran the build, and only that one. A JAR built on
Linux works on Linux. That is fine for an application, and not fine for a library you publish —
see [publishing native libraries](JNI-Publishing.md) for the two ways out and which one fits when
you cannot build every platform.

<seealso>
    <category ref="native">
        <a href="JNI-Support.md">JNI support</a>
        <a href="JNI-Publishing.md">Publishing native libraries</a>
        <a href="JNI-Configuration.md">Configuration reference</a>
        <a href="JNI-Troubleshooting.md">Troubleshooting</a>
    </category>
    <category ref="project">
        <a href="Publishing-Overview.md">Publishing</a>
    </category>
</seealso>
