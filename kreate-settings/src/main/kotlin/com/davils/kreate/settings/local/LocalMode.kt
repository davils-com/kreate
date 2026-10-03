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

package com.davils.kreate.settings.local

import com.davils.kreate.settings.InternalKreateApi
import java.io.Serializable

/**
 * Whether Kreate resolves locally published artifacts from the local Maven repository for this
 * build.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public sealed interface LocalMode : Serializable {
    /**
     * Local mode is on.
     *
     * @since 3.2.0
     */
    @InternalKreateApi
    public data class Active(
        /**
         * The libraries whose coordinates are substituted.
         * @since 3.2.0
         */
        val workspace: LocalWorkspace
    ) : LocalMode {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /**
     * Local mode is off.
     *
     * The reason is carried so that `kreateLocalStatus` can say why a published fix is not picked
     * up.
     *
     * @since 3.2.0
     */
    @InternalKreateApi
    public data class Inactive(
        /**
         * A sentence naming what turned local mode off.
         * @since 3.2.0
         */
        val reason: String
    ) : LocalMode {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}

/**
 * Whether this mode substitutes anything.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public val LocalMode.isActive: Boolean get() = this is LocalMode.Active

/**
 * The workspace being substituted, or an empty one when local mode is off.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public val LocalMode.workspace: LocalWorkspace
    get() = (this as? LocalMode.Active)?.workspace ?: LocalWorkspace.EMPTY
