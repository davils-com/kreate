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

package com.davils.kreate

/**
 * Marks a declaration that is public only so that Kreate's own artefacts can share it.
 *
 * `kreate-settings` and `kreate-plugin` are separate artefacts - the settings plugin must not carry
 * the Gradle plugins the project plugin depends on - and they share the local development core.
 * Kotlin's `internal` does not reach across that boundary, so the shared declarations are public and
 * carry this marker instead. They are hidden from the recorded binary interface, carry no
 * compatibility promise, and opting in is an error rather than a warning.
 *
 * @since 3.4.0
 */
@RequiresOptIn(
    message = "This is shared between Kreate's own artefacts and is not part of its API.",
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.TYPEALIAS
)
@MustBeDocumented
public annotation class InternalKreateApi
