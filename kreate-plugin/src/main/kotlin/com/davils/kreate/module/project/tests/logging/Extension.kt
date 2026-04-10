package com.davils.kreate.module.project.tests.logging

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class TestsLoggingExtension @Inject constructor(factory: ObjectFactory) {
    public val logPassedTests: Property<Boolean> = factory.property(Boolean::class.java).convention(true)
    public val logSkippedTests: Property<Boolean> = factory.property(Boolean::class.java).convention(true)
    public val logTestStarted: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
}
