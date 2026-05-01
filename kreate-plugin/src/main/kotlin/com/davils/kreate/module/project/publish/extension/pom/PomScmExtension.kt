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
 * Extension for configuring POM source control management (SCM) metadata.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class PomScmExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    factory: ObjectFactory
) {
    /**
     * The URL to the SCM web interface.
     * @since 1.0.0
     */
    public val url: Property<String> = factory.property(String::class.java)

    /**
     * The read-only connection URL for SCM.
     * @since 1.0.0
     */
    public val connection: Property<String> = factory.property(String::class.java)

    /**
     * The read-write connection URL for SCM.
     * @since 1.0.0
     */
    public val developerConnection: Property<String> = factory.property(String::class.java)
}
