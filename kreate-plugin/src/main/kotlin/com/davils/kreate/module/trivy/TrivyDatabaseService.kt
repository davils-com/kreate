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

package com.davils.kreate.module.trivy

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

internal const val TRIVY_DATABASE_SERVICE: String = "kreateTrivyDatabase"

internal abstract class TrivyDatabaseService : BuildService<BuildServiceParameters.None>

internal fun Project.trivyDatabaseService(): Provider<TrivyDatabaseService> =
    gradle.sharedServices.registerIfAbsent(TRIVY_DATABASE_SERVICE, TrivyDatabaseService::class.java) {
        maxParallelUsages.set(1)
    }
