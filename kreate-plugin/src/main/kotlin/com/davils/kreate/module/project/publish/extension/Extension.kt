package com.davils.kreate.module.project.publish.extension

import com.davils.kreate.module.project.publish.extension.pom.PomExtension
import com.davils.kreate.module.project.publish.extension.repository.MavenRepositoriesExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

public abstract class PublishExtension @Inject constructor(factory: ObjectFactory) {
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
    public val inceptionYear: Property<Int> = factory.property(Int::class.java).convention(2024)
    public val website: Property<String> = factory.property(String::class.java)

    @get:Nested
    public abstract val pom: PomExtension

    @get:Nested
    public abstract val repositories: MavenRepositoriesExtension

    public fun pom(action: Action<PomExtension>) {
        action.execute(pom)
    }

    public fun repositories(action: Action<MavenRepositoriesExtension>) {
        action.execute(repositories)
    }
}
