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

package com.davils.kreate.module.project.trivy

/**
 * Represents the vulnerability score levels for Trivy scans.
 *
 * These levels correspond to the severity of the found vulnerabilities.
 *
 * @since 1.2.0
 */
public enum class Score {
    /**
     * Critical severity level. Requires immediate attention.
     * @since 1.2.0
     */
    CRITICAL,

    /**
     * High severity level. Should be addressed as soon as possible.
     * @since 1.2.0
     */
    HIGH,

    /**
     * Medium severity level. Important to address but not urgent.
     * @since 1.2.0
     */
    MEDIUM,

    /**
     * Low severity level. Minimal impact on security.
     * @since 1.2.0
     */
    LOW;
}
