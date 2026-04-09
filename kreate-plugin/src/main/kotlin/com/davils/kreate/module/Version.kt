package com.davils.kreate.module

import org.gradle.api.Project

public fun Project.getProjectVersion(): String {
    val ciTag = System.getenv("CI_COMMIT_TAG")
    if (ciTag != null) return ciTag

    val versionProp = findProperty("version")?.toString()
    if (versionProp != null && versionProp != "unspecified") {
        return versionProp
    }

    return "1.0.0"
}
