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
package com.davils.kreate.assurance.task

import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingDocumentJson
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Base task class of the sources of findings")
internal abstract class FindingCollectTask(taskDescription: String, taskGroup: KreateTaskGroup) :
    KreateTask(taskDescription, taskGroup) {
    @get:OutputFile
    public abstract val documentFile: RegularFileProperty

    protected fun writeDocument(document: FindingDocument) {
        val file = documentFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(FindingDocumentJson.render(document))
    }
}
