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

package com.davils.buildlogic

public object Project {
    public object Identity {
        public const val NAME: String = "Kreate"

        public const val DESCRIPTION: String = "A helper plugin for setting up enterprise-grade Gradle Kotlin projects."

        public const val GROUP: String = "com.davils"

        public const val INCEPTION_YEAR: Int = 2025
    }

    public object Compatibility {
        public const val TARGET_JAVA_VERSION: Int = 17

        public const val MIN_GRADLE_VERSION: String = "9.0"
    }

    public object Quality {
        public const val MINIMUM_COVERAGE: Int = 80

        public const val KREATE_RULES_MODULE: String = "com.davils:kreate-detekt-rules"
    }

    public object Organization {
        public const val NAME: String = "Davils"

        public const val EMAIL: String = "development@davils.com"

        public const val WEBSITE_URL: String = "https://www.davils.com"

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
