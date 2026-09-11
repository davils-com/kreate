# Task reference

<link-summary>
Every task %product% registers: what enables it, what it consumes, and what it produces.
</link-summary>

<card-summary>
A complete index of %product%'s tasks, their inputs and outputs, and their caching behaviour.
</card-summary>

Task names are part of %product%'s public contract. They all follow one scheme: the `kreate`
prefix, then the feature, then the action, in camel case. The test suites are the one exception,
and deliberately so — see below.

<tip>
List what is actually registered in your project with
<code>./gradlew tasks --group kreate</code>. A task you do not see is a feature you have not
enabled — nothing is registered speculatively.
</tip>

## JNI

Registered when `platform.jvm.jni.enabled` is `true`. See [JNI build pipeline](JNI-Build-Pipeline.md)
for how they fit together.

<table>
    <tr>
        <td>Task</td>
        <td>Purpose</td>
        <td>Inputs</td>
        <td>Outputs</td>
    </tr>
    <tr>
        <td><code>kreateJniInitialize</code></td>
        <td>Scaffolds the native project on first run.</td>
        <td>Project name</td>
        <td><code>jni/&lt;name&gt;/</code></td>
    </tr>
    <tr>
        <td><code>kreateJniHeaders</code></td>
        <td>Generates JNI declarations from compiled <code>native</code> methods.</td>
        <td>Compiled main classes</td>
        <td><code>build/generated/jni/include/</code></td>
    </tr>
    <tr>
        <td><code>kreateJniConfigure</code></td>
        <td>Runs the CMake configure step.</td>
        <td><code>CMakeLists.txt</code>, include paths, JDK, absolute paths</td>
        <td><code>build/jni/&lt;os&gt;-&lt;arch&gt;/cmake/CMakeCache.txt</code></td>
    </tr>
    <tr>
        <td><code>kreateJniBuild</code></td>
        <td>Compiles and links the shared library.</td>
        <td>Native sources, generated headers, CMake cache</td>
        <td><code>build/jni/&lt;os&gt;-&lt;arch&gt;/lib/</code></td>
    </tr>
    <tr>
        <td><code>kreateJniLoader</code></td>
        <td>Generates the runtime loader for packaged natives.</td>
        <td>Package name, resource path</td>
        <td><code>build/generated/jni/kotlin/</code></td>
    </tr>
    <tr>
        <td><code>kreateJniVerifyPlatforms</code></td>
        <td>
            Fails when a platform selected for publishing has no binary. Registered only with
            <a href="JNI-Publishing.md">per-platform publishing</a>.
        </td>
        <td>Selected platforms, staged and built libraries</td>
        <td>—</td>
    </tr>
    <tr>
        <td><code>kreateJniNativeJar&lt;Platform&gt;</code></td>
        <td>
            Packages one platform's library, for example <code>kreateJniNativeJarLinuxX64</code>.
            One task per selected platform.
        </td>
        <td>Staged or built library for that platform</td>
        <td><code>build/libs/&lt;name&gt;-&lt;platform&gt;.jar</code></td>
    </tr>
    <tr>
        <td><code>kreateJniNativeJars</code></td>
        <td>Lifecycle task over every selected platform.</td>
        <td>—</td>
        <td>—</td>
    </tr>
</table>

<note>
The JNI pipeline runs <b>after</b> Kotlin compilation, not before it. The headers are derived
from the compiled <code>external</code> declarations, so the ordering is a requirement rather
than a preference — and it keeps a native build off the critical path of every Kotlin compile.
Test and run tasks depend on <code>kreateJniBuild</code>, which is where the library is actually
needed.
</note>

## C-interop

Registered when `platform.multiplatform.cInterop.enabled` is `true`. Which tasks exist depends on
the configured `language`.

<table>
    <tr>
        <td>Task</td>
        <td>Languages</td>
        <td>Purpose</td>
    </tr>
    <tr>
        <td><code>kreateCInteropInitialize</code></td>
        <td>All</td>
        <td>Scaffolds the Cargo or CMake project.</td>
    </tr>
    <tr>
        <td><code>kreateCInteropDependencies</code></td>
        <td>Rust</td>
        <td>Adds the required crates to <code>Cargo.toml</code>.</td>
    </tr>
    <tr>
        <td><code>kreateCInteropConfigure</code></td>
        <td>Rust</td>
        <td>Configures the Cargo build for the target list.</td>
    </tr>
    <tr>
        <td><code>kreateCInteropScript</code></td>
        <td>Rust</td>
        <td>Generates <code>build.rs</code> for header generation.</td>
    </tr>
    <tr>
        <td><code>kreateCInteropCompile</code></td>
        <td>All</td>
        <td>Builds the native library for every configured target.</td>
    </tr>
    <tr>
        <td><code>kreateCInteropDefinitions</code></td>
        <td>All</td>
        <td>Writes the <code>.def</code> files Kotlin/Native consumes.</td>
    </tr>
</table>

## Security and compliance

Registered when `trivy.enabled` is `true`.

<table>
    <tr>
        <td>Task</td>
        <td>Purpose</td>
        <td>Consumes</td>
    </tr>
    <tr>
        <td><code>kreateTrivyScan</code></td>
        <td>Aggregate — runs all three scans below.</td>
        <td>—</td>
    </tr>
    <tr>
        <td><code>kreateTrivyVulnerabilityScan</code></td>
        <td>Finds known CVEs in dependencies.</td>
        <td>Dependency lock files</td>
    </tr>
    <tr>
        <td><code>kreateTrivyLicenseScan</code></td>
        <td>Checks licence compliance.</td>
        <td>Dependency lock files</td>
    </tr>
    <tr>
        <td><code>kreateTrivySecretScan</code></td>
        <td>Finds hard-coded credentials.</td>
        <td>Source files, <code>trivy-secret.yaml</code></td>
    </tr>
</table>

<warning>
The vulnerability and licence scans read Gradle dependency lock files. Without them the scans
have nothing to inspect. Enable Kreate's locking feature with
<code>project { dependencyLocking { enabled = true } }</code> and write the lock files with
<code>./gradlew kreateResolveAndLockAll --write-locks</code>. See
<a href="Dependency-Locking.md">Dependency locking</a>.
</warning>

## API validation

Registered when `project.apiValidation.enabled` is `true`. See
[Binary compatibility validation](API-Validation-Overview.md).

<table>
    <tr>
        <td>Task</td>
        <td>Purpose</td>
        <td>Outputs</td>
    </tr>
    <tr>
        <td><code>kreateApiDump</code></td>
        <td>Records the public binary interface. Commit the result.</td>
        <td><code>api/&lt;project&gt;.api</code></td>
    </tr>
    <tr>
        <td><code>kreateApiCheck</code></td>
        <td>Fails when the interface no longer matches the dump. Runs as part of <code>check</code>.</td>
        <td><code>build/kreate/api/&lt;project&gt;.api</code></td>
    </tr>
</table>

## Benchmarks

Registered when `project.benchmark.enabled` is `true`. None of these is wired into `check`
or `build`. See [Benchmarks](Benchmark-Overview.md).

<table>
    <tr>
        <td>Task</td>
        <td>Purpose</td>
        <td>Outputs</td>
    </tr>
    <tr>
        <td><code>kreateBenchmarkReport</code></td>
        <td>Copies the newest kotlinx-benchmark run to a path other tasks can depend on.</td>
        <td><code>build/reports/kreate/benchmark/&lt;profile&gt;/</code></td>
    </tr>
    <tr>
        <td><code>kreateBenchmarkBaseline</code></td>
        <td>Records the current results as the baseline. Commit the file.</td>
        <td><code>benchmarks/baseline.json</code></td>
    </tr>
    <tr>
        <td><code>kreateBenchmarkCheck</code></td>
        <td>Runs the benchmarks and fails when one got measurably slower.</td>
        <td><code>build/reports/kreate/benchmark/comparison.md</code></td>
    </tr>
</table>

## Dependency locking

Registered when `project.dependencyLocking.enabled` is `true`. See
[Dependency locking](Dependency-Locking.md).

<table>
    <tr>
        <td>Task</td>
        <td>Purpose</td>
        <td>Outputs</td>
    </tr>
    <tr>
        <td><code>kreateResolveAndLockAll</code></td>
        <td>
            Resolves every locked classpath so that <code>--write-locks</code> records all of
            them. Requires <code>--write-locks</code> and is not compatible with the
            configuration cache.
        </td>
        <td><code>gradle.lockfile</code></td>
    </tr>
</table>

## Build constants

<table>
    <tr>
        <td>Task</td>
        <td>Purpose</td>
        <td>Outputs</td>
    </tr>
    <tr>
        <td><code>kreateBuildConstants</code></td>
        <td>Generates a Kotlin object from the configured constants.</td>
        <td><code>build/&lt;path&gt;/&lt;package&gt;/&lt;ClassName&gt;.kt</code></td>
    </tr>
</table>

Registered when `project.buildConstant.enabled` is `true`, and wired to run before Kotlin
compilation so the generated code is always available to your sources.

## Test suites

<table>
    <tr>
        <td>Task</td>
        <td>Purpose</td>
        <td>On <code>check</code></td>
    </tr>
    <tr>
        <td><code>unitTest</code></td>
        <td>Runs the tests in <code>src/unitTest/kotlin</code>.</td>
        <td>yes</td>
    </tr>
    <tr>
        <td><code>integrationTest</code></td>
        <td>Runs the tests in <code>src/integrationTest/kotlin</code>, ordered after <code>unitTest</code>.</td>
        <td>no</td>
    </tr>
    <tr>
        <td><code>&lt;suite&gt;</code></td>
        <td>One task per registered suite, named after it.</td>
        <td>per <code>runOnCheck</code></td>
    </tr>
    <tr>
        <td><code>&lt;target&gt;&lt;Suite&gt;</code></td>
        <td>Multiplatform: one task per JVM target, for example <code>jvmUnitTest</code>. The suite's own task runs all of them.</td>
        <td>per <code>runOnCheck</code></td>
    </tr>
</table>

Registered when `project.tests.enabled` is `true`. These carry no `kreate` prefix and sit in
Gradle's `verification` group rather than a `kreate` one, because a suite is not a %product%
feature: it is the project's own test task under a more honest name, and it belongs where a
developer and a CI pipeline already look for it. See [](Testing-Suites.md).

### Inputs and outputs

<deflist type="wide">
    <def title="Inputs">
        The suite's compiled classes and its runtime classpath, plus every execution setting —
        tag filters, system properties, environment variables and JVM arguments — as task inputs.
        Changing any of them re-runs the task.
    </def>
    <def title="Outputs">
        <code>build/test-results/&lt;suite&gt;/*.xml</code> when XML reporting is on, and
        <code>build/reports/tests/&lt;suite&gt;/index.html</code> when HTML reporting is on.
    </def>
    <def title="Up-to-date">
        Like any Gradle test task. Set <code>alwaysRun = true</code> on the suite, or
        <code>alwaysRunTests = true</code> on the block, to opt out of the check.
    </def>
</deflist>

### Ordering

`mustRunAfterSuites` produces ordering constraints, not dependencies. `integrationTest` runs after
`unitTest` when both are in the build, and asking for either alone pulls in only that one. On a
multiplatform project the constraint is applied per target as well, so `jvmIntegrationTest` runs
after `jvmUnitTest` even when the aggregate tasks are not requested.

### The conventional test task

Disabled and removed from `check` unless `project.tests.legacyTestSourceSet` says otherwise — see
[](Testing-Suites-Migration.md). On a multiplatform project the per-target tasks are disabled but
the `check` → `allTests` edge survives, because the Kotlin plugin's aggregate report has no removal
API; the tasks are reported as `SKIPPED`.

## Tasks %product% configures but does not register

Some integrations configure someone else's tasks rather than adding their own. %product% applies
those plugins for you — see [](Plugin-Management.md) — but the tasks are theirs, and so is their
contract. These names carry no `kreate` prefix.

<table>
    <tr>
        <td>Task</td>
        <td>Registered by</td>
        <td>Purpose</td>
    </tr>
    <tr>
        <td><code>detekt</code></td>
        <td><code>dev.detekt</code></td>
        <td>Static analysis. See <a href="Detekt-Overview.md">Detekt</a>.</td>
    </tr>
    <tr>
        <td><code>koverHtmlReport</code></td>
        <td><code>org.jetbrains.kotlinx.kover</code></td>
        <td>Browsable coverage report.</td>
    </tr>
    <tr>
        <td><code>koverXmlReport</code></td>
        <td><code>org.jetbrains.kotlinx.kover</code></td>
        <td>JaCoCo-format XML, for CI and dashboards.</td>
    </tr>
    <tr>
        <td><code>koverLog</code></td>
        <td><code>org.jetbrains.kotlinx.kover</code></td>
        <td>Prints the coverage summary a CI system parses.</td>
    </tr>
    <tr>
        <td><code>koverBinaryReport</code></td>
        <td><code>org.jetbrains.kotlinx.kover</code></td>
        <td>Intermediate coverage data for the command line tooling.</td>
    </tr>
    <tr>
        <td><code>koverVerify</code></td>
        <td><code>org.jetbrains.kotlinx.kover</code></td>
        <td>
            Fails the build below the configured bounds. Runs as part of <code>check</code>.
            See <a href="Coverage-Verification.md">Verification</a>.
        </td>
    </tr>
</table>

## Task groups

Tasks are grouped so that `./gradlew tasks` stays readable.

| Group | Contains |
|---|---|
| `kreate jni` | The JNI pipeline |
| `kreate c-interop` | The C-interop pipeline |
| `kreate trivy` | The security scans |
| `kreate build-constants` | Constant generation |
| `verification` | The test suites, beside Gradle's own `test` and `check` |

## Caching and up-to-date behaviour

<deflist type="wide">
    <def title="Cacheable">
        <code>kreateJniHeaders</code> and <code>kreateJniLoader</code> produce output that
        depends only on their declared inputs, so they are safe to share through the build cache.
    </def>
    <def title="Up-to-date checked, not cached">
        The native build tasks. Their output is tied to the local toolchain and to absolute
        paths, which makes it correct to reuse in place but wrong to relocate. They will skip
        when nothing changed, and re-run when a source, header, or the configuration does.
    </def>
    <def title="Never up to date">
        The Trivy scans. A dependency that was clean yesterday can have a CVE today, so a result
        cached against unchanged inputs would be actively misleading.
    </def>
</deflist>

<include from="lib.topic" element-id="config-cache-note"></include>

<seealso>
    <category ref="reference">
        <a href="Compatibility.md">Compatibility</a>
        <a href="CI-Integration.md">CI integration</a>
    </category>
    <category ref="native">
        <a href="JNI-Build-Pipeline.md">JNI build pipeline</a>
        <a href="C-Interoperation-Gradle-Task.md">C-interop tasks</a>
    </category>
</seealso>
