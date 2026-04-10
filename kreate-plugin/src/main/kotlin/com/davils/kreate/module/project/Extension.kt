package com.davils.kreate.module.project

import com.davils.kreate.module.project.constants.BuildConstantsExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

public abstract class ProjectExtension @Inject constructor(factory: ObjectFactory) {
    public val name: Property<String> = factory.property(String::class.java)
    public val description: Property<String> = factory.property(String::class.java).convention("A Davils project.")

    @get:Nested
    public abstract val buildConstants: BuildConstantsExtension

    public fun buildConstant(action: Action<BuildConstantsExtension>) {
        action.execute(buildConstants)
    }
}