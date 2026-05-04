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

package com.davils.kreate.module.project.trivy.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

public abstract class TrivyExtension @Inject constructor(factory: ObjectFactory) {
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    @get:Nested
    public abstract val vulnerability: TrivyCVEVulnerabilityExtension

    @get:Nested
    public abstract val license: TrivyLicenseExtension

    @get:Nested
    public abstract val secrets: TrivySecretExtension

    public fun vulnerability(action: Action<TrivyCVEVulnerabilityExtension>) {
        action.execute(vulnerability)
    }

    public fun license(action: Action<TrivyLicenseExtension>) {
        action.execute(license)
    }

    public fun secrets(action: Action<TrivySecretExtension>) {
        action.execute(secrets)
    }
}
