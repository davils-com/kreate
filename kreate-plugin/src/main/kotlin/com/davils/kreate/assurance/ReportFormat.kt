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
 * A file format the assurance report is written in.
 *
 * @since 4.0.0
 */
public enum class ReportFormat {
    /**
     * A JSON document with a stable schema, for scripts.
     *
     * @since 4.0.0
     */
    JSON,

    /**
     * SARIF 2.1.0, for code scanning.
     *
     * @since 4.0.0
     */
    SARIF,

    /**
     * Plain text, for reading.
     *
     * @since 4.0.0
     */
    TEXT,

    /**
     * A self-contained HTML dashboard with a strict content security policy.
     *
     * @since 4.0.0
     */
    HTML,

    /**
     * Markdown, for a pull request comment or a job summary.
     *
     * @since 4.0.0
     */
    MARKDOWN,

    /**
     * JUnit XML, one test suite per source of findings.
     *
     * @since 4.0.0
     */
    JUNIT,

    /**
     * Checkstyle XML.
     *
     * @since 4.0.0
     */
    CHECKSTYLE,

    /**
     * The code quality report of GitLab.
     *
     * @since 4.0.0
     */
    GITLAB,

    /**
     * The generic issue import format of SonarQube.
     *
     * @since 4.0.0
     */
    SONARQUBE
}
