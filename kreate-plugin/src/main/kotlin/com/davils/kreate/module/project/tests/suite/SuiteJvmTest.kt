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

package com.davils.kreate.module.project.tests.suite

import org.gradle.api.tasks.CacheableTask
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import javax.inject.Inject

/**
 * The test task of one suite on one JVM target of a multiplatform project.
 *
 * Exists only to set `targetName` in the constructor rather than in a configuration action.
 * The coverage engine finds multiplatform test tasks by walking the task's superclasses and
 * reading that property, and it does so from an action registered when its own plugin was
 * applied - which is to say before the action Kreate attaches when it registers the task. Set
 * the property the ordinary way and the coverage plugin reads `null` and fails the build.
 *
 * @param name The target name this task runs the suite for.
 * @since 3.0.0
 */
@CacheableTask
public abstract class SuiteJvmTest @Inject constructor(name: String) : KotlinJvmTest() {
    init {
        targetName = name
    }
}
