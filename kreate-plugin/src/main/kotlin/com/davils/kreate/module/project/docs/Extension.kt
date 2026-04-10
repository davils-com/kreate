package com.davils.kreate.module.project.docs

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class DocsExtension @Inject constructor(factory: ObjectFactory) {
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
    public val moduleName: Property<String> = factory.property(String::class.java)
    public val outputDirectory: Property<String> = factory.property(String::class.java)
}