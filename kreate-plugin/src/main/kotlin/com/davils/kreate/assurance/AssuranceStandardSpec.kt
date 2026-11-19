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
 * The details of one standard a project declares in `assurance { standards { } }`.
 *
 * @since 5.3.0
 */
public class AssuranceStandardSpec internal constructor() {
    /**
     * Why the project implements the standard or which part of it, recorded with every review.
     *
     * @since 5.3.0
     */
    public var note: String? = null

    /**
     * How long a review of the standard stands before it is due again, overriding
     * `standards { reviewEvery }`. Required for `din()` and `document()` when no default is set,
     * because no service reports their changes.
     *
     * @since 5.3.0
     */
    public var reviewEvery: Duration? = null

    /**
     * A regular expression the tags of a `github()` repository have to match, such as `^1\.2\.`.
     * Without it, the latest release counts, or the latest tag of a repository without releases.
     *
     * @since 5.3.0
     */
    public var tags: String? = null

    /**
     * An HTTPS address of a `din()` or `document()` standard whose content is watched for changes.
     *
     * @since 5.3.0
     */
    public var url: String? = null
}
