import com.davils.buildsrc.publish.applyPomConfiguration

plugins {
    `maven-publish`
}

// TODO: From Github pipeline
public val projectVersion: String = System.getenv(/* name = */ "CI_COMMIT_TAG") ?: "0.0.0"
version = projectVersion

publishing {
//    applyPublishRepository(repositories)
    publications {
        withType<MavenPublication> {
            pom {
                applyPomConfiguration()
            }
        }
    }
}

