package com.davils.buildsrc.publish

import com.davils.buildsrc.Project
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomCiManagement
import org.gradle.api.publish.maven.MavenPomDeveloperSpec
import org.gradle.api.publish.maven.MavenPomIssueManagement
import org.gradle.api.publish.maven.MavenPomLicenseSpec
import org.gradle.api.publish.maven.MavenPomScm

/**
 * Applies standard POM configuration to a Maven POM.
 *
 * This function configures the POM with project identity, organization details,
 * developers, CI management, licenses, SCM, and issue management information.
 *
 * @receiver The [MavenPom] instance to be configured.
 * @since 1.0.0
 * @author Nils Jaekel
 */
public fun MavenPom.applyPomConfiguration() {
    name.set(Project.Identity.NAME)
    description.set(Project.Identity.DESCRIPTION)
    inceptionYear.set(Project.Identity.INCEPTION_YEAR.toString())
    url.set(Project.Organization.WEBSITE_URL)

    developers {
        configureDevelopers()
    }

    ciManagement {
        configureCiManagement()
    }

    licenses {
        configureLicenses()
    }

    scm {
        configureScm()
    }

    issueManagement {
        configureIssueManagement()
    }
}

/**
 * Configures the developers section of a Maven POM.
 *
 * This function adds a developer entry with details from the [Project.Organization] constants.
 *
 * @receiver The [MavenPomDeveloperSpec] to be configured.
 * @since 1.0.0
 * @author Nils Jaekel
 */
private fun MavenPomDeveloperSpec.configureDevelopers() {
    developer {
        id.set(Project.Organization.NAME.lowercase())
        name.set(Project.Organization.NAME)
        email.set(Project.Organization.EMAIL)
        organization.set(Project.Organization.NAME)
        timezone.set(Project.Organization.TIMEZONE)
    }
}

/**
 * Configures the CI management section of a Maven POM.
 *
 * This function sets the CI system and URL using [Project.VersionControl] constants.
 *
 * @receiver The [MavenPomCiManagement] to be configured.
 * @since 1.0.0
 * @author Nils Jaekel
 */
private fun MavenPomCiManagement.configureCiManagement() {
    system.set(Project.VersionControl.CI_SYSTEM)
    url.set(Project.VersionControl.CI_URL)
}

/**
 * Configures the licenses section of a Maven POM.
 *
 * This function adds a license entry based on the [Project.Legal] constants.
 *
 * @receiver The [MavenPomLicenseSpec] to be configured.
 * @since 1.0.0
 * @author Nils Jaekel
 */
private fun MavenPomLicenseSpec.configureLicenses() {
    license {
        name.set(Project.Legal.LICENSE_NAME)
        distribution.set(Project.Legal.LICENSE_DISTRIBUTION)
        url.set(Project.Legal.LICENSE_URL)
    }
}

/**
 * Configures the SCM section of a Maven POM.
 *
 * This function sets the SCM URL and connection details using [Project.VersionControl] constants.
 *
 * @receiver The [MavenPomScm] to be configured.
 * @since 1.0.0
 * @author Nils Jaekel
 */
private fun MavenPomScm.configureScm() {
    url.set(Project.VersionControl.SCM_URL)
    connection.set(Project.VersionControl.SCM_CONNECTION)
    developerConnection.set(Project.VersionControl.SCM_DEVELOPER_CONNECTION)
}

/**
 * Configures the issue management section of a Maven POM.
 *
 * This function sets the issue tracking system and URL using [Project.IssueManagement] constants.
 *
 * @receiver The [MavenPomIssueManagement] to be configured.
 * @since 1.0.0
 * @author Nils Jaekel
 */
private fun MavenPomIssueManagement.configureIssueManagement() {
    system.set(Project.IssueManagement.SYSTEM)
    url.set(Project.IssueManagement.URL)
}
