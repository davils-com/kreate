package com.davils.kreate.module

import com.davils.kreate.KreateExtension
import org.gradle.api.Project

public interface Module {
    public fun apply(project: Project, extension: KreateExtension)
}