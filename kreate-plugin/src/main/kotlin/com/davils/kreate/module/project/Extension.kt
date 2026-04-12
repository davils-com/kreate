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
    public val description: Property<String> = factory.property(String::class.java).convention("A Davils project.")

    @get:Nested
    public abstract val buildConstants: BuildConstantsExtension

    @get:Nested
    public abstract val docs: DocsExtension

    @get:Nested
    public abstract val tests: TestsExtension

    @get:Nested
    public abstract val publish: PublishExtension

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