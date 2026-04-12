package com.davils.kreate.module.project.publish.extension.pom

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class PomIssueManagementExtension @Inject constructor(factory: ObjectFactory) {
    public val system: Property<String> = factory.property(String::class.java)
    public val url: Property<String> = factory.property(String::class.java)
}