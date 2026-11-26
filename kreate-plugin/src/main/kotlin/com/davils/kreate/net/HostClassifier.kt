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

import com.davils.konnect.internetprotocol.address.IpAddress
import com.davils.konnect.internetprotocol.address.Ipv6Address
import com.davils.konnect.internetprotocol.network.IpNetwork

private const val LOCALHOST: String = "localhost"

private val CANONICAL_IPV4_SHAPE: Regex = Regex("""\d{1,3}(\.\d{1,3}){3}""")

private val SITE_LOCAL: IpNetwork = IpNetwork.parse("fec0::/10")

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

    private fun isAddressLiteral(host: String): Boolean = ':' in host || CANONICAL_IPV4_SHAPE.matches(host)

    private fun classifyLiteral(literal: String): HostKind {
        val parsed = IpAddress.parseOrNull(literal) ?: return HostKind.AMBIGUOUS
        val address = (parsed as? Ipv6Address)?.toIpv4OrNull() ?: parsed
        if (address.isLoopback) return HostKind.LOOPBACK
        val isPublic = address.isPublic && address !in SITE_LOCAL
        if (isPublic) return HostKind.PUBLIC
        return HostKind.PRIVATE
    }
}
