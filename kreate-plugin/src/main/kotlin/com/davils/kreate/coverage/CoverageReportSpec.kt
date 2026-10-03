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

package com.davils.kreate.coverage

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * The settings every coverage report format shares.
 *
 * There is deliberately no `enabled` flag. A coverage report task always exists and can always be
 * invoked by name; the only thing a build can actually decide is whether it runs on its own as
 * part of `check`, which is what [onCheck] expresses. A flag that claimed to disable a task you
 * can still run would be a lie in the DSL.
 *
 * @param factory The object factory used for creating properties.
 * @since 2.2.0
 */
public abstract class CoverageReportSpec @Inject constructor(
    factory: ObjectFactory
) {
    /**
     * Whether this report is generated as part of the `check` task.
     *
     * Defaults to `false`. Generating every report on every `check` slows down the inner
     * development loop for output nobody reads locally; CI asks for the reports it needs by name.
     * The verification gate is the exception — see [CoverageVerifyExtension.runOnCheck].
     *
     * @since 2.2.0
     */
    public val onCheck: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
}
