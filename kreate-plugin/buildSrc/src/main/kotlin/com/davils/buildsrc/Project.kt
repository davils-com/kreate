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

package com.davils.buildsrc

/**
 * Contains project-wide constants and configuration values.
 *
 * This object provides centralized access to project identity, organization details,
 * version control information, legal information, and issue management details.
 *
 * @since 1.0.0
 */
public object Project {
    /**
     * Contains constants related to project identity.
     *
     * @since 1.0.0
     */
    public object Identity {
        /**
         * The name of the project.
         *
         * @since 1.0.0
         */
        public const val NAME: String = "Kreate"

        /**
         * The description of the project.
         *
         * @since 1.0.0
         */
        public const val DESCRIPTION: String = "A helper plugin for setting up enterprise-grade Gradle Kotlin projects."

        /**
         * The group ID for the project.
         *
         * @since 1.0.0
         */
        public const val GROUP: String = "com.davils"

        /**
         * The year the project was started.
         *
         * @since 1.0.0
         */
        public const val INCEPTION_YEAR: Int = 2025
    }

    /**
     * Contains constants related to the organization.
     *
     * @since 1.0.0
     */
    public object Organization {
        /**
         * The name of the organization.
         *
         * @since 1.0.0
         */
        public const val NAME: String = "Davils"

        /**
         * The email address for the organization.
         *
         * @since 1.0.0
         */
        public const val EMAIL: String = "development@davils.com"

        /**
         * The website URL for the organization.
         *
         * @since 1.0.0
         */
        public const val WEBSITE_URL: String = "https://www.davils.com"

        /**
         * The timezone for the organization.
         *
         * @since 1.0.0
         */
        public const val TIMEZONE: String = "Europe/Berlin"
    }
    
    /**
     * Contains constants related to version control and continuous integration.
     *
     * @since 1.0.0
     */
    public object VersionControl {
        /**
         * The name of the CI system used for the project.
         *
         * @since 1.0.0
         */
        public const val CI_SYSTEM: String = "Github Actions"

        /**
         * The URL of the CI system.
         *
         * @since 1.0.0
         */
        public const val CI_URL: String = "https://github.com/davils-com/kreate/actions"

        /**
         * The connection URL for the source control management system.
         *
         * @since 1.0.0
         */
        public const val SCM_CONNECTION: String = "scm:git:https://github.com/davils-com/kreate.git"

        /**
         * The developer connection URL for the source control management system.
         *
         * @since 1.0.0
         */
        public const val SCM_DEVELOPER_CONNECTION: String = "scm:git:ssh://git@github.com:davils-com/kreate.git"

        /**
         * The public URL of the source control management system.
         *
         * @since 1.0.0
         */
        public const val SCM_URL: String = "https://github.com/davils-com/kreate.git"
    }

    /**
     * Contains constants related to legal information and licensing.
     *
     * @since 1.0.0
     * @author Nils Jaekel
     */
    public object Legal {
        /**
         * The name of the project's license.
         *
         * @since 1.0.0
         */
        public const val LICENSE_NAME: String = "Apache 2.0"

        /**
         * The URL to the license text.
         *
         * @since 1.0.0
         */
        public const val LICENSE_URL: String = "https://github.com/davils-com/kreate/blob/main/LICENSE"

        /**
         * The distribution mode of the license.
         *
         * @since 1.0.0
         */
        public const val LICENSE_DISTRIBUTION: String = "repo"
    }

    /**
     * Contains constants related to issue management and tracking.
     *
     * @since 1.0.0
     */
    public object IssueManagement {
        /**
         * The name of the issue management system.
         *
         * @since 1.0.0
         */
        public const val SYSTEM: String = "Github Issues"

        /**
         * The URL to the issue tracker.
         *
         * @since 1.0.0
         */
        public const val URL: String = "https://github.com/davils-com/kreate/issues"
    }
}
