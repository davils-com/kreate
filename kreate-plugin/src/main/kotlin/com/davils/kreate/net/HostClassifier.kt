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

package com.davils.kreate.net

import java.net.InetAddress
import java.net.UnknownHostException

private const val LOCALHOST: String = "localhost"

private const val OCTET: String = """(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)"""

private const val BYTE_MASK: Int = 0xFF

private const val CARRIER_GRADE_NAT_FIRST_OCTET: Int = 100

private const val CARRIER_GRADE_NAT_MASK: Int = 0xC0

private const val CARRIER_GRADE_NAT_PREFIX: Int = 0x40

private const val UNIQUE_LOCAL_MASK: Int = 0xFE

private const val UNIQUE_LOCAL_PREFIX: Int = 0xFC

private const val IPV4_LENGTH: Int = 4

private const val IPV6_LENGTH: Int = 16

private val CANONICAL_IPV4: Regex = Regex("$OCTET(\\.$OCTET){3}")

private val NUMERIC_LABEL: Regex = Regex("""\d+|0[xX][0-9a-fA-F]*""")

internal object HostClassifier {
    fun classify(host: String): HostKind {
        val bracketless = host.removeSurrounding("[", "]")
        val normalized = bracketless.lowercase().removeSuffix(".")

        if (normalized == LOCALHOST) return HostKind.LOOPBACK
        if (isAddressLiteral(normalized)) return classifyLiteral(normalized)

        val lastLabel = normalized.substringAfterLast('.')
        if (NUMERIC_LABEL.matches(lastLabel)) return HostKind.AMBIGUOUS
        return HostKind.PUBLIC
    }

    private fun isAddressLiteral(host: String): Boolean = ':' in host || CANONICAL_IPV4.matches(host)

    private fun classifyLiteral(literal: String): HostKind {
        val address = try {
            InetAddress.getByName(literal)
        } catch (expected: UnknownHostException) {
            return HostKind.AMBIGUOUS
        }

        if (address.isLoopbackAddress) return HostKind.LOOPBACK
        if (isPrivate(address)) return HostKind.PRIVATE
        return HostKind.PUBLIC
    }

    private fun isPrivate(address: InetAddress): Boolean {
        val isLocalRange = address.isAnyLocalAddress || address.isSiteLocalAddress || address.isLinkLocalAddress
        return isLocalRange || address.isMulticastAddress || isCarrierGradeNat(address) || isUniqueLocal(address)
    }

    private fun isCarrierGradeNat(address: InetAddress): Boolean {
        val bytes = address.address
        if (bytes.size != IPV4_LENGTH) return false

        val isInFirstOctet = unsigned(bytes[0]) == CARRIER_GRADE_NAT_FIRST_OCTET
        return isInFirstOctet && unsigned(bytes[1]) and CARRIER_GRADE_NAT_MASK == CARRIER_GRADE_NAT_PREFIX
    }

    private fun isUniqueLocal(address: InetAddress): Boolean {
        val bytes = address.address
        if (bytes.size != IPV6_LENGTH) return false
        return unsigned(bytes[0]) and UNIQUE_LOCAL_MASK == UNIQUE_LOCAL_PREFIX
    }

    private fun unsigned(byte: Byte): Int = byte.toInt() and BYTE_MASK
}
