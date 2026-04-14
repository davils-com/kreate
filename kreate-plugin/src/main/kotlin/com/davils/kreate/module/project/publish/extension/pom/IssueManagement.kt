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

package com.davils.kreate.module.project.publish.extension.pom

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension for configuring POM issue management metadata.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class PomIssueManagementExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    factory: ObjectFactory
) {
    /**
     * The name of the issue management system (e.g., "GitHub Issues").
     * @since 1.0.0
     */
    public val system: Property<String> = factory.property(String::class.java)

    /**
     * The URL to the issue management system.
     * @since 1.0.0
     */
    public val url: Property<String> = factory.property(String::class.java)
}