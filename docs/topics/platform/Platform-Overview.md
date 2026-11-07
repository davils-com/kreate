# Platform configuration

<link-summary>
What the platform block controls, and how it adapts to JVM and Multiplatform projects.
</link-summary>

<card-summary>
Toolchains, compiler policy, and the two native bridges — all under one block.
</card-summary>

The `platform` block covers everything about *how your code is compiled*: the Java toolchain, the
Kotlin compiler settings, and the bridges to native code.

```kotlin
kreate {
    platform {
        javaVersion = JavaVersion.VERSION_25
        explicitApi = true
        allWarningsAsErrors = true

        jvm {
            jni { /* C/C++ on the JVM */ }
        }

        multiplatform {
            cInterop { /* Rust, C, C++ on Kotlin/Native */ }
        }
    }
}
```

## Detection, not guessing

%product% reacts to the Kotlin plugin you applied rather than inspecting your source layout:

<deflist type="medium">
    <def title="org.jetbrains.kotlin.jvm">
        Configures the Kotlin JVM extension and the Java plugin, and enables the
        <a href="JNI-Support.md">JNI</a> path.
    </def>
    <def title="org.jetbrains.kotlin.multiplatform">
        Configures the multiplatform extension, enables
        <a href="C-Interoperation-Overview.md">C-interop</a> for native targets, and applies JNI
        to the JVM target only.
    </def>
    <def title="Neither">
        The <code>platform</code> block does nothing. The <code>trivy</code> block still works.
    </def>
</deflist>

## Settings

<table>
    <tr><td>Property</td><td>Default</td><td>Topic</td></tr>
    <tr>
        <td><code>javaVersion</code></td>
        <td><code>JavaVersion.VERSION_25</code></td>
        <td><a href="Platform-Java-Version.md">Java version</a></td>
    </tr>
    <tr>
        <td><code>explicitApi</code></td>
        <td><code>false</code></td>
        <td><a href="Platform-Explicit-API.md">Explicit API mode</a></td>
    </tr>
    <tr>
        <td><code>allWarningsAsErrors</code></td>
        <td><code>true</code></td>
        <td><a href="Platform-Warning-As-Errors.md">Warnings as errors</a></td>
    </tr>
</table>

<tip>
The Java version is applied to the Kotlin toolchain <i>and</i> to
<code>sourceCompatibility</code>/<code>targetCompatibility</code> together. Setting them
separately is one of the most common sources of "works on my machine" bytecode mismatches.
</tip>

## The two native bridges

They are not alternatives — they solve different problems and can be used in the same module.

<table>
    <tr><td></td><td>JNI</td><td>C-interop</td></tr>
    <tr><td>Runs on</td><td>The JVM</td><td>Kotlin/Native</td></tr>
    <tr><td>Languages</td><td>C, C++</td><td>Rust, C, C++</td></tr>
    <tr><td>Build tool</td><td>CMake</td><td>Cargo or CMake</td></tr>
    <tr><td>Library type</td><td>Shared</td><td>Static</td></tr>
    <tr><td>Binding</td><td>Generated JNI headers</td><td>Generated <code>.def</code> files</td></tr>
    <tr><td>Called from</td><td><code>external fun</code></td><td>The generated cinterop package</td></tr>
</table>

<seealso>
    <category ref="platform">
        <a href="Platform-Java-Version.md">Java version</a>
        <a href="Platform-Explicit-API.md">Explicit API mode</a>
        <a href="Platform-Warning-As-Errors.md">Warnings as errors</a>
    </category>
    <category ref="native">
        <a href="JNI-Support.md">JNI support</a>
        <a href="C-Interoperation-Overview.md">C-interop</a>
    </category>
</seealso>
