package com.davils.kreate.module.builder

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.Module
import org.gradle.api.Project

internal data class KreateModuleRegistryData(
    val extension: KreateExtension,
    val project: Project,
    val modules: List<Module>
)