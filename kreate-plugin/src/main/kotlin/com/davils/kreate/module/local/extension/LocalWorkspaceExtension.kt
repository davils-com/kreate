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

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * Declares the repositories a local publish can be orchestrated across.
 *
 * A Davils workspace is a dozen checkouts side by side, and a change at the root of the
 * dependency graph has to be pushed through every library above it before it can be tried in the
 * one that matters. Doing that by hand means remembering both the set and the order, and getting
 * the order wrong produces a build that resolves a stale snapshot without saying so.
 *
 * The edges are declared here rather than derived from each repository's version catalog. That is
 * deliberate: the catalog records what a library was last *released* against, and the edge that
 * matters during a refactor is usually the one that is not in the catalog yet.
 *
 * ```kotlin
 * kreate {
 *     local {
 *         workspace {
 *             root = file("../..")
 *
 *             library("arc")  { path = "libraries/arc" }
 *             library("rise") { path = "libraries/rise"; dependsOn("arc") }
 *             library("leaf") { path = "libraries/leaf"; dependsOn("arc", "rise") }
 *         }
 *     }
 * }
 * ```
 *
 * @param factory The object factory used for creating properties and the container.
 * @since 3.2.0
 */
public abstract class LocalWorkspaceExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 3.2.0
     */
    factory: ObjectFactory
) {
    /**
     * The directory every declared path is resolved against.
     *
     * Defaults to the parent of the project directory, which is the layout a workspace of sibling
     * checkouts already has.
     *
     * @since 3.2.0
     */
    public abstract val root: DirectoryProperty

    /**
     * The declared libraries.
     *
     * @since 3.2.0
     */
    public val libraries: NamedDomainObjectContainer<LocalLibrarySpec> =
        factory.domainObjectContainer(LocalLibrarySpec::class.java)

    /**
     * Declares a library, or reconfigures one that was already declared.
     *
     * @param name The library's name, which has to match the root project name of its build so
     * that the record written by `kreateLocalPublish` can be matched back to this entry.
     * @param action The configuration action.
     * @since 3.2.0
     */
    public fun library(name: String, action: Action<LocalLibrarySpec>) {
        libraries.maybeCreate(name).also(action::execute)
    }

    /**
     * Declares a library whose path is its name under [root].
     *
     * @param name The library's name.
     * @since 3.2.0
     */
    public fun library(name: String) {
        libraries.maybeCreate(name)
    }
}
