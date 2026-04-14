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

public abstract class PomExtension @Inject constructor(factory: ObjectFactory) {
    @get:Nested
    public abstract val issueManagement: PomIssueManagementExtension

    @get:Nested
    public abstract val ciManagement: PomCiManagementExtension

    @get:Nested
    public abstract val licenses: PomLicensesExtension

    @get:Nested
    public abstract val developers: PomDevelopersExtension

    @get:Nested
    public abstract val scm: PomScmExtension

    public fun issueManagement(action: Action<PomIssueManagementExtension>) {
        action.execute(issueManagement)
    }

    public fun ciManagement(action: Action<PomCiManagementExtension>) {
        action.execute(ciManagement)
    }

    public fun licenses(action: Action<PomLicensesExtension>) {
        action.execute(licenses)
    }

    public fun developers(action: Action<PomDevelopersExtension>) {
        action.execute(developers)
    }

    public fun scm(action: Action<PomScmExtension>) {
        action.execute(scm)
    }
}