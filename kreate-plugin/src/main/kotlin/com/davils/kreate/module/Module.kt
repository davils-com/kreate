package com.davils.kreate.module

import com.davils.kreate.KreateExtension
import org.gradle.api.Project

internal interface Module {
    fun apply(project: Project, extension: KreateExtension)
}