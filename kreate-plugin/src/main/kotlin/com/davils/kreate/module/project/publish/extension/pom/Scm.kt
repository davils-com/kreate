package com.davils.kreate.module.project.publish.extension.pom

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class PomScmExtension @Inject constructor(factory: ObjectFactory) {
    public val url: Property<String> = factory.property(String::class.java)
    public val connection: Property<String> = factory.property(String::class.java)
    public val developerConnection: Property<String> = factory.property(String::class.java)
}