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

package com.davils.kreate

import com.davils.kreate.module.platform.PlatformExtension
import com.davils.kreate.module.project.ProjectExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested
import javax.inject.Inject

public abstract class KreateExtension @Inject constructor(factory: ObjectFactory) {
    @get:Nested
    public abstract val project: ProjectExtension

    @get:Nested
    public abstract val platform: PlatformExtension

    public fun project(action: Action<ProjectExtension>) {
        action.execute(project)
    }

    public fun platform(action: Action<PlatformExtension>) {
        action.execute(platform)
    }
}