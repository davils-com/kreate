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
package com.davils.kreate.assurance

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

/**
 * Configuration of the assurance reports.
 *
 * @param factory The object factory used to create the properties.
 * @since 4.0.0
 */
public abstract class AssuranceReportsExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * The formats to write. Defaults to [ReportFormat.JSON] and [ReportFormat.SARIF].
     *
     * @since 4.0.0
     */
    public val formats: SetProperty<ReportFormat> = factory.setProperty(ReportFormat::class.java)
        .convention(setOf(ReportFormat.JSON, ReportFormat.SARIF))
}
