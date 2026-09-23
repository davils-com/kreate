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

package com.davils.kreate.module.local

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for locating and inspecting the local Maven repository.
 *
 * Resolution has to agree with Gradle's own `mavenLocal()`. A build that publishes into one
 * directory and resolves out of another produces no error at all — it simply never picks the fix
 * up, which is indistinguishable from the feature being broken.
 */
@DisplayName("local Maven repository")
class MavenLocalTest {

    @TempDir
    lateinit var home: File

    private fun artifact(repository: File, path: String) {
        File(repository, path).apply {
            mkdirs()
            File(this, "${name.substringBeforeLast('/')}.pom").writeText("<project/>")
        }
    }

    @Nested
    @DisplayName("resolution order")
    inner class Resolution {

        @Test
        @DisplayName("prefers the maven.repo.local system property")
        fun systemProperty() {
            val configured = File(home, "explicit")

            resolveMavenLocal(
                mapOf(MAVEN_REPO_LOCAL_PROPERTY to configured.absolutePath),
                home
            ) shouldBe configured.absoluteFile
        }

        @Test
        @DisplayName("falls back to localRepository in settings.xml")
        fun settingsXml() {
            val configured = File(home, "from-settings")
            File(home, ".m2").mkdirs()
            File(home, ".m2/settings.xml").writeText(
                """
                    <settings>
                        <localRepository>${configured.absolutePath}</localRepository>
                    </settings>
                """.trimIndent()
            )

            resolveMavenLocal(emptyMap(), home) shouldBe configured.absoluteFile
        }

        @Test
        @DisplayName("expands a leading tilde in settings.xml")
        fun tildeExpansion() {
            File(home, ".m2").mkdirs()
            File(home, ".m2/settings.xml").writeText(
                "<settings><localRepository>~/custom-repo</localRepository></settings>"
            )

            resolveMavenLocal(emptyMap(), home) shouldBe File(home, "custom-repo").absoluteFile
        }

        @Test
        @DisplayName("falls back to ~/.m2/repository when nothing is configured")
        fun default() {
            resolveMavenLocal(emptyMap(), home) shouldBe File(home, ".m2/repository")
        }

        @Test
        @DisplayName("falls back rather than failing on a malformed settings.xml")
        fun malformedSettings() {
            File(home, ".m2").mkdirs()
            File(home, ".m2/settings.xml").writeText("<settings><localRepository>")

            // Maven's settings are not Kreate's to validate. Refusing to configure a build
            // because of a file this feature merely consults would be an overreach.
            resolveMavenLocal(emptyMap(), home) shouldBe File(home, ".m2/repository")
        }
    }

    @Nested
    @DisplayName("locating a module")
    inner class Layout {

        @Test
        @DisplayName("maps a group onto its directory path")
        fun groupPath() {
            groupDirectory(home, "com.example") shouldBe File(home, "com/example")
        }

        @Test
        @DisplayName("maps a module version onto its directory")
        fun modulePath() {
            moduleDirectory(home, LocalModule("com.example", "library-core"), "1.1.0-SNAPSHOT") shouldBe
                File(home, "com/example/library-core/1.1.0-SNAPSHOT")
        }
    }

    @Nested
    @DisplayName("verifying a recorded library")
    inner class Verification {

        private fun library(vararg modules: String) = LocalLibrary(
            library = "library",
            group = "com.example",
            repository = File("/workspace/library"),
            version = "1.1.0-SNAPSHOT",
            publishedAt = null,
            kreateVersion = "3.2.0",
            modules = modules.map { LocalModule("com.example", it) }
        )

        @Test
        @DisplayName("reports nothing missing when every coordinate is installed")
        fun allPresent() {
            File(home, "com/example/library-core/1.1.0-SNAPSHOT").mkdirs()

            missingArtifacts(home, library("library-core")) shouldBe emptyList()
        }

        @Test
        @DisplayName("names the coordinates that were removed after the record was written")
        fun someMissing() {
            File(home, "com/example/library-core/1.1.0-SNAPSHOT").mkdirs()

            missingArtifacts(home, library("library-core", "library-retry")) shouldContainExactly
                listOf("com.example:library-retry:1.1.0-SNAPSHOT")
        }
    }

    @Nested
    @DisplayName("finding snapshots to purge")
    inner class Purging {

        @Test
        @DisplayName("finds a version directory nested below a same-named group")
        fun nestedGroup() {
            // A Gradle plugin is exactly this shape: `com.example:tool` sits at
            // com/example/tool/<v> while the marker `com.example.tool:com.example.tool.gradle.plugin`
            // sits one level further down, because the group is a prefix of the artifact. A depth
            // limited walk would leave the marker behind, pinning a plugin version whose artefact
            // is gone.
            artifact(home, "com/example/tool/3.2.0-SNAPSHOT")
            artifact(home, "com/example/tool/com.example.tool.gradle.plugin/3.2.0-SNAPSHOT")

            // `invariantSeparatorsPath`, not `path`: the latter is `\` separated on Windows and
            // the assertion would compare a real result against a Unix-shaped literal.
            val found = snapshotDirectories(home, "com.example")
                .map { it.relativeTo(home).invariantSeparatorsPath }

            found shouldContainExactly listOf(
                "com/example/tool/3.2.0-SNAPSHOT",
                "com/example/tool/com.example.tool.gradle.plugin/3.2.0-SNAPSHOT"
            )
        }

        @Test
        @DisplayName("leaves releases alone, because something else put them there")
        fun ignoresReleases() {
            artifact(home, "com/example/extra/3.0.0")
            artifact(home, "com/example/extra/3.0.0-SNAPSHOT")

            snapshotDirectories(home, "com.example").map { it.name } shouldContainExactly
                listOf("3.0.0-SNAPSHOT")
        }

        @Test
        @DisplayName("ignores an empty directory that merely looks like a version")
        fun ignoresEmptyDirectories() {
            File(home, "com/example/weird-SNAPSHOT").mkdirs()

            snapshotDirectories(home, "com.example") shouldBe emptyList()
        }

        @Test
        @DisplayName("returns nothing for a group that was never published")
        fun unknownGroup() {
            snapshotDirectories(home, "com.example") shouldBe emptyList()
        }
    }

    @Nested
    @DisplayName("deriving a local version")
    inner class SnapshotVersions {

        @Test
        @DisplayName("appends the suffix to a release version")
        fun appends() {
            snapshotVersionOf("3.0.0") shouldBe "3.0.0-SNAPSHOT"
        }

        @Test
        @DisplayName("is idempotent, so a snapshot does not grow a second suffix")
        fun idempotent() {
            snapshotVersionOf("3.0.0-SNAPSHOT") shouldBe "3.0.0-SNAPSHOT"
        }

        @Test
        @DisplayName("refuses a version that was never resolved")
        fun refusesUnspecified() {
            // `unspecified-SNAPSHOT` would publish and resolve perfectly well, and mean nothing.
            shouldThrow<IllegalArgumentException> { snapshotVersionOf(UNSPECIFIED_VERSION) }
            shouldThrow<IllegalArgumentException> { snapshotVersionOf("  ") }
        }
    }
}
