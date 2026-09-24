/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kreate

import com.davils.kreate.module.local.LocalTaskNames
import com.davils.kreate.system.platformTaskSuffix

/**
 * The names of every task Kreate registers.
 *
 * Task names are part of the plugin's public contract — users type them, and CI pipelines
 * hard-code them. Collecting them in one place is what keeps that contract reviewable, and
 * what made it possible to notice that the 1.x names followed three different conventions
 * at once (`kreate-jni-build`, `kreate-c-interop-compile`, `trivyScan`).
 *
 * All names follow the same scheme in 2.0.0: the `kreate` prefix, then the feature, then
 * the action, in camel case.
 *
 * @since 2.0.0
 */
public object KreateTasks {
    /**
     * The task group shared by every Kreate task, used as a prefix for feature groups.
     *
     * @since 2.0.0
     */
    public const val GROUP: String = "kreate"

    /**
     * Names of the JNI pipeline tasks.
     *
     * @since 2.0.0
     */
    public object Jni {
        /**
         * Scaffolds the native C++ project. Was `kreate-jni-initialize` in 1.x.
         * @since 2.0.0
         */
        public const val INITIALIZE: String = "kreateJniInitialize"

        /**
         * Generates JNI headers from the compiled classes. New in 2.0.0.
         * @since 2.0.0
         */
        public const val HEADERS: String = "kreateJniHeaders"

        /**
         * Runs the CMake configure step. New in 2.0.0; was part of `kreate-jni-build`.
         * @since 2.0.0
         */
        public const val CONFIGURE: String = "kreateJniConfigure"

        /**
         * Builds the native library. Was `kreate-jni-build` in 1.x.
         * @since 2.0.0
         */
        public const val BUILD: String = "kreateJniBuild"

        /**
         * Generates the runtime loader for packaged natives. New in 2.0.0.
         * @since 2.0.0
         */
        public const val LOADER: String = "kreateJniLoader"

        /**
         * Writes the SHA-256 of every packaged native library as a `.properties` manifest.
         *
         * @since 3.0.0
         */
        public const val DIGEST_MANIFEST: String = "kreateJniDigestManifest"

        /**
         * Builds the per-platform native JARs that are published alongside the library.
         * New in 2.2.0.
         * @since 2.2.0
         */
        public const val NATIVE_JARS: String = "kreateJniNativeJars"

        /**
         * Checks that every selected platform actually has a binary to publish. New in 2.2.0.
         * @since 2.2.0
         */
        public const val VERIFY_PLATFORMS: String = "kreateJniVerifyPlatforms"

        /**
         * The task group for JNI tasks.
         * @since 2.0.0
         */
        public const val GROUP: String = "kreate jni"

        /**
         * Returns the name of the native JAR task for the given platform.
         *
         * Unlike the other names in this object, this one is parameterised: there is one task
         * per platform a release publishes, and the set is chosen by the consumer.
         *
         * @param platformId The platform identifier, for example `linux-x86_64`.
         * @return The task name, for example `kreateJniNativeJarLinuxX86_64`.
         * @since 2.2.0
         */
        public fun nativeJar(platformId: String): String =
            "kreateJniNativeJar" + platformTaskSuffix(platformId)
    }

    /**
     * Names of the C-interop pipeline tasks.
     *
     * @since 2.0.0
     */
    public object CInterop {
        /**
         * Scaffolds the native project. Was `kreate-c-interop-initialize` in 1.x.
         * @since 2.0.0
         */
        public const val INITIALIZE: String = "kreateCInteropInitialize"

        /**
         * Adds the Rust dependencies. Was `kreate-c-interop-dependencies` in 1.x.
         * @since 2.0.0
         */
        public const val DEPENDENCIES: String = "kreateCInteropDependencies"

        /**
         * Configures Cargo. Was `kreate-c-interop-configure` in 1.x.
         * @since 2.0.0
         */
        public const val CONFIGURE: String = "kreateCInteropConfigure"

        /**
         * Generates the Rust build script. Was `kreate-c-interop-script` in 1.x.
         * @since 2.0.0
         */
        public const val SCRIPT: String = "kreateCInteropScript"

        /**
         * Compiles the native sources. Was `kreate-c-interop-compile` in 1.x.
         * @since 2.0.0
         */
        public const val COMPILE: String = "kreateCInteropCompile"

        /**
         * Generates the `.def` files. Was `kreate-c-interop-definitions` in 1.x.
         * @since 2.0.0
         */
        public const val DEFINITIONS: String = "kreateCInteropDefinitions"

        /**
         * The task group for C-interop tasks.
         * @since 2.0.0
         */
        public const val GROUP: String = "kreate c-interop"
    }

    /**
     * Names of the Trivy scan tasks.
     *
     * @since 2.0.0
     */
    public object Trivy {
        /**
         * Runs every enabled scan. Was `trivyScan` in 1.x.
         * @since 2.0.0
         */
        public const val SCAN: String = "kreateTrivyScan"

        /**
         * Scans for hard-coded secrets. Was `trivySecretScan` in 1.x.
         * @since 2.0.0
         */
        public const val SECRETS: String = "kreateTrivySecretScan"

        /**
         * Scans dependency licences. Was `trivyLicenseScan` in 1.x.
         * @since 2.0.0
         */
        public const val LICENSES: String = "kreateTrivyLicenseScan"

        /**
         * Scans dependencies for known vulnerabilities. Was `trivyVulnerabilityScan` in 1.x.
         * @since 2.0.0
         */
        public const val VULNERABILITIES: String = "kreateTrivyVulnerabilityScan"

        /**
         * The task group for Trivy tasks.
         * @since 2.0.0
         */
        public const val GROUP: String = "kreate trivy"
    }

    /**
     * Names of the binary compatibility validation tasks.
     *
     * @since 2.1.0
     */
    public object ApiValidation {
        /**
         * Records the public binary interface in the checked-in `.api` dump. New in 2.1.0.
         * @since 2.1.0
         */
        public const val DUMP: String = "kreateApiDump"

        /**
         * Verifies the public binary interface against the dump. New in 2.1.0.
         * @since 2.1.0
         */
        public const val CHECK: String = "kreateApiCheck"

        /**
         * The task group for binary compatibility validation tasks.
         * @since 2.1.0
         */
        public const val GROUP: String = "kreate api"
    }

    /**
     * Names of the configuration schema tasks.
     *
     * The same pair as [ApiValidation] and for the same reason: a configuration schema is a promise
     * to every document already written against it, exactly as a published signature is a promise to
     * everything compiled against one. `DUMP` and `CHECK` must never run in one invocation - they
     * read and write the same files, and Gradle refuses the implicit dependency.
     *
     * @since 3.1.0
     */
    public object ConfigurationSchema {
        /**
         * Records every declared schema in its checked-in JSON Schema export. New in 3.1.0.
         * @since 3.1.0
         */
        public const val DUMP: String = "kreateConfigSchemaDump"

        /**
         * Verifies every declared schema against its export, failing on a breaking change. New in
         * 3.1.0.
         * @since 3.1.0
         */
        public const val CHECK: String = "kreateConfigSchemaCheck"

        /**
         * Reports what loading this repository's own configuration files would do. New in 3.1.0.
         * @since 3.1.0
         */
        public const val VALIDATE: String = "kreateConfigValidate"

        /**
         * The task group for configuration schema tasks.
         * @since 3.1.0
         */
        public const val GROUP: String = "kreate configuration"
    }

    /**
     * Names of the benchmark tasks.
     *
     * None of these is wired into `check` or `build`. A benchmark run takes minutes, and
     * attaching it to the ordinary build is the surest way to get it switched off.
     *
     * @since 2.2.0
     */
    public object Benchmark {
        /**
         * Copies the newest benchmark report to a stable location. New in 2.2.0.
         * @since 2.2.0
         */
        public const val REPORT: String = "kreateBenchmarkReport"

        /**
         * Records the current results as the committed baseline. New in 2.2.0.
         * @since 2.2.0
         */
        public const val BASELINE: String = "kreateBenchmarkBaseline"

        /**
         * Compares the current results against the baseline. New in 2.2.0.
         * @since 2.2.0
         */
        public const val CHECK: String = "kreateBenchmarkCheck"

        /**
         * The task group for benchmark tasks.
         * @since 2.2.0
         */
        public const val GROUP: String = "kreate benchmark"
    }

    /**
     * Names of the dependency locking tasks.
     *
     * @since 2.1.0
     */
    public object DependencyLocking {
        /**
         * Resolves every locked classpath so that `--write-locks` records all of them.
         * New in 2.1.0.
         * @since 2.1.0
         */
        public const val RESOLVE_AND_LOCK_ALL: String = "kreateResolveAndLockAll"

        /**
         * The task group for dependency locking tasks.
         * @since 2.1.0
         */
        public const val GROUP: String = "kreate locking"
    }

    /**
     * Task names and the group for the local development workflow.
     *
     * These are the tasks that let a fix in one library be tried in another without a release.
     * Before 3.2.0 the only way to do that was to tag a version and wait for a pipeline to push
     * it to a registry, which made the cost of trying a one line change the same as the cost of
     * shipping one.
     *
     * @since 3.2.0
     */
    public object Local {
        /**
         * Installs this build into the local Maven repository at a snapshot version and records
         * it, so that consumers substitute it in.
         *
         * @since 3.2.0
         */
        public const val PUBLISH: String = LocalTaskNames.PUBLISH

        /**
         * Runs [PUBLISH] across a declared workspace in dependency order.
         *
         * @since 3.2.0
         */
        public const val PUBLISH_ALL: String = LocalTaskNames.PUBLISH_ALL

        /**
         * Reports what is currently published locally, or why local mode is off.
         *
         * @since 3.2.0
         */
        public const val STATUS: String = LocalTaskNames.STATUS

        /**
         * Removes the local publications and the state that records them.
         *
         * @since 3.2.0
         */
        public const val CLEAN: String = LocalTaskNames.CLEAN

        /**
         * The task group for local development tasks.
         * @since 3.2.0
         */
        public const val GROUP: String = "kreate local"
    }

    /**
     * Task names and the group for static analysis.
     *
     * @since 3.0.0
     */
    public object Detekt {
        /**
         * Runs Detekt over every source set that has sources.
         *
         * Detekt registers one task per source set, and its own aggregate `detekt` task analyses
         * **nothing** on a multiplatform project: every file belongs to a source set, so the
         * aggregate has no sources of its own and passes without reading a line. A CI job that ran
         * it would be green and blind.
         *
         * Until 3.0.0 the answer was for every project to spell the per-source-set task names out
         * in its pipeline definition - a list that had to be revisited whenever a target or a module
         * of a new kind was added, and whose failure mode when somebody forgot was silence. This
         * task is that list, computed.
         *
         * @since 3.0.0
         */
        public const val ANALYSE: String = "kreateDetekt"

        /**
         * The group the analysis task is filed under.
         *
         * @since 3.0.0
         */
        public const val GROUP: String = "kreate detekt"
    }

    /**
     * Name of the build constants generation task.
     *
     * Was `kreate-build-constants` in 1.x.
     *
     * @since 2.0.0
     */
    public const val BUILD_CONSTANTS: String = "kreateBuildConstants"

    /**
     * The task group for build constant tasks.
     *
     * @since 2.0.0
     */
    public const val BUILD_CONSTANTS_GROUP: String = "kreate build-constants"

    /**
     * Names of the test suite tasks.
     *
     * These are the only names in this object without a `kreate` prefix, and the only tasks
     * Kreate puts in Gradle's verification group rather than a `kreate ...` one. A test suite
     * is not a Kreate feature: it is the project's own test task under a more honest name, and
     * it belongs where a developer and a CI pipeline already look for it.
     *
     * @since 3.0.0
     */
    public object Tests {
        /**
         * Name of the suite for fast, hermetic tests, registered by default.
         *
         * @since 3.0.0
         */
        public const val UNIT: String = "unitTest"

        /**
         * Name of the suite for tests that reach outside the process, registered by default.
         *
         * @since 3.0.0
         */
        public const val INTEGRATION: String = "integrationTest"

        /**
         * Name of the conventional test task the suites replace.
         *
         * @since 3.0.0
         */
        public const val LEGACY: String = "test"
    }
}
