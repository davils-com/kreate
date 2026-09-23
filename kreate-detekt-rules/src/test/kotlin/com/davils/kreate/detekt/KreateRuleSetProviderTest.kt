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

package com.davils.kreate.detekt

import dev.detekt.api.RuleSetProvider
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.ServiceLoader

/**
 * Tests for the wiring that makes the rule set reachable.
 *
 * None of these assert behaviour of a rule. They assert the three things that are invisible until a
 * consumer's build silently analyses nothing: the service registration, the rule set id, and the
 * agreement between the shipped default configuration and the rules that actually exist.
 */
@DisplayName("KreateRuleSetProvider")
class KreateRuleSetProviderTest {

    @Test
    @DisplayName("is discoverable through the service loader Detekt uses")
    fun discoverableAsService() {
        val providers = ServiceLoader.load(RuleSetProvider::class.java, javaClass.classLoader).toList()

        providers.map { provider -> provider::class.java.name } shouldContainExactly
            listOf(KreateRuleSetProvider::class.java.name)
    }

    @Test
    @DisplayName("is configured under the id the documentation names")
    fun ruleSetId() {
        KreateRuleSetProvider().ruleSetId.value shouldBe KREATE_RULE_SET_ID
    }

    @Test
    @DisplayName("ships a default configuration that names exactly the rules it provides")
    fun defaultConfigMatchesRules() {
        // A rule missing from `config/config.yml` is inactive for every project that builds upon
        // the default configuration, and a rule listed there but since renamed fails Detekt's own
        // config validation. Neither shows up in a rule's own tests.
        val provided = KreateRuleSetProvider().instance().rules.keys.map { name -> name.value }

        configuredRuleNames() shouldContainExactly provided
    }

    /**
     * The rule names listed under the rule set in the shipped default configuration.
     *
     * @return The names, in the order the file lists them.
     */
    private fun configuredRuleNames(): List<String> {
        val resource = requireNotNull(javaClass.classLoader.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            "The rule set ships '$DEFAULT_CONFIG_RESOURCE', which is how Detekt learns its defaults."
        }
        val contents = resource.use { stream -> stream.reader().readText() }

        return RULE_KEY.findAll(contents)
            .map { match -> match.groupValues[1] }
            .filterNot { key -> key == ACTIVE_KEY }
            .toList()
    }

    private companion object {
        /**
         * The classpath location Detekt reads a plugin's default configuration from.
         */
        private const val DEFAULT_CONFIG_RESOURCE: String = "config/config.yml"

        /**
         * The rule set's own switch, which is a key at the same depth as a rule name.
         */
        private const val ACTIVE_KEY: String = "active"

        /**
         * A key nested one level below the rule set, which is where rule names live.
         */
        private val RULE_KEY: Regex = Regex("""^ {2}(\w+):""", RegexOption.MULTILINE)
    }
}
