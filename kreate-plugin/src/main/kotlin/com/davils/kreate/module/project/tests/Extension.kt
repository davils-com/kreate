package com.davils.kreate.module.project.tests

import com.davils.kreate.module.project.tests.logging.TestsLoggingExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

public abstract class TestsExtension @Inject constructor(factory: ObjectFactory) {
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
    public val maxParallelForks: Property<Int> = factory.property(Int::class.java).convention(Runtime.getRuntime().availableProcessors() / 2)
    public val timeoutMinutes: Property<Long> = factory.property(Long::class.java).convention(10L)
    public val ignoreFailures: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
    public val alwaysRunTests: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
    public val failOnNoDiscoveredTests: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    @get:Nested
    public abstract val logging: TestsLoggingExtension

    public fun logging(action: Action<TestsLoggingExtension>) {
        action.execute(logging)
    }
}