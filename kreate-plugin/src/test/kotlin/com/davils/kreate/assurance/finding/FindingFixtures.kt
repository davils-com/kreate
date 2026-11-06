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
package com.davils.kreate.assurance.finding

import com.davils.kreate.assurance.Severity

internal fun finding(
    rule: String = "RULE",
    source: FindingSource = FindingSource.INSPECTION,
    severity: Severity = Severity.HIGH,
    module: String? = "com.example:lib",
    path: String? = null,
    line: Int? = null
): Finding = Finding(
    ruleId = rule,
    source = source,
    severity = severity,
    title = "A finding",
    location = path?.let { SourceLocation(it, line) },
    module = module
)
