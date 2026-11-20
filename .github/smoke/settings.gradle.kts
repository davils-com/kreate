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

pluginManagement {
    val kreateRepository = providers.gradleProperty("kreateRepository").getOrElse("https://repo.davils.com/releases")
    val releasesRepository = providers.gradleProperty("kreateReleasesRepository")
        .getOrElse("https://repo.davils.com/releases")
    val kreateVersion = providers.gradleProperty("kreateVersion").get()
    val tokenName = providers.environmentVariable("DAVILS_MAVEN_NAME").orNull
    val tokenSecret = providers.environmentVariable("DAVILS_MAVEN_SECRET").orNull

    repositories {
        val davilsUrls = listOf(kreateRepository, releasesRepository).distinct()
        val davils = davilsUrls.map { url ->
            maven(url) {
                if (tokenName != null && tokenSecret != null) {
                    credentials {
                        username = tokenName
                        password = tokenSecret
                    }
                }
            }
        }
        exclusiveContent {
            forRepositories(*davils.toTypedArray())
            filter {
                includeGroupAndSubgroups("com.davils")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("com.davils.kreate") version kreateVersion
        id("org.jetbrains.kotlin.jvm") version "2.4.21"
    }
}

dependencyResolutionManagement {
    val kreateRepository = providers.gradleProperty("kreateRepository").getOrElse("https://repo.davils.com/releases")
    val releasesRepository = providers.gradleProperty("kreateReleasesRepository")
        .getOrElse("https://repo.davils.com/releases")
    val tokenName = providers.environmentVariable("DAVILS_MAVEN_NAME").orNull
    val tokenSecret = providers.environmentVariable("DAVILS_MAVEN_SECRET").orNull

    repositories {
        val davilsUrls = listOf(kreateRepository, releasesRepository).distinct()
        val davils = davilsUrls.map { url ->
            maven(url) {
                if (tokenName != null && tokenSecret != null) {
                    credentials {
                        username = tokenName
                        password = tokenSecret
                    }
                }
            }
        }
        exclusiveContent {
            forRepositories(*davils.toTypedArray())
            filter {
                includeGroupAndSubgroups("com.davils")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "kreate-smoke"
