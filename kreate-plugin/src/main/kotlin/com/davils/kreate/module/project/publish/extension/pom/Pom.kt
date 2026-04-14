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

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring Maven POM metadata.
 *
 * This extension provides nested configurations for issue management,
 * CI management, licenses, developers, and SCM.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class PomExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    factory: ObjectFactory
) {
    /**
     * Configuration for issue management.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val issueManagement: PomIssueManagementExtension

    /**
     * Configuration for CI management.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val ciManagement: PomCiManagementExtension

    /**
     * Configuration for licenses.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val licenses: PomLicensesExtension

    /**
     * Configuration for developers.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val developers: PomDevelopersExtension

    /**
     * Configuration for source control management.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val scm: PomScmExtension

    /**
     * Configures the [PomIssueManagementExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun issueManagement(action: Action<PomIssueManagementExtension>) {
        action.execute(issueManagement)
    }

    /**
     * Configures the [PomCiManagementExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun ciManagement(action: Action<PomCiManagementExtension>) {
        action.execute(ciManagement)
    }

    /**
     * Configures the [PomLicensesExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun licenses(action: Action<PomLicensesExtension>) {
        action.execute(licenses)
    }

    /**
     * Configures the [PomDevelopersExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun developers(action: Action<PomDevelopersExtension>) {
        action.execute(developers)
    }

    /**
     * Configures the [PomScmExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun scm(action: Action<PomScmExtension>) {
        action.execute(scm)
    }
}