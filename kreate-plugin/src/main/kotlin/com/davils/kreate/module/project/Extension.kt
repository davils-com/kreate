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
import com.davils.kreate.module.project.docs.DocsExtension
import com.davils.kreate.module.project.publish.extension.PublishExtension
import com.davils.kreate.module.project.tests.TestsExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

public abstract class ProjectExtension @Inject constructor(factory: ObjectFactory) {
    public val name: Property<String> = factory.property(String::class.java)
    public val description: Property<String> = factory.property(String::class.java).convention("A Kreate project.")
    public val projectGroup: Property<String> = factory.property(String::class.java)

    @get:Nested
    public abstract val version: ProjectExtensionVersion

    @get:Nested
    public abstract val buildConstants: BuildConstantsExtension
    @get:Nested
    public abstract val docs: DocsExtension

    @get:Nested
    public abstract val tests: TestsExtension

    @get:Nested
    public abstract val publish: PublishExtension

    public fun version(action: Action<ProjectExtensionVersion>) {
        action.execute(version)
    }

    public fun buildConstant(action: Action<BuildConstantsExtension>) {
        action.execute(buildConstants)
    }

    public fun docs(action: Action<DocsExtension>) {
        action.execute(docs)
    }

    public fun tests(action: Action<TestsExtension>) {
        action.execute(tests)
    }

    public fun publish(action: Action<PublishExtension>) {
        action.execute(publish)
    }
}

public abstract class ProjectExtensionVersion @Inject constructor(factory: ObjectFactory) {
    public val environment: Property<String> = factory.property(String::class.java).convention("VERSION")
    public val property: Property<String> = factory.property(String::class.java).convention("version")
}