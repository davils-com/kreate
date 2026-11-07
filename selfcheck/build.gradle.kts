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

import com.davils.kreate.assurance.ReportFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kreate)
}

dependencies {
    implementation("com.davils:kreate-plugin")
}

kreate {
    project {
        changelog {
            enabled = true
            runOnCheck = false
        }
    }

    assurance {
        enabled = true
        runOnCheck = false

        advisories {
            enabled = true
        }

        inspection {
            enabled = true
        }

        trust {
            enabled = true
        }

        confusion {
            enabled = true
        }

        aggregate {
            enabled = true
        }

        dependencyPolicy {
            enabled = true
            checkOutdated = true
        }

        reports {
            formats = ReportFormat.entries.toSet()
        }
    }
}
