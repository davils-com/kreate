package com.davils.kreate.module.platform

import org.gradle.api.JavaVersion
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class PlatformExtension @Inject constructor(factory: ObjectFactory) {
    public val javaVersion: Property<JavaVersion> = factory.property(JavaVersion::class.java).convention(JavaVersion.VERSION_25)
}