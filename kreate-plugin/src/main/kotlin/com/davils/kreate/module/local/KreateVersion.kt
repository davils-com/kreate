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

import com.davils.kreate.Kreate

/**
 * The version reported when the plugin cannot name its own.
 *
 * @since 3.2.0
 */
internal const val UNKNOWN_KREATE_VERSION: String = "unknown"

/**
 * The version of Kreate that is running.
 *
 * Read from `Implementation-Version` in the plugin JAR's manifest, which the build writes. It is
 * recorded alongside every local publication so that a record written by an older Kreate can be
 * recognised as such rather than silently misread — the record's format is a contract between two
 * checkouts that upgrade independently.
 *
 * Returns [UNKNOWN_KREATE_VERSION] when the plugin is loaded from a directory of classes rather
 * than a JAR, which is the case in Kreate's own unit tests. That is a cosmetic gap in a field
 * nothing depends on, and not worth a generated source file to close.
 *
 * @since 3.2.0
 */
internal val KREATE_VERSION: String
    get() = Kreate::class.java.`package`?.implementationVersion ?: UNKNOWN_KREATE_VERSION
