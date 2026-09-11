# JNI support

<link-summary>
How %product% turns a C++ source tree into a shared library the JVM can load.
</link-summary>

<card-summary>
Generated headers, an automated CMake build against the right JDK, and natives packaged into
your JAR.
</card-summary>

<tldr>
<p><b>Enable</b>: <code>platform { jvm { jni { enabled = true } } }</code></p>
<p><b>Needs</b>: CMake %min_cmake%+ and a C++17 compiler</p>
<p><b>Build</b>: <code>./gradlew kreateJniBuild</code></p>
</tldr>

Writing JNI by hand means keeping two languages in agreement about a set of mangled symbol
names that neither compiler checks. `external fun greet(): String` in Kotlin has to be matched
by `Java_com_example_Native_greet` in C++, and if it is not, everything compiles cleanly and
fails at runtime with `UnsatisfiedLinkError`.

%product% removes that entire class of problem by generating the C++ declarations from your
compiled Kotlin, then building them with CMake against the JDK your build actually targets.

## What you get

<deflist type="medium">
    <def title="Generated headers">
        A header containing the exact declaration for every <code>external</code> function in
        your module — correct mangling, correct types, correct overload disambiguation. Include
        it in your C++ and the compiler verifies your definitions against it. Kotlin has no
        <code>javac -h</code> equivalent, so this is the only automated way to get it.
        See <a href="JNI-Headers.md">Header generation</a>.
    </def>
    <def title="Scaffolding">
        On the first build, a working <code>CMakeLists.txt</code> and a placeholder translation
        unit. After that the CMake project is yours and %product% never rewrites it.
        See <a href="JNI-Scaffolding.md">Scaffolding</a>.
    </def>
    <def title="A correct CMake build">
        The JDK comes from your Gradle toolchain rather than whatever the machine defaults to,
        output paths are pinned so multi-configuration generators behave like single-configuration
        ones, and the full compiler output is surfaced when something fails.
        See <a href="JNI-Build-Pipeline.md">Build pipeline</a>.
    </def>
    <def title="Runtime resolution">
        Test and run tasks get <code>-Djava.library.path</code> pointing at the build output, so
        <code>System.loadLibrary</code> just works locally.
    </def>
    <def title="Distributable artifacts">
        Optionally, the shared library is packaged into your JAR with a generated loader, so a
        consumer of your published artifact needs no native setup at all.
        See <a href="JNI-Packaging.md">Packaging natives</a>.
    </def>
</deflist>

## Prerequisites

<include from="lib.topic" element-id="native-prerequisites"/>

## A complete example

<procedure title="From nothing to a working native call" id="jni-walkthrough">
    <step>
        <p>Enable the feature:</p>
        <code-block lang="kotlin">
<![CDATA[
            kreate {
                platform {
                    jvm {
                        jni {
                            enabled = true
                        }
                    }
                }
            }
]]>
        </code-block>
    </step>
    <step>
        <p>Declare the native function in Kotlin:</p>
        <code-block lang="kotlin">
<![CDATA[
            package com.example

            class Native {
                init {
                    System.loadLibrary("my_module")
                }

                external fun greet(): String
            }
]]>
        </code-block>
    </step>
    <step>
        <p>Generate the header:</p>
        <code-block lang="bash">./gradlew kreateJniHeaders</code-block>
        <p>
            This writes <code>build/generated/jni/include/my_module_jni.h</code> containing the
            declaration you have to implement.
        </p>
    </step>
    <step>
        <p>
            Implement it in <code>jni/my_module/src/my_module.cpp</code>. Include the generated
            header — that is what makes the compiler check you:
        </p>
        <code-block lang="c++">
<![CDATA[
            #include "my_module_jni.h"

            JNIEXPORT jstring JNICALL
            Java_com_example_Native_greet(JNIEnv* env, jobject receiver) {
                return env->NewStringUTF("Hello from C++");
            }
]]>
        </code-block>
    </step>
    <step>
        <p>Build and run:</p>
        <code-block lang="bash">./gradlew build</code-block>
        <p>
            The library is compiled and <code>java.library.path</code> is configured for your
            test and run tasks automatically.
        </p>
    </step>
</procedure>

## Where everything lives

Sources are yours and stay in the source tree. Everything generated goes under `build/`.

```text
my-module/
├── build.gradle.kts
├── src/main/kotlin/               # Kotlin sources
├── jni/                           # ← yours, version controlled
│   └── my_module/
│       ├── CMakeLists.txt         # scaffolded once, then yours
│       ├── include/               # optional, your own headers
│       └── src/
│           └── my_module.cpp
└── build/                         # ← generated, never version controlled
    ├── generated/jni/include/     # generated JNI headers
    ├── generated/jni/kotlin/      # generated loader (if packaging is on)
    └── jni/<os>-<arch>/
        ├── cmake/                 # CMake scratch directory
        └── lib/                   # the shared library
```

<warning>
The build output is scoped by operating system and architecture (<code>linux-x86_64</code>,
<code>macos-aarch64</code>, …). Without that separation, a shared build cache or a checkout mounted
into containers of different architectures would mix incompatible binaries.
</warning>

## Multiplatform projects

The same `platform.jvm.jni` block applies to a Kotlin Multiplatform project. Only the JVM
target's compilation, test, and run tasks are affected — Kotlin/Native and Kotlin/JS targets are
left alone, since a JVM shared library is meaningless to them.

<tip>
For calling native code from Kotlin/Native targets, use
<a href="C-Interoperation-Overview.md">C-interop</a> instead. The two features are independent
and can be enabled in the same module.
</tip>

<seealso>
    <category ref="native">
        <a href="JNI-Configuration.md">Configuration reference</a>
        <a href="JNI-Headers.md">Header generation</a>
        <a href="JNI-Build-Pipeline.md">Build pipeline</a>
        <a href="JNI-Packaging.md">Packaging natives</a>
        <a href="JNI-Troubleshooting.md">Troubleshooting</a>
    </category>
    <category ref="external">
        <a href="https://docs.oracle.com/en/java/javase/21/docs/specs/jni/index.html">JNI specification</a>
        <a href="https://cmake.org/cmake/help/latest/module/FindJNI.html">CMake FindJNI</a>
    </category>
</seealso>
