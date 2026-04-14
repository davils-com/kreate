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
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

public abstract class PomDevelopersExtension @Inject constructor(factory: ObjectFactory) {
    @get:Nested
    public abstract val developer: PomDeveloperExtension

    public fun developer(action: Action<PomDeveloperExtension>) {
        action.execute(developer)
    }
}

public abstract class PomDeveloperExtension @Inject constructor(factory: ObjectFactory) {
    public val id: Property<String> = factory.property(String::class.java)
    public val name: Property<String> = factory.property(String::class.java)
    public val email: Property<String> = factory.property(String::class.java)
    public val organization: Property<String> = factory.property(String::class.java)
    public val timezone: Property<String> = factory.property(String::class.java)
}