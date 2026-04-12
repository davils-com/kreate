package com.davils.kreate.module.project.publish.extension.repository

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class MavenCentralExtension @Inject constructor(factory: ObjectFactory) {
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
    public val automaticRelease: Property<Boolean> = factory.property(Boolean::class.java).convention(true)
    public val signPublications: Property<Boolean> = factory.property(Boolean::class.java).convention(true)
}