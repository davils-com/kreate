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
 * The names of the tasks the Trivy feature registers.
 *
 * @since 4.0.0
 */
public object TrivyTaskNames {
    /**
     * Runs every enabled Trivy scan.
     *
     * @since 4.0.0
     */
    public const val SCAN: String = "kreateTrivyScan"

    /**
     * Scans the sources for secrets.
     *
     * @since 4.0.0
     */
    public const val SECRETS: String = "kreateTrivySecretScan"

    /**
     * Scans the dependencies for licenses.
     *
     * @since 4.0.0
     */
    public const val LICENSES: String = "kreateTrivyLicenseScan"

    /**
     * Scans the dependencies for vulnerabilities.
     *
     * @since 4.0.0
     */
    public const val VULNERABILITIES: String = "kreateTrivyVulnerabilityScan"
}
