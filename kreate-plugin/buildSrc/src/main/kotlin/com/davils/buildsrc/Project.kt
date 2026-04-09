package com.davils.buildsrc

/**
 * Contains project-wide constants and configuration values.
 *
 * This object provides centralized access to project identity, organization details,
 * version control information, legal information, and issue management details.
 *
 * @since 1.0.0
 * @author Nils Jaekel
 */
public object Project {
    /**
     * Contains constants related to project identity.
     *
     * @since 1.0.0
     * @author Nils Jaekel
     */
    public object Identity {
        /**
         * The name of the project.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val NAME: String = "Kreate"

        /**
         * The description of the project.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val DESCRIPTION: String = "A helper plugin for setting up enterprise-grade Gradle Kotlin projects."

        /**
         * The group ID for the project.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val GROUP: String = "com.davils"

        /**
         * The year the project was started.
         *
         * @since 1.0.0
         * @author Nils Jaekel
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
         * @author Nils Jaekel
         */
        public const val NAME: String = "Davils"

        /**
         * The email address for the organization.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val EMAIL: String = "development@davils.com"

        /**
         * The website URL for the organization.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val WEBSITE_URL: String = "https://www.davils.com"

        /**
         * The timezone for the organization.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val TIMEZONE: String = "Europe/Berlin"
    }

    /**
     * Contains constants related to version control.
     *
     * @since 1.0.0
     */
    public object VersionControl {
        /**
         * The name of the CI system used.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val CI_SYSTEM: String = "Github Actions"

        /**
         * The URL for the CI system.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val CI_URL: String = "https://github.com/davils-com/kreate/actions"

        /**
         * The SCM connection URL.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val SCM_CONNECTION: String = "scm:git:https://github.com/davils-com/kreate.git"

        /**
         * The SCM developer connection URL.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val SCM_DEVELOPER_CONNECTION: String = "scm:git:ssh://git@github.com:davils-com/kreate.git"

        /**
         * The SCM URL.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val SCM_URL: String = "https://github.com/davils-com/kreate.git"
    }

    /**
     * Contains constants related to legal information.
     *
     * @since 1.0.0
     * @author Nils Jaekel
     */
    public object Legal {
        /**
         * The name of the license.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val LICENSE_NAME: String = "Apache 2.0"

        /**
         * The URL for the license.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val LICENSE_URL: String = "https://github.com/davils-com/kreate/blob/main/LICENSE"

        /**
         * The distribution type for the license.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val LICENSE_DISTRIBUTION: String = "repo"
    }

    /**
     * Contains constants related to issue management.
     *
     * @since 1.0.0
     * @author Nils Jaekel
     */
    public object IssueManagement {
        /**
         * The name of the issue management system.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val SYSTEM: String = "Gitlab Issues"

        /**
         * The URL for the issue management system.
         *
         * @since 1.0.0
         * @author Nils Jaekel
         */
        public const val URL: String = "https://github.com/davils-com/kreate/issues"
    }
}
