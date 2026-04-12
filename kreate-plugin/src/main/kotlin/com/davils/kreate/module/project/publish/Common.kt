package com.davils.kreate.module.project.publish

import com.davils.kreate.module.project.publish.extension.PublishExtension
import org.gradle.api.publish.maven.MavenPom

internal fun MavenPom.configurePom(
    publishConfig: PublishExtension,
    projectName: String?,
    projectDescription: String?
) {
    val pomConfig = publishConfig.pom

    (projectName)?.let { name.set(it) }
    (projectDescription)?.let { description.set(it) }
    publishConfig.inceptionYear.orNull?.let { inceptionYear.set(it.toString()) }
    publishConfig.website.orNull?.let { url.set(it) }

    issueManagement {
        pomConfig.issueManagement.system.orNull?.let { system.set(it) }
        pomConfig.issueManagement.url.orNull?.let { url.set(it) }
    }

    ciManagement {
        pomConfig.ciManagement.system.orNull?.let { system.set(it) }
        pomConfig.ciManagement.url.orNull?.let { url.set(it) }
    }

    licenses {
        license {
            pomConfig.licenses.license.name.orNull?.let { name.set(it) }
            pomConfig.licenses.license.url.orNull?.let { url.set(it) }
            pomConfig.licenses.license.distribution.orNull?.let { distribution.set(it) }
        }
    }

    developers {
        developer {
            pomConfig.developers.developer.id.orNull?.let { id.set(it) }
            pomConfig.developers.developer.name.orNull?.let { name.set(it) }
            pomConfig.developers.developer.email.orNull?.let { email.set(it) }
            pomConfig.developers.developer.organization.orNull?.let { organization.set(it) }
            pomConfig.developers.developer.timezone.orNull?.let { timezone.set(it) }
        }
    }

    scm {
        pomConfig.scm.url.orNull?.let { url.set(it) }
        pomConfig.scm.connection.orNull?.let { connection.set(it) }
        pomConfig.scm.developerConnection.orNull?.let { developerConnection.set(it) }
    }
}
