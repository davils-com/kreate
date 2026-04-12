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