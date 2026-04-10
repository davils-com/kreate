package com.davils.kreate.module.project.tests.report

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class TestsReportExtension @Inject constructor(factory: ObjectFactory) {
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
    public val xml: Property<Boolean> = factory.property(Boolean::class.java).convention(true)
    public val html: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
}
