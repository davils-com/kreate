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

import java.time.Duration

/**
 * The details of a test suite a project vendors, declared with `suite()` in
 * `assurance { standards { } }`.
 *
 * A suite comes either from an [archive] pinned by its [sha256] or from a [github] repository
 * pinned by a [commit]. The audit checks that the files under [vendored] are byte for byte files of
 * the pinned archive or commit, records a digest of them at every review and reports when either
 * changes, and watches the suite upstream for newer versions.
 *
 * @since 5.4.0
 */
public class AssuranceStandardSuiteSpec internal constructor() {
    /**
     * Why the project vendors the suite or which part of it, recorded with every review.
     *
     * @since 5.4.0
     */
    public var note: String? = null

    /**
     * How long a review of the suite stands before it is due again, overriding
     * `standards { reviewEvery }`.
     *
     * @since 5.4.0
     */
    public var reviewEvery: Duration? = null

    /**
     * The HTTPS address of the archive the suite is taken from, a `.tar.gz`, `.tar` or `.zip` file
     * such as `https://www.w3.org/XML/Test/xmlts20130923.tar.gz`. Needs [sha256].
     *
     * @since 5.4.0
     */
    public var archive: String? = null

    /**
     * The SHA-256 of [archive] as 64 hexadecimal digits.
     *
     * @since 5.4.0
     */
    public var sha256: String? = null

    /**
     * The GitHub repository the suite is taken from, such as `w3c/xsdtests`. Needs [commit].
     *
     * @since 5.4.0
     */
    public var github: String? = null

    /**
     * The commit of [github] the vendored files come from, as 40 hexadecimal digits.
     *
     * @since 5.4.0
     */
    public var commit: String? = null

    /**
     * The directory of the project that holds the vendored files, relative to the project
     * directory, such as `src/test/resources/xmlconf`. Required.
     *
     * @since 5.4.0
     */
    public var vendored: String? = null

    /**
     * The directory of the archive or the repository that [vendored] mirrors. Defaults to the one
     * directory every entry of an archive lies in, if there is one, and to the root of a repository.
     *
     * @since 5.4.0
     */
    public var root: String? = null

    /**
     * An HTTPS address of a page that announces new versions of the suite, such as
     * `https://www.w3.org/XML/Test/`, watched for changes.
     *
     * @since 5.4.0
     */
    public var watch: String? = null
}
