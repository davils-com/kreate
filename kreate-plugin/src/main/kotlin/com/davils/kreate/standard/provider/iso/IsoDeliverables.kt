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
package com.davils.kreate.standard.provider.iso

import com.davils.kreate.standard.model.StandardSnapshot
import com.davils.kreate.standard.provider.JsonRecords
import com.davils.kreate.standard.provider.JsonRecords.child
import com.davils.kreate.standard.provider.JsonRecords.number
import com.davils.kreate.standard.provider.JsonRecords.numbers
import com.davils.kreate.standard.provider.JsonRecords.text

private const val FIRST_PUBLISHED_STAGE: Int = 6060

private const val LAST_VALID_STAGE: Int = 9099

private const val STAGE_DIVISOR: Int = 100

private const val YEAR_LENGTH: Int = 4

private const val YEAR_SEPARATOR: Char = ':'

private const val SUPPLEMENT_SEPARATOR: Char = '/'

internal object IsoDeliverables {
    fun markerOf(reference: String): String = "\"$reference$YEAR_SEPARATOR"

    fun recordOf(line: String): IsoRecord? {
        val json = JsonRecords.objectOf(line) ?: return null
        val reference = json.text("reference") ?: return null
        val stage = json.number("currentStage") ?: return null
        val titles = json.child("title")
        return IsoRecord(
            reference = reference,
            title = titles?.text("en") ?: reference,
            stage = stage.toInt(),
            publicationDate = json.text("publicationDate"),
            replacedBy = json.numbers("replacedBy")
        )
    }

    fun snapshotOf(reference: String, records: List<IsoRecord>): StandardSnapshot? {
        val editions = records.filter { record -> isEditionOf(record, reference) }
        if (editions.isEmpty()) return null
        val valid = editions.filter { record -> isValid(record) }
        val current = valid.ifEmpty { editions }.maxBy { record -> record.reference }
        val supplements = records.filter { record -> isSupplementOf(record, current) && isValid(record) }
        val withdrawn = valid.isEmpty()
        val replacements = current.replacedBy.map { id -> "ISO deliverable $id" }
        return StandardSnapshot(
            title = current.title,
            status = stageOf(current.stage),
            revision = current.reference,
            published = current.publicationDate,
            withdrawn = withdrawn,
            replacedBy = replacements.takeIf { withdrawn }.orEmpty(),
            updatedBy = supplements.map { record -> record.reference }.sorted()
        )
    }

    private fun isEditionOf(record: IsoRecord, reference: String): Boolean {
        val prefix = "$reference$YEAR_SEPARATOR"
        if (!record.reference.startsWith(prefix)) return false
        val year = record.reference.removePrefix(prefix)
        return year.length == YEAR_LENGTH && year.all(Char::isDigit)
    }

    private fun isSupplementOf(record: IsoRecord, edition: IsoRecord): Boolean =
        record.reference.startsWith("${edition.reference}$SUPPLEMENT_SEPARATOR")

    private fun isValid(record: IsoRecord): Boolean = record.stage in FIRST_PUBLISHED_STAGE..LAST_VALID_STAGE

    private fun stageOf(stage: Int): String {
        val major = stage / STAGE_DIVISOR
        val minor = stage % STAGE_DIVISOR
        return "$major.${minor.toString().padStart(2, '0')}"
    }
}
