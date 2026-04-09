package com.davils.kreate.module.project

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class ProjectExtension @Inject constructor(factory: ObjectFactory) {
    public val name: Property<String> = factory.property(String::class.java)
    public val description: Property<String> = factory.property(String::class.java).convention("A Davils project.")
}