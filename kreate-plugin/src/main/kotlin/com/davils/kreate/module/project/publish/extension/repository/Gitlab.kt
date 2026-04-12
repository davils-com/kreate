package com.davils.kreate.module.project.publish.extension.repository

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class GitlabExtension @Inject constructor(factory: ObjectFactory) {
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
    public val name: Property<String> = factory.property(String::class.java)
    public val tokenEnv: Property<String> = factory.property(String::class.java).convention("CI_JOB_TOKEN")
    public val projectIdEnv: Property<String> = factory.property(String::class.java).convention("CI_PROJECT_ID")
    public val apiUrlEnv: Property<String> = factory.property(String::class.java).convention("CI_API_V4_URL")
}