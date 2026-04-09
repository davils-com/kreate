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