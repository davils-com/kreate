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

package com.davils.kreate.module.project.detekt

import com.davils.kreate.Kreate
import com.davils.kreate.KreateExtension
import com.davils.kreate.module.local.UNKNOWN_KREATE_VERSION
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for the decision to put the Kreate rule set on Detekt's analysis classpath.
 *
 * The version is the whole of the decision, and it is the part that cannot be observed from a
 * consumer's build until resolution fails: the rule set is published per Kreate version, so a
 * Kreate that cannot name its own has nothing to ask for.
 */
@DisplayName("Kreate rule set")
class KreateRuleSetTest {

    @Test
    @DisplayName("is on the classpath unless a project asks for it to be left off")
    fun enabledByDefault() {
        val project: Project = ProjectBuilder.builder().withName("sample").build()
        project.pluginManager.apply(Kreate::class.java)
        val extension = project.extensions.getByType(KreateExtension::class.java)

        extension.project.detekt.kreateRules.get() shouldBe true
    }

    @Test
    @DisplayName("is pinned to the version of Kreate that configures it")
    fun pinnedToKreateVersion() {
        kreateRuleSetNotation("3.3.0") shouldBe "com.davils:kreate-detekt-rules:3.3.0"
    }

    @Test
    @DisplayName("is left off the classpath when Kreate cannot name its own version")
    fun skippedWithoutVersion() {
        // Kreate reads its version from the JAR manifest. Loaded from a directory of classes -
        // its own functional tests, or a script classpath assembled by hand - there is none, and
        // a guessed version would fail resolution for a reason that reads like a missing release.
        kreateRuleSetNotation(UNKNOWN_KREATE_VERSION) shouldBe null
    }
}
