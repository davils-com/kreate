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

package com.davils.kreate.module.local

import com.davils.kreate.InternalKreateApi

/**
 * The names of the local development tasks, where both artefacts can read them.
 *
 * The settings plugin names these tasks in the messages it prints and recognises a local publish by
 * its name, while `kreate-plugin` registers them and publishes the names as `KreateTasks.Local`,
 * which refers to these rather than repeating them.
 *
 * @since 3.4.0
 */
@InternalKreateApi
public object LocalTaskNames {
    /**
     * Installs a build into the local Maven repository and records it.
     *
     * @since 3.4.0
     */
    public const val PUBLISH: String = "kreateLocalPublish"

    /**
     * Runs [PUBLISH] across a declared workspace in dependency order.
     *
     * @since 3.4.0
     */
    public const val PUBLISH_ALL: String = "kreateLocalPublishAll"

    /**
     * Reports what is published locally, or why local mode is off.
     *
     * @since 3.4.0
     */
    public const val STATUS: String = "kreateLocalStatus"

    /**
     * Removes the local publications and the state that records them.
     *
     * @since 3.4.0
     */
    public const val CLEAN: String = "kreateLocalClean"
}
