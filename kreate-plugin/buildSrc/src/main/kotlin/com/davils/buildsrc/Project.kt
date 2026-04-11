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
    
    public object VersionControl {
        public const val CI_SYSTEM: String = "Github Actions"
        public const val CI_URL: String = "https://github.com/davils-com/kreate/actions"
        public const val SCM_CONNECTION: String = "scm:git:https://github.com/davils-com/kreate.git"
        public const val SCM_DEVELOPER_CONNECTION: String = "scm:git:ssh://git@github.com:davils-com/kreate.git"
        public const val SCM_URL: String = "https://github.com/davils-com/kreate.git"
    }

    public object Legal {
        public const val LICENSE_NAME: String = "Apache 2.0"
        public const val LICENSE_URL: String = "https://github.com/davils-com/kreate/blob/main/LICENSE"
        public const val LICENSE_DISTRIBUTION: String = "repo"
    }

    public object IssueManagement {
        public const val SYSTEM: String = "Github Issues"
        public const val URL: String = "https://github.com/davils-com/kreate/issues"
    }
}
