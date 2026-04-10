package com.davils.kreate.module.project.constants

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class BuildConstantsExtension @Inject constructor(factory: ObjectFactory) {
    private val properties: MapProperty<String, String> = factory.mapProperty(String::class.java, String::class.java).convention(emptyMap())
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
    public val packageNameOverride: Property<String> = factory.property(String::class.java)
    public val className: Property<String> = factory.property(String::class.java).convention("BuildConstants")
    public val path: Property<String> = factory.property(String::class.java).convention("generated/compile")

    public fun constant(key: String, value: String) {
        if (key.isBlank()) {
            throw IllegalArgumentException("Key cannot be blank.")
        }

        properties.put(key, value)
    }

    public fun constant(key: String, value: Any) {
        constant(key, value.toString())
    }

    public fun getConstants(): Map<String, String> {
        return properties.get().toMap()
    }
}