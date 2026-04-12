package com.davils.kreate.module.project.publish.extension.pom

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

public abstract class PomLicensesExtension @Inject constructor(factory: ObjectFactory) {
    @get:Nested
    public abstract val license: PomLicenseExtension

    public fun license(action: Action<PomLicenseExtension>) {
        action.execute(license)
    }
}

public abstract class PomLicenseExtension @Inject constructor(factory: ObjectFactory) {
    public val name: Property<String> = factory.property(String::class.java)
    public val url: Property<String> = factory.property(String::class.java)
    public val distribution: Property<String> = factory.property(String::class.java)
}