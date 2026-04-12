package com.davils.kreate.module.project.publish

import com.davils.kreate.module.project.publish.extension.PublishExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.MavenPublishBasePlugin
import org.gradle.api.Project

internal fun Project.configureMavenCentral(
    publishConfig: PublishExtension,
    projectName: String?,
    projectDescription: String?
) {
    val mavenCentralConfig = publishConfig.repositories.mavenCentral
    if (!mavenCentralConfig.enabled.get()) {
        return
    }

    pluginManager.apply(MavenPublishBasePlugin::class.java)

    extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
        publishToMavenCentral(automaticRelease = mavenCentralConfig.automaticRelease.get())
        if (mavenCentralConfig.signPublications.get()) {
            signAllPublications()
        }

        pom {
            configurePom(publishConfig, projectName, projectDescription)
        }
    }
}
