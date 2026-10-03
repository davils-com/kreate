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

package com.davils.kreate.api.metadata

private const val HASH_MULTIPLIER = 31

internal data class KotlinMetadataValues(
    val kind: Int,
    val metadataVersion: IntArray,
    val data1: Array<String>,
    val data2: Array<String>,
    val extraString: String,
    val packageName: String,
    val extraInt: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KotlinMetadataValues) return false

        return kind == other.kind &&
            metadataVersion.contentEquals(other.metadataVersion) &&
            data1.contentEquals(other.data1) &&
            data2.contentEquals(other.data2) &&
            extraString == other.extraString &&
            packageName == other.packageName &&
            extraInt == other.extraInt
    }

    override fun hashCode(): Int {
        var result = kind
        result = HASH_MULTIPLIER * result + metadataVersion.contentHashCode()
        result = HASH_MULTIPLIER * result + data1.contentHashCode()
        result = HASH_MULTIPLIER * result + data2.contentHashCode()
        result = HASH_MULTIPLIER * result + extraString.hashCode()
        result = HASH_MULTIPLIER * result + packageName.hashCode()
        result = HASH_MULTIPLIER * result + extraInt
        return result
    }
}
