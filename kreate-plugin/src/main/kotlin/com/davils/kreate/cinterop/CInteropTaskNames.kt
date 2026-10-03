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

package com.davils.kreate.cinterop

/**
 * The names of the tasks the C interoperation feature registers.
 *
 * @since 4.0.0
 */
public object CInteropTaskNames {
    /**
     * Scaffolds the native project.
     *
     * @since 4.0.0
     */
    public const val INITIALIZE: String = "kreateCInteropInitialize"

    /**
     * Adds the Rust dependencies to the native project.
     *
     * @since 4.0.0
     */
    public const val DEPENDENCIES: String = "kreateCInteropDependencies"

    /**
     * Configures Cargo for the native project.
     *
     * @since 4.0.0
     */
    public const val CONFIGURE: String = "kreateCInteropConfigure"

    /**
     * Generates the Rust build script.
     *
     * @since 4.0.0
     */
    public const val SCRIPT: String = "kreateCInteropScript"

    /**
     * Compiles the native code.
     *
     * @since 4.0.0
     */
    public const val COMPILE: String = "kreateCInteropCompile"

    /**
     * Generates the cinterop definition files.
     *
     * @since 4.0.0
     */
    public const val DEFINITIONS: String = "kreateCInteropDefinitions"
}
