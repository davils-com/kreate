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

package com.davils.kreate.coverage

/**
 * The entity a verification rule is evaluated against.
 *
 * The choice decides what a failure means. With [APPLICATION] the bound is checked once against
 * the whole codebase, so a well-tested module can hide an untested one. With [CLASS] the bound is
 * checked separately for every class, and a single neglected class fails the build — far stricter,
 * and usually only realistic on a codebase that started out with the rule in place.
 *
 * @since 2.2.0
 */
public enum class Grouping {
    /**
     * One measurement for the entire application. The default.
     * @since 2.2.0
     */
    APPLICATION,

    /**
     * A separate measurement per class. Every class must satisfy the bound on its own.
     * @since 2.2.0
     */
    CLASS,

    /**
     * A separate measurement per package.
     * @since 2.2.0
     */
    PACKAGE
}
