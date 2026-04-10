package com.davils.kreate.module.project.docs

import com.davils.kreate.KreateExtension
import org.gradle.api.Project

internal fun Project.initializeDocs(extension: KreateExtension) {
    val docsConfig = extension.project.docs
    if (!docsConfig.enabled.get()) {
        return
    }

    applyDokkaPlugin()
    configureDokkaExtension(extension.project.docs)
}
