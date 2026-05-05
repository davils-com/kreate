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
 * Represents the severity levels for license scanning.
 *
 * @since 1.2.0
 */
public enum class LicenseSeverity {
    /**
     * Critical license issue. High risk of non-compliance.
     * @since 1.2.0
     */
    CRITICAL,

    /**
     * High severity license issue. Significant compliance risk.
     * @since 1.2.0
     */
    HIGH,

    /**
     * Medium severity license issue. Moderate risk.
     * @since 1.2.0
     */
    MEDIUM,

    /**
     * Low severity license issue. Minor risk.
     * @since 1.2.0
     */
    LOW,

    /**
     * Unknown license severity. The license could not be clearly categorized.
     * @since 1.2.0
     */
    UNKNOWN
}

/**
 * Represents the severity levels for secret scanning.
 *
 * @since 1.2.0
 */
public enum class SecretSeverity {
    /**
     * Critical secret found. Requires immediate revocation.
     * @since 1.2.0
     */
    CRITICAL,

    /**
     * High severity secret found. Should be revoked and rotated.
     * @since 1.2.0
     */
    HIGH,

    /**
     * Medium severity secret found. Potential security risk.
     * @since 1.2.0
     */
    MEDIUM,

    /**
     * Low severity secret found. Minimal security impact.
     * @since 1.2.0
     */
    LOW
}
