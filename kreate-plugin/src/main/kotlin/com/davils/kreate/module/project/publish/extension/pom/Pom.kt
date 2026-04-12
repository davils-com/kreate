package com.davils.kreate.module.project.publish.extension.pom

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested
import javax.inject.Inject

public abstract class PomExtension @Inject constructor(factory: ObjectFactory) {
    @get:Nested
    public abstract val issueManagement: PomIssueManagementExtension

    @get:Nested
    public abstract val ciManagement: PomCiManagementExtension

    @get:Nested
    public abstract val licenses: PomLicensesExtension

    @get:Nested
    public abstract val developers: PomDevelopersExtension

    @get:Nested
    public abstract val scm: PomScmExtension

    public fun issueManagement(action: Action<PomIssueManagementExtension>) {
        action.execute(issueManagement)
    }

    public fun ciManagement(action: Action<PomCiManagementExtension>) {
        action.execute(ciManagement)
    }

    public fun licenses(action: Action<PomLicensesExtension>) {
        action.execute(licenses)
    }

    public fun developers(action: Action<PomDevelopersExtension>) {
        action.execute(developers)
    }

    public fun scm(action: Action<PomScmExtension>) {
        action.execute(scm)
    }
}