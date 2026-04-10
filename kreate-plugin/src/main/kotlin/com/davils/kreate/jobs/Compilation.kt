package com.davils.kreate.jobs

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

public fun Project.executeTaskBeforeCompile(task: Task) {
    require(tasks.contains(task)) { "Task ${task.name} is not registered in the project" }

    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        dependsOn(task)
    }
}
