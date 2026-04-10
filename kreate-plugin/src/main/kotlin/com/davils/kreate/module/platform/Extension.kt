package com.davils.kreate.module.platform

import com.davils.kreate.module.platform.multiplatform.MultiplatformExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

public abstract class PlatformExtension @Inject constructor(factory: ObjectFactory) {
    public val javaVersion: Property<JavaVersion> = factory.property(JavaVersion::class.java).convention(JavaVersion.VERSION_25)
    public val explicitApi: Property<Boolean> = factory.property(Boolean::class.java).convention(true)
    public val allWarningsAsErrors: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    @get:Nested
    public abstract val multiplatform: MultiplatformExtension

    public fun multiplatform(action: Action<MultiplatformExtension>) {
        action.execute(multiplatform)
    }
}