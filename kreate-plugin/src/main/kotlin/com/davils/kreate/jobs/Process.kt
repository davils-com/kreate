package com.davils.kreate.jobs

import com.davils.kreate.Davils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Exec

internal interface Process {
    fun execute()
}

public abstract class Task(desc: String) : DefaultTask(), Process {
    init {
        group = Davils.Organization.NAME.lowercase()
        description = desc
    }
}

public abstract class Executable(desc: String) : Exec(), Process {
    init {
        group = Davils.Organization.NAME.lowercase()
        description = desc
    }
}
