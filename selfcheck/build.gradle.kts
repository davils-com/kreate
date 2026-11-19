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
import java.time.Duration

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
        internalGroups = setOf("com.davils")

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

        standards {
            enabled = true
            reviewEvery = Duration.ofDays(730)

            github("semver/semver") {
                note = "The release guard, the API check and the changelog read Semantic Versioning 2.0.0."
            }
            github("olivierlacan/keep-a-changelog") {
                note = "The changelog tasks write and check Keep a Changelog 1.1.0."
            }
            github("ossf/osv-schema") {
                note = "The advisory audit reads OSV records."
            }
            github("oasis-tcs/sarif-spec") {
                note = "The SARIF report writes SARIF 2.1.0; a new committee draft is a reason to look."
                tags = "^sarif-"
            }
            document("FIRST CVSS 3.1") {
                note = "The severity of an advisory is computed from its CVSS 3 vector."
                reviewEvery = Duration.ofDays(365)
            }
            document("Conventional Commits 1.0.0") {
                note = "The changelog and the API check read conventional commits."
                reviewEvery = Duration.ofDays(365)
            }
        }
    }
}
