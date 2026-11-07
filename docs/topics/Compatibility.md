# Compatibility

<link-summary>
Supported Gradle, JDK, Kotlin, and native toolchain versions, and what is verified in CI.
</link-summary>

<card-summary>
Which versions %product% supports, which are actually tested, and the policy for changing them.
</card-summary>

Every version stated here is asserted by the test suite rather than merely documented. Where a
range is claimed, a build actually runs against both ends of it.

## Supported versions

<table>
    <tr>
        <td>Component</td>
        <td>Minimum</td>
        <td>Verified in CI</td>
        <td>Notes</td>
    </tr>
    <tr>
        <td>Gradle</td>
        <td>%min_gradle%</td>
        <td>%min_gradle% and the current release</td>
        <td>Both are exercised by the full functional suite, not a smoke test.</td>
    </tr>
    <tr>
        <td>JDK (build)</td>
        <td>%min_java%</td>
        <td>%tested_java%</td>
        <td>The JDK that runs Gradle.</td>
    </tr>
    <tr>
        <td>JDK (target)</td>
        <td>17</td>
        <td>Any version your toolchain provides</td>
        <td>Set through <code>platform { javaVersion }</code>.</td>
    </tr>
    <tr>
        <td>Kotlin</td>
        <td>%kotlin_api%</td>
        <td>%kotlin_api%</td>
        <td>The version %product% compiles against; your build may use a newer one.</td>
    </tr>
    <tr>
        <td>Operating system</td>
        <td>—</td>
        <td>Linux, macOS, Windows</td>
        <td>All three run the native tests, not just the JVM ones.</td>
    </tr>
</table>

## Why the plugin targets old versions on purpose

%product% is compiled against Kotlin %kotlin_api%, the version embedded in the *minimum* supported
Gradle, not the newest available, and for Java %min_java%, the JDK that Gradle has to run on.

This is deliberate. A Gradle plugin compiled against a newer API does not fail at build time in
the plugin's own repository; it fails at *runtime*, on a consumer's machine, with a
`NoSuchMethodError` that names an internal Gradle class. Building against the floor of the
supported range is what turns that into a compile error here instead.

<note>
This constrains %product%, not you. Your build is free to use any newer Kotlin or Java version;
the <code>platform { javaVersion }</code> setting controls what <i>your</i> code targets.
</note>

## Native toolchains

Required only for the features that use them.

<deflist type="medium">
    <def title="CMake %min_cmake%+">
        Needed by <a href="JNI-Support.md">JNI</a> and by C/C++
        <a href="C-Interoperation-Overview.md">C-interop</a>. Resolved from the <code>PATH</code>
        first, then from conventional install locations, and finally from an explicit
        <code>cmakeExecutable</code> override.
    </def>
    <def title="A C++17 compiler">
        GCC, Clang, or MSVC. Multi-configuration generators (Visual Studio, Xcode) are supported:
        %product% pins the per-configuration output directories so the artifact lands in the same
        place on every platform.
    </def>
    <def title="Cargo">
        Needed by Rust C-interop. Resolved from the <code>PATH</code> and from the conventional
        rustup location <code>~/.cargo/bin</code> on every platform.
    </def>
    <def title="Trivy">
        Needed by the <a href="Trivy-Overview.md">security scans</a>. The scan tasks fail with an
        actionable message if it is missing rather than silently reporting a clean result.
    </def>
</deflist>

<warning>
A tool that is installed but not on the <code>PATH</code> is the most common cause of a native
build failing only inside an IDE. Gradle daemons started from a desktop launcher do not inherit
your interactive shell's <code>PATH</code>. %product% searches the conventional install
directories for exactly this reason, but an unusual location still needs an explicit override.
</warning>

## Project types

<deflist type="narrow">
    <def title="Kotlin/JVM">
        Fully supported. All <code>platform</code>, <code>project</code>, and
        <code>trivy</code> features apply.
    </def>
    <def title="Kotlin Multiplatform">
        Fully supported. <a href="C-Interoperation-Overview.md">C-interop</a> applies to the
        native targets; <a href="JNI-Support.md">JNI</a>,
        <a href="API-Validation-Overview.md">API validation</a> and the
        <a href="Benchmark-Overview.md">benchmark</a> source set apply to the JVM target only
        and leave the other targets untouched.
    </def>
    <def title="Neither">
        The <code>trivy</code> block still works. It is deliberately independent of the platform
        modules so that any repository can be scanned.
    </def>
</deflist>

## Versioning policy

%product% follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html). For a build plugin,
the public API is larger than just the Kotlin declarations:

<deflist type="medium">
    <def title="The DSL">
        Every property and block inside <code>kreate { }</code>. Guarded by a checked-in binary
        compatibility dump, so a change cannot be merged without being recorded.
    </def>
    <def title="Task names">
        Users type them and pipelines hard-code them. They are collected in one place in the
        source and listed in the <a href="Task-Reference.md">task reference</a>.
    </def>
    <def title="Default values">
        Changing a default silently changes the behaviour of every build that did not set it.
        Defaults are covered by tests for this reason.
    </def>
    <def title="Output locations">
        Where generated sources, headers, reports, and native artifacts are written.
    </def>
</deflist>

Raising a minimum supported version is a major release. Support for a Gradle or JDK version is
never dropped in a minor one.

<seealso>
    <category ref="start">
        <a href="Getting-Started.md">Getting started</a>
        <a href="Task-Reference.md">Task reference</a>
    </category>
    <category ref="external">
        <a href="https://docs.gradle.org/current/userguide/compatibility.html">Gradle compatibility matrix</a>
        <a href="https://kotlinlang.org/docs/gradle-configure-project.html">Kotlin Gradle plugin</a>
    </category>
</seealso>
