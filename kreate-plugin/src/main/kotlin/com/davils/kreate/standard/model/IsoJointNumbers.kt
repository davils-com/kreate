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
package com.davils.kreate.standard.model

private const val IEC: String = "iec"

private const val IEC_IEEE: String = "iec-ieee"

private val JOINT_BODIES: Map<String, String> = mapOf(
    "646" to IEC, "2022" to IEC, "5962" to IEC, "7064" to IEC, "7816" to IEC, "8824" to IEC,
    "8825" to IEC, "8859" to IEC, "9075" to IEC, "9594" to IEC, "9899" to IEC, "10646" to IEC,
    "10918" to IEC, "11172" to IEC, "13818" to IEC, "14443" to IEC, "14496" to IEC, "14882" to IEC,
    "15408" to IEC, "15444" to IEC, "16262" to IEC, "18004" to IEC, "19790" to IEC, "21320" to IEC,
    "21778" to IEC, "26300" to IEC, "27001" to IEC, "27002" to IEC, "29500" to IEC,
    "9945" to IEC_IEEE, "12207" to IEC_IEEE, "15288" to IEC_IEEE, "42010" to IEC_IEEE
)

internal object IsoJointNumbers {
    fun qualified(name: String): String {
        if (name.firstOrNull()?.isDigit() != true) return name
        val body = JOINT_BODIES[name.substringBefore('-')] ?: return name
        return "$body-$name"
    }
}
