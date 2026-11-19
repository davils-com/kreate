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
package com.davils.kreate.standard.task

import com.davils.kreate.assurance.finding.AssuranceInputException
import com.davils.kreate.standard.lock.ReviewException
import com.davils.kreate.standard.lock.ReviewPlan
import com.davils.kreate.standard.lock.SnapshotsJson
import com.davils.kreate.standard.lock.StandardsLock
import com.davils.kreate.standard.lock.StandardsLockJson
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option
import java.time.LocalDate

@UntrackedTask(because = "It rewrites a file that is under version control")
internal abstract class StandardsReview : KreateTask(
    "Records the review of standards in the lock file, with what their bodies publish today.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Internal
    @get:Option(option = "standard", description = "A standard to record as reviewed, such as rfc:9562.")
    public abstract val standards: ListProperty<String>

    @get:Internal
    @get:Option(option = "all", description = "Records every declared standard as reviewed.")
    public abstract val all: Property<Boolean>

    @get:Internal
    @get:Option(option = "note", description = "What the review found, recorded with every reviewed standard.")
    public abstract val note: Property<String>

    @get:Internal
    @get:Option(option = "prune", description = "Removes the reviews of standards that are no longer declared.")
    public abstract val prune: Property<Boolean>

    @get:Internal
    public abstract val declarations: ListProperty<StandardDeclaration>

    @get:Internal
    public abstract val snapshotsFile: RegularFileProperty

    @get:Internal
    public abstract val lockFile: RegularFileProperty

    @get:Internal
    public abstract val lockPath: Property<String>

    @get:Internal
    public abstract val today: Property<String>

    @TaskAction
    public fun review() {
        try {
            record()
        } catch (cause: ReviewException) {
            throw InvalidUserDataException(cause.message.orEmpty(), cause)
        } catch (cause: AssuranceInputException) {
            throw InvalidUserDataException(cause.message.orEmpty(), cause)
        }
    }

    private fun record() {
        val lock = readLock()
        val snapshots = SnapshotsJson.parse(snapshotsFile.get().asFile.readText(), "the snapshots of the audit")
        val plan = ReviewPlan(declarations.get(), lock, snapshots)
        val isPruning = prune.getOrElse(false)
        val chosen = plan.chosen(standards.getOrElse(emptyList()), all.getOrElse(false))
        if (chosen.isEmpty() && !isPruning) throw ReviewException(nothingChosen(plan))
        val text = note.orNull.orEmpty()
        if (chosen.isNotEmpty() && text.isBlank()) {
            throw ReviewException("Say what the review found with --note=\"...\".")
        }
        val reviewed = plan.reviewed(chosen, text, LocalDate.parse(today.get()))
        val pruned = plan.orphaned.takeIf { isPruning }.orEmpty()
        write(reviewed.without(pruned))
        logger.lifecycle("Recorded ${chosen.size} review(s) and removed ${pruned.size} record(s) in ${lockPath.get()}.")
    }

    private fun nothingChosen(plan: ReviewPlan): String {
        val pending = plan.pending
        if (pending.isEmpty()) return "Every declared standard is reviewed. Name one with --standard=<id> or use --all."
        val listed = pending.joinToString()
        return "Name the standards to record with --standard=<id> or use --all. Waiting for a review: $listed."
    }

    private fun readLock(): StandardsLock {
        val file = lockFile.get().asFile
        if (!file.isFile) return StandardsLock()
        return StandardsLockJson.parse(file.readText(), lockPath.get())
    }

    private fun write(lock: StandardsLock) {
        val file = lockFile.get().asFile
        file.parentFile?.mkdirs()
        file.writeText(StandardsLockJson.render(lock))
    }
}
