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

package com.davils.kreate.platform

/**
 * An environment a JavaScript or Wasm target of a Kotlin Multiplatform project runs in.
 *
 * The environment decides which test tasks the Kotlin plugin creates and where executables run. A
 * library klib is the same for every environment, so a library that only declares [NODE] can still
 * be used from the browser.
 *
 * @since 5.1.0
 */
public enum class WebEnvironment {
    /**
     * The browser, tested through Karma and a headless browser that has to be installed.
     *
     * @since 5.1.0
     */
    BROWSER,

    /**
     * Node.js, which the Kotlin plugin downloads and tests with directly.
     *
     * @since 5.1.0
     */
    NODE
}
