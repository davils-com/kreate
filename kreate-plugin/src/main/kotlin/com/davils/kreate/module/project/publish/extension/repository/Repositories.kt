package com.davils.kreate.module.project.publish.extension.repository

import org.gradle.api.Action
import org.gradle.api.tasks.Nested
import javax.inject.Inject

public abstract class MavenRepositoriesExtension @Inject constructor() {
    @get:Nested
    public abstract val gitlab: GitlabExtension

    @get:Nested
    public abstract val mavenCentral: MavenCentralExtension

    public fun gitlab(action: Action<GitlabExtension>) {
        gitlab.enabled.set(true)
        action.execute(gitlab)
    }

    public fun mavenCentral(action: Action<MavenCentralExtension>) {
        mavenCentral.enabled.set(true)
        action.execute(mavenCentral)
    }
}