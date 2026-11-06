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
package com.davils.kreate.assurance

/**
 * How serious a finding is, from [CRITICAL] down to [UNKNOWN].
 *
 * [UNKNOWN] means that the source gave no usable rating, not that the finding is harmless.
 *
 * @since 4.0.0
 */
public enum class Severity {
    /**
     * Exploitable or already compromised. Requires immediate attention.
     *
     * @since 4.0.0
     */
    CRITICAL,

    /**
     * A serious problem that should be addressed as soon as possible.
     *
     * @since 4.0.0
     */
    HIGH,

    /**
     * A real problem that is important to address, but not urgent.
     *
     * @since 4.0.0
     */
    MEDIUM,

    /**
     * A problem with minimal impact.
     *
     * @since 4.0.0
     */
    LOW,

    /**
     * The source gave no usable rating.
     *
     * @since 4.0.0
     */
    UNKNOWN
}
