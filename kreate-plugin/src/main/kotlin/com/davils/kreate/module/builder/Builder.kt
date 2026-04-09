package com.davils.kreate.module.builder

import com.davils.kreate.KreateDsl
import com.davils.kreate.KreateExtension
import com.davils.kreate.module.Module
import org.gradle.api.Project

@KreateDsl
internal class KreateModuleRegistryBuilder(
    private val project: Project,
    private val extension: KreateExtension
) {
    private val modules = mutableListOf<Module>()

    fun add(module: Module) {
        if (modules.contains(module)) return
        modules.add(module)
    }

    fun addAll(modules: Iterable<Module>) {
        this.modules.addAll(modules)
    }

    fun addAll(vararg modules: Module) {
        addAll(modules.asList())
    }

    operator fun Module.unaryPlus() {
        add(this)
    }

    fun build(): KreateModuleRegistryData {
        return KreateModuleRegistryData(
            extension = extension,
            project = project,
            modules = modules.toList()
        )
    }
}