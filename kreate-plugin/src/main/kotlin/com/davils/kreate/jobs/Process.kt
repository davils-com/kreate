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

package com.davils.kreate.jobs

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Exec
import org.gradle.work.DisableCachingByDefault

/**
 * Represents a process that can be executed.
 *
 * This interface defines the contract for any executable logic within the plugin.
 *
 * @since 1.0.0
 */
internal interface Process {
    /**
     * Executes the logic associated with this process.
     *
     * @since 1.0.0
     */
    fun execute()
}

/**
 * A base class for Kreate-specific Gradle tasks.
 *
 * This class extends [DefaultTask] and implements [Process].
 * It automatically assigns the task to the "kreate" group by default,
 * but allows overriding through the [group] parameter.
 *
 * @param desc A description of what this task does.
 * @param group The Gradle task group this task belongs to. Defaults to "kreate".
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Base task class for Kreate tasks")
public abstract class Task(desc: String, group: String = "kreate") : DefaultTask(), Process {
    init {
        this.group = group
        description = desc
    }
}

/**
 * A base class for Kreate-specific executable Gradle tasks.
 *
 * This class extends [Exec] and implements [Process].
 * It automatically assigns the task to the "kreate" group by default,
 * but allows overriding through the [group] parameter.
 *
 * @param desc A description of what this executable task does.
 * @param group The Gradle task group this task belongs to. Defaults to "kreate".
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Base executable task class for Kreate tasks")
public abstract class Executable(desc: String, group: String = "kreate") : Exec(), Process {
    init {
        this.group = group
        description = desc
    }
}
