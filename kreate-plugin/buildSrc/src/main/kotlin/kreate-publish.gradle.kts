import com.davils.buildsrc.Project

plugins {
    com.vanniktech.maven.publish
    org.jetbrains.kotlin.jvm
    signing
}

version = "1.0.0-dev1"

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    coordinates(Project.Identity.GROUP, Project.Identity.NAME.lowercase(), version.toString())

    pom {
        name = (Project.Identity.NAME)
        description = Project.Identity.DESCRIPTION
        inceptionYear = Project.Identity.INCEPTION_YEAR.toString()
        url = Project.Organization.WEBSITE_URL

        issueManagement {
            system = Project.IssueManagement.SYSTEM
            url = Project.IssueManagement.URL
        }

        ciManagement {
            system = Project.VersionControl.CI_SYSTEM
            url = Project.VersionControl.CI_URL
        }

        licenses {
            license {
                name = Project.Legal.LICENSE_NAME
                url = Project.Legal.LICENSE_URL
                distribution = Project.Legal.LICENSE_DISTRIBUTION
            }
        }

        developers {
            developer {
                id = Project.Organization.NAME.lowercase()
                name = Project.Organization.NAME
                email = Project.Organization.EMAIL
                organization = Project.Organization.NAME
                timezone = Project.Organization.TIMEZONE
            }
        }

        scm {
            url = Project.VersionControl.SCM_URL
            connection = Project.VersionControl.SCM_CONNECTION
            developerConnection = Project.VersionControl.SCM_DEVELOPER_CONNECTION
        }
    }
}
