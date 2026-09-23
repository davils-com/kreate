/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kreate.module.local.extension

import com.davils.kreate.KreateTasks
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * One repository in a declared workspace.
 *
 * @param name The library's name.
 * @param factory The object factory used for creating properties.
 * @since 3.2.0
 */
public abstract class LocalLibrarySpec @Inject constructor(
    private val name: String,
    /**
     * The object factory instance.
     * @since 3.2.0
     */
    factory: ObjectFactory
) : Named {
    /**
     * The path of the repository, relative to the workspace root.
     *
     * Defaults to the library's name.
     *
     * @since 3.2.0
     */
    public val path: Property<String> = factory.property(String::class.java).convention(name)

    /**
     * The libraries that have to be published before this one.
     *
     * Only direct edges need declaring; the transitive order follows from them.
     *
     * @since 3.2.0
     */
    public val dependencies: ListProperty<String> = factory.listProperty(String::class.java)

    /**
     * The tasks run in this repository to publish it locally.
     *
     * Defaults to [KreateTasks.Local.PUBLISH] alone. Override it for a repository that has more
     * to install than its own task reaches — most often one that also builds a Gradle plugin of
     * its own as an included build, which consumers resolve as an ordinary artifact and which the
     * library's `kreateLocalPublish` therefore never sees.
     *
     * @since 3.2.0
     */
    public val tasks: ListProperty<String> = factory
        .listProperty(String::class.java)
        .convention(listOf(KreateTasks.Local.PUBLISH))

    /**
     * Declares that this library depends on others.
     *
     * @param libraries The names of the libraries that have to be published first.
     * @since 3.2.0
     */
    public fun dependsOn(vararg libraries: String) {
        dependencies.addAll(libraries.toList())
    }

    /**
     * Sets the tasks run to publish this library locally.
     *
     * @param names The task names, in the order they should run.
     * @since 3.2.0
     */
    public fun tasks(vararg names: String) {
        tasks.set(names.toList())
    }

    /**
     * Returns the library's name.
     *
     * @return The name this specification was declared under.
     * @since 3.2.0
     */
    override fun getName(): String = name
}
