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

package com.davils.kreate.module.project

import com.davils.kreate.module.project.constants.BuildConstantsExtension
import com.davils.kreate.module.project.detekt.extension.DetektExtension
import com.davils.kreate.module.project.docs.DocsExtension
import com.davils.kreate.module.project.publish.extension.PublishExtension
import com.davils.kreate.module.project.tests.TestsExtension
import com.davils.kreate.module.project.trivy.extension.TrivyExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring project-level settings in Kreate.
 *
 * This extension provides properties for project name, description, group,
 * and nested configurations for versioning, build constants, documentation,
 * testing, and publishing.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class ProjectExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    factory: ObjectFactory
) {
    /**
     * The name of the project.
     * @since 1.0.0
     */
    public val name: Property<String> = factory.property(String::class.java)

    /**
     * The description of the project.
     * Defaults to "A Kreate project."
     * @since 1.0.0
     */
    public val description: Property<String> = factory.property(String::class.java).convention("A Kreate project.")

    /**
     * Configuration for project versioning.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val version: ProjectExtensionVersion

    /**
     * Configuration for build constants generation.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val buildConstants: BuildConstantsExtension

    /**
     * Configuration for documentation generation.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val docs: DocsExtension

    /**
     * Configuration for testing.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val tests: TestsExtension

    /**
     * Configuration for publishing.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val publish: PublishExtension

    /**
     * Configuration for detekt.
     *
     * @since 1.2.0
     */
    @get:Nested
    public abstract val detekt: DetektExtension

    @get:Nested
    public abstract val trivy: TrivyExtension

    /**
     * Configures the [ProjectExtensionVersion] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun version(action: Action<ProjectExtensionVersion>) {
        action.execute(version)
    }

    /**
     * Configures the [BuildConstantsExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun buildConstant(action: Action<BuildConstantsExtension>) {
        action.execute(buildConstants)
    }

    /**
     * Configures the [DocsExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun docs(action: Action<DocsExtension>) {
        action.execute(docs)
    }

    /**
     * Configures the [TestsExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun tests(action: Action<TestsExtension>) {
        action.execute(tests)
    }

    /**
     * Configures the [PublishExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun publish(action: Action<PublishExtension>) {
        action.execute(publish)
    }

    /**
     * Configures the [DetektExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.2.0
     */
    public fun detekt(action: Action<DetektExtension>) {
        action.execute(detekt)
    }

    public fun trivy(action: Action<TrivyExtension>) {
        action.execute(trivy)
    }
}

/**
 * Extension for configuring versioning settings.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class ProjectExtensionVersion @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    factory: ObjectFactory
) {
    /**
     * The environment variable name to check for the version.
     * Defaults to "VERSION".
     * @since 1.0.0
     */
    public val environment: Property<String> = factory.property(String::class.java).convention("VERSION")

    /**
     * The project property name to check for the version.
     * Defaults to "version".
     * @since 1.0.0
     */
    public val property: Property<String> = factory.property(String::class.java).convention("version")
}
