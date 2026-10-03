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

package com.davils.kreate.rules

import dev.detekt.api.RuleSetProvider
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.ServiceLoader

class KreateRuleSetProviderTest : FunSpec({

    test("is discoverable through the service loader Detekt uses") {
        val loader = ServiceLoader.load(RuleSetProvider::class.java, javaClass.classLoader)
        val providers = loader.toList()

        val providerNames = providers.map { provider -> provider::class.java.name }

        providerNames shouldContainExactly listOf(KreateRuleSetProvider::class.java.name)
    }

    test("is configured under the id the documentation names") {
        val ruleSetId = KreateRuleSetProvider().ruleSetId

        ruleSetId.value shouldBe KREATE_RULE_SET_ID
    }

    test("ships a default configuration that names exactly the rules it provides") {
        val ruleSet = KreateRuleSetProvider().instance()
        val provided = ruleSet.rules.keys.map { name -> name.value }

        configuredRuleNames() shouldContainExactly provided
    }
})

private const val DEFAULT_CONFIG_RESOURCE: String = "config/config.yml"

private const val ACTIVE_KEY: String = "active"

private val RULE_KEY: Regex = Regex("""^ {2}(\w+):""", RegexOption.MULTILINE)

private fun configuredRuleNames(): List<String> {
    val classLoader = KreateRuleSetProvider::class.java.classLoader
    val resource = requireNotNull(classLoader.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
        "The rule set ships '$DEFAULT_CONFIG_RESOURCE', which is how Detekt learns its defaults."
    }
    val contents = resource.use { stream -> stream.reader().readText() }

    val keys = RULE_KEY.findAll(contents)
    val ruleKeys = keys.map { match -> match.groupValues[1] }
    val ruleNames = ruleKeys.filterNot { key -> key == ACTIVE_KEY }
    return ruleNames.toList()
}
