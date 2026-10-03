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

package com.davils.kreate.trivy

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
