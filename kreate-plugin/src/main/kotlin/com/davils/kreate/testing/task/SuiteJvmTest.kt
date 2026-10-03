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

package com.davils.kreate.testing.task

import org.gradle.api.tasks.CacheableTask
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import javax.inject.Inject

@CacheableTask
internal abstract class SuiteJvmTest @Inject constructor(jvmTargetName: String) : KotlinJvmTest() {
    init {
        targetName = jvmTargetName
    }
}
