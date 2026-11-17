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

package com.davils.kreate.publish

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration of the release guard, which refuses to publish a version that does not belong to the
 * build it comes from.
 *
 * Every publish task depends on `kreateReleaseGuard`. The guard fails the build when
 * - the resolved version differs from the version declared in the version property, so no
 *   environment variable can change what is published;
 * - a build without a release tag would publish anything but a `-SNAPSHOT`;
 * - a tag build would publish a version other than the one its tag names, or a version that is not a
 *   Semantic Versioning 2.0.0 release without build metadata;
 * - the tagged commit is not part of [releaseBranch] on [remote];
 * - a tag build runs with snapshot tooling: Kreate, the Kotlin Gradle plugin or Detekt.
 *
 * The release tag is read from `GITHUB_REF_TYPE` and `GITHUB_REF_NAME` on GitHub Actions and from
 * `CI_COMMIT_TAG` on GitLab CI. The guard needs Git and, on a tag build, the history of the release
 * branch.
 *
 * @param factory The object factory used to create the properties.
 * @since 5.0.0
 */
public abstract class ReleaseGuardExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether `kreateReleaseGuard` is registered and every publish task depends on it. Defaults to
     * `false`.
     *
     * @since 5.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The branch every release tag must be part of. Defaults to `main`.
     *
     * @since 5.0.0
     */
    public val releaseBranch: Property<String> = factory.property(String::class.java).convention("main")

    /**
     * The remote the release branch is read from. Defaults to `origin`.
     *
     * @since 5.0.0
     */
    public val remote: Property<String> = factory.property(String::class.java).convention("origin")

    /**
     * The prefix of release tags, such as `v`. Defaults to none.
     *
     * @since 5.0.0
     */
    public val tagPrefix: Property<String> = factory.property(String::class.java).convention("")
}
