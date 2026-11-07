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
package com.davils.kreate.provenance.marker

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.provenance.provenanceFinding
import com.davils.kreate.provenance.typosquat.requiredSharedLabels
import com.davils.kreate.provenance.typosquat.sharedLabels

internal class PluginMarkerChecker(private val allow: Set<String>) {
    fun findingsOf(markers: List<PluginMarker>): List<Finding> {
        val checked = markers.filterNot { marker -> marker.pluginId in allow }
        return checked.mapNotNull { marker -> findingOf(marker) }
    }

    private fun findingOf(marker: PluginMarker): Finding? {
        val required = requiredSharedLabels(marker.pluginId)
        val related = marker.implementations.any { module -> isRelated(marker.pluginId, module.group, required) }
        if (related) return null

        val targets = marker.implementations.joinToString { module -> module.module }.ifEmpty { "nothing" }
        val title = "The plugin ${marker.pluginId} is implemented by $targets"
        return provenanceFinding("PLUGIN_MARKER_MISMATCH", marker.marker, title, mapOf("pluginId" to marker.pluginId))
    }

    private fun isRelated(pluginId: String, group: String, required: Int): Boolean {
        if (group in allow) return true
        return sharedLabels(pluginId, group) >= required
    }
}
