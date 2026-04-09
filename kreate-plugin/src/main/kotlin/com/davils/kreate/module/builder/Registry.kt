package com.davils.kreate.module.builder

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.Module
import org.gradle.api.Project

internal class KreateModuleRegistry(private val data: KreateModuleRegistryData) {
    private var isApplied = false

    fun getModules(): List<Module> {
        return data.modules.toList()
    }

    fun applyAll() {
        if (isApplied) {
            return
        }

        data.modules.forEach { module ->
            module.apply(data.project, data.extension)
        }
        isApplied = true
    }
}

internal fun Project.modules(extension: KreateExtension, builder: KreateModuleRegistryBuilder.() -> Unit): KreateModuleRegistry {
    val registryBuilder = KreateModuleRegistryBuilder(this, extension)
    registryBuilder.builder()
    val registryData = registryBuilder.build()
    return KreateModuleRegistry(registryData)
}